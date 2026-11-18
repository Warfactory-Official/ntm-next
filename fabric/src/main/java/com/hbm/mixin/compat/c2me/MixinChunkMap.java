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

package com.hbm.mixin.compat.c2me;

import com.hbm.util.datafix.HbmDataFixers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkMap.class, priority = 1200)
public abstract class MixinChunkMap {

    @Shadow
    private CompoundTag upgradeChunkTag(CompoundTag tag) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "readChunk", at = @At("RETURN"))
    private CompletableFuture<Optional<CompoundTag>> hbm$upgradeSkippedChunk(
            CompletableFuture<Optional<CompoundTag>> read) {
        return read.thenCompose(
                tag ->
                        tag.isPresent() && !HbmDataFixers.chunkCurrent(tag.get())
                                ? CompletableFuture.supplyAsync(
                                        () -> Optional.of(upgradeChunkTag(tag.get())),
                                        Util.backgroundExecutor().forName("upgradeChunk"))
                                : CompletableFuture.completedFuture(tag));
    }
}
