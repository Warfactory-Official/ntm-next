/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.rail;

import com.hbm.blockentity.RailBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import org.jspecify.annotations.Nullable;

public abstract class NtmRailBlock extends MultiblockCoreBlock
        implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected NtmRailBlock(Properties props) {
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

    public abstract RailGauge gauge(Level level, int x, int y, int z);

    public abstract void travel(
            Level level,
            int x,
            int y,
            int z,
            double trainX,
            double trainY,
            double trainZ,
            double headingX,
            double headingY,
            double headingZ,
            double speed,
            RailStep step,
            double collisionReach);

    public static final class Owner {
        public int coreX, coreY, coreZ;
        public Direction facing;
        public NtmRailBlock rail;
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
        if (!(state.getBlock() instanceof NtmRailBlock rail)) return false;
        out.coreX = cx;
        out.coreY = cy;
        out.coreZ = cz;
        out.facing = state.getValue(FACING);
        out.rail = rail;
        return true;
    }

    private static long coreOfAnyPacked(Level level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (here.getBlock() instanceof NtmRailBlock rail && !rail.isCellState(here))
            return pos.asLong();
        if (level instanceof ServerLevel server) {
            return MultiblockSurface.indexedCorePacked(server, pos.getX(), pos.getY(), pos.getZ());
        }
        BlockPos core = MultiblockSurface.clientCoreOf(level, pos);
        return core == null ? MultiblockSurface.NO_CORE : core.asLong();
    }

    protected record StraightRun(int from, int to, int exit, double lift, double shift) {}

    protected final void travelStraight(
            StraightRun run,
            int y,
            double trainX,
            double trainZ,
            double motionX,
            double motionZ,
            double speed,
            RailStep step,
            double collisionReach) {
        Owner owner = step.owner;
        boolean alongX = owner.facing.getAxis() == Direction.Axis.X;
        int core = alongX ? owner.coreX : owner.coreZ;
        double motion = alongX ? motionX : motionZ;
        double target = alongX ? trainX : trainZ;
        if (motion > 0) target += speed;
        else target -= speed;
        double along = Mth.clamp(target, core + run.from(), core + run.to());
        Direction clockwise = owner.facing.getClockWise();
        double across =
                (alongX ? owner.coreZ : owner.coreX)
                        + 0.5
                        + (alongX ? clockwise.getStepZ() : clockwise.getStepX()) * run.shift();
        double height = straightFloor(owner, y, core, target) + run.lift();
        if (straightStop(core, target, along, across, height, step, collisionReach)) return;
        step.leftover(Math.abs(target - along) * Math.signum(speed));
        boolean towardPositive = motion * speed > 0;
        int exit = towardPositive ? run.exit() : -run.exit();
        step.exitAt(
                alongX ? owner.coreX + exit : owner.coreX,
                straightExitY(owner, y, towardPositive),
                alongX ? owner.coreZ : owner.coreZ + exit);
        placeOnRun(step, along, across, height);
    }

    protected double straightFloor(Owner owner, int y, int core, double target) {
        return y;
    }

    protected int straightExitY(Owner owner, int y, boolean towardPositive) {
        return y;
    }

    protected boolean straightStop(
            int core,
            double target,
            double along,
            double across,
            double height,
            RailStep step,
            double collisionReach) {
        return false;
    }

    protected static void placeOnRun(RailStep step, double along, double across, double height) {
        if (step.owner.facing.getAxis() == Direction.Axis.X) step.reach(along, height, across);
        else step.reach(across, height, along);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RailBlockEntity(pos, state);
    }
}
