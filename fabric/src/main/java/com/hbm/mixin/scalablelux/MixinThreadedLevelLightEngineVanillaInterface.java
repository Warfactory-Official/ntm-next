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
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import ca.spottedleaf.starlight.common.light.vanillainterface.ThreadedLevelLightEngineVanillaInterface;
import com.hbm.compat.scalablelux.ScalableLuxBatch;
import com.hbm.interfaces.injected.BulkLightEngine;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ThreadedLevelLightEngineVanillaInterface.class, remap = false)
public abstract class MixinThreadedLevelLightEngineVanillaInterface implements BulkLightEngine {
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

    @Override
    public void hbm$checkBlocks(LevelChunk chunk, long[] positions) {
        scalablelux$queueTaskForSection(
                chunk.getPos().x(),
                chunk.getMinSectionY(),
                chunk.getPos().z(),
                new ScalableLuxBatch(lightEngine, chunk, LongArrayList.wrap(positions), 0L));
    }
}
