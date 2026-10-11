// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.ScalableLuxBatchQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StarLightInterface.class, remap = false)
public abstract class MixinStarLightInterface implements ScalableLuxBatchQueue {
    @Shadow @Final protected LightQueue lightQueue;

    @Override
    public ChunkTasks hbm$enqueueLightBatch(ScalableLuxBatch batch) {
        return ((ScalableLuxBatchQueue) (Object) lightQueue).hbm$enqueueLightBatch(batch);
    }
}
