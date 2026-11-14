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

import ca.spottedleaf.starlight.common.light.StarLightInterface.ConcurrentLightQueue;
import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.ScalableLuxBatchQueue;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConcurrentLightQueue.class, remap = false)
public abstract class MixinConcurrentLightQueue implements ScalableLuxBatchQueue {
    @Shadow
    private ChunkTasks enqueueImpl(long key, Consumer<ChunkTasks> action) {
        throw new AssertionError();
    }

    @Override
    public synchronized ChunkTasks hbm$enqueueLightBatch(ScalableLuxBatch batch) {
        return enqueueImpl(batch.chunkKey, batch);
    }
}
