// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import java.util.function.LongConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public interface RadiationBackend extends AutoCloseable {
    static long sectionKey(int sectionX, int slot, int sectionZ) {
        return ((long) (sectionX & 0x3FFFFF) << 42)
                | ((long) (sectionZ & 0x3FFFFF) << 20)
                | (slot & 0xFFFFFL);
    }

    void chunkLoaded(LevelChunk chunk);

    void submitSectionGeometry(LevelChunk chunk, int baseSlot, int slotMask);

    void submitSectionSources(
            long[] keys,
            int[] entryCounts,
            short[] pockets,
            int[] multiplicities,
            double[] emissions,
            double[] saturations,
            int sectionCount,
            int entryTotal);

    void submitSectionDiffusivity(
            long[] keys, int[] pocketCounts, float[] values, int sectionCount, int valueTotal);

    void chunkUnloaded(int cx, int cz);

    void emitRad(BlockPos pos, double emission, double saturation);

    void addRad(BlockPos pos, double amount);

    void setRad(BlockPos pos, double value);

    double getRad(BlockPos pos);

    int sectionId(long ck, int slot);

    double uniformDensity(int sectionId);

    boolean sectionActive(int sectionId);

    double pocketDensity(long ck, int slot, int local);

    int dumpChunk(long ck, int[] sypi, double[] density);

    void loadChunk(long ck, int count, int[] sypi, double[] density);

    int step(long epochSalt, int epoch, LongConsumer fog, LongConsumer destroy);

    @Override
    void close();
}
