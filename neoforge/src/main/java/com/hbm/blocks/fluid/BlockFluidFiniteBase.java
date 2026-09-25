// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockFluidFiniteBase extends Block {

    private static final Direction[] HORIZONTALS = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    protected final int densityDir = -1;
    protected IntegerProperty levelProperty;
    private final VoxelShape[] fluidShapes;

    protected BlockFluidFiniteBase(Properties props) {
        super(props);
        registerDefaultState(
                getStateDefinition().any().setValue(levelProperty, quantaPerBlock() - 1));
        fluidShapes = new VoxelShape[quantaPerBlock()];
        for (int i = 0; i < fluidShapes.length; i++) {
            fluidShapes[i] = Shapes.box(0, 0, 0, 1, (i + 1.0) / fluidShapes.length, 1);
        }
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    public VoxelShape fluidPickShape(BlockState state, boolean sourceOnly) {
        int amount = state.getValue(levelProperty);

        return sourceOnly && amount != fluidShapes.length - 1
                ? Shapes.empty()
                : fluidShapes[amount];
    }

    protected int quantaPerBlock() {
        return 5;
    }

    protected int density() {
        return 3000;
    }

    protected static boolean isLiquid(BlockState state) {
        return state.getBlock() instanceof BlockFluidFiniteBase || !state.getFluidState().isEmpty();
    }

    public abstract int flowDelay();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        this.levelProperty = IntegerProperty.create("level", 0, quantaPerBlock() - 1);
        builder.add(levelProperty);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        level.scheduleTick(pos, this, flowDelay());
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        level.scheduleTick(pos, this, flowDelay());
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateTick(state, level, pos, random);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateTick(state, level, pos, random);
    }

    protected void updateTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        boolean changed = false;
        int quantaRemaining = state.getValue(levelProperty) + 1;

        int prevRemaining = quantaRemaining;
        quantaRemaining = tryToFlowVerticallyInto(level, pos, quantaRemaining);

        if (quantaRemaining < 1) {
            return;
        } else if (quantaRemaining != prevRemaining) {
            changed = true;
            if (quantaRemaining == 1) {
                level.setBlock(
                        pos,
                        state.setValue(levelProperty, quantaRemaining - 1),
                        Block.UPDATE_CLIENTS);
                return;
            }
        } else if (quantaRemaining == 1) {
            return;
        }

        int lowerThan = quantaRemaining - 1;
        int total = quantaRemaining;
        int count = 1;

        for (Direction side : HORIZONTALS) {
            BlockPos off = pos.relative(side);
            if (displaceIfPossible(level, off))
                level.setBlock(off, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            int quanta = getQuantaValueBelow(level, off, lowerThan);
            if (quanta >= 0) {
                count++;
                total += quanta;
            }
        }

        if (count == 1) {
            if (changed) {
                level.setBlock(
                        pos,
                        state.setValue(levelProperty, quantaRemaining - 1),
                        Block.UPDATE_CLIENTS);
            }
            return;
        }

        int each = total / count;
        int rem = total % count;

        for (Direction side : HORIZONTALS) {
            BlockPos off = pos.relative(side);
            int quanta = getQuantaValueBelow(level, off, lowerThan);
            if (quanta >= 0) {
                int newQuanta = each;
                if (rem == count || rem > 1 && rand.nextInt(count - rem) != 0) {
                    ++newQuanta;
                    --rem;
                }

                if (newQuanta != quanta) {
                    if (newQuanta == 0) {
                        level.setBlock(off, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        level.setBlock(
                                off,
                                defaultBlockState().setValue(levelProperty, newQuanta - 1),
                                Block.UPDATE_CLIENTS);
                    }
                    level.scheduleTick(off, this, flowDelay());
                }
                --count;
            }
        }

        if (rem > 0) ++each;
        level.setBlock(pos, state.setValue(levelProperty, each - 1), Block.UPDATE_CLIENTS);
    }

    protected int tryToFlowVerticallyInto(Level level, BlockPos pos, int amtToInput) {
        BlockState myState = level.getBlockState(pos);
        BlockPos other = pos.offset(0, densityDir, 0);
        if (level.isOutsideBuildHeight(other)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return 0;
        }

        int quanta = quantaPerBlock();
        int amt = getQuantaValueBelow(level, other, quanta);
        if (amt >= 0) {
            amt += amtToInput;
            if (amt > quanta) {
                level.setBlock(
                        other, myState.setValue(levelProperty, quanta - 1), Block.UPDATE_CLIENTS);
                level.scheduleTick(other, this, flowDelay());
                return amt - quanta;
            } else if (amt > 0) {
                level.setBlock(
                        other, myState.setValue(levelProperty, amt - 1), Block.UPDATE_CLIENTS);
                level.scheduleTick(other, this, flowDelay());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return 0;
            }
            return amtToInput;
        }

        int densityOther = getDensityAt(level, other);
        if (densityOther == Integer.MAX_VALUE) {
            if (displaceIfPossible(level, other)) {
                level.setBlock(
                        other,
                        myState.setValue(levelProperty, amtToInput - 1),
                        Block.UPDATE_CLIENTS);
                level.scheduleTick(other, this, flowDelay());
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return 0;
            }
            return amtToInput;
        }

        if (densityOther < density()) {
            BlockState swap = level.getBlockState(other);
            level.setBlock(
                    other, myState.setValue(levelProperty, amtToInput - 1), Block.UPDATE_CLIENTS);
            level.setBlock(pos, swap, Block.UPDATE_CLIENTS);
            level.scheduleTick(other, this, flowDelay());
            return 0;
        }
        return amtToInput;
    }

    public int getQuantaValue(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return 0;
        if (state.getBlock() != this) return -1;
        return state.getValue(levelProperty) + 1;
    }

    public int getQuantaValueBelow(Level level, BlockPos pos, int belowThis) {
        int q = getQuantaValue(level, pos);
        return q >= belowThis ? -1 : q;
    }

    protected int getDensityAt(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof BlockFluidFiniteBase fluid ? fluid.density() : Integer.MAX_VALUE;
    }

    protected boolean canDisplace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.getBlock() == this) return false;
        return !state.blocksMotion();
    }

    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        return canDisplace(level, pos);
    }
}
