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

package com.hbm.mixin;

import com.hbm.blockentity.ChunkPublication;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerChunkCache.class, priority = 1100)
public abstract class MixinServerChunkCache {

    @Shadow @Final private ServerLevel level;

    @Inject(
            method =
                    "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerChunkCache;getChunkFutureMainThread(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;"),
            cancellable = true,
            require = 0)
    private void hbm$loadingChunk(
            int x,
            int z,
            ChunkStatus targetStatus,
            boolean loadOrGenerate,
            CallbackInfoReturnable<ChunkAccess> cir) {
        LevelChunk loading = ChunkPublication.loading(level, x, z);
        if (loading != null) cir.setReturnValue(loading);
    }

    @ModifyExpressionValue(
            method = "getChunkNow",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ChunkHolder;getChunkIfPresent(Lnet/minecraft/world/level/chunk/status/ChunkStatus;)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private @Nullable ChunkAccess hbm$loadingChunkNow(
            @Nullable ChunkAccess chunk,
            @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int z) {
        return chunk != null ? chunk : ChunkPublication.loading(level, x, z);
    }

    @ModifyExpressionValue(
            method = "runDistanceManagerUpdates",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/DistanceManager;runAllUpdates(Lnet/minecraft/server/level/ChunkMap;)Z"))
    private boolean hbm$settleRisenChunks(boolean updated) {
        if (updated) ChunkPublication.settle(level);
        return updated;
    }
}
