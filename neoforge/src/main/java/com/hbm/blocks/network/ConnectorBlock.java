// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.tileentity.network.BlockEntityConnector;
import com.hbm.tileentity.network.BlockEntityConnectorSuper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ConnectorBlock extends PylonBlockBase implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final boolean superConnector;
    private final VoxelShape[] shapes;
    private final MapCodec<ConnectorBlock> ownCodec;

    public ConnectorBlock(boolean superConnector, Properties props) {
        super(props);
        this.superConnector = superConnector;
        this.shapes = buildShapes(superConnector);
        this.ownCodec = simpleCodec(p -> new ConnectorBlock(superConnector, p));
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return ownCodec;
    }

    private static VoxelShape[] buildShapes(boolean superConnector) {
        VoxelShape[] shapes = new VoxelShape[Direction.VALUES.length];
        double min = 5 / 16D, max = 11 / 16D;
        for (Direction facing : Direction.VALUES) {
            Direction wall = facing.getOpposite();
            boolean x = superConnector && wall.getAxis() == Direction.Axis.X;
            boolean y = superConnector && wall.getAxis() == Direction.Axis.Y;
            boolean z = superConnector && wall.getAxis() == Direction.Axis.Z;
            shapes[facing.ordinal()] =
                    Shapes.box(
                            x || wall == Direction.WEST ? 0 : min,
                            y || wall == Direction.DOWN ? 0 : min,
                            z || wall == Direction.NORTH ? 0 : min,
                            x || wall == Direction.EAST ? 1 : max,
                            y || wall == Direction.UP ? 1 : max,
                            z || wall == Direction.SOUTH ? 1 : max);
        }
        return shapes;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes[state.getValue(FACING).ordinal()];
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected int nodeMask(BlockState state) {
        return 1 << state.getValue(FACING).getOpposite().ordinal();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return superConnector
                ? new BlockEntityConnectorSuper(pos, state)
                : new BlockEntityConnector(pos, state);
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
