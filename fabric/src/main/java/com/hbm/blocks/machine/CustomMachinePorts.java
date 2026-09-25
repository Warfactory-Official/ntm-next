// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityCustomMachine;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class CustomMachinePorts {

    private CustomMachinePorts() {}

    public static @Nullable BlockEntityCustomMachine ownerAt(
            Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BlockCMPort) || !(level instanceof ServerLevel server))
            return null;
        long packed =
                MultiblockSurface.recordedCorePacked(server, pos.getX(), pos.getY(), pos.getZ());
        if (!MultiblockSurface.hasCore(packed)) return null;
        BlockPos core = BlockPos.of(packed);
        LevelChunk chunk = BlockMultiblockCore.readableChunk(server, core.getX(), core.getZ());
        if (chunk == null) return null;
        if (!(chunk.getBlockEntity(core) instanceof BlockEntityCustomMachine machine)) return null;
        return machine.isRemoved() || !machine.claimsPort(pos) ? null : machine;
    }

    public static void index(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        BlockMultiblockCore.claimCells(level, core, cells);
        EndpointRegistry endpoints = EndpointRegistry.of(level);
        for (BlockPos cell : cells) {
            EndpointRegistry.Endpoint declared = endpoints.declared(cell.asLong());
            if (declared != null && declared.owner() == core.asLong()) continue;
            ICapabilityBlock block = (ICapabilityBlock) level.getBlockState(cell).getBlock();
            endpoints.declare(
                    cell, core, BlockMultiblockCore.MASK_ALL, block.caps().declaredBits());
            Services.CAPS.invalidateCaps(level, cell);
            LevelNodeGraph.invalidateEndpointsAround(level, cell);
            BlockMultiblockCore.renotifyNeighbours(level, cell);
        }
    }

    public static void unindex(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        long corePacked = core.asLong();
        for (BlockPos cell : cells) {
            if (MultiblockSurface.recordedCorePacked(level, cell.getX(), cell.getY(), cell.getZ())
                    != corePacked) {
                continue;
            }
            BlockMultiblockCore.unindexCell(level, cell);
        }
        withdraw(level, core, cells);

        for (BlockPos cell : cells) BlockMultiblockCore.renotifyNeighbours(level, cell);
    }

    public static void withdraw(ServerLevel level, BlockPos core, List<BlockPos> cells) {
        EndpointRegistry endpoints = EndpointRegistry.of(level);
        for (BlockPos cell : cells) {
            EndpointRegistry.Endpoint declared = endpoints.declared(cell.asLong());
            if (declared != null && declared.owner() != core.asLong()) continue;
            endpoints.withdraw(cell);
            Services.CAPS.invalidateCaps(level, cell);
            LevelNodeGraph.invalidateEndpointsAround(level, cell);
        }
    }
}
