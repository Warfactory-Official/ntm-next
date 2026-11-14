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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.CustomMachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.MachineCaps;
import com.hbm.uninos.graph.EndpointRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class CustomMachinePorts {

    private CustomMachinePorts() {}

    public static @Nullable CustomMachineBlockEntity ownerAt(
            Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CMPortBlock) || !(level instanceof ServerLevel server))
            return null;
        long packed =
                MultiblockSurface.recordedCorePacked(server, pos.getX(), pos.getY(), pos.getZ());
        if (!MultiblockSurface.hasCore(packed)) return null;
        BlockPos core = BlockPos.of(packed);
        LevelChunk chunk = MultiblockCoreBlock.readableChunk(server, core.getX(), core.getZ());
        if (chunk == null) return null;
        if (!(chunk.getBlockEntity(core) instanceof CustomMachineBlockEntity machine)) return null;
        return machine.isRemoved() || !machine.ownsPort(pos) ? null : machine;
    }

    public static void index(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        MultiblockCoreBlock.claimCells(level, core, cells, MultiblockCoreIndex.PORT);
        EndpointRegistry endpoints = EndpointRegistry.of(level);
        for (BlockPos cell : cells) {
            EndpointRegistry.Endpoint declared = endpoints.declared(cell.asLong());
            if (declared != null && declared.owner() == core.asLong()) continue;
            MachineCaps caps = MachineCaps.declared(level.getBlockState(cell).getBlock());
            endpoints.declare(
                    cell,
                    core,
                    MultiblockCoreBlock.MASK_ALL,
                    caps == null ? 0 : caps.declaredBits());
            MultiblockCoreBlock.notifyCell(level, cell);
            MultiblockCoreBlock.renotifyNeighbours(level, cell);
        }
    }

    public static void unindex(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        long corePacked = core.asLong();
        for (BlockPos cell : cells) {
            if (MultiblockSurface.recordedCorePacked(level, cell.getX(), cell.getY(), cell.getZ())
                    != corePacked) {
                continue;
            }
            MultiblockCoreBlock.unindexCell(level, cell);
        }
        withdraw(level, core, cells);
        for (BlockPos cell : cells) MultiblockCoreBlock.renotifyNeighbours(level, cell);
    }

    public static void withdraw(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        EndpointRegistry endpoints = EndpointRegistry.of(level);
        for (BlockPos cell : cells) {
            EndpointRegistry.Endpoint declared = endpoints.declared(cell.asLong());
            if (declared != null && declared.owner() != core.asLong()) continue;
            endpoints.withdraw(cell);
            MultiblockCoreBlock.notifyCell(level, cell);
        }
    }
}
