// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class CoreIndexScratch {

    private static final ThreadLocal<CoreIndexScratch> LOCAL =
            ThreadLocal.withInitial(CoreIndexScratch::new);
    private static final ThreadLocal<BlockPos.MutableBlockPos> BOX_CURSOR =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    public final BlockPos.MutableBlockPos cellPos = new BlockPos.MutableBlockPos();
    public final BlockPos.MutableBlockPos notifyPos = new BlockPos.MutableBlockPos();
    public long[] cells = new long[64];
    public int cellCount;
    public long[] chunkKeys = new long[8];
    public int chunkCount;
    public long[] packed = new long[64];
    private boolean inUse;

    public static CoreIndexScratch borrow() {
        CoreIndexScratch scratch = LOCAL.get();
        if (scratch.inUse) return new CoreIndexScratch();
        scratch.inUse = true;
        return scratch;
    }

    public static BlockPos.MutableBlockPos boxCursor() {
        return BOX_CURSOR.get();
    }

    private static long[] grow(long[] array) {
        long[] out = new long[array.length * 3 / 2 + 8];
        System.arraycopy(array, 0, out, 0, array.length);
        return out;
    }

    public void release() {
        this.inUse = false;
        this.cellCount = 0;
        this.chunkCount = 0;
    }

    public void addCell(BlockPos pos) {
        if (cellCount == cells.length) cells = grow(cells);
        cells[cellCount++] = pos.asLong();
        long key = ChunkPos.pack(pos);
        for (int i = 0; i < chunkCount; i++) {
            if (chunkKeys[i] == key) return;
        }
        if (chunkCount == chunkKeys.length) chunkKeys = grow(chunkKeys);
        chunkKeys[chunkCount++] = key;
    }

    public void ensurePacked(int needed) {
        if (packed.length < needed) packed = new long[Math.max(needed, packed.length * 3 / 2 + 8)];
    }
}
