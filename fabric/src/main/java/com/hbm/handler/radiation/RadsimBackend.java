// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.internal.natives.RadsimBindings;
import com.hbm.lib.internal.natives.RadsimKeys;
import com.hbm.lib.internal.natives.RadsimWorld;
import java.util.Arrays;
import java.util.function.LongConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

final class RadsimBackend implements AutoCloseable {

    private final ServerLevel level;
    private final RadsimWorld world;
    private final int minSectionY;
    private final int sectionsPerChunk;

    private final long[] maskScratch;
    private final long[] keyScratch;

    private int epoch;

    RadsimBackend(ServerLevel level, int threads) {
        this.level = level;
        this.minSectionY = level.getMinSectionY();
        this.sectionsPerChunk = level.getSectionsCount();
        RadiationSettings.Resolved settings = RadiationSettings.forLevel(level).resolve();
        this.world =
                new RadsimWorld(
                        level.dimension().hashCode(),
                        level.getSeed(),
                        settings.minBound(),
                        threads,
                        sectionsPerChunk);
        RadiationSystemNT.applyBackendParams(world, settings);

        world.setFeatureFlags(
                RadiationDiffusivity.transportEnabled(level)
                        ? RadsimWorld.FEATURE_DIFFUSIVITY_TRANSPORT
                        : 0);
        this.maskScratch = new long[sectionsPerChunk * RadsimWorld.MASK_WORDS_PER_SECTION];
        this.keyScratch = new long[sectionsPerChunk];
    }

    void chunkLoaded(LevelChunk chunk) {
        long ck = ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z());
        world.chunkLoaded(ck);

        LevelChunkSection[] sections = chunk.getSections();
        Arrays.fill(maskScratch, 0L);
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            keyScratch[slot] = RadsimKeys.sectionKey(chunk.getPos().x(), slot, chunk.getPos().z());
            RadiationSystemNT.SectionMask mask =
                    slot < sections.length
                            ? RadiationSystemNT.scanResistantMask(sections[slot])
                            : null;
            if (mask == null) continue;
            System.arraycopy(
                    mask.words,
                    0,
                    maskScratch,
                    slot * RadsimWorld.MASK_WORDS_PER_SECTION,
                    RadsimWorld.MASK_WORDS_PER_SECTION);
        }
        world.submitDirtySections(keyScratch, maskScratch, sectionsPerChunk);
    }

    void submitSectionGeometry(LevelChunk chunk, int baseSlot, int slotMask) {
        if (slotMask == 0) return;
        LevelChunkSection[] sections = chunk.getSections();
        final int cx = chunk.getPos().x();
        final int cz = chunk.getPos().z();
        int count = 0;
        for (int m = slotMask; m != 0; m &= (m - 1)) {
            int slot = baseSlot + Integer.numberOfTrailingZeros(m);
            if (slot >= sectionsPerChunk) continue;

            int base = count * RadsimWorld.MASK_WORDS_PER_SECTION;
            Arrays.fill(maskScratch, base, base + RadsimWorld.MASK_WORDS_PER_SECTION, 0L);
            keyScratch[count] = RadsimKeys.sectionKey(cx, slot, cz);
            RadiationSystemNT.SectionMask mask =
                    slot < sections.length
                            ? RadiationSystemNT.scanResistantMask(sections[slot])
                            : null;
            if (mask != null) {
                System.arraycopy(
                        mask.words, 0, maskScratch, base, RadsimWorld.MASK_WORDS_PER_SECTION);
            }
            count++;
        }
        if (count > 0) world.submitDirtySections(keyScratch, maskScratch, count);
    }

    void submitSectionSources(
            long[] keys,
            int[] entryCounts,
            short[] pockets,
            int[] multiplicities,
            double[] emissions,
            double[] saturations,
            int sectionCount,
            int entryTotal) {
        world.submitSectionSources(
                keys,
                entryCounts,
                pockets,
                multiplicities,
                emissions,
                saturations,
                sectionCount,
                entryTotal);
    }

    void submitSectionDiffusivity(
            long[] keys, int[] pocketCounts, float[] values, int sectionCount, int valueTotal) {
        world.submitSectionDiffusivity(keys, pocketCounts, values, sectionCount, valueTotal);
    }

    int sectionSourceCount(long sectionKey) {
        return RadsimBindings.sectionSourceCount(world.handle(), sectionKey);
    }

    void chunkUnloaded(int cx, int cz) {
        world.chunkUnloaded(ChunkPos.pack(cx, cz));
    }

    void chunkRemoved(int cx, int cz) {
        world.chunkRemoved(ChunkPos.pack(cx, cz));
    }

    public void debugKernelFlags(int flags) {
        world.debugKernelFlags(flags);
    }

    void emitRad(BlockPos pos, double emission, double saturation) {
        int slot = RadsimKeys.slotOf(SectionPos.blockToSectionCoord(pos.getY()), minSectionY);
        if (slot < 0 || slot >= sectionsPerChunk) return;
        world.emitRad(
                RadsimKeys.sectionKey(
                        SectionPos.blockToSectionCoord(pos.getX()),
                        slot,
                        SectionPos.blockToSectionCoord(pos.getZ())),
                localIndex(pos),
                emission,
                saturation);
    }

    void setRad(BlockPos pos, double value) {
        int slot = RadsimKeys.slotOf(SectionPos.blockToSectionCoord(pos.getY()), minSectionY);
        if (slot < 0 || slot >= sectionsPerChunk) return;
        world.setRad(
                RadsimKeys.sectionKey(
                        SectionPos.blockToSectionCoord(pos.getX()),
                        slot,
                        SectionPos.blockToSectionCoord(pos.getZ())),
                localIndex(pos),
                value);
    }

    void addRad(BlockPos pos, double amount) {
        int slot = RadsimKeys.slotOf(SectionPos.blockToSectionCoord(pos.getY()), minSectionY);
        if (slot < 0 || slot >= sectionsPerChunk) return;
        long key =
                RadsimKeys.sectionKey(
                        SectionPos.blockToSectionCoord(pos.getX()),
                        slot,
                        SectionPos.blockToSectionCoord(pos.getZ()));
        world.addRad(key, localIndex(pos), amount);
    }

    double getRad(BlockPos pos) {
        int slot = RadsimKeys.slotOf(SectionPos.blockToSectionCoord(pos.getY()), minSectionY);
        if (slot < 0 || slot >= sectionsPerChunk) return 0.0D;
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        long ck = ChunkPos.pack(cx, cz);
        int chunkId = world.chunkId(ck);
        if (chunkId < 0) return 0.0D;

        int sectionId = world.tables().sectionId(chunkId, slot);
        if (sectionId < 0) return 0.0D;
        byte kind = world.tables().sectionKind(sectionId);
        if (kind == RadsimTablesKinds.NONE) return 0.0D;
        if (kind == RadsimTablesKinds.UNIFORM) return world.tables().uniformDensity(sectionId);

        return world.localDensity(RadsimKeys.sectionKey(cx, slot, cz), localIndex(pos));
    }

    int sectionId(long ck, int slot) {
        int chunkId = world.chunkId(ck);
        return chunkId < 0 ? -1 : world.tables().sectionId(chunkId, slot);
    }

    double uniformDensity(int sectionId) {
        return world.tables().uniformDensity(sectionId);
    }

    boolean sectionActive(int sectionId) {
        return world.tables().sectionActive(sectionId);
    }

    double pocketDensity(long ck, int slot, int local) {
        return world.localDensity(
                RadsimKeys.sectionKey(ChunkPos.getX(ck), slot, ChunkPos.getZ(ck)), local);
    }

    int dumpChunk(long ck, int[] sypi, double[] density) {
        return world.dumpChunkEntries(ck, sypi, density);
    }

    void loadChunk(long ck, int count, int[] sypi, double[] density) {
        world.loadChunkEntries(ck, count, sypi, density);
    }

    int step(long epochSalt, int epoch, LongConsumer fog, LongConsumer destroy) {
        int bytes = step(epochSalt, epoch);
        world.drainEvents(bytes, fog, destroy);
        return bytes;
    }

    int step(long epochSalt, int epoch) {
        return world.step(epochSalt, epoch, 0);
    }

    int step() {
        return step(level.getSeed(), epoch++);
    }

    long[] lastStepProfileNanos() {
        return world.lastStepProfileNanos();
    }

    @Override
    public void close() {
        world.close();
    }

    private static int localIndex(BlockPos pos) {
        return ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
    }

    static final class RadsimTablesKinds {
        static final byte NONE = 0;
        static final byte UNIFORM = 1;
        static final byte SINGLE = 2;
        static final byte MULTI = 3;

        private RadsimTablesKinds() {}
    }
}
