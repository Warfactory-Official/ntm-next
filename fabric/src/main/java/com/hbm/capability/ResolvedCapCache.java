// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.platform.Services;
import com.hbm.util.ChunkPositionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class ResolvedCapCache {

    private final Long2ObjectOpenHashMap<Bucket> byPos = new Long2ObjectOpenHashMap<>();
    private final ChunkPositionIndex byChunk = new ChunkPositionIndex();

    public static ResolvedCapCache of(ServerLevel level) {
        return level.hbm$resolvedCaps();
    }

    public static void init() {
        Services.SERVER.onChunkUnload(
                (level, chunk) -> of(level).onChunkUnload(chunk.getPos().pack()));
    }

    public void onChunkUnload(long chunkKey) {
        LongOpenHashSet positions = byChunk.take(chunkKey);
        if (positions == null) return;
        for (LongIterator it = positions.iterator(); it.hasNext(); ) byPos.remove(it.nextLong());
    }

    public <T> @Nullable T serve(
            BlockPos pos,
            BlockState state,
            Direction side,
            Class<T> type,
            NtmCapabilities.CapRole role,
            int passivePlane) {
        int slot = slot(side, role, passivePlane);
        if (slot < 0) return null;
        long posKey = pos.asLong();
        Bucket bucket = byPos.get(posKey);
        if (bucket == null) return null;
        Entry entry = bucket.find(slot);
        if (entry == null) return null;
        Object value = entry.value.get();
        BlockEntity anchor = entry.anchor.get();
        LevelChunk anchorChunk = entry.anchorChunk.get();
        if (value != null
                && anchor != null
                && anchorChunk != null
                && state == entry.queriedState
                && !anchor.isRemoved()
                && anchorChunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)
                && anchor.getBlockState() == entry.coreState) {
            return type.cast(value);
        }
        bucket.remove(slot);
        if (bucket.size == 0) {
            byPos.remove(posKey);
            byChunk.remove(posKey);
        }
        return null;
    }

    public <T> void arm(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Direction side,
            Class<T> type,
            NtmCapabilities.CapRole role,
            int passivePlane,
            T value,
            BlockEntity anchor) {
        int slot = slot(side, role, passivePlane);
        if (slot < 0) return;
        LevelChunk anchorChunk =
                BlockMultiblockCore.readableChunk(
                        level, anchor.getBlockPos().getX(), anchor.getBlockPos().getZ());
        if (anchorChunk == null) return;
        Entry entry = new Entry(slot, value, anchor, anchorChunk, anchor.getBlockState(), state);
        long posKey = pos.asLong();
        byPos.computeIfAbsent(posKey, k -> new Bucket()).put(entry);
        byChunk.add(posKey);
    }

    public boolean isArmed(BlockPos pos) {
        Bucket bucket = byPos.get(pos.asLong());
        return bucket != null && bucket.size > 0;
    }

    private static int slot(Direction side, NtmCapabilities.CapRole role, int passivePlane) {
        int typeIndex =
                switch (role) {
                    case POWER_IN -> 0;
                    case POWER_OUT -> 1;
                    case FLUID_IN -> 2;
                    case FLUID_OUT -> 3;
                };
        if (typeIndex < 0) return -1;
        int planeIndex =
                switch (passivePlane) {
                    case BlockMultiblockCore.PASSIVE_NONE -> 0;
                    case BlockMultiblockCore.PASSIVE_POWER_IN -> 1;
                    case BlockMultiblockCore.PASSIVE_FLUID_IN -> 2;
                    default -> -1;
                };
        if (planeIndex < 0) return -1;
        return side.ordinal() | (typeIndex << 3) | (planeIndex << 5);
    }

    private static final class Entry {

        final int slot;

        final WeakReference<Object> value;
        final WeakReference<BlockEntity> anchor;
        final WeakReference<LevelChunk> anchorChunk;
        final BlockState coreState;
        final BlockState queriedState;

        Entry(
                int slot,
                Object value,
                BlockEntity anchor,
                LevelChunk anchorChunk,
                BlockState coreState,
                BlockState queriedState) {
            this.slot = slot;
            this.value = new WeakReference<>(value);
            this.anchor = new WeakReference<>(anchor);
            this.anchorChunk = new WeakReference<>(anchorChunk);
            this.coreState = coreState;
            this.queriedState = queriedState;
        }
    }

    private static final class Bucket {

        private Entry[] entries = new Entry[2];
        private int size;

        @Nullable Entry find(int slot) {
            for (int i = 0; i < size; i++) {
                if (entries[i].slot == slot) return entries[i];
            }
            return null;
        }

        void put(Entry entry) {
            for (int i = 0; i < size; i++) {
                if (entries[i].slot == entry.slot) {
                    entries[i] = entry;
                    return;
                }
            }
            if (size == entries.length) {
                Entry[] grown = new Entry[entries.length * 2];
                System.arraycopy(entries, 0, grown, 0, size);
                entries = grown;
            }
            entries[size++] = entry;
        }

        void remove(int slot) {
            for (int i = 0; i < size; i++) {
                if (entries[i].slot == slot) {
                    entries[i] = entries[--size];
                    entries[size] = null;
                    return;
                }
            }
        }
    }
}
