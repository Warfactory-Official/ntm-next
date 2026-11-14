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

package com.hbm.compat.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ScalableLuxBatch implements Supplier<ChunkTasks>, Consumer<ChunkTasks> {
    public final long chunkKey;
    private final StarLightInterface engine;
    private final LevelChunk chunk;
    private final LongList positions;
    private final long sectionMask;
    private ChunkTasks submitted;

    public ScalableLuxBatch(
            StarLightInterface engine, LevelChunk chunk, LongList positions, long sectionMask) {
        this.engine = engine;
        this.chunk = chunk;
        this.positions = positions;
        this.sectionMask = sectionMask;
        this.chunkKey = chunk.getPos().pack();
    }

    @Override
    public ChunkTasks get() {
        return submitted =
                ((com.hbm.interfaces.injected.ScalableLuxBatchQueue) (java.lang.Object) (engine))
                        .hbm$enqueueLightBatch(this);
    }

    @Override
    public void accept(ChunkTasks task) {
        ObjectOpenHashSet<BlockPos> changed = (ObjectOpenHashSet<BlockPos>) task.changedPositions;
        changed.ensureCapacity(changed.size() + positions.size());
        for (int i = 0; i < positions.size(); i++) {
            changed.add(BlockPos.of(positions.getLong(i)));
        }
        if (sectionMask != 0) {
            LevelChunkSection[] sections = chunk.getSections();
            if (task.changedSectionSet == null)
                task.changedSectionSet = new Boolean[sections.length];
            for (long mask = sectionMask; mask != 0; mask &= mask - 1) {
                int section = Long.numberOfTrailingZeros(mask);
                task.changedSectionSet[section] = sections[section].hasOnlyAir();
            }
        }
    }

    public CompletableFuture<Void> completion() {
        return submitted.onComplete;
    }
}
