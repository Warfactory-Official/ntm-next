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

import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.network.FluidDuctBlock;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FluidPipeTintBulkLayout;
import com.hbm.packet.layout.FluidPipeTintLayout;
import com.hbm.packet.layout.FluidPipeTintRequestLayout;
import com.hbm.util.ChunkPositionIndex;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class FluidPipeTintData {

    public static final int DEFAULT_COLOR = 0xFF888888;
    public static final int UNKNOWN_FLUID_COLOR = CommonColors.WHITE;

    public static int lightened(int color) {
        int r = color >>> 16 & 255, g = color >>> 8 & 255, b = color & 255;
        r = (int) (r + (255 - r) * 0.25);
        g = (int) (g + (255 - g) * 0.25);
        b = (int) (b + (255 - b) * 0.25);
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static final long VALUE_MASK = (1L << MultiblockCoreIndex.KEY_SHIFT) - 1;
    private static final long[] EMPTY = new long[0];
    private static final ThreadLocal<LastMiss> LAST_MISS = ThreadLocal.withInitial(LastMiss::new);
    private static final VarHandle ACTIVE;
    private static final VarHandle CACHE_IDS;
    private static volatile Cache active;
    private static volatile long nextCacheId = 1;

    static {
        try {
            ACTIVE =
                    MethodHandles.lookup()
                            .findStaticVarHandle(FluidPipeTintData.class, "active", Cache.class);
            CACHE_IDS =
                    MethodHandles.lookup()
                            .findStaticVarHandle(
                                    FluidPipeTintData.class, "nextCacheId", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final class Cache {
        final long id = (long) CACHE_IDS.getAndAdd(1L);
        final Level level;
        volatile Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
        final ConcurrentLinkedQueue<Long> pending = new ConcurrentLinkedQueue<>();
        final LongOpenHashSet requested = new LongOpenHashSet();
        volatile int requestEpoch;

        Cache(Level level) {
            this.level = level;
        }
    }

    private static final class Chunk {
        volatile long[] entries = EMPTY;
    }

    private static final class LastMiss {
        long cacheId;
        int epoch;
        long posKey;
    }

    private FluidPipeTintData() {}

    private static long chunkOf(long posKey) {
        return ChunkPositionIndex.chunkOf(posKey);
    }

    private static Cache select(Level level) {
        for (; ; ) {
            Cache current = active;
            if (current != null && current.level == level) return current;
            Cache replacement = new Cache(level);
            if (ACTIVE.compareAndSet(current, replacement)) return replacement;
        }
    }

    private static Chunk chunkForWrite(Cache cache, long chunkKey) {
        Chunk chunk = cache.chunks.get(chunkKey);
        if (chunk != null) return chunk;
        chunk = new Chunk();
        Long2ObjectOpenHashMap<Chunk> next = cache.chunks.clone();
        next.put(chunkKey, chunk);
        cache.chunks = next;
        return chunk;
    }

    private static int key(Level level, long posKey) {
        return MultiblockCoreIndex.key(
                level, BlockPos.getX(posKey), BlockPos.getY(posKey), BlockPos.getZ(posKey));
    }

    private static boolean accepts(Level level, long posKey) {
        if (!level.isClientSide()) return true;
        int x = BlockPos.getX(posKey), z = BlockPos.getZ(posKey);
        if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)))
            return false;
        return level.getBlockState(BlockPos.of(posKey)).getBlock() instanceof FluidDuctBlock;
    }

    private static long pack(int key, Fluid fluid) {
        int id = BuiltInRegistries.FLUID.getId(fluid);
        assert id >= 0 && id + 1L <= VALUE_MASK;
        return ((long) key << MultiblockCoreIndex.KEY_SHIFT) | (id + 1L);
    }

    private static int indexOf(long[] entries, int key) {
        return MultiblockCoreIndex.indexOf(entries, entries.length, key);
    }

    private static Fluid fluidOf(long entry) {
        return BuiltInRegistries.FLUID.byId((int) ((entry & VALUE_MASK) - 1));
    }

    public static int colorFor(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return DEFAULT_COLOR;
        NTMFluidProperty prop = NTMFluidProperties.get(fluid);
        return prop != null ? prop.colorARGB() : UNKNOWN_FLUID_COLOR;
    }

    public static int colorAt(Level level, BlockPos pos) {
        return colorFor(fluidAt(level, pos));
    }

    public static Fluid fluidAt(Level level, BlockPos pos) {
        Fluid fluid = fluidAtIfKnown(level, pos);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public static @Nullable Fluid fluidAtIfKnown(Level level, BlockPos pos) {
        Cache cache = select(level);
        long posKey = pos.asLong();
        long chunkKey = chunkOf(posKey);
        Chunk chunk = cache.chunks.get(chunkKey);
        if (chunk != null) {
            long[] entries = chunk.entries;
            int at = indexOf(entries, key(level, posKey));
            if (at >= 0) return fluidOf(entries[at]);
        }
        LastMiss last = LAST_MISS.get();
        int epoch = cache.requestEpoch;
        if (last.cacheId != cache.id || last.epoch != epoch || last.posKey != posKey) {
            last.cacheId = cache.id;
            last.epoch = epoch;
            last.posKey = posKey;
            cache.pending.add(posKey);
        }
        return null;
    }

    public static void flushRequests(@Nullable Level level) {
        if (level == null) {
            active = null;
            return;
        }
        Cache cache = select(level);
        cache.requestEpoch++;
        if (cache.pending.isEmpty()) return;
        LongArrayList requests = new LongArrayList();
        Long pending;
        while ((pending = cache.pending.poll()) != null) {
            long posKey = pending;
            if (!level.getChunkSource()
                    .hasChunk(
                            SectionPos.blockToSectionCoord(BlockPos.getX(posKey)),
                            SectionPos.blockToSectionCoord(BlockPos.getZ(posKey)))) continue;
            Chunk chunk = cache.chunks.get(chunkOf(posKey));
            if (chunk != null && indexOf(chunk.entries, key(level, posKey)) >= 0) continue;
            if (cache.requested.add(posKey)) requests.add(posKey);
        }
        if (!requests.isEmpty())
            PacketWire.sendToServer(FluidPipeTintRequestLayout.encode(requests));
    }

    public static void set(Level level, long posKey, Fluid fluid) {
        Cache cache = select(level);
        cache.requested.remove(posKey);
        if (!accepts(level, posKey)) return;
        Chunk chunk = chunkForWrite(cache, chunkOf(posKey));
        long[] old = chunk.entries;
        int key = key(level, posKey);
        int at = indexOf(old, key);
        long value = pack(key, fluid);
        if (at < 0) {
            int insert = -at - 1;
            long[] entries = new long[old.length + 1];
            System.arraycopy(old, 0, entries, 0, insert);
            entries[insert] = value;
            System.arraycopy(old, insert, entries, insert + 1, old.length - insert);
            chunk.entries = entries;
        } else if (old[at] != value) {
            long[] entries = old.clone();
            entries[at] = value;
            chunk.entries = entries;
        }
    }

    public static void setAll(
            Level level, @Nullable Object storage, long address, int count, Fluid fluid) {
        if (count == 0) return;
        if (count == 1) {
            set(level, U.getLong(storage, address), fluid);
            return;
        }
        Cache cache = select(level);
        Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
        for (int index = 0; index < count; index++) {
            long posKey = U.getLong(storage, address + 8L * index);
            cache.requested.remove(posKey);
            if (!accepts(level, posKey)) continue;
            byChunk.computeIfAbsent(chunkOf(posKey), ignored -> new LongArrayList())
                    .add(pack(key(level, posKey), fluid));
        }
        if (byChunk.isEmpty()) return;
        for (var group : byChunk.long2ObjectEntrySet()) {
            Chunk chunk = chunkForWrite(cache, group.getLongKey());
            long[] incoming = group.getValue().toLongArray();
            Arrays.sort(incoming);
            int unique = 0;
            for (long entry : incoming) {
                if (unique == 0
                        || MultiblockCoreIndex.keyOf(incoming[unique - 1])
                                != MultiblockCoreIndex.keyOf(entry)) {
                    incoming[unique++] = entry;
                }
            }
            chunk.entries = merge(chunk.entries, incoming, unique);
        }
    }

    private static long[] merge(long[] old, long[] incoming, int count) {
        int added = 0;
        boolean changed = false;
        for (int j = 0; j < count; j++) {
            long entry = incoming[j];
            int key = MultiblockCoreIndex.keyOf(entry);
            int at = indexOf(old, key);
            if (at < 0) added++;
            else if (old[at] != entry) changed = true;
        }
        if (added == 0 && !changed) return old;
        long[] result = new long[old.length + added];
        int i = 0, j = 0, w = 0;
        while (i < old.length || j < count) {
            if (j == count) {
                result[w++] = old[i++];
            } else if (i == old.length) {
                result[w++] = incoming[j++];
            } else {
                int oldKey = MultiblockCoreIndex.keyOf(old[i]);
                int newKey = MultiblockCoreIndex.keyOf(incoming[j]);
                if (oldKey < newKey) result[w++] = old[i++];
                else {
                    result[w++] = incoming[j++];
                    if (oldKey == newKey) i++;
                }
            }
        }
        assert w == result.length;
        return result;
    }

    public static void evictChunk(Level level, long chunkKey) {
        Cache cache = active;
        if (cache == null || cache.level != level) return;
        if (cache.chunks.containsKey(chunkKey)) {
            Long2ObjectOpenHashMap<Chunk> next = cache.chunks.clone();
            next.remove(chunkKey);
            cache.chunks = next;
        }
        cache.requestEpoch++;
        cache.requested.removeIf((long key) -> chunkOf(key) == chunkKey);
    }

    public static void sync(ServerLevel level, BlockPos pos, Fluid fluid) {
        PacketWire.sendToAllTracking(
                FluidPipeTintLayout.encode(pos, BuiltInRegistries.FLUID.getId(fluid)),
                new TargetPoint(
                        level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D));
    }

    public static void sync(ServerLevel level, long[] posKeys, Fluid fluid) {
        if (posKeys.length == 0) return;
        Long2ObjectOpenHashMap<LongArrayList> byChunk = new Long2ObjectOpenHashMap<>();
        for (long posKey : posKeys) {
            byChunk.computeIfAbsent(chunkOf(posKey), k -> new LongArrayList()).add(posKey);
        }
        for (var entry : byChunk.long2ObjectEntrySet()) {
            List<ServerPlayer> watching =
                    level.getChunkSource()
                            .chunkMap
                            .getPlayers(ChunkPos.unpack(entry.getLongKey()), false);
            if (watching.isEmpty()) continue;
            PacketWire.sendToPlayers(
                    FluidPipeTintBulkLayout.encode(
                            BuiltInRegistries.FLUID.getId(fluid), entry.getValue()),
                    watching);
        }
    }

    public static void syncTo(ServerPlayer player, BlockPos pos, Fluid fluid) {
        PacketWire.sendTo(
                FluidPipeTintLayout.encode(pos, BuiltInRegistries.FLUID.getId(fluid)), player);
    }

    public static void syncTo(ServerPlayer player, LongList posKeys, Fluid fluid) {
        if (posKeys.isEmpty()) return;
        PacketWire.sendTo(
                FluidPipeTintBulkLayout.encode(BuiltInRegistries.FLUID.getId(fluid), posKeys),
                player);
    }

    public static void acceptClient(Player player, BlockPos pos, int fluidId) {
        Level level = player.level();
        Fluid fluid = fluidById(fluidId);
        set(level, pos.asLong(), fluid);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluidPipeBlock) {
            level.sendBlockUpdated(pos, state, state, 0);
        }
    }

    public static void acceptClient(
            Player player, int fluidId, @Nullable Object storage, long address, int count) {
        Level level = player.level();
        Fluid fluid = fluidById(fluidId);
        setAll(level, storage, address, count, fluid);
        LongOpenHashSet dirtySections = new LongOpenHashSet();
        for (int index = 0; index < count; index++) {
            BlockPos pos = BlockPos.of(U.getLong(storage, address + 8L * index));
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FluidPipeBlock
                    && dirtySections.add(SectionPos.asLong(pos))) {
                level.sendBlockUpdated(pos, state, state, 0);
            }
        }
    }

    private static Fluid fluidById(int fluidId) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (BuiltInRegistries.FLUID.getId(fluid) != fluidId) {
            throw new DecoderException("Unknown fluid registry id " + fluidId);
        }
        return fluid;
    }
}
