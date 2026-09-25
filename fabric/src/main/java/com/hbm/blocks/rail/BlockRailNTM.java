// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.rail;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.BlockEntityRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockRailNTM extends BlockMultiblockCore
        implements IRailNTM, EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected BlockRailNTM(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(
                        WATERLOGGED,
                        ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
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

    @Override
    public boolean cellsWaterlog() {
        return true;
    }

    public static final class Owner {
        public int coreX, coreY, coreZ;
        public Direction facing;
        public BlockRailNTM rail;
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        private final BlockPos.MutableBlockPos core = new BlockPos.MutableBlockPos();

        boolean matches(BlockPos pos) {
            return pos.getX() == coreX && pos.getY() == coreY && pos.getZ() == coreZ;
        }
    }

    public static boolean railAt(Level level, int x, int y, int z, Owner out) {
        return railAt(level, out.probe.set(x, y, z), out);
    }

    public static boolean railAt(Level level, BlockPos pos, Owner out) {

        if (out.rail != null) {
            BlockPos core = out.core.set(out.coreX, out.coreY, out.coreZ);
            if (out.matches(pos) || MultiblockSurface.coreClaims(level, core, pos)) {
                BlockState cached = level.getBlockState(core);
                if (cached.getBlock() == out.rail && cached.getValue(FACING) == out.facing)
                    return true;
            }
        }

        long packed = coreOfAnyPacked(level, pos);
        if (!MultiblockSurface.hasCore(packed)) return false;
        int cx = BlockPos.getX(packed), cy = BlockPos.getY(packed), cz = BlockPos.getZ(packed);
        BlockState state = level.getBlockState(out.core.set(cx, cy, cz));
        if (!(state.getBlock() instanceof BlockRailNTM rail)) return false;
        out.coreX = cx;
        out.coreY = cy;
        out.coreZ = cz;
        out.facing = state.getValue(FACING);
        out.rail = rail;
        return true;
    }

    private static long coreOfAnyPacked(Level level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (here.getBlock() instanceof BlockRailNTM rail && !rail.isCellState(here))
            return pos.asLong();
        if (level instanceof ServerLevel server) {
            return MultiblockSurface.indexedCorePacked(server, pos.getX(), pos.getY(), pos.getZ());
        }
        BlockPos core = MultiblockSurface.clientCoreOf(level, pos);
        return core == null ? MultiblockSurface.NO_CORE : core.asLong();
    }

    @Override
    public @Nullable VoxelShape cellShape(
            int lx, int ly, int lz, Direction facing, boolean open, boolean forCollision) {
        return Shapes.create(0, 0, 0, 1, 0.125, 1);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityRail(pos, state);
    }
}
