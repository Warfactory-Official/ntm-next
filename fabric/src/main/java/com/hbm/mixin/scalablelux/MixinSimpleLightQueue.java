// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
