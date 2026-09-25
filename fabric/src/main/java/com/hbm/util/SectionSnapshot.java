// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class SectionSnapshot {
    private final LevelChunk chunk;
    private final LevelChunkSection[] original;
    private final long[] generations;
    private final long observed;
    public final LevelChunkSection[] sections;
    private long changed;

    private SectionSnapshot(LevelChunk chunk, long observed) {
        this.chunk = chunk;
        this.observed = observed;
        LevelChunkSection[] live = chunk.getSections();
        this.original = new LevelChunkSection[live.length];
        this.generations = new long[live.length];
        this.sections = new LevelChunkSection[live.length];
        for (long mask = observed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            LevelChunkSection source = live[index];
            original[index] = source;
            generations[index] = SectionGeneration.generation(source);
            sections[index] = source.copy();
        }
    }

    static long representable(int sectionCount) {
        if (sectionCount > Long.SIZE) {
            throw new IllegalStateException(
                    "dimension has "
                            + sectionCount
                            + " sections; bulk section edits span "
                            + Long.SIZE);
        }
        return sectionCount == Long.SIZE ? -1L : (1L << sectionCount) - 1;
    }

    public static SectionSnapshot capture(LevelChunk chunk, long observed) {
        assert ((ServerLevel) chunk.getLevel()).getServer().isSameThread();
        return new SectionSnapshot(chunk, observed & representable(chunk.getSections().length));
    }

    public void changed(int index) {
        assert (observed & (1L << index)) != 0;
        changed |= 1L << index;
    }

    public LevelChunk chunk() {
        return chunk;
    }

    public boolean publish() {
        ServerLevel level = (ServerLevel) chunk.getLevel();
        assert level.getServer().isSameThread();
        if (ChunkUtil.liveChunkNow(level, chunk.getPos().pack()) != chunk) return false;
        LevelChunkSection[] live = chunk.getSections();
        for (long mask = observed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            if (live[index] != original[index]
                    || SectionGeneration.generation(live[index]) != generations[index]) {
                return false;
            }
        }

        for (long mask = changed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            boolean replaced =
                    ChunkUtil.casSectionAt(original[index], sections[index], live, index);
            assert replaced;
        }
        for (long mask = changed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            SectionMetadata.replaced(
                    level,
                    SectionPos.asLong(
                            chunk.getPos().x(), level.getMinSectionY() + index, chunk.getPos().z()),
                    sections[index]);
        }
        if (changed != 0) chunk.markUnsaved();
        return true;
    }
}
