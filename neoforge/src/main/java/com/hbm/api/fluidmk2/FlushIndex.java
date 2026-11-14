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

package com.hbm.api.fluidmk2;

import com.hbm.platform.ServerEvents;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class FlushIndex {

    private final Long2ObjectMap<ReferenceOpenHashSet<FlushRegister>> byChunk =
            new Long2ObjectOpenHashMap<>();

    private FlushIndex() {}

    public static void init() {
        final BiConsumer<ServerLevel, LevelChunk> callback =
                (level, chunk) -> {
                    FlushIndex index =
                            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                                    .hbm$flushIndex();
                    if (index != null) index.evict(chunk.getPos().pack());
                };
        ServerEvents.CHUNK_UNLOAD.add(callback);
    }

    static FlushIndex of(ServerLevel level) {
        FlushIndex index =
                ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$flushIndex();
        if (index == null)
            ((com.hbm.interfaces.injected.ServerLevelExtension) (level))
                    .hbm$flushIndex(index = new FlushIndex());
        return index;
    }

    public static void invalidateAll(ServerLevel level) {
        FlushIndex index =
                ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$flushIndex();
        if (index == null) return;
        for (ReferenceOpenHashSet<FlushRegister> bucket : index.byChunk.values()) {
            for (FlushRegister register : bucket) register.invalidate();
        }
    }

    public static void invalidateAt(ServerLevel level, long posKey) {
        ReferenceOpenHashSet<FlushRegister> bucket = bucket(level, LevelNodeGraph.chunkOf(posKey));
        if (bucket == null) return;
        for (FlushRegister register : bucket) register.invalidateAt(posKey);
    }

    public static void invalidateChunk(ServerLevel level, long chunkKey) {
        ReferenceOpenHashSet<FlushRegister> bucket = bucket(level, chunkKey);
        if (bucket == null) return;
        for (FlushRegister register : bucket) register.invalidateChunk(chunkKey);
    }

    public static void onChunkFull(ServerLevel level, LevelChunk chunk) {
        ReferenceOpenHashSet<FlushRegister> bucket = bucket(level, chunk.getPos().pack());
        if (bucket == null) return;
        for (FlushRegister register : bucket) register.unpark();
    }

    private static @Nullable ReferenceOpenHashSet<FlushRegister> bucket(
            ServerLevel level, long chunkKey) {
        FlushIndex index =
                ((com.hbm.interfaces.injected.ServerLevelExtension) (level)).hbm$flushIndex();
        return index == null ? null : index.byChunk.get(chunkKey);
    }

    void rebind(FlushRegister register, long[] keys) {
        for (long key : register.buckets()) drop(key, register);
        register.setBuckets(keys);
        for (long key : keys) add(key, register);
    }

    void park(long chunkKey, FlushRegister register) {
        add(chunkKey, register);
        register.rememberBucket(chunkKey);
    }

    private void add(long chunkKey, FlushRegister register) {
        byChunk.computeIfAbsent(chunkKey, k -> new ReferenceOpenHashSet<>(2)).add(register);
    }

    private void drop(long chunkKey, FlushRegister register) {
        ReferenceOpenHashSet<FlushRegister> bucket = byChunk.get(chunkKey);
        if (bucket == null) return;
        bucket.remove(register);
        if (bucket.isEmpty()) byChunk.remove(chunkKey);
    }

    private void evict(long chunkKey) {
        ReferenceOpenHashSet<FlushRegister> bucket = byChunk.get(chunkKey);
        if (bucket == null) return;
        for (FlushRegister register : bucket.toArray(new FlushRegister[0])) {
            if (register.machineChunk() != chunkKey) continue;
            for (long key : register.buckets()) drop(key, register);
            register.setBuckets(new long[0]);
        }
    }
}
