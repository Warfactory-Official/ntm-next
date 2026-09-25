// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDirectionalBase;
import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockSpotlight extends BlockDirectionalBase
        implements ISpotlight, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final int TURN_OFF_DELAY = 4;

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final int MAX_BEAM_LENGTH = 32;
    private final int beamLength;
    private final LightType type;
    private final VoxelShape[] shapesByDirection = new VoxelShape[6];

    private final MapCodec<BlockSpotlight> ownCodec;

    public BlockSpotlight(int beamLength, LightType type, Properties props) {
        super(props);
        this.beamLength = beamLength;
        this.type = type;
        this.ownCodec = simpleCodec(p -> new BlockSpotlight(beamLength, type, p));
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.DOWN)
                        .setValue(BROKEN, false)
                        .setValue(LIT, true)
                        .setValue(WATERLOGGED, false));
        for (Direction dir : Direction.VALUES) {
            shapesByDirection[dir.ordinal()] = buildShape(dir);
        }
    }

    public static void propagateBeam(ServerLevel level, BlockPos pos, Direction dir, int distance) {
        BlockPos cursor = pos;
        while (true) {
            if (--distance <= 0) return;
            cursor = cursor.relative(dir);
            BlockState state = level.getBlockState(cursor);
            if (isBeam(state)) continue;
            if (!state.isAir()) return;
            level.setBlock(
                    cursor, ModBlocks.SPOTLIGHT_BEAM.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void unpropagateBeam(
            ServerLevel level, BlockPos pos, Direction dir, int distance) {
        BlockPos cursor = pos;
        while (true) {
            if (--distance <= 0) return;
            cursor = cursor.relative(dir);
            if (!isBeam(level.getBlockState(cursor))) return;
            if (!stillLitByAnySpotlight(level, cursor)) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static boolean stillLitByAnySpotlight(ServerLevel level, BlockPos cell) {
        return !sourceDirections(level, cell).isEmpty();
    }

    public static List<Direction> sourceDirections(ServerLevel level, BlockPos cell) {
        List<Direction> dirs = new ArrayList<>(1);
        for (Direction dir : Direction.VALUES) {
            BlockPos cursor = cell;
            for (int step = 1; step < MAX_BEAM_LENGTH; step++) {
                cursor = cursor.relative(dir.getOpposite());
                BlockState state = level.getBlockState(cursor);
                if (state.getBlock() instanceof ISpotlight source) {
                    if (source.lights(state, dir) && step < source.getBeamLength()) dirs.add(dir);
                    break;
                }
                if (!state.isAir() && !isBeam(state)) break;
            }
        }
        return dirs;
    }

    public static void repropagateThrough(ServerLevel level, BlockPos cell) {
        for (Direction dir : sourceDirections(level, cell)) {
            BlockPos cursor = cell;
            for (int step = 1; step < MAX_BEAM_LENGTH; step++) {
                cursor = cursor.relative(dir.getOpposite());
                BlockState state = level.getBlockState(cursor);
                if (state.getBlock() instanceof ISpotlight source) {
                    propagateBeam(level, cursor, dir, source.getBeamLength());
                    break;
                }
            }
        }
    }

    static boolean isBeam(BlockState state) {
        return state.is(ModBlocks.SPOTLIGHT_BEAM.get());
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return ownCodec;
    }

    public LightType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BROKEN, LIT, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(FACING, ctx.getClickedFace())
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !state.is(oldState.getBlock())) {
            if (!updatePower(state, server, pos)) updateBeam(state, server, pos);
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
        if (!(level instanceof ServerLevel server)) return;
        if (block == Blocks.LIGHT || block == Blocks.AIR) return;
        if (!state.canSurvive(server, pos)) {
            dropResources(state, server, pos);
            server.removeBlock(pos, false);
            return;
        }
        if (!updatePower(state, server, pos)) updateBeam(state, server, pos);
    }

    private boolean updatePower(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(BROKEN)) return false;
        boolean powered = level.hasNeighborSignal(pos);
        boolean lit = state.getValue(LIT);
        if (lit && powered) {
            level.scheduleTick(pos, this, TURN_OFF_DELAY);
            return true;
        } else if (!lit && !powered) {
            setLit(level, pos, state, true);
            return true;
        }
        return false;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && level.hasNeighborSignal(pos)) {
            setLit(level, pos, state, false);
        }
    }

    private void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        Direction facing = state.getValue(FACING);
        if (lit) propagateBeam(level, pos, facing, beamLength);
        else unpropagateBeam(level, pos, facing, beamLength);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(LIT)) unpropagateBeam(level, pos, state.getValue(FACING), beamLength);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(BROKEN)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) repair(server, pos);
        return InteractionResult.SUCCESS;
    }

    private void repair(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || !state.getValue(BROKEN)) return;

        level.setBlock(pos, state.setValue(BROKEN, false).setValue(LIT, true), Block.UPDATE_ALL);
        propagateBeam(level, pos, state.getValue(FACING), beamLength);
        for (Direction dir : Direction.VALUES) repair(level, pos.relative(dir));
    }

    private void updateBeam(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(LIT)) propagateBeam(level, pos, state.getValue(FACING), beamLength);
    }

    private VoxelShape buildShape(Direction dir) {
        float[] b =
                switch (dir.getAxis()) {
                    case X -> new float[] {type.bounds[2], type.bounds[1], type.bounds[0]};
                    case Y -> new float[] {type.bounds[1], type.bounds[2], type.bounds[0]};
                    case Z -> type.bounds;
                };
        float offX = 0.5F - dir.getStepX() * (0.5F - b[0]);
        float offY = 0.5F - dir.getStepY() * (0.5F - b[1]);
        float offZ = 0.5F - dir.getStepZ() * (0.5F - b[2]);
        return Shapes.box(
                offX - b[0], offY - b[1], offZ - b[2], offX + b[0], offY + b[1], offZ + b[2]);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapesByDirection[state.getValue(FACING).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public int getBeamLength() {
        return beamLength;
    }

    @Override
    public boolean lights(BlockState state, Direction dir) {
        return state.getValue(FACING) == dir && state.getValue(LIT) && !state.getValue(BROKEN);
    }

    public enum LightType {
        INCANDESCENT(new float[] {0.25F, 0.2F, 0.15F}),
        FLUORESCENT(new float[] {0.5F, 0.5F, 0.1F}),
        HALOGEN(new float[] {0.35F, 0.25F, 0.2F});

        final float[] bounds;

        LightType(float[] bounds) {
            this.bounds = bounds;
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
