// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.blockentity.ChunkPublication;
import java.util.concurrent.Executor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder extends GenerationChunkHolder {

    @Shadow private int oldTicketLevel;

    public MixinChunkHolder(ChunkPos pos) {
        super(pos);
    }

    @Inject(method = "updateFutures", at = @At("HEAD"))
    private void hbm$risenIntoFull(
            ChunkMap scheduler, Executor mainThreadExecutor, CallbackInfo ci) {
        if (ChunkLevel.fullStatus(oldTicketLevel).isOrAfter(FullChunkStatus.FULL)) return;
        if (!ChunkLevel.fullStatus(getTicketLevel()).isOrAfter(FullChunkStatus.FULL)) return;
        if (getChunkIfPresentUnchecked(ChunkStatus.FULL) instanceof LevelChunk chunk)
            ChunkPublication.risen(chunk);
    }
}
