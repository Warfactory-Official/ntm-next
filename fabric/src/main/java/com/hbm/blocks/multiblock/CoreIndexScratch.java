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

package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class CoreIndexScratch {

    private static final ThreadLocal<CoreIndexScratch> LOCAL =
            ThreadLocal.withInitial(CoreIndexScratch::new);
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
