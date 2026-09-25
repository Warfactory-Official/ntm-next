// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.tileentity.PendingCoreInvalidation;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMapFullStatus {

    @Shadow @Final private ServerLevel level;

    @Inject(method = "onFullChunkStatusChange", at = @At("TAIL"))
    private void hbm$onRisingFullStatus(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        if (status != FullChunkStatus.FULL) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) return;
        AssembledMembers.onChunkFull(level, chunk);
        LevelNodeGraph.onChunkFull(level, chunk);
        FlushIndex.onChunkFull(level, chunk);
        PendingCoreInvalidation.onChunkFull(level, chunk);
    }
}
