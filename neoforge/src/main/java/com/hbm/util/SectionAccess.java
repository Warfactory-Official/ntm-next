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

package com.hbm.util;

import com.hbm.lib.internal.UnsafeHolder;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class SectionAccess {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private SectionAccess() {}

    public static @Nullable LevelChunkSection getVolatile(LevelChunkSection[] sections, int index) {
        return (LevelChunkSection)
                U.getReferenceVolatile(sections, UnsafeHolder.offReference(index));
    }

    public static boolean compareAndSet(
            LevelChunkSection[] sections,
            int index,
            @Nullable LevelChunkSection expected,
            @Nullable LevelChunkSection replacement) {
        return U.compareAndSetReference(
                sections, UnsafeHolder.offReference(index), expected, replacement);
    }

    public static BlockState read(
            LevelChunkSection[] sections, int minSectionY, int x, int y, int z) {
        int index = (y >> 4) - minSectionY;
        if (index < 0 || index >= sections.length) return AIR;
        var section = sections[index];
        return section.hasOnlyAir() ? AIR : section.getBlockState(x & 15, y & 15, z & 15);
    }

    public static long emptyMask(LevelChunkSection[] sections) {
        long mask = 0;
        for (int index = 0, count = Math.min(sections.length, Long.SIZE); index < count; index++) {
            if (sections[index].hasOnlyAir()) mask |= 1L << index;
        }
        return mask;
    }

    public static final class Cursor {
        private LevelChunkSection[] sections;
        private int minSectionY;

        public void bind(ChunkAccess chunk) {
            bind(chunk.getSections(), chunk.getMinSectionY());
        }

        public void bind(LevelChunkSection[] sections, int minSectionY) {
            this.sections = sections;
            this.minSectionY = minSectionY;
        }

        public BlockState read(int x, int y, int z) {
            return SectionAccess.read(sections, minSectionY, x, y, z);
        }
    }

    public abstract static class Reader {
        private static final int CACHE_BITS = 8;
        private final int[] cachedX = new int[1 << CACHE_BITS];
        private final int[] cachedZ = new int[1 << CACHE_BITS];
        private final @Nullable ChunkAccess[] cached = new ChunkAccess[1 << CACHE_BITS];
        private @Nullable ChunkAccess chunk;
        private int chunkX;
        private int chunkZ;
        private LevelChunkSection @Nullable [] sections;
        private int minSectionY;
        private int minY;
        private int maxY;

        protected abstract @Nullable ChunkAccess resolve(int chunkX, int chunkZ);

        public final @Nullable ChunkAccess chunkAt(int x, int z) {
            int cx = x >> 4;
            int cz = z >> 4;
            return chunk != null && cx == chunkX && cz == chunkZ ? chunk : move(cx, cz);
        }

        private @Nullable ChunkAccess move(int x, int z) {
            int slot = (x * 0x9E3779B9 ^ z * 0x85EBCA6B) >>> (32 - CACHE_BITS);
            var resolved = cached[slot];
            if (resolved == null || cachedX[slot] != x || cachedZ[slot] != z) {
                resolved = resolve(x, z);
                if (resolved == null) return null;
                cached[slot] = resolved;
                cachedX[slot] = x;
                cachedZ[slot] = z;
            }
            chunk = resolved;
            chunkX = x;
            chunkZ = z;
            sections = resolved.getSections();
            minSectionY = resolved.getMinSectionY();
            minY = resolved.getMinY();
            maxY = resolved.getMaxY();
            return resolved;
        }

        public final @Nullable BlockState read(int x, int y, int z) {
            var current = chunkAt(x, z);
            if (current == null) return null;
            if (y < minY || y > maxY) return current.getBlockState(new BlockPos(x, y, z));
            return SectionAccess.read(sections, minSectionY, x, y, z);
        }

        public final @Nullable BlockState read(BlockPos pos) {
            return read(pos.getX(), pos.getY(), pos.getZ());
        }

        public final void clear() {
            chunk = null;
            sections = null;
            Arrays.fill(cached, null);
        }
    }

    public static final class WorldgenReader extends Reader {
        private final WorldGenLevel level;

        public WorldgenReader(WorldGenLevel level) {
            this.level = level;
        }

        @Override
        protected @Nullable ChunkAccess resolve(int chunkX, int chunkZ) {
            return level.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
        }
    }

    public static final class LoadedReader extends Reader {
        private final Level level;

        public LoadedReader(Level level) {
            this.level = level;
        }

        @Override
        protected @Nullable ChunkAccess resolve(int chunkX, int chunkZ) {
            return ChunkUtil.chunkIfLoaded(level, chunkX, chunkZ);
        }
    }

    public static final class LoadingReader extends Reader {
        private final Level level;

        public LoadingReader(Level level) {
            this.level = level;
        }

        @Override
        protected ChunkAccess resolve(int chunkX, int chunkZ) {
            return level.getChunk(chunkX, chunkZ);
        }
    }

    public static class PaletteView {
        private @Nullable LevelChunkSection section;
        protected @Nullable BitStorage storage;
        protected @Nullable Palette<BlockState> pal;
        protected boolean single;
        protected @Nullable BlockState singleState;

        public final boolean matches(LevelChunkSection section) {
            return this.section == section;
        }

        public final void clear() {
            section = null;
        }

        public final void capture(LevelChunkSection section) {
            PalettedContainer.Data<BlockState> data = section.getStates().data;
            storage = data.storage();
            pal = data.palette();
            single = storage.getBits() == 0 || pal instanceof SingleValuePalette<?>;
            singleState = single ? pal.valueFor(0) : null;
            this.section = section;
        }

        public final BlockState stateAt(int x, int y, int z) {
            return single ? singleState : pal.valueFor(storage.get(LocalBlockIndex.pack(x, y, z)));
        }
    }
}
