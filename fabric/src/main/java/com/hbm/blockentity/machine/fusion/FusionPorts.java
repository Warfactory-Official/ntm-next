/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.machine.fusion.FusionMachineBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Caps;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RegistryUtil;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FusionPorts {

    public static BlockApiLookup<BlockEntity, Direction> KLYSTRON;
    public static BlockApiLookup<BlockEntity, Direction> PLASMA;

    private FusionPorts() {}

    public static void register() {
        KLYSTRON =
                BlockApiLookup.get(
                        RegistryUtil.id("fusion_klystron"), BlockEntity.class, Direction.class);
        PLASMA =
                BlockApiLookup.get(
                        RegistryUtil.id("fusion_plasma"), BlockEntity.class, Direction.class);
    }

    public static BlockApiLookup<BlockEntity, Direction> token(Kind kind) {
        return kind == Kind.KLYSTRON ? KLYSTRON : PLASMA;
    }

    public static @Nullable BlockEntity peer(ServerLevel level, Port port) {
        return token(port.kind())
                .find((Level) level, port.cell().relative(port.face()), port.face().getOpposite());
    }

    public static BlockLookupCache<BlockEntity> cache(ServerLevel level, Port port) {
        return Caps.createCache(
                token(port.kind()),
                level,
                port.cell().relative(port.face()),
                port.face().getOpposite());
    }

    public static <T> @Nullable T peer(ServerLevel level, Port port, Class<T> type) {
        BlockEntity be = peer(level, port);
        return type.isInstance(be) ? type.cast(be) : null;
    }

    public static void declareAtCell(RegistryHandle<? extends Block> cell) {
        for (Kind kind : Kind.values()) {
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
                            token(kind),
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) -> portCap(level, pos, side, kind)));
        }
    }

    private static @Nullable BlockEntity portCap(
            Level level, BlockPos pos, @Nullable Direction side, Kind kind) {
        if (side == null) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof FusionMachineBlock machine)) return null;
        BlockEntity coreBe = level.getBlockEntity(core);
        if (coreBe == null || coreBe.isRemoved()) return null;
        Direction facing = coreState.getValue(MultiblockCoreBlock.FACING);
        return machine.exposes(core, facing, kind, pos, side) ? coreBe : null;
    }

    public static boolean exposes(
            Iterable<Port> ports, Kind kind, BlockPos pos, @Nullable Direction side) {
        if (side == null) return false;
        for (Port port : ports) {
            if (port.kind() == kind && port.face() == side && port.cell().equals(pos)) return true;
        }
        return false;
    }

    public enum Kind {
        KLYSTRON,
        PLASMA
    }

    public record Port(Kind kind, BlockPos cell, Direction face) {}
}
