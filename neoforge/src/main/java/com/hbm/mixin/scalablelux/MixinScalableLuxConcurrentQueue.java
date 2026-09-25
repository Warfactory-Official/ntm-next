// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.ScalableLuxBatchQueue;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
        targets = "ca.spottedleaf.starlight.common.light.StarLightInterface$ConcurrentLightQueue",
        remap = false)
public abstract class MixinScalableLuxConcurrentQueue implements ScalableLuxBatchQueue {
    @Shadow
    private ChunkTasks enqueueImpl(long key, Consumer<ChunkTasks> action) {
        throw new AssertionError();
    }

    @Override
    public synchronized ChunkTasks hbm$enqueueLightBatch(ScalableLuxBatch batch) {

        return enqueueImpl(batch.chunkKey, batch);
    }
}
