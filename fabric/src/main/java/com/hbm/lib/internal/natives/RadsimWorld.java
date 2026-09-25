// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.LongConsumer;

public final class RadsimWorld implements AutoCloseable {

    private static final int EVENT_HEADER_BYTES = 16;

    public static final int MASK_WORDS_PER_SECTION = 64;

    private final Arena arena = Arena.ofShared();
    private final Long2IntOpenHashMap chunkIds = new Long2IntOpenHashMap();
    private final long handle;
    private final RadsimTables tables;

    private long droppedEventSteps;
    private long droppedEventBytes;
    private MemorySegment profile;
    private MemorySegment events;
    private int eventCapacity;
    private MemorySegment sourceKeys,
            sourceEntryCounts,
            sourcePockets,
            sourceMultiplicities,
            sourceEmissions,
            sourceSaturations;
    private int sourceSectionCapacity, sourceEntryCapacity;
    private MemorySegment diffKeys, diffPocketCounts, diffValues;
    private int diffSectionCapacity, diffValueCapacity;
    private MemorySegment edits;
    private long editCapacity;
    private MemorySegment dirtyKeys;
    private MemorySegment dirtyMasks;
    private int dirtyCapacity;
    private boolean closed;
    private long setSequence;

    private final ThreadLocal<EntryScratch> entryScratch =
            ThreadLocal.withInitial(EntryScratch::new);

    private static final class EntryScratch {
        MemorySegment sypi;
        MemorySegment density;
        int rows;
    }

    public RadsimWorld(
            int dimension, long seed, double minBound, int threads, int sectionsPerChunk) {
        RadsimBindings.ensureBound();
        RadsimBindings.configureRuntime(threads > 1, threads);
        this.handle = RadsimBindings.worldCreate(dimension, seed, minBound, sectionsPerChunk);
        if (handle == 0L) {
            throw new IllegalStateException(
                    "rad_world_create refused "
                            + sectionsPerChunk
                            + " sections per chunk; the backend holds at most "
                            + RadsimBindings.sectionsPerChunk());
        }
        this.chunkIds.defaultReturnValue(-1);
        this.tables = new RadsimTables(handle);
        this.eventCapacity = EVENT_HEADER_BYTES;
        this.events = arena.allocate(eventCapacity, Long.BYTES);
    }

    public long handle() {
        return handle;
    }

    public RadsimTables tables() {
        return tables;
    }

    public void setParams(
            double diffusionDt,
            double uniformExchange,
            double retentionDt,
            long fogProbU64,
            long destroyProbU64,
            double fogThreshold,
            double eps,
            double maxValue) {
        RadsimBindings.worldSetParams(
                handle,
                diffusionDt,
                uniformExchange,
                retentionDt,
                fogProbU64,
                destroyProbU64,
                fogThreshold,
                eps,
                maxValue);
    }

    public static final int COLUMN_PATH_FORCE_SCALAR = 1;
    public static final int COLUMN_PATH_DISABLED = 2;

    public void columnPathFlags(int flags) {
        RadsimBindings.columnPathFlags(handle, flags);
    }

    public void forceScalarColumns(boolean force) {
        columnPathFlags(force ? COLUMN_PATH_FORCE_SCALAR : 0);
    }

    public static final int KERNEL_BYPASS_COEFFICIENT_CACHE = 1;

    public void debugKernelFlags(int flags) {
        RadsimBindings.debugKernelFlags(handle, flags);
    }

    public void bypassCoefficientCache(boolean force) {
        debugKernelFlags(force ? KERNEL_BYPASS_COEFFICIENT_CACHE : 0);
    }

    public int sectionsPerChunk() {
        return tables.sectionsPerChunk();
    }

    public long[] lastStepProfileNanos() {
        int n = RadsimBindings.PHASE_NAMES.length;
        if (profile == null) profile = arena.allocate((long) n * Long.BYTES);
        RadsimBindings.lastStepProfileNanos(handle, profile, n);
        long[] out = new long[n];
        MemorySegment.copy(profile, ValueLayout.JAVA_LONG, 0L, out, 0, n);
        return out;
    }

    public void chunkLoaded(long ck) {
        RadsimBindings.chunkLoaded(handle, ck);
        int id = RadsimBindings.chunkId(handle, ck);
        if (id >= 0) chunkIds.put(ck, id);

        tables.refresh();
    }

    public void chunkUnloaded(long ck) {
        RadsimBindings.chunkUnloaded(handle, ck);
    }

    public void chunkRemoved(long ck) {
        RadsimBindings.chunkRemoved(handle, ck);
        chunkIds.remove(ck);
    }

    public int chunkId(long ck) {
        return chunkIds.get(ck);
    }

    public int step(long epochSalt, int epoch, int permBits) {
        int result = RadsimBindings.step(handle, epochSalt, epoch, permBits, events, eventCapacity);
        if (result < 0) {
            droppedEventSteps++;
            droppedEventBytes += -result;
            growEvents(-result);
            result = 0;
        }

        tables.refresh();
        return result;
    }

    public void drainEvents(int bytes, LongConsumer fog, LongConsumer destroy) {
        if (bytes < EVENT_HEADER_BYTES) return;
        int fogCount = events.get(ValueLayout.JAVA_INT, 0);
        int destroyCount = events.get(ValueLayout.JAVA_INT, 4);
        long offset = EVENT_HEADER_BYTES;
        for (int i = 0; i < fogCount; i++, offset += 8L)
            fog.accept(events.get(ValueLayout.JAVA_LONG, offset));
        for (int i = 0; i < destroyCount; i++, offset += 8L) {
            destroy.accept(events.get(ValueLayout.JAVA_LONG, offset));
        }
    }

    public long droppedEventSteps() {
        return droppedEventSteps;
    }

    public long droppedEventBytes() {
        return droppedEventBytes;
    }

    public double density(long ck, int sy) {
        int id = chunkIds.get(ck);
        return id < 0 ? 0.0D : tables.density(id, sy);
    }

    public void submitSectionSources(
            long[] keys,
            int[] entryCounts,
            short[] pockets,
            int[] multiplicities,
            double[] emissions,
            double[] saturations,
            int sectionCount,
            int entryTotal) {
        if (sectionCount <= 0) return;
        growSources(sectionCount, entryTotal);
        MemorySegment.copy(keys, 0, sourceKeys, ValueLayout.JAVA_LONG, 0L, sectionCount);
        MemorySegment.copy(
                entryCounts, 0, sourceEntryCounts, ValueLayout.JAVA_INT, 0L, sectionCount);
        if (entryTotal > 0) {
            MemorySegment.copy(pockets, 0, sourcePockets, ValueLayout.JAVA_SHORT, 0L, entryTotal);
            MemorySegment.copy(
                    multiplicities, 0, sourceMultiplicities, ValueLayout.JAVA_INT, 0L, entryTotal);
            MemorySegment.copy(
                    emissions, 0, sourceEmissions, ValueLayout.JAVA_DOUBLE, 0L, entryTotal);
            MemorySegment.copy(
                    saturations, 0, sourceSaturations, ValueLayout.JAVA_DOUBLE, 0L, entryTotal);
        }
        RadsimBindings.submitSectionSources(
                handle,
                sectionCount,
                sourceKeys,
                sourceEntryCounts,
                sourcePockets,
                sourceMultiplicities,
                sourceEmissions,
                sourceSaturations);
    }

    public static final int FEATURE_DIFFUSIVITY_TRANSPORT = 1;

    public void setFeatureFlags(int flags) {
        RadsimBindings.worldSetFeatureFlags(handle, flags);
    }

    public long diffusivityFailures() {
        return RadsimBindings.diffusivityFailures(handle);
    }

    public long[] validationFailures() {
        MemorySegment out = arena.allocate(3L * Long.BYTES, Long.BYTES);
        RadsimBindings.validationFailures(handle, out, 3);
        return new long[] {
            out.get(ValueLayout.JAVA_LONG, 0L),
            out.get(ValueLayout.JAVA_LONG, 8L),
            out.get(ValueLayout.JAVA_LONG, 16L)
        };
    }

    public void submitSectionDiffusivity(
            long[] keys, int[] pocketCounts, float[] values, int sectionCount, int valueTotal) {
        if (sectionCount <= 0) return;
        growDiffusivity(sectionCount, valueTotal);
        MemorySegment.copy(keys, 0, diffKeys, ValueLayout.JAVA_LONG, 0L, sectionCount);
        MemorySegment.copy(
                pocketCounts, 0, diffPocketCounts, ValueLayout.JAVA_INT, 0L, sectionCount);
        if (valueTotal > 0) {
            MemorySegment.copy(values, 0, diffValues, ValueLayout.JAVA_FLOAT, 0L, valueTotal);
        }
        RadsimBindings.submitSectionDiffusivity(
                handle, sectionCount, diffKeys, diffPocketCounts, diffValues);
    }

    private void growDiffusivity(int sectionCount, int valueTotal) {
        if (sectionCount > diffSectionCapacity) {
            int capacity = Math.max(sectionCount, Math.max(32, diffSectionCapacity * 2));
            diffKeys = arena.allocate((long) capacity * Long.BYTES, Long.BYTES);
            diffPocketCounts = arena.allocate((long) capacity * Integer.BYTES, Integer.BYTES);
            diffSectionCapacity = capacity;
        }

        if (valueTotal > diffValueCapacity || diffValues == null) {
            int capacity = Math.max(valueTotal, Math.max(64, diffValueCapacity * 2));
            diffValues = arena.allocate((long) capacity * Float.BYTES, Float.BYTES);
            diffValueCapacity = capacity;
        }
    }

    private void growSources(int sectionCount, int entryTotal) {
        if (sectionCount > sourceSectionCapacity) {
            int capacity = Math.max(sectionCount, Math.max(32, sourceSectionCapacity * 2));
            sourceKeys = arena.allocate((long) capacity * Long.BYTES, Long.BYTES);
            sourceEntryCounts = arena.allocate((long) capacity * Integer.BYTES, Integer.BYTES);
            sourceSectionCapacity = capacity;
        }

        if (entryTotal > sourceEntryCapacity || sourcePockets == null) {
            int capacity = Math.max(entryTotal, Math.max(64, sourceEntryCapacity * 2));
            sourcePockets = arena.allocate((long) capacity * Short.BYTES, Short.BYTES);
            sourceMultiplicities = arena.allocate((long) capacity * Integer.BYTES, Integer.BYTES);
            sourceEmissions = arena.allocate((long) capacity * Double.BYTES, Double.BYTES);
            sourceSaturations = arena.allocate((long) capacity * Double.BYTES, Double.BYTES);
            sourceEntryCapacity = capacity;
        }
    }

    public void submitDirtySections(long[] keys, long[] maskWords, int count) {
        if (count <= 0) return;
        growDirty(count);
        MemorySegment.copy(keys, 0, dirtyKeys, ValueLayout.JAVA_LONG, 0L, count);
        MemorySegment.copy(
                maskWords,
                0,
                dirtyMasks,
                ValueLayout.JAVA_LONG,
                0L,
                count * MASK_WORDS_PER_SECTION);
        RadsimBindings.submitDirtySections(handle, count, dirtyKeys, dirtyMasks);

        tables.refresh();
    }

    public void setRad(long sectionKey, int local, double value) {
        growEdits(1);
        edits.set(ValueLayout.JAVA_LONG, RadsimKeys.EDIT_OFFSET_KEY, sectionKey);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_ADD, 0.0D);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_SET, value);
        edits.set(ValueLayout.JAVA_LONG, RadsimKeys.EDIT_OFFSET_SET_SEQ, ++setSequence);
        edits.set(ValueLayout.JAVA_SHORT, RadsimKeys.EDIT_OFFSET_LOCAL, (short) local);
        edits.set(
                ValueLayout.JAVA_BYTE, RadsimKeys.EDIT_OFFSET_FLAGS, RadsimKeys.EDIT_FLAG_HAS_SET);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_SATURATION, 0.0D);
        RadsimBindings.submitEdits(handle, 1, edits, RadsimKeys.EDIT_WIRE_BYTES);
        tables.refresh();
    }

    public void addRad(long sectionKey, int local, double amount) {
        stageEdit(sectionKey, local, amount, (byte) 0, 0.0D);
    }

    public void emitRad(long sectionKey, int local, double emission, double saturation) {
        stageEdit(sectionKey, local, emission, RadsimKeys.EDIT_FLAG_HAS_SATURATION, saturation);
    }

    private void stageEdit(long sectionKey, int local, double add, byte flags, double saturation) {
        growEdits(1);
        edits.set(ValueLayout.JAVA_LONG, RadsimKeys.EDIT_OFFSET_KEY, sectionKey);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_ADD, add);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_SET, 0.0D);
        edits.set(ValueLayout.JAVA_LONG, RadsimKeys.EDIT_OFFSET_SET_SEQ, 0L);
        edits.set(ValueLayout.JAVA_SHORT, RadsimKeys.EDIT_OFFSET_LOCAL, (short) local);
        edits.set(ValueLayout.JAVA_BYTE, RadsimKeys.EDIT_OFFSET_FLAGS, flags);
        edits.set(ValueLayout.JAVA_DOUBLE, RadsimKeys.EDIT_OFFSET_SATURATION, saturation);
        RadsimBindings.submitEdits(handle, 1, edits, RadsimKeys.EDIT_WIRE_BYTES);
        tables.refresh();
    }

    public double localDensity(long sectionKey, int local) {
        return RadsimBindings.queryLocalDensity(handle, sectionKey, local);
    }

    private EntryScratch growEntryScratch(int rows) {
        EntryScratch scratch = entryScratch.get();
        if (scratch.rows >= rows) return scratch;
        scratch.rows = Math.max(rows, Math.max(256, scratch.rows * 2));
        scratch.sypi = arena.allocate((long) scratch.rows * Integer.BYTES);
        scratch.density = arena.allocate((long) scratch.rows * Double.BYTES);
        return scratch;
    }

    public int dumpChunkEntries(long ck, int[] sypiOut, double[] densityOut) {
        int cap = Math.min(sypiOut.length, densityOut.length);
        EntryScratch scratch = growEntryScratch(Math.max(cap, 1));
        int rows = readChunkEntries(ck, scratch);
        if (rows < 0) {
            scratch = growEntryScratch(-rows);
            rows = readChunkEntries(ck, scratch);
            assert rows >= 0;
        }
        rows = Math.min(rows, cap);
        MemorySegment.copy(scratch.sypi, ValueLayout.JAVA_INT, 0L, sypiOut, 0, rows);
        MemorySegment.copy(scratch.density, ValueLayout.JAVA_DOUBLE, 0L, densityOut, 0, rows);
        return rows;
    }

    private synchronized int readChunkEntries(long ck, EntryScratch scratch) {
        return RadsimBindings.dumpChunkEntries(
                handle,
                ck,
                scratch.sypi,
                (long) scratch.rows * Integer.BYTES,
                scratch.density,
                (long) scratch.rows * Double.BYTES);
    }

    public void loadChunkEntries(long ck, int count, int[] sypi, double[] density) {
        if (count <= 0) return;
        EntryScratch scratch = growEntryScratch(count);
        MemorySegment.copy(sypi, 0, scratch.sypi, ValueLayout.JAVA_INT, 0L, count);
        MemorySegment.copy(density, 0, scratch.density, ValueLayout.JAVA_DOUBLE, 0L, count);
        RadsimBindings.loadPendingEntries(handle, ck, count, scratch.sypi, scratch.density);
    }

    private void growEdits(int count) {
        long needed = (long) count * RadsimKeys.EDIT_WIRE_BYTES;
        if (edits != null && editCapacity >= needed) return;
        editCapacity = Math.max(needed, 16L * RadsimKeys.EDIT_WIRE_BYTES);
        edits = arena.allocate(editCapacity);
    }

    private void growDirty(int count) {
        if (count <= dirtyCapacity) return;
        int capacity = Math.max(count, Math.max(16, dirtyCapacity * 2));
        dirtyKeys = arena.allocate((long) capacity * Long.BYTES);
        dirtyMasks = arena.allocate((long) capacity * MASK_WORDS_PER_SECTION * Long.BYTES);
        dirtyCapacity = capacity;
    }

    private void growEvents(int needed) {
        if (needed <= eventCapacity) return;
        int capacity = Math.max(needed, eventCapacity * 2);

        events = arena.allocate(capacity, Long.BYTES);
        eventCapacity = capacity;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        RadsimBindings.worldDestroy(handle);

        arena.close();
    }
}
