// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.platform.FabricServerAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapUnloadMixin {

    @Shadow @Final private ServerLevel level;

    @Inject(
            method = "lambda$scheduleUnload$0",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z",
                            shift = At.Shift.AFTER))
    private void hbm$afterUnloadSave(
            ChunkHolder chunkHolder,
            CompletableFuture<?> saveSyncFuture,
            long chunkPos,
            CallbackInfo ci,
            @Local(name = "chunk") ChunkAccess chunk) {
        if (chunk instanceof LevelChunk levelChunk) {
            FabricServerAccessor.dispatchChunkUnload(this.level, levelChunk);
        }
    }
}
