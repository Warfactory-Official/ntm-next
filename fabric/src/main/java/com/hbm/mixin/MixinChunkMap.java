// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.entity.item.EntityFallingBlockBatch;
import com.hbm.entity.item.PendingFallers;
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
public abstract class MixinChunkMap {

    @Shadow @Final private ServerLevel level;

    @Inject(method = "onFullChunkStatusChange", at = @At("TAIL"))
    private void hbm$onReadableChunkStatus(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        if (status != FullChunkStatus.ENTITY_TICKING)
            EntityFallingBlockBatch.freezeChunk(level, pos);
        if (!status.isOrAfter(FullChunkStatus.FULL)) {
            ChunkPublication.demote(level, pos);
            return;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x(), pos.z());
        if (chunk == null) return;
        if (status == FullChunkStatus.ENTITY_TICKING) PendingFallers.promote(level, chunk);
        ChunkPublication.promote(level, chunk);
    }
}
