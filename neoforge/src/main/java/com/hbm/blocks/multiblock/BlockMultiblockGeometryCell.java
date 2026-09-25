// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.blocks.ClimbBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockMultiblockGeometryCell extends BlockMultiblockCell implements ClimbBox {

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static CellBuckets.@Nullable Geometry constructing;

    private final CellBuckets.Geometry bucket;
    private final boolean analog;

    private BlockMultiblockGeometryCell(Properties properties) {
        super(properties);
        this.bucket = pending();
        this.analog = bucket.comparator();
        BlockState any = stateDefinition.any().setValue(bucket.hi(), 0).setValue(bucket.lo(), 0);
        registerDefaultState(
                any.trySetValue(OPEN, Boolean.FALSE).trySetValue(COMPARATOR, Boolean.FALSE));
    }

    public static BlockMultiblockGeometryCell create(
            CellBuckets.Geometry bucket, Properties properties) {
        constructing = bucket;
        try {
            return bucket.part().waterlogs()
                    ? new Waterlogged(properties)
                    : new BlockMultiblockGeometryCell(properties);
        } finally {
            constructing = null;
        }
    }

    private static CellBuckets.Geometry pending() {
        CellBuckets.Geometry bucket = constructing;
        if (bucket == null) {
            throw new IllegalStateException(
                    "a geometry cell must be built through"
                            + " BlockMultiblockGeometryCell.create(bucket, properties)");
        }
        return bucket;
    }

    public static int shapeId(BlockState state) {
        return cell(state).idOf(state);
    }

    public static BlockState withShape(BlockState state, int id) {
        return cell(state).withId(state, id);
    }

    public static boolean isOpenCell(BlockState state) {
        return cell(state).isOpen(state);
    }

    public static BlockState openCell(BlockState state, boolean open) {
        return cell(state).withOpen(state, open);
    }

    public static void setCellOpen(ServerLevel level, BlockPos cell, BlockPos core, boolean open) {
        BlockState state = level.getBlockState(cell);
        if (!(state.getBlock() instanceof BlockMultiblockGeometryCell)) return;
        if (isOpenCell(state) == open) return;
        if (MultiblockSurface.recordedCorePacked(level, cell.getX(), cell.getY(), cell.getZ())
                != core.asLong()) {
            return;
        }
        level.setBlock(cell, openCell(state, open), Block.UPDATE_ALL);
    }

    private static BlockMultiblockGeometryCell cell(BlockState state) {
        return (BlockMultiblockGeometryCell) state.getBlock();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CellBuckets.Geometry bucket = pending();
        builder.add(bucket.hi(), bucket.lo());
        if (bucket.part().opens()) builder.add(OPEN);
        if (bucket.comparator()) builder.add(COMPARATOR);
    }

    public CellBuckets.Geometry bucket() {
        return bucket;
    }

    public GeometryCellPartition partition() {
        return bucket.part();
    }

    public int idOf(BlockState state) {
        return state.getValue(bucket.hi()) * bucket.radix() + state.getValue(bucket.lo());
    }

    public BlockState withId(BlockState state, int id) {
        return state.setValue(bucket.hi(), id / bucket.radix())
                .setValue(bucket.lo(), id % bucket.radix());
    }

    public boolean isOpen(BlockState state) {
        return bucket.part().opens() && state.getValue(OPEN);
    }

    public BlockState withOpen(BlockState state, boolean open) {
        return bucket.part().opens() ? state.setValue(OPEN, open) : state;
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return MultiblockCellShapes.sound(bucket, idOf(state));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return MultiblockCellShapes.shape(bucket, idOf(state), isOpen(state), false);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return MultiblockCellShapes.shape(bucket, idOf(state), isOpen(state), true);
    }

    @Override
    public @Nullable AABB climbBox(BlockState state) {
        return MultiblockCellShapes.climbBox(bucket, idOf(state));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return analog && state.getValue(COMPARATOR);
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        return analogOutputAtCell(level, pos, direction);
    }

    private static final class Waterlogged extends BlockMultiblockGeometryCell
            implements SimpleWaterloggedBlock {

        private Waterlogged(Properties properties) {
            super(properties);
            registerDefaultState(
                    defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(BlockStateProperties.WATERLOGGED);
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
            if (state.getValue(BlockStateProperties.WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(
                    state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(BlockStateProperties.WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
        }
    }
}
