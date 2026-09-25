// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockWoodBarrier extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty FRONT = BooleanProperty.create("front");
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    public static final MapCodec<BlockWoodBarrier> CODEC = simpleCodec(BlockWoodBarrier::new);

    private static final double OFF = 0.125;
    private static final VoxelShape[] PANEL = new VoxelShape[4];
    private static final VoxelShape[] COLLISION = new VoxelShape[16];

    static {
        PANEL[Direction.WEST.get2DDataValue()] = Shapes.box(0, 0, 0, OFF, 1, 1);
        PANEL[Direction.NORTH.get2DDataValue()] = Shapes.box(0, 0, 0, 1, 1, OFF);
        PANEL[Direction.EAST.get2DDataValue()] = Shapes.box(1 - OFF, 0, 0, 1, 1, 1);
        PANEL[Direction.SOUTH.get2DDataValue()] = Shapes.box(0, 0, 1 - OFF, 1, 1, 1);
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int side = 0; side < 4; side++)
                if ((mask & (1 << side)) != 0) shape = Shapes.or(shape, PANEL[side]);
            COLLISION[mask] = shape;
        }
    }

    public BlockWoodBarrier(Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(FRONT, false)
                        .setValue(LEFT, false)
                        .setValue(RIGHT, false)
                        .setValue(WATERLOGGED, false));
    }

    private static @Nullable BooleanProperty propertyFor(Direction facing, Direction side) {
        if (side == facing) return FRONT;
        if (side == facing.getCounterClockWise()) return LEFT;
        if (side == facing.getClockWise()) return RIGHT;
        return null;
    }

    public static boolean panel(BlockState state, Direction side) {
        Direction facing = state.getValue(FACING);
        if (side == facing.getOpposite()) return true;
        BooleanProperty property = side.getAxis().isHorizontal() ? propertyFor(facing, side) : null;
        return property != null && state.getValue(property);
    }

    private static BlockState withNeighbours(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BooleanProperty property = propertyFor(facing, side);
            if (property != null)
                state =
                        state.setValue(
                                property, level.getBlockState(pos.relative(side)).isSolidRender());
        }
        return state;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FRONT, LEFT, RIGHT, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {

        Direction clicked = ctx.getClickedFace();
        Direction facing =
                clicked.getAxis().isHorizontal()
                        ? clicked
                        : ctx.getHorizontalDirection().getOpposite();
        BlockState state =
                defaultBlockState()
                        .setValue(FACING, facing)
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        return withNeighbours(state, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE
                ? mirrored
                : mirrored.setValue(LEFT, state.getValue(RIGHT))
                        .setValue(RIGHT, state.getValue(LEFT));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PANEL[state.getValue(FACING).getOpposite().get2DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction side : Direction.Plane.HORIZONTAL)
            if (panel(state, side)) mask |= 1 << side.get2DDataValue();
        return COLLISION[mask];
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
        BooleanProperty property =
                direction.getAxis().isHorizontal()
                        ? propertyFor(state.getValue(FACING), direction)
                        : null;
        return property == null ? state : state.setValue(property, neighbourState.isSolidRender());
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
