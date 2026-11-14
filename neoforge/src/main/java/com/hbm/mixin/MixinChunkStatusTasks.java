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

import com.hbm.explosion.FalloutJournal;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkStatusTasks.class)
public abstract class MixinChunkStatusTasks {
    @WrapMethod(method = "light")
    private static CompletableFuture<ChunkAccess> hbm$falloutBeforeLight(
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk,
            Operation<CompletableFuture<ChunkAccess>> original) {
        CompletableFuture<ChunkAccess> converted =
                FalloutJournal.beforeLight(
                        context.level(),
                        ((com.hbm.interfaces.injected.BulkLightEngine) (context.lightEngine())),
                        chunk);
        return converted == null
                ? original.call(context, step, chunks, chunk)
                : converted.thenCompose(c -> original.call(context, step, chunks, c));
    }
}
