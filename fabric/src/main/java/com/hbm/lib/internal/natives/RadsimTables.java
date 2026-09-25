// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import com.hbm.lib.internal.UnsafeHolder;

public final class RadsimTables {

    public static final int BUF_SECTION_UNIFORM_DENSITY = 0;
    public static final int BUF_SECTION_KIND = 1;
    public static final int BUF_SECTION_ACTIVE = 2;
    public static final int BUF_CHUNK_SECTION_ID_BY_SY = 3;
    public static final int BUF_CHUNK_KINDS = 4;
    public static final int BUF_CHUNK_ACTIVE_MASK = 5;

    private static final int BUF_COUNT = 6;

    public static final byte KIND_NONE = 0;

    private final long handle;
    private final int sectionsPerChunk;
    private final int wordsPerChunk;

    private final long[] address = new long[BUF_COUNT];
    private final long[] count = new long[BUF_COUNT];
    private int generation = Integer.MIN_VALUE;

    public RadsimTables(long handle) {
        this.handle = handle;
        this.sectionsPerChunk = RadsimBindings.worldSectionsPerChunk(handle);
        this.wordsPerChunk = (sectionsPerChunk + 31) >>> 5;
        refresh();
    }

    public int sectionsPerChunk() {
        return sectionsPerChunk;
    }

    public void refresh() {
        int current = RadsimBindings.bufferGeneration(handle);
        if (current == generation) return;
        for (int which = 0; which < BUF_COUNT; which++) {
            address[which] = RadsimBindings.bufferPtr(handle, which);
            count[which] = RadsimBindings.bufferCount(handle, which);
        }
        generation = current;
    }

    public int sectionId(int chunkId, int sy) {
        if (chunkId < 0 || sy < 0 || sy >= sectionsPerChunk) return -1;
        long index = (long) chunkId * sectionsPerChunk + sy;
        if (index < 0 || index >= count[BUF_CHUNK_SECTION_ID_BY_SY]) return -1;
        return UnsafeHolder.U.getInt(address[BUF_CHUNK_SECTION_ID_BY_SY] + (index << 2));
    }

    public double uniformDensity(int sectionId) {
        if (sectionId < 0 || sectionId >= count[BUF_SECTION_UNIFORM_DENSITY]) return 0.0D;
        return UnsafeHolder.U.getDouble(
                address[BUF_SECTION_UNIFORM_DENSITY] + ((long) sectionId << 3));
    }

    public byte sectionKind(int sectionId) {
        if (sectionId < 0 || sectionId >= count[BUF_SECTION_KIND]) return KIND_NONE;
        return UnsafeHolder.U.getByte(address[BUF_SECTION_KIND] + sectionId);
    }

    public boolean sectionActive(int sectionId) {
        if (sectionId < 0 || sectionId >= count[BUF_SECTION_ACTIVE]) return false;
        return UnsafeHolder.U.getByte(address[BUF_SECTION_ACTIVE] + sectionId) != 0;
    }

    public long chunkKinds(int chunkId, int word) {
        long index = (long) chunkId * wordsPerChunk + word;
        if (chunkId < 0 || word < 0 || word >= wordsPerChunk || index >= count[BUF_CHUNK_KINDS])
            return 0L;
        return UnsafeHolder.U.getLong(address[BUF_CHUNK_KINDS] + (index << 3));
    }

    public int chunkActiveMask(int chunkId, int word) {
        long index = (long) chunkId * wordsPerChunk + word;
        if (chunkId < 0
                || word < 0
                || word >= wordsPerChunk
                || index >= count[BUF_CHUNK_ACTIVE_MASK]) return 0;
        return UnsafeHolder.U.getInt(address[BUF_CHUNK_ACTIVE_MASK] + (index << 2));
    }

    public double density(int chunkId, int sy) {
        int id = sectionId(chunkId, sy);
        if (id < 0 || sectionKind(id) == KIND_NONE) return 0.0D;
        return uniformDensity(id);
    }

    public long sectionCapacity() {
        return count[BUF_SECTION_UNIFORM_DENSITY];
    }

    public long chunkCapacity() {
        return count[BUF_CHUNK_KINDS] / wordsPerChunk;
    }
}
