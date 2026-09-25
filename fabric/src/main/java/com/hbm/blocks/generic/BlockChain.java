// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockChain extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final MapCodec<BlockChain> CODEC = simpleCodec(BlockChain::new);

    public static final BooleanProperty END = BooleanProperty.create("end");
    public static final EnumProperty<Direction> FACING =
            EnumProperty.create("facing", Direction.class, direction -> direction != Direction.UP);

    private static final VoxelShape VERTICAL = Shapes.box(0.375, 0, 0.375, 0.625, 1, 0.625);
    private static final VoxelShape WALL_NORTH = Shapes.box(0.375, 0, 0.875, 0.625, 1, 1.0);
    private static final VoxelShape WALL_SOUTH = Shapes.box(0.375, 0, 0, 0.625, 1, 0.125);
    private static final VoxelShape WALL_WEST = Shapes.box(0.875, 0, 0.375, 1.0, 1, 0.625);
    private static final VoxelShape WALL_EAST = Shapes.box(0, 0, 0.375, 0.125, 1, 0.625);
    private static final VoxelShape END_VERTICAL = Shapes.box(0.375, 0.25, 0.375, 0.625, 1, 0.625);
    private static final VoxelShape END_NORTH = Shapes.box(0.375, 0.25, 0.875, 0.625, 1, 1.0);
    private static final VoxelShape END_SOUTH = Shapes.box(0.375, 0.25, 0, 0.625, 1, 0.125);
    private static final VoxelShape END_WEST = Shapes.box(0.875, 0.25, 0.375, 1.0, 1, 0.625);
    private static final VoxelShape END_EAST = Shapes.box(0, 0.25, 0.375, 0.125, 1, 0.625);

    public BlockChain(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.DOWN)
                        .setValue(END, true)
                        .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, END, WATERLOGGED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean end = state.getValue(END);
        return switch (state.getValue(FACING)) {
            case DOWN -> end ? END_VERTICAL : VERTICAL;
            case NORTH -> end ? END_NORTH : WALL_NORTH;
            case SOUTH -> end ? END_SOUTH : WALL_SOUTH;
            case WEST -> end ? END_WEST : WALL_WEST;
            case EAST -> end ? END_EAST : WALL_EAST;
            case UP -> throw new IllegalStateException();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = anchoring(ctx);
        return state == null
                ? null
                : withEnd(state, ctx.getLevel(), ctx.getClickedPos())
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }

    private @Nullable BlockState anchoring(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (face.getAxis().isHorizontal()
                && level.getBlockState(pos.relative(face.getOpposite()))
                        .isFaceSturdy(level, pos.relative(face.getOpposite()), face)) {
            return defaultBlockState().setValue(FACING, face);
        }
        BlockPos above = pos.above();
        BlockState support = level.getBlockState(above);
        if (support.is(this)) {
            return defaultBlockState().setValue(FACING, support.getValue(FACING));
        }
        if (support.isFaceSturdy(level, above, Direction.DOWN)) return defaultBlockState();

        for (int side = 5; side >= 2; side--) {
            Direction facing = Direction.from3DDataValue(side);
            BlockPos neighbor = pos.relative(facing.getOpposite());
            if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, facing)) {
                return defaultBlockState().setValue(FACING, facing);
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState top = level.getBlockState(above);
        if (top.is(this) && top.getValue(FACING) == state.getValue(FACING)) return true;
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    private BlockState withEnd(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState bottom = level.getBlockState(below);
        boolean continues =
                bottom.isFaceSturdy(level, below, Direction.UP)
                        || bottom.is(this) && bottom.getValue(FACING) == state.getValue(FACING);
        return state.setValue(END, !continues);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return direction == Direction.DOWN ? withEnd(state, level, pos) : state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
