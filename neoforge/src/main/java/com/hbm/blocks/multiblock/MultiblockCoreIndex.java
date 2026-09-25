// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class MultiblockCoreIndex {

    public static final long NO_ENTRY = Long.MIN_VALUE;
    private static final ThreadLocal<RemovalBatch> REMOVALS = new ThreadLocal<>();

    private static final int Y_INDEX_BITS = 12;

    private static final int DX_BITS = 13;

    private static final int DY_BITS = Y_INDEX_BITS + 1;
    private static final int DZ_BITS = DX_BITS;
    private static final int DELTA_BITS = DX_BITS + DY_BITS + DZ_BITS;

    private static final int KEY_MASK = (1 << (8 + Y_INDEX_BITS)) - 1;
    public static final int MAX_INDEXABLE_HEIGHT = 1 << Y_INDEX_BITS;

    private MultiblockCoreIndex() {}

    public static int key(LevelHeightAccessor level, int x, int y, int z) {
        int yIndex = y - level.getMinY();
        if (yIndex < 0 || yIndex >= MAX_INDEXABLE_HEIGHT) {
            throw new IllegalStateException(
                    "multiblock core index: y "
                            + y
                            + " is outside the indexable window ["
                            + level.getMinY()
                            + ", "
                            + (level.getMinY() + MAX_INDEXABLE_HEIGHT)
                            + "); a dimension taller than "
                            + MAX_INDEXABLE_HEIGHT
                            + " cannot be indexed");
        }
        return ((x & 15) << (4 + Y_INDEX_BITS)) | ((z & 15) << Y_INDEX_BITS) | yIndex;
    }

    public static void checkDimension(LevelHeightAccessor level) {
        if (level.getHeight() > MAX_INDEXABLE_HEIGHT) {
            throw new IllegalStateException(
                    "multiblock core index: dimension height "
                            + level.getHeight()
                            + " exceeds the indexable "
                            + MAX_INDEXABLE_HEIGHT
                            + "; widen MultiblockCoreIndex.Y_INDEX_BITS to support it");
        }
    }

    public static long pack(int key, int dx, int dy, int dz) {
        checkRange("dx", dx, DX_BITS);
        checkRange("dy", dy, DY_BITS);
        checkRange("dz", dz, DZ_BITS);
        long delta =
                ((long) (dz & mask(DZ_BITS)) << (DY_BITS + DX_BITS))
                        | ((long) (dy & mask(DY_BITS)) << DX_BITS)
                        | (dx & mask(DX_BITS));
        return ((long) key << DELTA_BITS) | delta;
    }

    private static int mask(int bits) {
        return (1 << bits) - 1;
    }

    private static void checkRange(String name, int value, int bits) {
        int limit = 1 << (bits - 1);
        if (value < -limit || value >= limit) {
            throw new IllegalStateException(
                    "multiblock core index: "
                            + name
                            + " = "
                            + value
                            + " does not fit "
                            + bits
                            + " signed bits ["
                            + (-limit)
                            + ", "
                            + (limit - 1)
                            + "]");
        }
    }

    private static int signExtend(int value, int bits) {
        int shift = Integer.SIZE - bits;
        return (value << shift) >> shift;
    }

    public static int keyOf(long entry) {
        return (int) (entry >>> DELTA_BITS) & KEY_MASK;
    }

    public static int dxOf(long entry) {
        return signExtend((int) entry & mask(DX_BITS), DX_BITS);
    }

    public static int dyOf(long entry) {
        return signExtend((int) (entry >>> DX_BITS) & mask(DY_BITS), DY_BITS);
    }

    public static int dzOf(long entry) {
        return signExtend((int) (entry >>> (DX_BITS + DY_BITS)) & mask(DZ_BITS), DZ_BITS);
    }

    public static long cellPacked(ChunkPos chunk, LevelHeightAccessor level, long entry) {
        int key = keyOf(entry);
        return BlockPos.asLong(
                chunk.getMinBlockX() + (key >>> (4 + Y_INDEX_BITS) & 15),
                level.getMinY() + (key & mask(Y_INDEX_BITS)),
                chunk.getMinBlockZ() + (key >>> Y_INDEX_BITS & 15));
    }

    public static long corePacked(long entry, int x, int y, int z) {
        return BlockPos.asLong(x + dxOf(entry), y + dyOf(entry), z + dzOf(entry));
    }

    public static boolean coreInSameChunk(int x, int z, long entry) {
        return SectionPos.blockToSectionCoord(x + dxOf(entry)) == SectionPos.blockToSectionCoord(x)
                && SectionPos.blockToSectionCoord(z + dzOf(entry))
                        == SectionPos.blockToSectionCoord(z);
    }

    private static int indexOf(long[] sorted, int length, int key) {
        int lo = 0;
        int hi = length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midKey = keyOf(sorted[mid]);
            if (midKey < key) lo = mid + 1;
            else if (midKey > key) hi = mid - 1;
            else return mid;
        }
        return -1;
    }

    public static long lookup(ChunkAccess chunk, LevelHeightAccessor level, int x, int y, int z) {
        long[] entries = chunk.hbm$coreIndex();
        if (entries == null) return NO_ENTRY;
        int at = indexOf(entries, chunk.hbm$coreIndexSize(), key(level, x, y, z));
        return at < 0 || entries[at] < 0 ? NO_ENTRY : entries[at];
    }

    public static int size(ChunkAccess chunk) {
        int size = chunk.hbm$coreIndexSize();
        for (RemovalBatch batch = REMOVALS.get(); batch != null; batch = batch.previous)
            size -= batch.removed(chunk);
        return size;
    }

    public static BlockPos coreOf(
            ChunkAccess chunk, LevelHeightAccessor level, int x, int y, int z) {
        long e = lookup(chunk, level, x, y, z);
        return e == NO_ENTRY ? null : BlockPos.of(corePacked(e, x, y, z));
    }

    public static void insert(ChunkAccess chunk, long[] packed, int count) {
        if (count == 0) return;
        Arrays.sort(packed, 0, count);

        long[] entries = chunk.hbm$coreIndex();
        int size = entries == null ? 0 : chunk.hbm$coreIndexSize();

        int w = 0;
        for (int r = 0; r < size; r++) {
            if (entries[r] >= 0 && indexOf(packed, count, keyOf(entries[r])) < 0)
                entries[w++] = entries[r];
        }
        size = w;

        if (entries == null || entries.length < size + count) {
            int grown = Math.max(size + count, (entries == null ? 0 : entries.length) * 3 / 2 + 8);
            entries = entries == null ? new long[grown] : Arrays.copyOf(entries, grown);
        }
        System.arraycopy(packed, 0, entries, size, count);
        size += count;
        Arrays.sort(entries, 0, size);
        chunk.hbm$setCoreIndex(entries, dedupeKeepingLast(entries, size));
        resetRemovals(chunk);
    }

    public static void remove(ChunkAccess chunk, int key) {
        long[] entries = chunk.hbm$coreIndex();
        if (entries == null) return;
        int size = chunk.hbm$coreIndexSize();
        int at = indexOf(entries, size, key);
        if (at < 0 || entries[at] < 0) return;
        RemovalBatch batch = REMOVALS.get();
        if (batch != null) {

            entries[at] |= Long.MIN_VALUE;
            batch.record(chunk);
            return;
        }
        System.arraycopy(entries, at + 1, entries, at, size - at - 1);
        chunk.hbm$setCoreIndex(size == 1 ? null : entries, size - 1);
    }

    public static RemovalBatch removalBatch() {
        RemovalBatch batch = new RemovalBatch(REMOVALS.get());
        REMOVALS.set(batch);
        return batch;
    }

    public static long[] copyForSave(ChunkAccess chunk) {
        long[] entries = chunk.hbm$coreIndex();
        if (entries == null) return null;
        int count = chunk.hbm$coreIndexSize();
        long[] copy = new long[count];
        int write = 0;
        for (int i = 0; i < count; i++) if (entries[i] >= 0) copy[write++] = entries[i];
        return write == 0 ? null : write == count ? copy : Arrays.copyOf(copy, write);
    }

    private static void resetRemovals(ChunkAccess chunk) {
        for (RemovalBatch batch = REMOVALS.get(); batch != null; batch = batch.previous) {
            for (int i = 0; i < batch.count; i++)
                if (batch.chunks[i] == chunk) batch.removed[i] = 0;
        }
    }

    public static final class RemovalBatch implements AutoCloseable {
        private final RemovalBatch previous;
        private ChunkAccess[] chunks = new ChunkAccess[4];
        private int[] removed = new int[4];
        private int count;

        private RemovalBatch(RemovalBatch previous) {
            this.previous = previous;
        }

        private void record(ChunkAccess chunk) {
            for (int i = 0; i < count; i++) {
                if (chunks[i] == chunk) {
                    removed[i]++;
                    return;
                }
            }
            if (count == chunks.length) {
                chunks = Arrays.copyOf(chunks, count * 2);
                removed = Arrays.copyOf(removed, count * 2);
            }
            chunks[count] = chunk;
            removed[count++] = 1;
        }

        private int removed(ChunkAccess chunk) {
            for (int i = 0; i < count; i++) if (chunks[i] == chunk) return removed[i];
            return 0;
        }

        @Override
        public void close() {
            assert REMOVALS.get() == this;
            try {
                for (int c = 0; c < count; c++) {
                    if (removed[c] == 0) continue;
                    ChunkAccess chunk = chunks[c];
                    long[] entries = chunk.hbm$coreIndex();
                    int write = 0;
                    for (int read = 0, size = chunk.hbm$coreIndexSize(); read < size; read++) {
                        if (entries[read] >= 0) entries[write++] = entries[read];
                    }
                    chunk.hbm$setCoreIndex(write == 0 ? null : entries, write);
                    resetRemovals(chunk);
                }
            } finally {
                REMOVALS.set(previous);
            }
        }
    }

    private static int dedupeKeepingLast(long[] sorted, int length) {
        int w = 0;
        for (int r = 0; r < length; r++) {
            if (w > 0 && keyOf(sorted[w - 1]) == keyOf(sorted[r])) w--;
            sorted[w++] = sorted[r];
        }
        return w;
    }
}
