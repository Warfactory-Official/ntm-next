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

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class AssemblyPattern {
    private final long[] offsets;
    private final long[] sorted;
    private final int[] indices;
    private final Block[] blocks;
    private final Block[] alternatives;
    private final int[] variants;
    private final int[] missing;
    private final boolean rotatable;

    private AssemblyPattern(Builder builder) {
        int count = builder.cells.size();
        offsets = new long[count];
        indices = new int[count];
        blocks = new Block[count];
        alternatives = new Block[count];
        variants = new int[count];
        missing = new int[builder.variantCount];
        rotatable = builder.rotatable;
        for (int i = 0; i < count; i++) {
            Cell cell = builder.cells.get(i);
            offsets[i] = cell.offset;
            blocks[i] = cell.block;
            alternatives[i] = cell.alternative;
            variants[i] = cell.variants;
            if (cell.offset == 0) continue;
            for (int bits = cell.variants; bits != 0; bits &= bits - 1) {
                missing[Integer.numberOfTrailingZeros(bits)]++;
            }
        }
        sorted = offsets.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < count; i++) indices[Arrays.binarySearch(sorted, offsets[i])] = i;
    }

    public int size() {
        return offsets.length;
    }

    public long getOffset(int cell) {
        return offsets[cell];
    }

    public Block getBlock(int cell) {
        return blocks[cell];
    }

    public int getVariantMask(int cell) {
        return variants[cell];
    }

    public int getVariantCount() {
        return missing.length;
    }

    public boolean isRotatable() {
        return rotatable;
    }

    public int[] createMissingCounts() {
        return missing.clone();
    }

    public int findCell(long offset) {
        int index = Arrays.binarySearch(sorted, offset);
        return index < 0 ? -1 : indices[index];
    }

    public boolean matches(int cell, BlockState state) {
        Block block = state.getBlock();
        return block == blocks[cell] || block == alternatives[cell];
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(Block block, int x, int y, int z);
    }

    public static final class Builder {
        private final ArrayList<Cell> cells = new ArrayList<>();
        private final int variantCount;
        private final boolean rotatable;

        public Builder(int variantCount, boolean rotatable) {
            this.variantCount = variantCount;
            this.rotatable = rotatable;
        }

        public void add(Block block, int x, int y, int z) {
            add(block, null, (1 << variantCount) - 1, x, y, z);
        }

        public void add(
                Block block, @Nullable Block alternative, int variants, int x, int y, int z) {
            cells.add(new Cell(BlockPos.asLong(x, y, z), block, alternative, variants));
        }

        public AssemblyPattern build() {
            return new AssemblyPattern(this);
        }
    }

    private record Cell(long offset, Block block, @Nullable Block alternative, int variants) {}
}
