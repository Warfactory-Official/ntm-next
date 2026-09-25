// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.platform.Services;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class FlushIndex {

    private static final Map<ServerLevel, FlushIndex> BY_LEVEL = new WeakHashMap<>();

    private final Long2ObjectMap<ReferenceOpenHashSet<FlushRegister>> byChunk =
            new Long2ObjectOpenHashMap<>();

    private FlushIndex() {}

    public static void init() {
        Services.SERVER.onServerStopping(server -> BY_LEVEL.clear());
        Services.SERVER.onChunkUnload(
                (level, chunk) -> {
                    FlushIndex index = BY_LEVEL.get(level);
                    if (index != null) index.evict(chunk.getPos().pack());
                });
    }

    static FlushIndex of(ServerLevel level) {
        return BY_LEVEL.computeIfAbsent(level, l -> new FlushIndex());
    }

    public static void invalidateAt(ServerLevel level, long posKey) {
        ReferenceOpenHashSet<FlushRegister> bucket = bucket(level, LevelNodeGraph.chunkOf(posKey));
        if (bucket == null) return;
        for (FlushRegister register : bucket) register.invalidateAt(posKey);
    }

    public static void onChunkFull(ServerLevel level, LevelChunk chunk) {
        ReferenceOpenHashSet<FlushRegister> bucket = bucket(level, chunk.getPos().pack());
        if (bucket == null) return;
        for (FlushRegister register : bucket) register.unpark();
    }

    private static @Nullable ReferenceOpenHashSet<FlushRegister> bucket(
            ServerLevel level, long chunkKey) {
        FlushIndex index = BY_LEVEL.get(level);
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
