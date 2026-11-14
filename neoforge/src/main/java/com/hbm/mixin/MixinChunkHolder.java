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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.objectweb.asm.Opcodes;
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

    @WrapOperation(
            method = "scheduleFullChunkPromotion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ljava/util/concurrent/CompletableFuture;thenRunAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            require = 1,
            allow = 1)
    private CompletableFuture<?> hbm$rememberQueuedPublication(
            CompletableFuture<?> confirmation,
            Runnable callback,
            Executor executor,
            Operation<CompletableFuture<?>> original,
            @Share("queuedPublication") LocalRef<CompletableFuture<?>> queuedPublication) {
        var dependent = original.call(confirmation, callback, executor);
        queuedPublication.set(dependent);
        return dependent;
    }

    @WrapOperation(
            method = "scheduleFullChunkPromotion",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/server/level/ChunkHolder;pendingFullStateConfirmation:Ljava/util/concurrent/CompletableFuture;",
                            opcode = Opcodes.PUTFIELD),
            require = 1,
            allow = 1)
    private void hbm$retainQueuedPublication(
            ChunkHolder holder,
            CompletableFuture<?> ignoredConfirmation,
            Operation<Void> original,
            @Share("queuedPublication") LocalRef<CompletableFuture<?>> queuedPublication) {
        original.call(holder, queuedPublication.get());
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
