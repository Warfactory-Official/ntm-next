// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.blockentity.ChunkPublication;
import net.caffeinemc.mods.lithium.common.world.ChunkLoadTricks;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkLoadTricks.class, remap = false)
public abstract class MixinChunkLoadTricks {

    @Inject(method = "tryRetrieveCurrentlyLoading", at = @At("HEAD"), cancellable = true)
    private static void hbm$loadingChunk(
            @Nullable ChunkHolder holder, CallbackInfoReturnable<ChunkAccess> cir) {
        LevelChunk loading = holder == null ? null : ChunkPublication.loading(holder);
        if (loading != null) cir.setReturnValue(loading);
    }
}
