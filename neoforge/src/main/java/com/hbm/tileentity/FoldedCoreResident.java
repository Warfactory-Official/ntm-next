// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface FoldedCoreResident {

    static void onLoad(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel server)) return;
        if (!(be.getBlockState().getBlock() instanceof BlockMultiblockCore folded)) return;
        BlockPos core = be.getBlockPos().immutable();

        MinecraftServer server1 = server.getServer();
        server1.schedule(
                server1.wrapRunnable(
                        () -> {
                            if (be.isRemoved()) return;
                            if (BlockMultiblockCore.readableChunk(server, core.getX(), core.getZ())
                                    == null) {
                                PendingCoreInvalidation.await(server, core);
                                return;
                            }
                            folded.reindexLoadedCells(server, core);
                        }));
    }

    static void onRemove(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (!(be.getLevel() instanceof ServerLevel server)) return;
        if (!(state.getBlock() instanceof BlockMultiblockCore folded)) return;
        folded.invalidateCellCaps(server, be.getBlockPos(), BlockMultiblockCore.coreFacing(state));
    }
}
