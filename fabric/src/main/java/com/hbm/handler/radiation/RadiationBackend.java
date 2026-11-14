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

    int step(long epochSalt, int epoch, LongConsumer fog, LongConsumer destroy, LongConsumer dirty);

    void ackChunkDirty(long ck);

    @Override
    void close();
}
