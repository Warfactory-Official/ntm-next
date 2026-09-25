// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.lib.Library;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.AutoRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@AutoRotate
public class CableBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final int OPEN_ALL = 0x3F;

    private static final VoxelShape[] SHAPES = buildShapes();

    public CableBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(WATERLOGGED, false));
    }

    private static VoxelShape[] buildShapes() {

        VoxelShape core = Shapes.box(5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D);
        VoxelShape[] arm = new VoxelShape[6];
        arm[Direction.DOWN.ordinal()] =
                Shapes.box(5 / 16D, 0, 5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D);
        arm[Direction.UP.ordinal()] = Shapes.box(5 / 16D, 11 / 16D, 5 / 16D, 11 / 16D, 1, 11 / 16D);
        arm[Direction.NORTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 0, 11 / 16D, 11 / 16D, 5 / 16D);
        arm[Direction.SOUTH.ordinal()] =
                Shapes.box(5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D, 11 / 16D, 1);
        arm[Direction.WEST.ordinal()] =
                Shapes.box(0, 5 / 16D, 5 / 16D, 5 / 16D, 11 / 16D, 11 / 16D);
        arm[Direction.EAST.ordinal()] =
                Shapes.box(11 / 16D, 5 / 16D, 5 / 16D, 1, 11 / 16D, 11 / 16D);
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int o = 0; o < 6; o++) {
                if ((mask & (1 << o)) != 0) shape = Shapes.or(shape, arm[o]);
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    private static BooleanProperty propertyFor(Direction dir) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state =
                defaultBlockState()
                        .setValue(
                                WATERLOGGED,
                                ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
        for (Direction dir : Direction.VALUES) {
            BlockPos npos = ctx.getClickedPos().relative(dir);
            state =
                    state.setValue(
                            propertyFor(dir),
                            Library.canConnect(ctx.getLevel(), npos, dir.getOpposite()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction dir,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED))
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        boolean joins = Library.canConnect(level, pos.relative(dir), dir.getOpposite());
        if (level.isClientSide())
            return Library.predictArm(state, propertyFor(dir), neighbourState, joins);
        return state.setValue(propertyFor(dir), joins);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        for (Direction dir : Direction.VALUES) {
            if (state.getValue(propertyFor(dir))) mask |= 1 << dir.ordinal();
        }
        return SHAPES[mask];
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel sl && !oldState.is(this)) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(sl);
            graph.addNode(pos.asLong(), PowerGraphProvider.INSTANCE.createData(state), OPEN_ALL);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PowerGraph.get(level).removeNode(pos.asLong());
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
        if (level instanceof ServerLevel sl) {
            LevelNodeGraph.invalidateEndpointsAt(sl, pos);
            Library.redrawArms(state, sl, pos);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }
}
