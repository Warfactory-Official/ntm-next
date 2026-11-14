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

package com.hbm.blockentity.machine.storage;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities.CapRole;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.PhysicalGraph;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

final class FluidBufferCells {

    private FluidBufferCells() {}

    static Long2IntOpenHashMap activePlane(BlockPos core, BlockState state) {
        Long2IntOpenHashMap masks = new Long2IntOpenHashMap();
        masks.put(core.asLong(), 0);
        MultiblockSurface.forEachActiveFace(
                (MultiblockCoreBlock) state.getBlock(),
                core,
                state.getValue(MultiblockCoreBlock.FACING),
                CapRole.FLUID_OUT,
                (cell, side) ->
                        masks.mergeInt(cell.asLong(), 1 << side.ordinal(), (a, b) -> a | b));
        return masks;
    }

    static Long2IntOpenHashMap endPorts(BlockPos core, BlockState state, int reach) {
        Direction facing = state.getValue(MultiblockCoreBlock.FACING);
        Long2IntOpenHashMap masks = new Long2IntOpenHashMap();
        masks.put(core.asLong(), 0);
        masks.put(core.relative(facing, reach).asLong(), 1 << facing.ordinal());
        masks.put(core.relative(facing, -reach).asLong(), 1 << facing.getOpposite().ordinal());
        return masks;
    }

    static void join(ServerLevel server, BlockPos core, Long2IntOpenHashMap masks, Fluid fluid) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.get(server, fluid);
        PipeData data = new PipeData(fluid);
        long coreKey = core.asLong();
        try (var _ = PhysicalGraph.begin(server)) {
            graph.addMachineNode(coreKey, data, masks.get(coreKey), true);
            for (var entry : masks.long2IntEntrySet()) {
                long cell = entry.getLongKey();
                if (cell == coreKey) continue;
                graph.addMachineNode(cell, data, entry.getIntValue(), false);
                graph.addRemoteLink(coreKey, cell);
            }
        }
    }

    static void leave(ServerLevel server, Long2IntOpenHashMap masks) {
        try (var _ = PhysicalGraph.begin(server)) {
            for (long cell : masks.keySet()) release(server, cell);
        }
    }

    static void release(ServerLevel server, long key) {
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(server, key);
        if (graph == null) return;
        GraphNode<PipeData> node = graph.getNode(key);
        if (node != null && node.machineOwned) graph.removeNode(key);
    }
}
