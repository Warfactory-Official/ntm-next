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

package com.hbm.mixin.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import ca.spottedleaf.starlight.common.light.StarLightInterface.SimpleLightQueue;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.ScalableLuxBatchQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SimpleLightQueue.class, remap = false)
public abstract class MixinSimpleLightQueue implements ScalableLuxBatchQueue {
    @Shadow @Final protected Long2ObjectLinkedOpenHashMap<ChunkTasks> chunkTasks;
    @Shadow protected volatile boolean queueDirty;

    @Override
    public synchronized ChunkTasks hbm$enqueueLightBatch(ScalableLuxBatch batch) {
        ChunkTasks task = chunkTasks.computeIfAbsent(batch.chunkKey, ChunkTasks::new);
        batch.accept(task);
        queueDirty = true;
        return task;
    }
}
