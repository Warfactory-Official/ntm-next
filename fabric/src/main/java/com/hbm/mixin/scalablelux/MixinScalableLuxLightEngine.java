// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.IBulkLightEngine;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
        targets =
                "ca.spottedleaf.starlight.common.light.vanillainterface.ThreadedLevelLightEngineVanillaInterface",
        remap = false)
public abstract class MixinScalableLuxLightEngine implements IBulkLightEngine {
    @Shadow @Final protected StarLightInterface lightEngine;

    @Shadow
    private void scalablelux$queueTaskForSection(int x, int y, int z, Supplier<ChunkTasks> update) {
        throw new AssertionError();
    }

    @Override
    public CompletableFuture<Void> hbm$updateLight(
            LevelChunk chunk, LongList positions, long sectionMask) {
        ScalableLuxBatch batch = new ScalableLuxBatch(lightEngine, chunk, positions, sectionMask);

        scalablelux$queueTaskForSection(
                chunk.getPos().x(), chunk.getMinSectionY(), chunk.getPos().z(), batch);
        return batch.completion();
    }
}
