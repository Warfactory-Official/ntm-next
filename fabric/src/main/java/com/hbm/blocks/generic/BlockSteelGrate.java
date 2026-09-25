// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockSteelGrate extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final SoundType SOUND =
            new SoundType(
                    0.5F,
                    1.0F,
                    SoundEvents.STONE_BREAK,
                    SoundEvents.METAL_STEP,
                    SoundEvents.STONE_PLACE,
                    SoundEvents.STONE_HIT,
                    SoundEvents.STONE_FALL);

    public static final MapCodec<BlockSteelGrate> CODEC =
            simpleCodec(props -> new BlockSteelGrate(props, false));
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 9);
    private static final VoxelShape[] SHAPES = buildShapes();
    private final boolean wide;

    public BlockSteelGrate(Properties props, boolean wide) {
        super(props);
        this.wide = wide;
        registerDefaultState(
                stateDefinition.any().setValue(HEIGHT, 0).setValue(WATERLOGGED, false));
    }

    private static VoxelShape shapeFor(int height) {
        double fy = height == 9 ? -2 / 16D : height * 2 / 16D;
        return Shapes.box(0D, fy, 0D, 1D, fy + 2 / 16D, 1D);
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[10];
        for (int h = 0; h < 10; h++) shapes[h] = shapeFor(h);
        return shapes;
    }

    private static boolean hasGapAgainst(
            Level level, BlockPos neighborPos, boolean neighborBelowUs) {
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.isAir()) return false;
        VoxelShape shape = neighbor.getCollisionShape(level, neighborPos);
        if (shape.isEmpty()) return true;
        return neighborBelowUs
                ? shape.min(Direction.Axis.Y) > 0.05D
                : shape.max(Direction.Axis.Y) < 0.95D;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(HEIGHT)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(HEIGHT)];
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        int height;
        if (face == Direction.DOWN) {
            height = 7;
        } else if (face == Direction.UP) {
            height = 0;
        } else {
            double hitY = ctx.getClickLocation().y;
            double frac = hitY - Mth.floor(hitY);
            height = Mth.floor(frac * 8D);
        }
        return defaultBlockState()
                .setValue(HEIGHT, height)
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer == null || !placer.isShiftKeyDown() || level.isClientSide()) return;

        int height = state.getValue(HEIGHT);
        if (height == 0 && hasGapAgainst(level, pos.below(), false)) {
            level.setBlock(pos, state.setValue(HEIGHT, 9), 3);
        } else if (height == 7 && hasGapAgainst(level, pos.above(), true)) {
            level.setBlock(pos, state.setValue(HEIGHT, 8), 3);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.isClientSide()) return;

        int height = state.getValue(HEIGHT);
        boolean breakIt;
        if (height == 9) {
            breakIt = !hasGapAgainst(level, pos.below(), false);
        } else if (height == 8) {
            breakIt = !hasGapAgainst(level, pos.above(), true);
        } else {
            return;
        }
        if (breakIt) level.destroyBlock(pos, true);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (!wide || !(entity instanceof ItemEntity || entity instanceof ExperienceOrb)) return;
        int height = state.getValue(HEIGHT);
        if (entity.getY() < pos.getY() + height * 0.125D + 0.375D) {
            entity.setDeltaMovement(0D, -0.25D, 0D);
            entity.setPos(entity.getX(), entity.getY() - 0.125D, entity.getZ());
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(
                state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
