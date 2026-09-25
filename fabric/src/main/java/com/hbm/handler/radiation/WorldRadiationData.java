// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.NuclearTech;
import com.hbm.config.RadiationConfig;
import com.hbm.interfaces.ServerThread;
import com.hbm.lib.Library;
import com.hbm.lib.TLPool;
import com.hbm.lib.internal.natives.NativeLibrary;
import com.hbm.lib.internal.natives.RadsimKeys;
import com.hbm.lib.queues.MpscUnboundedXaddArrayLongQueue;
import com.hbm.util.DecodeException;
import com.hbm.util.ObjectPool;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.radiation.RadiationSystemNT.*;
import static com.hbm.lib.internal.UnsafeHolder.U;
import static com.hbm.lib.internal.UnsafeHolder.fieldOffset;

final class WorldRadiationData {
    final ServerLevel world;
    final long worldSalt;

    final double diffusionDt, uuE, retentionDt;
    final long fogProbU64;
    final double fogRad;
    final boolean worldRadEffects;
    final double minBound;
    final double ambientRad;

    final boolean diffusivityTransport;

    final int sectionsPerChunk;
    final int wordsPerChunk;
    final int minSectionY;
    final long allUniKinds;
    final long activeMaskAll;

    final Long2IntOpenHashMap coordToId = new Long2IntOpenHashMap(4225);
    final LongOpenHashSet diffusivityDirty = new LongOpenHashSet();
    private final IntArrayList diffusivityRefreshIds = new IntArrayList();
    private final int[] singleVolumeScratch = new int[1];
    final DirtyChunkTracker dirtyCk;
    final TLPool<short[]> pocketDataPool =
            new TLPool<>(
                    () -> {
                        short[] a = new short[4096];
                        Arrays.fill(a, NO_POCKET);
                        return a;
                    },
                    a -> Arrays.fill(a, NO_POCKET),
                    256,
                    4096);
    final LongArrayList dirtyToRebuildScratch = new LongArrayList(16384);
    final IntArrayList editedChunkIds = new IntArrayList(256);
    final ObjectPool<EditTable> editTablePool =
            new ObjectPool<>(() -> new EditTable(32), EditTable::clear, 64);
    final MpscUnboundedXaddArrayLongQueue destructionQueue =
            new MpscUnboundedXaddArrayLongQueue(64);
    final MpscUnboundedXaddArrayLongQueue fogQueue = new MpscUnboundedXaddArrayLongQueue(64);
    int capacity = 4096;
    int nextId;
    int[] freeIds = new int[1024];
    int freeTop;
    long[] cks = new long[capacity];
    LevelChunk[] mcChunks = new LevelChunk[capacity];
    PendingRad[] pending = new PendingRad[capacity];
    long[] chunkKinds;
    long[] chunkActiveDirty;
    long[] chunkSourceMask;
    int @Nullable [] touchedSectionWords;
    int[] eastNeighborId = new int[capacity];
    int[] southNeighborId = new int[capacity];
    double[] uniformRads;
    float @Nullable [] uniformDiffusivity;
    SectionRef[] complexSecs;
    RadiationSystemNT.SectionSources[] sectionSources;
    byte[] myBucket = new byte[capacity];
    int[] myBucketIndex = new int[capacity];
    int[][] xPairAByBucket =
            new int[][] {new int[4096], new int[4096], new int[4096], new int[4096]};
    int[][] xPairBByBucket =
            new int[][] {new int[4096], new int[4096], new int[4096], new int[4096]};
    int[][] zPairAByBucket =
            new int[][] {new int[4096], new int[4096], new int[4096], new int[4096]};
    int[][] zPairBByBucket =
            new int[][] {new int[4096], new int[4096], new int[4096], new int[4096]};
    int[] xPairCounts = new int[4];
    int[] zPairCounts = new int[4];
    boolean pairListsDirty = true;
    int[][] parityBucketIds =
            new int[][] {new int[4096], new int[4096], new int[4096], new int[4096]};
    int[] parityCounts = new int[4];
    EditTable[] editsById = new EditTable[capacity];
    int[] radiationDirtyIds = new int[1024];
    long[] radiationDirtyCks = new long[1024];
    int[] radiationDirtyIndex = new int[capacity];
    int radiationDirtyCount;
    long[] linkScratch = new long[512];
    int[] dirtyChunkIdsScratch = new int[1024];
    int[] dirtyChunkMasksScratchArr = new int[4096];
    long pocketToDestroy = Long.MIN_VALUE;
    int workEpoch, executionSampleCount;
    long workEpochSalt, profSteps, setSeq;
    double profTotalMs, profMaxMs, executionTimeAccumulator;
    DoubleArrayList profSamplesMs;

    static final boolean ROUTE_TO_BACKEND =
            !"false".equalsIgnoreCase(System.getProperty("hbm.radsim.route", "true"));

    final @Nullable RadsimBackend backend;

    private long[] diffKeyScratch = new long[0];
    private int[] diffCountScratch = new int[0];
    private float[] diffValueScratch = new float[0];

    private long[] sourceKeyScratch = new long[0];
    private int[] sourceCountScratch = new int[0];
    private short[] sourcePocketScratch = new short[0];
    private int[] sourceMultScratch = new int[0];
    private double[] sourceEmissionScratch = new double[0];
    private double[] sourceSaturationScratch = new double[0];

    WorldRadiationData(ServerLevel world) {
        this.world = world;
        int n = world.getSectionsCount();
        if (n <= 0 || n > MAX_SECTIONS_PER_CHUNK) {
            throw new IllegalStateException(
                    "RadiationSystemNT only supports 1.."
                            + MAX_SECTIONS_PER_CHUNK
                            + " sections per chunk; dimension "
                            + world.dimension().identifier()
                            + " has "
                            + n);
        }
        this.sectionsPerChunk = n;
        this.wordsPerChunk = (n + 31) >>> 5;
        this.chunkKinds = new long[capacity * wordsPerChunk];
        this.chunkActiveDirty = new long[capacity * wordsPerChunk];
        this.chunkSourceMask = new long[capacity * wordsPerChunk];
        this.touchedSectionWords = wordsPerChunk == 1 ? null : new int[capacity * wordsPerChunk];
        this.dirtyCk = new DirtyChunkTracker(2048, wordsPerChunk);
        this.dirtyChunkMasksScratchArr = new int[4096 * wordsPerChunk];
        this.minSectionY = world.getMinSectionY();
        this.allUniKinds =
                (n >= 32) ? 0x5555555555555555L : (0x5555555555555555L & ((1L << (n * 2)) - 1));
        this.activeMaskAll = (1L << Math.min(n, 32)) - 1L;
        this.uniformRads = new double[capacity * n];
        this.complexSecs = new SectionRef[capacity * n];
        this.sectionSources = new RadiationSystemNT.SectionSources[capacity * n];

        RadiationSettings.Resolved settings =
                RadiationSettings.forLevel(world).resolve(RadiationSystemNT.dT);
        diffusionDt = settings.diffusionDt();
        uuE = settings.uuE();
        retentionDt = settings.retentionDt();
        fogProbU64 = settings.fogProbU64();
        fogRad = settings.fogRad();
        worldRadEffects = settings.worldRadEffects();
        minBound = settings.minBound();
        ambientRad = settings.ambientRad();

        diffusivityTransport = RadiationDiffusivity.transportEnabled(world);
        if (diffusivityTransport) {
            uniformDiffusivity = new float[capacity * n];
            Arrays.fill(uniformDiffusivity, RadiationDiffusivity.NEUTRAL);
        }

        worldSalt =
                HashCommon.murmurHash3(
                        world.getSeed()
                                ^ (long) world.dimension().identifier().hashCode()
                                        * 0x9E3779B97F4A7C15L
                                ^ 0xD1B54A32D192ED03L);
        coordToId.defaultReturnValue(-1);
        Arrays.fill(eastNeighborId, -1);
        Arrays.fill(southNeighborId, -1);
        Arrays.fill(myBucket, (byte) -1);
        Arrays.fill(myBucketIndex, -1);
        Arrays.fill(radiationDirtyIndex, -1);
        for (int b = 0; b < 4; b++) Arrays.fill(parityBucketIds[b], -1);

        RadsimBackend opened = null;
        if (NativeLibrary.AVAILABLE && ROUTE_TO_BACKEND) {
            try {
                opened =
                        new RadsimBackend(
                                world,
                                Integer.getInteger("hbm.radsim.threads", defaultRadsimThreads()));
            } catch (RuntimeException ex) {
                NuclearTech.LOGGER.warn(
                        "Radiation backend unavailable for dimension {}; using the Java sweep",
                        world.dimension().identifier(),
                        ex);
            }
        }
        backend = opened;
    }

    static int defaultRadsimThreads() {
        return Math.max(1, Math.min(16, Runtime.getRuntime().availableProcessors() / 2));
    }

    static double mulClamp(double a, int b) {
        if (a == 0.0D) return 0.0D;
        if (!Double.isFinite(a)) return Math.copySign(Double.MAX_VALUE, a);
        double lim = Double.MAX_VALUE / (double) b;
        double aa = Math.abs(a);
        if (aa >= lim) return Math.copySign(Double.MAX_VALUE, a);
        return a * (double) b;
    }

    static double addClamp(double a, double b) {
        double s = a + b;
        if (s == Double.POSITIVE_INFINITY) return Double.MAX_VALUE;
        if (s == Double.NEGATIVE_INFINITY) return -Double.MAX_VALUE;
        return Double.isNaN(s) ? 0.0D : s;
    }

    static int grow(int current, int need) {
        int n = Math.max(current, 16);
        while (n < need) n = n + (n >>> 1) + 16;
        return n;
    }

    static int[] ensureIntScratch(ThreadLocal<int[]> tl, int need) {
        int[] arr = tl.get();
        if (arr.length >= need) return arr;
        int n = grow(arr.length, need);
        arr = Arrays.copyOf(arr, n);
        tl.set(arr);
        return arr;
    }

    static long editKey(int slot, int localIdx) {
        return ((long) slot << 12) | (localIdx & 0xFFFL);
    }

    static int editKeySlot(long key) {
        return (int) (key >>> 12);
    }

    static int editKeyLocal(long key) {
        return (int) (key & 0xFFFL);
    }

    static int floodFillPockets(
            SectionMask resistant,
            short[] pocketData,
            int[] queue,
            int @Nullable [] vols,
            long @Nullable [] sumXYZ) {
        if (vols != null) Arrays.fill(vols, 0);
        if (sumXYZ != null) Arrays.fill(sumXYZ, 0);
        int pc = 0;
        for (int blockIndex = 0; blockIndex < SectionPos.SECTION_BLOCK_COUNT; blockIndex++) {
            if (pocketData[blockIndex] != NO_POCKET) continue;
            if (resistant.get(blockIndex)) continue;

            int pocketIndex = (pc >= MAX_POCKETS) ? 0 : pc++;
            int head = 0, tail = 0;
            queue[tail++] = blockIndex;
            pocketData[blockIndex] = (short) pocketIndex;

            if (vols != null) vols[pocketIndex]++;
            if (sumXYZ != null) {
                int base = pocketIndex * 3;
                sumXYZ[base] += Library.getLocalX(blockIndex);
                sumXYZ[base + 1] += Library.getLocalY(blockIndex);
                sumXYZ[base + 2] += Library.getLocalZ(blockIndex);
            }

            while (head != tail) {
                int cur = queue[head++];
                for (int f = 0; f < 6; f++) {
                    int nei = cur + LINEAR_OFFSETS[f];
                    if (((nei & 0xF000) | ((cur ^ nei) & BOUNDARY_MASKS[f])) != 0) continue;
                    if (pocketData[nei] != NO_POCKET) continue;
                    if (resistant.get(nei)) continue;

                    pocketData[nei] = (short) pocketIndex;
                    queue[tail++] = nei;

                    if (vols != null) vols[pocketIndex]++;
                    if (sumXYZ != null) {
                        int base = pocketIndex * 3;
                        sumXYZ[base] += Library.getLocalX(nei);
                        sumXYZ[base + 1] += Library.getLocalY(nei);
                        sumXYZ[base + 2] += Library.getLocalZ(nei);
                    }
                }
            }
        }
        return pc;
    }

    static void ensurePairBucketCapacity(
            int[][] aByBucket, int[][] bByBucket, int bucket, int need) {
        int[] aArr = aByBucket[bucket];
        if (aArr.length >= need) return;
        int n = aArr.length;
        while (n < need) n = n + (n >>> 1) + 16;
        aByBucket[bucket] = Arrays.copyOf(aArr, n);
        bByBucket[bucket] = Arrays.copyOf(bByBucket[bucket], n);
    }

    static double meanOfLargestK(double[] sortedAscending, int k) {
        int n = sortedAscending.length;
        double sum = 0.0;
        for (int i = n - k; i < n; i++) sum += sortedAscending[i];
        return sum / (double) k;
    }

    static double r3(double v) {
        return Math.rint(v * 1000.0) / 1000.0;
    }

    int slotOf(int absoluteSubY) {
        int slot = absoluteSubY - minSectionY;
        return (slot < 0 || slot >= sectionsPerChunk) ? -1 : slot;
    }

    void sweepX(
            int[][] aPairs,
            int[][] bPairs,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            boolean flip) {
        var t0 = new DiffuseXTask(aPairs[0], bPairs[0], 0, c0, th0);
        var t1 = new DiffuseXTask(aPairs[1], bPairs[1], 0, c1, th1);
        var t2 = new DiffuseXTask(aPairs[2], bPairs[2], 0, c2, th2);
        var t3 = new DiffuseXTask(aPairs[3], bPairs[3], 0, c3, th3);
        if (flip) {
            ForkJoinTask.invokeAll(t1, t3);
            ForkJoinTask.invokeAll(t0, t2);
        } else {
            ForkJoinTask.invokeAll(t0, t2);
            ForkJoinTask.invokeAll(t1, t3);
        }
    }

    void sweepZ(
            int[][] aPairs,
            int[][] bPairs,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            boolean flip) {
        var t0 = new DiffuseZTask(aPairs[0], bPairs[0], 0, c0, th0);
        var t1 = new DiffuseZTask(aPairs[1], bPairs[1], 0, c1, th1);
        var t2 = new DiffuseZTask(aPairs[2], bPairs[2], 0, c2, th2);
        var t3 = new DiffuseZTask(aPairs[3], bPairs[3], 0, c3, th3);
        if (flip) {
            ForkJoinTask.invokeAll(t2, t3);
            ForkJoinTask.invokeAll(t0, t1);
        } else {
            ForkJoinTask.invokeAll(t0, t1);
            ForkJoinTask.invokeAll(t2, t3);
        }
    }

    void sweepY(
            int[][] b,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            int startParity) {
        for (int p = 0; p < 2; p++) {
            int parity = startParity ^ p;
            var t0 = new DiffuseYTask(b[0], 0, c0, parity, th0).fork();
            var t1 = new DiffuseYTask(b[1], 0, c1, parity, th1).fork();
            var t2 = new DiffuseYTask(b[2], 0, c2, parity, th2).fork();
            new DiffuseYTask(b[3], 0, c3, parity, th3).invoke();
            t0.join();
            t1.join();
            t2.join();
        }
    }

    void diffuseXZ(int aId, int bId, int faceA, int faceB) {
        long kindsA = chunkKinds[aId];
        long kindsB = chunkKinds[bId];
        long actA = chunkActiveDirty[aId];
        long actB = chunkActiveDirty[bId];
        int N = sectionsPerChunk;
        int offA = aId * N;
        int offB = bId * N;
        double[] uniform = uniformRads;
        boolean d = false;

        if (kindsA == allUniKinds && kindsB == allUniKinds && !diffusivityTransport) {
            long changed = UniformExchange.INSTANCE.exchange(uniform, offA, offB, N, uuE);
            if (changed != 0L) {
                chunkActiveDirty[aId] |= changed | CHUNK_DIRTY_MASK;
                chunkActiveDirty[bId] |= changed | CHUNK_DIRTY_MASK;
            }
            return;
        }

        for (int sy = 0; sy < N; sy++) {
            long actMask = 1L << sy;
            if (((actA | actB) & actMask) == 0L) continue;

            int idxA = offA + sy;
            int idxB = offB + sy;
            int shift = sy << 1;
            int kA = (int) ((kindsA >>> shift) & 3);
            int kB = (int) ((kindsB >>> shift) & 3);

            if (kA == KIND_UNI && kB == KIND_UNI) {
                if (exchangeUni(uniform, idxA, idxB)) {
                    chunkActiveDirty[aId] |= actMask;
                    chunkActiveDirty[bId] |= actMask;
                    d = true;
                }
            } else {
                d |= exchangeFaceExact(idxA, kA, faceA, idxB, kB, faceB);
            }
        }
        if (d) {
            chunkActiveDirty[aId] |= CHUNK_DIRTY_MASK;
            chunkActiveDirty[bId] |= CHUNK_DIRTY_MASK;
        }
    }

    static long uniformKinds(int count) {
        return count == 32 ? 0x5555555555555555L : 0x5555555555555555L & ((1L << (count * 2)) - 1L);
    }

    void diffuseXZWide(int aId, int bId, int faceA, int faceB) {
        int metaA = aId * wordsPerChunk;
        int metaB = bId * wordsPerChunk;
        int baseA = aId * sectionsPerChunk;
        int baseB = bId * sectionsPerChunk;
        boolean dirty = false;
        for (int word = 0; word < wordsPerChunk; word++) {
            int aWord = metaA + word;
            int bWord = metaB + word;
            long active = (chunkActiveDirty[aWord] | chunkActiveDirty[bWord]) & 0xffffffffL;
            if (active == 0L) continue;
            int first = word << 5;
            int count = Math.min(32, sectionsPerChunk - first);
            long kindsA = chunkKinds[aWord];
            long kindsB = chunkKinds[bWord];
            long allUniform = uniformKinds(count);
            if (kindsA == allUniform && kindsB == allUniform && !diffusivityTransport) {
                long changed =
                        UniformExchange.INSTANCE.exchange(
                                uniformRads, baseA + first, baseB + first, count, uuE);
                chunkActiveDirty[aWord] |= changed;
                chunkActiveDirty[bWord] |= changed;
                dirty |= changed != 0L;
                continue;
            }
            for (long m = active; m != 0L; m &= m - 1L) {
                int lane = Long.numberOfTrailingZeros(m);
                int slot = first + lane;
                int kindA = (int) ((kindsA >>> (lane * 2)) & 3);
                int kindB = (int) ((kindsB >>> (lane * 2)) & 3);
                if (kindA == KIND_UNI && kindB == KIND_UNI) {
                    if (exchangeUni(uniformRads, baseA + slot, baseB + slot)) {
                        chunkActiveDirty[aWord] |= 1L << lane;
                        chunkActiveDirty[bWord] |= 1L << lane;
                        dirty = true;
                    }
                } else {
                    dirty |=
                            exchangeFaceExact(
                                    baseA + slot, kindA, faceA, baseB + slot, kindB, faceB);
                }
            }
        }
        if (dirty) {
            chunkActiveDirty[metaA] |= CHUNK_DIRTY_MASK;
            chunkActiveDirty[metaB] |= CHUNK_DIRTY_MASK;
        }
    }

    boolean exchangeUni(double[] uni, int idxA, int idxB) {
        if (!diffusivityTransport) return exchangeUniExactXZ(uni, idxA, idxB, uuE);
        return exchangeUniExactDiffusive(
                uni,
                idxA,
                idxB,
                uuE,
                diffusionDt,
                uniformDiffusivity[idxA],
                uniformDiffusivity[idxB]);
    }

    boolean exchangeFaceExactY(int idxA, int kA, int idxB, int kB) {
        if (kA == KIND_NONE || kB == KIND_NONE) return false;
        if (kA == KIND_UNI) {
            SectionRef b = complexSecs[idxB];
            return b.exchangeWithUniform(idxA, 0, 1);
        }
        if (kB == KIND_UNI) {
            SectionRef a = complexSecs[idxA];
            return a.exchangeWithUniform(idxB, 1, 0);
        }
        SectionRef a = complexSecs[idxA];
        SectionRef b = complexSecs[idxB];
        if (kB == KIND_SINGLE) return a.exchangeWithSingle((SingleMaskedSectionRef) b, 1, 0);
        return a.exchangeWithMulti((MultiSectionRef) b, 1, 0);
    }

    boolean exchangeFaceExact(int idxA, int kA, int faceA, int idxB, int kB, int faceB) {
        if (kA == KIND_NONE || kB == KIND_NONE) return false;
        if (kA == KIND_UNI) {
            SectionRef secB = complexSecs[idxB];
            return secB.exchangeWithUniform(idxA, faceB, faceA);
        } else if (kB == KIND_UNI) {
            SectionRef secA = complexSecs[idxA];
            return secA.exchangeWithUniform(idxB, faceA, faceB);
        } else {
            SectionRef secA = complexSecs[idxA];
            SectionRef secB = complexSecs[idxB];
            if (kB == KIND_SINGLE)
                return secA.exchangeWithSingle((SingleMaskedSectionRef) secB, faceA, faceB);
            return secA.exchangeWithMulti((MultiSectionRef) secB, faceA, faceB);
        }
    }

    boolean isSectionActive(int id, int sy) {
        return (chunkActiveDirty[wordIndex(id, sy)] & (1L << (sy & 31))) != 0L;
    }

    int getId(long ck) {
        return coordToId.get(ck);
    }

    int getKind(int id, int sy) {
        return (int) ((chunkKinds[wordIndex(id, sy)] >>> ((sy & 31) << 1)) & 3);
    }

    int wordIndex(int id, int slot) {
        return wordsPerChunk == 1 ? id : id * wordsPerChunk + (slot >>> 5);
    }

    int wordBase(int id) {
        return wordsPerChunk == 1 ? id : id * wordsPerChunk;
    }

    void markActive(int id, int slot) {
        chunkActiveDirty[wordIndex(id, slot)] |= 1L << (slot & 31);
    }

    boolean isChunkDirty(int id) {
        return (chunkActiveDirty[wordBase(id)] & CHUNK_DIRTY_MASK) != 0L;
    }

    void setChunkDirty(int id) {
        chunkActiveDirty[wordBase(id)] |= CHUNK_DIRTY_MASK;
    }

    void clearChunkDirty(int id) {
        chunkActiveDirty[wordBase(id)] &= ~CHUNK_DIRTY_MASK;
    }

    void ensureCapacity(int min) {
        if (capacity >= min) return;
        int oldCap = capacity;
        int n = grow(capacity, min);
        capacity = n;
        cks = Arrays.copyOf(cks, n);
        mcChunks = Arrays.copyOf(mcChunks, n);
        pending = Arrays.copyOf(pending, n);
        chunkKinds = Arrays.copyOf(chunkKinds, n * wordsPerChunk);
        chunkActiveDirty = Arrays.copyOf(chunkActiveDirty, n * wordsPerChunk);
        chunkSourceMask = Arrays.copyOf(chunkSourceMask, n * wordsPerChunk);
        if (touchedSectionWords != null) {
            touchedSectionWords = Arrays.copyOf(touchedSectionWords, n * wordsPerChunk);
        }
        eastNeighborId = Arrays.copyOf(eastNeighborId, n);
        southNeighborId = Arrays.copyOf(southNeighborId, n);
        Arrays.fill(eastNeighborId, oldCap, n, -1);
        Arrays.fill(southNeighborId, oldCap, n, -1);
        radiationDirtyIndex = Arrays.copyOf(radiationDirtyIndex, n);
        Arrays.fill(radiationDirtyIndex, oldCap, n, -1);
        uniformRads = Arrays.copyOf(uniformRads, n * sectionsPerChunk);
        if (uniformDiffusivity != null) {
            uniformDiffusivity = Arrays.copyOf(uniformDiffusivity, n * sectionsPerChunk);
            Arrays.fill(
                    uniformDiffusivity,
                    oldCap * sectionsPerChunk,
                    n * sectionsPerChunk,
                    RadiationDiffusivity.NEUTRAL);
        }
        complexSecs = Arrays.copyOf(complexSecs, n * sectionsPerChunk);
        sectionSources = Arrays.copyOf(sectionSources, n * sectionsPerChunk);
        editsById = Arrays.copyOf(editsById, n);
        myBucket = Arrays.copyOf(myBucket, n);
        myBucketIndex = Arrays.copyOf(myBucketIndex, n);
        Arrays.fill(myBucket, oldCap, n, (byte) -1);
        Arrays.fill(myBucketIndex, oldCap, n, -1);
    }

    void pushFreeId(int id) {
        if (freeTop == freeIds.length)
            freeIds = Arrays.copyOf(freeIds, grow(freeIds.length, freeTop + 1));
        freeIds[freeTop++] = id;
    }

    int popFreeId() {
        return freeIds[--freeTop];
    }

    int allocateId() {
        if (freeTop > 0) return popFreeId();
        int id = nextId++;
        if (id >= capacity) ensureCapacity(id + 1);
        return id;
    }

    void clearResidentState(int id) {
        mcChunks[id] = null;
        pending[id] = null;
        int metadataBase = wordBase(id);
        Arrays.fill(chunkKinds, metadataBase, metadataBase + wordsPerChunk, 0L);
        Arrays.fill(chunkActiveDirty, metadataBase, metadataBase + wordsPerChunk, 0L);
        Arrays.fill(chunkSourceMask, metadataBase, metadataBase + wordsPerChunk, 0L);
        clearTouchedSections(id);
        eastNeighborId[id] = -1;
        southNeighborId[id] = -1;
        EditTable t = editsById[id];
        if (t != null) {
            editTablePool.recycle(t);
            editsById[id] = null;
        }
        int si = id * sectionsPerChunk;
        Arrays.fill(uniformRads, si, si + sectionsPerChunk, 0.0D);
        if (uniformDiffusivity != null) {
            Arrays.fill(
                    uniformDiffusivity, si, si + sectionsPerChunk, RadiationDiffusivity.NEUTRAL);
        }
        Arrays.fill(complexSecs, si, si + sectionsPerChunk, null);
        Arrays.fill(sectionSources, si, si + sectionsPerChunk, null);
        myBucket[id] = -1;
        myBucketIndex[id] = -1;
    }

    int ensureId(long ck) {
        int id = coordToId.get(ck);
        if (id >= 0) return id;
        id = allocateId();
        coordToId.put(ck, id);
        cks[id] = ck;
        clearResidentState(id);
        return id;
    }

    void linkNonUniFace(SectionRef a, int kA, int faceA, int idxB) {
        assert kA > KIND_UNI;
        int kB;
        if (idxB < 0) {
            kB = KIND_NONE;
        } else {
            int ownerB = idxB / sectionsPerChunk;
            int slotB = idxB - ownerB * sectionsPerChunk;
            kB = getKind(ownerB, slotB);
        }
        if (kB == KIND_UNI) {
            a.linkFaceToUniform(faceA);
            return;
        }
        if (kB == KIND_NONE) {
            a.clearFaceAllPockets(faceA);
            return;
        }
        SectionRef b = complexSecs[idxB];
        if (b == null) {
            a.clearFaceAllPockets(faceA);
            return;
        }
        if (kB == KIND_MULTI) {
            a.linkFaceToMulti((MultiSectionRef) b, faceA);
        } else {
            a.linkFaceToSingle((SingleMaskedSectionRef) b, faceA);
        }
    }

    void remapPocketMass(
            int secIdx,
            int oldKind,
            @Nullable SectionRef old,
            int newPocketCount,
            short @Nullable [] newPocketData,
            int[] newVols,
            double[] outNewMass) {

        Arrays.fill(outNewMass, 0, newPocketCount, 0.0d);
        if (oldKind == KIND_NONE || newPocketCount == 0) return;

        if (newPocketData == null) {
            double totalMass = 0.0d;

            if (oldKind == KIND_UNI) {
                double d = uniformRads[secIdx];
                if (Math.abs(d) > RAD_EPSILON)
                    totalMass = mulClamp(d, SectionPos.SECTION_BLOCK_COUNT);
            } else if (old != null && old.pocketCount > 0) {
                if (oldKind == KIND_SINGLE) {
                    double d = uniformRads[secIdx];
                    if (Math.abs(d) > RAD_EPSILON)
                        totalMass = mulClamp(d, Math.max(1, ((SingleMaskedSectionRef) old).volume));
                } else {
                    MultiSectionRef m = (MultiSectionRef) old;
                    int oldCnt = m.pocketCount & 0xFFFF;
                    for (int p = 0; p < oldCnt; p++) {
                        double d = m.data[p << 1];
                        if (Math.abs(d) > RAD_EPSILON)
                            totalMass = addClamp(totalMass, mulClamp(d, Math.max(1, m.volume[p])));
                    }
                }
            }

            outNewMass[0] = totalMass;
            return;
        }

        if (oldKind == KIND_UNI) {
            double d = uniformRads[secIdx];
            if (Math.abs(d) <= RAD_EPSILON) return;

            double oldMass = mulClamp(d, SectionPos.SECTION_BLOCK_COUNT);
            long totalNewAir = 0L;
            for (int p = 0; p < newPocketCount; p++) totalNewAir += Math.max(1, newVols[p]);
            if (totalNewAir <= 0L) return;

            double massPerBlock = oldMass / (double) totalNewAir;
            for (int p = 0; p < newPocketCount; p++)
                outNewMass[p] = mulClamp(massPerBlock, Math.max(1, newVols[p]));
            return;
        }

        if (old == null || old.pocketCount <= 0) return;

        int oldCnt = old.pocketCount & 0xFFFF;
        short[] oldPocketData = old.pocketData;

        int[] overlaps = TL_FF_QUEUE.get();
        boolean directCounts =
                oldCnt <= DIRECT_OVERLAP_MAX_OLD_POCKETS
                        && oldCnt * newPocketCount <= DIRECT_OVERLAP_MAX_CELLS;
        int directCells = directCounts ? oldCnt * newPocketCount : 0;
        if (directCounts) Arrays.fill(overlaps, 0, directCells, 0);

        int[] oldTotals = ensureIntScratch(TL_TEMP_ARRAY, oldCnt);
        int pairCount = 0;
        for (int i = 0; i < SectionPos.SECTION_BLOCK_COUNT; i++) {
            int nIdx = newPocketData[i];
            if (nIdx < 0 || nIdx >= newPocketCount) continue;

            int oIdx = oldPocketData[i];
            if (oIdx < 0 || oIdx >= oldCnt) continue;

            if (directCounts) overlaps[oIdx * newPocketCount + nIdx]++;
            else overlaps[pairCount] = (oIdx << 11) | nIdx;
            pairCount++;
            oldTotals[oIdx]++;
        }
        if (pairCount == 0) return;
        if (!directCounts) Arrays.sort(overlaps, 0, pairCount);

        double[] oldMass = TL_DENSITIES.get();
        Arrays.fill(oldMass, 0, oldCnt, 0.0d);

        if (oldKind == KIND_SINGLE) {
            double d0 = uniformRads[secIdx];
            if (Math.abs(d0) > RAD_EPSILON)
                oldMass[0] = mulClamp(d0, Math.max(1, ((SingleMaskedSectionRef) old).volume));
        } else {
            MultiSectionRef m = (MultiSectionRef) old;
            for (int p = 0; p < oldCnt; p++) {
                double dp = m.data[p << 1];
                if (Math.abs(dp) > RAD_EPSILON) oldMass[p] = mulClamp(dp, Math.max(1, m.volume[p]));
            }
        }

        if (directCounts) {
            for (int o = 0; o < oldCnt; o++) {
                double mass = oldMass[o];
                int total = oldTotals[o];
                if (Math.abs(mass) <= RAD_EPSILON || total <= 0) continue;

                int row = o * newPocketCount;
                for (int n = 0; n < newPocketCount; n++) {
                    int count = overlaps[row + n];
                    if (count != 0)
                        outNewMass[n] =
                                addClamp(outNewMass[n], mulClamp(mass / (double) total, count));
                }
            }
            Arrays.fill(overlaps, 0, directCells, 0);
        } else {
            for (int i = 0; i < pairCount; ) {
                int key = overlaps[i];
                int j = i + 1;
                while (j < pairCount && overlaps[j] == key) j++;

                int o = key >>> 11;
                double mass = oldMass[o];
                if (Math.abs(mass) > RAD_EPSILON) {
                    int total = oldTotals[o];
                    if (total > 0) {
                        int n = key & 0x7FF;
                        int count = j - i;
                        outNewMass[n] =
                                addClamp(outNewMass[n], mulClamp(mass / (double) total, count));
                    }
                }
                i = j;
            }
        }
        Arrays.fill(oldTotals, 0, oldCnt, 0);
    }

    void remapSavedPocketMass(
            PendingRad.SavedSection old,
            int newPocketCount,
            short @Nullable [] newPocketData,
            double[] outNewMass) {
        Arrays.fill(outNewMass, 0, newPocketCount, 0.0D);
        if (newPocketCount == 0) return;
        if (newPocketData == null) {
            double mass = 0.0D;
            for (int pocket = 0; pocket < old.pocketCount(); pocket++) {
                double density = old.densities()[pocket];
                if (density != 0.0D)
                    mass = addClamp(mass, mulClamp(density, old.volumes()[pocket]));
            }
            outNewMass[0] = mass;
            return;
        }

        int[] overlaps = TL_FF_QUEUE.get();
        int pairCount = 0;
        short[] oldPocketData = old.pocketData();
        for (int block = 0; block < SectionPos.SECTION_BLOCK_COUNT; block++) {
            int oldPocket = oldPocketData == null ? 0 : oldPocketData[block];
            int newPocket = newPocketData[block];
            if (oldPocket < 0
                    || oldPocket >= old.pocketCount()
                    || newPocket < 0
                    || newPocket >= newPocketCount) continue;
            overlaps[pairCount++] = oldPocket << 11 | newPocket;
        }
        if (pairCount == 0) return;
        Arrays.sort(overlaps, 0, pairCount);
        for (int i = 0; i < pairCount; ) {
            int key = overlaps[i];
            int j = i + 1;
            while (j < pairCount && overlaps[j] == key) j++;
            int oldPocket = key >>> 11;
            double density = old.densities()[oldPocket];
            if (density != 0.0D) {
                int newPocket = key & 0x7FF;
                outNewMass[newPocket] = addClamp(outNewMass[newPocket], mulClamp(density, j - i));
            }
            i = j;
        }
    }

    void ensureParityBucketCapacity(int bucket, int need) {
        int[] arr = parityBucketIds[bucket];
        if (arr.length >= need) return;
        int oldLen = arr.length;
        int n = oldLen;
        while (n < need) n = n + (n >>> 1) + 16;
        parityBucketIds[bucket] = Arrays.copyOf(arr, n);
        Arrays.fill(parityBucketIds[bucket], oldLen, n, -1);
    }

    void addLoadedToBucket(int id) {
        assert id >= 0;
        LevelChunk c = mcChunks[id];
        assert c != null : "Adding to bucket requires loaded chunk";
        assert myBucketIndex[id] < 0 : "Double-add to parity bucket";
        byte b = (byte) ((cks[id] & 1L) | ((cks[id] >>> 31) & 2L));
        int next = parityCounts[b] + 1;
        ensureParityBucketCapacity(b, next);
        int i = parityCounts[b]++;
        parityBucketIds[b][i] = id;
        myBucket[id] = b;
        myBucketIndex[id] = i;
        pairListsDirty = true;
    }

    void removeLoadedFromBucket(int id) {
        int i = myBucketIndex[id];
        if (i < 0) return;
        int b = myBucket[id];
        assert b >= 0 && b < 4;

        int last = --parityCounts[b];
        assert last >= 0;

        int[] ids = parityBucketIds[b];
        int swapId = ids[last];

        ids[i] = swapId;
        if (swapId >= 0) myBucketIndex[swapId] = i;

        ids[last] = -1;

        myBucket[id] = -1;
        myBucketIndex[id] = -1;
        pairListsDirty = true;
    }

    void clearBuckets() {
        for (int b = 0; b < 4; b++) {
            int[] ids = parityBucketIds[b];
            int n = parityCounts[b];
            for (int i = 0; i < n; i++) {
                int id = ids[i];
                if (id >= 0 && id < nextId) {
                    myBucket[id] = -1;
                    myBucketIndex[id] = -1;
                    clearChunkDirty(id);
                }
                ids[i] = -1;
            }
            parityCounts[b] = 0;
            xPairCounts[b] = 0;
            zPairCounts[b] = 0;
        }
        pairListsDirty = true;
    }

    void runExactExchangeSweeps() {
        rebuildPairListsIfNeeded();
        int[][] yBuckets = parityBucketIds;
        int[] yCounts = parityCounts;
        int yc0 = yCounts[0], yc1 = yCounts[1], yc2 = yCounts[2], yc3 = yCounts[3];
        int yth0 = getTaskThreshold(yc0, 64),
                yth1 = getTaskThreshold(yc1, 64),
                yth2 = getTaskThreshold(yc2, 64),
                yth3 = getTaskThreshold(yc3, 64);
        int xc0 = xPairCounts[0], xc1 = xPairCounts[1], xc2 = xPairCounts[2], xc3 = xPairCounts[3];
        int zc0 = zPairCounts[0], zc1 = zPairCounts[1], zc2 = zPairCounts[2], zc3 = zPairCounts[3];
        int xth0 = getTaskThreshold(xc0, 64),
                xth1 = getTaskThreshold(xc1, 64),
                xth2 = getTaskThreshold(xc2, 64),
                xth3 = getTaskThreshold(xc3, 64);
        int zth0 = getTaskThreshold(zc0, 64),
                zth1 = getTaskThreshold(zc1, 64),
                zth2 = getTaskThreshold(zc2, 64),
                zth3 = getTaskThreshold(zc3, 64);

        int s = workEpoch;
        boolean fx = (s & 1) != 0, fz = (s & 2) != 0;
        int yPar = (s & 4) != 0 ? 1 : 0;
        int perm = s % 6;
        if (perm < 0) perm += 6;

        switch (perm) {
            case 0 -> {
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
            }
            case 1 -> {
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
            }
            case 2 -> {
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
            }
            case 3 -> {
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
            }
            case 4 -> {
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
            }
            default -> {
                sweepZ(
                        zPairAByBucket,
                        zPairBByBucket,
                        zc0,
                        zc1,
                        zc2,
                        zc3,
                        zth0,
                        zth1,
                        zth2,
                        zth3,
                        fz);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepX(
                        xPairAByBucket,
                        xPairBByBucket,
                        xc0,
                        xc1,
                        xc2,
                        xc3,
                        xth0,
                        xth1,
                        xth2,
                        xth3,
                        fx);
            }
        }
    }

    void runDecayEmissionAndEffects() {
        int[][] b = parityBucketIds;
        int[] c = parityCounts;
        int c0 = c[0], c1 = c[1], c2 = c[2], c3 = c[3];
        int th0 = getTaskThreshold(c0, 64),
                th1 = getTaskThreshold(c1, 64),
                th2 = getTaskThreshold(c2, 64),
                th3 = getTaskThreshold(c3, 64);
        var t0 = new DecayEmissionTask(b[0], 0, c0, th0).fork();
        var t1 = new DecayEmissionTask(b[1], 0, c1, th1).fork();
        var t2 = new DecayEmissionTask(b[2], 0, c2, th2).fork();
        new DecayEmissionTask(b[3], 0, c3, th3).invoke();
        t0.join();
        t1.join();
        t2.join();
    }

    void maybeQueueDestroy(long sck, int pocketIndex, int slot) {
        if (!worldRadEffects) return;
        long pk = pocketKey(sck, pocketIndex, slot);
        if (pk == Long.MIN_VALUE) return;
        long seed = HashCommon.mix(pk ^ workEpochSalt);
        if (Long.compareUnsigned(HashCommon.mix(seed + 0xD1B54A32D192ED03L), DESTROY_PROB_U64)
                < 0) {
            if (tickDelay == 1) pocketToDestroy = pk;
            else destructionQueue.offer(pk);
        }
    }

    void maybeQueueFog(long sck, int pocketIndex, int slot) {
        if (fogProbU64 == 0L) return;
        long pk = pocketKey(sck, pocketIndex, slot);
        if (pk == Long.MIN_VALUE) return;
        long seed = HashCommon.mix(pk ^ workEpochSalt);
        if (Long.compareUnsigned(seed, fogProbU64) < 0) fogQueue.offer(pk);
    }

    void ensureRadiationDirtyCapacity(int need) {
        if (radiationDirtyIds.length >= need) return;
        int n = grow(radiationDirtyIds.length, need);
        radiationDirtyIds = Arrays.copyOf(radiationDirtyIds, n);
        radiationDirtyCks = Arrays.copyOf(radiationDirtyCks, n);
    }

    @ServerThread
    void queueRadiationDirty(int id) {
        if (radiationDirtyIndex[id] >= 0) return;
        ensureRadiationDirtyCapacity(radiationDirtyCount + 1);
        int i = radiationDirtyCount++;
        radiationDirtyIds[i] = id;
        radiationDirtyCks[i] = cks[id];
        radiationDirtyIndex[id] = i;
    }

    @ServerThread
    void removeRadiationDirty(int id) {
        int i = radiationDirtyIndex[id];
        if (i < 0) return;
        int last = --radiationDirtyCount;
        int swapId = radiationDirtyIds[last];
        long swapCk = radiationDirtyCks[last];
        if (i != last) {
            radiationDirtyIds[i] = swapId;
            radiationDirtyCks[i] = swapCk;
            if (swapId >= 0 && swapId < radiationDirtyIndex.length) radiationDirtyIndex[swapId] = i;
        }
        radiationDirtyIds[last] = -1;
        radiationDirtyCks[last] = 0L;
        radiationDirtyIndex[id] = -1;
    }

    @ServerThread
    void collectMarkedRadiationDirty() {
        for (int id = 0, n = nextId; id < n; id++) {
            if (mcChunks[id] == null || !isChunkDirty(id)) continue;
            clearChunkDirty(id);
            queueRadiationDirty(id);
        }
    }

    @ServerThread
    @SuppressWarnings("unchecked")
    void saveRadiationDirty(boolean flush) {
        rebuildLoadedSections();

        if (flush) applyAndClearQueuedWrites();
        collectMarkedRadiationDirty();
        CompletableFuture<SidecarPayload>[] futures =
                (CompletableFuture<SidecarPayload>[]) new CompletableFuture[radiationDirtyCount];
        int count = 0;
        while (radiationDirtyCount > 0) {
            int i = radiationDirtyCount - 1;
            int id = radiationDirtyIds[i];
            long ck = radiationDirtyCks[i];
            removeRadiationDirty(id);
            if (id < 0 || id >= nextId || cks[id] != ck) continue;
            LevelChunk chunk = mcChunks[id];
            if (chunk != null) {
                CompletableFuture<SidecarPayload> future = encodeSidecarIfNeeded(id, ck, chunk);
                if (future != null) futures[count++] = future;
            }
        }
        flushEncodedSidecars(futures, count, !flush);
    }

    @ServerThread
    void saveRadiationDirty(LevelChunk chunk) {
        rebuildLoadedSections();
        long ck = ChunkPos.pack(chunk.getPos().x(), chunk.getPos().z());
        int id = getId(ck);
        if (id < 0 || id >= nextId || cks[id] != ck) return;

        applyAndClearQueuedWrites(id);
        if (isChunkDirty(id)) {
            clearChunkDirty(id);
            queueRadiationDirty(id);
        }
        if (radiationDirtyIndex[id] < 0) return;
        removeRadiationDirty(id);
        byte[] payload;
        try {
            payload = tryEncodePayload(ck);
        } catch (RuntimeException ex) {
            queueRadiationDirty(id);
            throw ex;
        }
        writeSidecar(world, chunk.getPos(), payload, true);
    }

    @ServerThread
    @Nullable CompletableFuture<SidecarPayload> encodeSidecarIfNeeded(
            int id, long ck, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return new SidecarPayload(id, ck, pos, tryEncodePayload(ck), null);
                    } catch (RuntimeException ex) {
                        return new SidecarPayload(id, ck, pos, null, ex);
                    }
                },
                RAD_POOL);
    }

    @ServerThread
    void flushEncodedSidecars(
            CompletableFuture<SidecarPayload>[] futures, int count, boolean logAsyncFailure) {
        RuntimeException failure = null;
        for (int i = 0; i < count; i++) {
            try {
                flushEncodedSidecar(futures[i], logAsyncFailure);
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    @ServerThread
    void flushEncodedSidecar(CompletableFuture<SidecarPayload> future, boolean logAsyncFailure) {
        SidecarPayload encoded;
        try {
            encoded = future.join();
        } catch (RuntimeException ex) {
            if (logAsyncFailure) {
                NuclearTech.LOGGER.error(
                        "Radiation sidecar encode failed for chunk in dimension {}",
                        world.dimension().identifier(),
                        ex);
                return;
            }
            throw ex;
        }
        if (encoded.id < 0 || encoded.id >= nextId || cks[encoded.id] != encoded.ck) return;
        if (encoded.failure != null) {
            queueRadiationDirty(encoded.id);
            if (logAsyncFailure) {
                NuclearTech.LOGGER.error(
                        "Radiation sidecar encode failed for chunk {} in dimension {}",
                        encoded.pos,
                        world.dimension().identifier(),
                        encoded.failure);
                return;
            }
            throw encoded.failure;
        }
        writeSidecar(world, encoded.pos, encoded.payload, logAsyncFailure);
    }

    @ServerThread
    void clearQueuedRadiationDirty() {
        for (int i = 0; i < radiationDirtyCount; i++) {
            int id = radiationDirtyIds[i];
            if (id >= 0 && id < radiationDirtyIndex.length) radiationDirtyIndex[id] = -1;
            radiationDirtyIds[i] = -1;
            radiationDirtyCks[i] = 0L;
        }
        radiationDirtyCount = 0;
    }

    @ServerThread
    void rebuildLoadedSections() {
        rebuildDirtySections();
        refreshDiffusivity();
    }

    @ServerThread
    void refreshDiffusivity() {
        if (diffusivityDirty.isEmpty()) return;
        IntArrayList ids = diffusivityRefreshIds;
        ids.clear();
        for (LongIterator it = diffusivityDirty.iterator(); it.hasNext(); ) {
            long sck = it.nextLong();
            int id = getId(Library.sectionToChunkLong(sck));
            int slot = slotOf(SectionPos.y(sck));
            if (id < 0 || slot < 0 || mcChunks[id] == null) continue;
            LevelChunkSection[] stor = mcChunks[id].getSections();
            LevelChunkSection section = slot < stor.length ? stor[slot] : null;
            int secIdx = id * sectionsPerChunk + slot;
            switch (getKind(id, slot)) {
                case KIND_UNI ->
                        uniformDiffusivity[secIdx] =
                                RadiationSystemNT.scanSectionDiffusivity(
                                        section, null, 1, singleVolumeScratch, null);
                case KIND_SINGLE -> {
                    SingleMaskedSectionRef single = (SingleMaskedSectionRef) complexSecs[secIdx];
                    singleVolumeScratch[0] = single.volume;
                    single.diffusivity =
                            RadiationSystemNT.scanSectionDiffusivity(
                                    section, single.pocketData, 1, singleVolumeScratch, null);
                }
                case KIND_MULTI -> {
                    MultiSectionRef multi = (MultiSectionRef) complexSecs[secIdx];
                    RadiationSystemNT.scanSectionDiffusivity(
                            section,
                            multi.pocketData,
                            multi.pocketCount & 0xFFFF,
                            multi.volume,
                            multi.pocketDiffusivity);
                }
                default -> {
                    continue;
                }
            }
            if (!ids.contains(id)) ids.add(id);
        }
        diffusivityDirty.clear();
        for (int i = 0; i < ids.size(); i++) pushDiffusivityToBackend(ids.getInt(i));
    }

    void runSweepPhases() {
        long time = System.nanoTime();
        applyAndClearQueuedWrites();
        nextWorkEpoch();
        if (backend != null) {

            backend.step(workEpochSalt, workEpoch, fogQueue::offer, destructionQueue::offer);
        } else {

            runDecayEmissionAndEffects();
            runExactExchangeSweeps();
        }
        cleanupAndLog(time);
    }

    void cleanupAndLog(long time) {
        logProfilingMessage(time);
    }

    void logProfilingMessage(long stepStartNs) {
        if (!RadiationConfig.enableDebugMode) return;
        double ms = (System.nanoTime() - stepStartNs) * 1.0e-6;
        profSteps++;
        profTotalMs += ms;
        if (ms > profMaxMs) profMaxMs = ms;
        DoubleArrayList samples = profSamplesMs;
        if (samples == null) {
            profSamplesMs = samples = new DoubleArrayList(8192);
        }
        int n = samples.size();
        if (n < 8192) {
            samples.add(ms);
        } else {
            long seen = profSteps;
            long r = HashCommon.mix(workEpochSalt + seen * 0x9E3779B97F4A7C15L);
            long j = Long.remainderUnsigned(r, seen);
            if (j < 8192) samples.set((int) j, ms);
        }
        executionTimeAccumulator += ms;
        if (++executionSampleCount < PROFILE_WINDOW) return;
        double totalMs = executionTimeAccumulator;
        double avgWinMs = r3(totalMs / PROFILE_WINDOW);
        NuclearTech.LOGGER.info(
                "[RadiationSystemNT] dim {} avg {} ms/step over last {} steps (total {} ms, last {} ms)",
                world.dimension().identifier(),
                avgWinMs,
                PROFILE_WINDOW,
                (int) Math.rint(totalMs),
                r3(ms));
        executionTimeAccumulator = 0.0D;
        executionSampleCount = 0;
    }

    @ServerThread
    void logLifetimeProfiling() {
        if (!RadiationConfig.enableDebugMode) return;
        long steps = profSteps;
        if (steps <= 0) return;
        Object dimId = world.dimension().identifier();
        double avgMs = profTotalMs / (double) steps;
        DoubleArrayList samples = profSamplesMs;
        int n = (samples == null) ? 0 : samples.size();
        if (n == 0) {
            NuclearTech.LOGGER.info(
                    "[RadiationSystemNT] dim {} lifetime: steps={}, avg={} ms, max={} ms",
                    dimId,
                    steps,
                    r3(avgMs),
                    r3(profMaxMs));
            return;
        }
        double[] a = Arrays.copyOf(samples.elements(), n);
        DoubleArrays.radixSort(a);
        int k1 = Math.max(1, (int) Math.ceil(n * 0.01));
        int k01 = Math.max(1, (int) Math.ceil(n * 0.001));
        double onePctHighAvg = meanOfLargestK(a, k1);
        double pointOnePctHigh = meanOfLargestK(a, k01);
        double p99 = a[Math.min(n - 1, (int) Math.ceil(n * 0.99) - 1)];
        double p999 = a[Math.min(n - 1, (int) Math.ceil(n * 0.999) - 1)];
        NuclearTech.LOGGER.info(
                "[RadiationSystemNT] dim {} lifetime: steps={}, avg={} ms, 1% high(avg)={} ms, 0.1% high(avg)={} ms, p99={} ms, p999={} ms, max={} ms (sampleN={})",
                dimId,
                steps,
                r3(avgMs),
                r3(onePctHighAvg),
                r3(pointOnePctHigh),
                r3(p99),
                r3(p999),
                r3(profMaxMs),
                n);
        profSamplesMs = null;
    }

    void rebuildDirtySections() {
        int dirtyChunks = dirtyCk.slotSize;
        if (dirtyChunks == 0) return;
        ensureDirtyChunkRefCapacity(dirtyChunks);
        LongArrayList toRelink = dirtyToRebuildScratch;
        toRelink.clear();
        int batch = 0;
        for (int i = 0; i < dirtyChunks; i++) {
            int pos = dirtyCk.slots[i];
            long ck = dirtyCk.keys[pos];
            int id = dirtyCk.ids[pos];
            if (id < 0 || id >= nextId || cks[id] != ck || mcChunks[id] == null) continue;
            int maskBase = pos * wordsPerChunk;
            int batchBase = batch * wordsPerChunk;
            int any = 0;
            long sckBase = Library.sectionToLong(ck, minSectionY);
            for (int word = 0; word < wordsPerChunk; word++) {
                int mask = dirtyCk.masks[maskBase + word];
                dirtyChunkMasksScratchArr[batchBase + word] = mask;
                any |= mask;
                if (mask == 0) continue;
                int baseSlot = word << 5;
                if (backend != null) backend.submitSectionGeometry(mcChunks[id], baseSlot, mask);
                for (int m = mask; m != 0; m &= m - 1) {
                    int slot = baseSlot + Integer.numberOfTrailingZeros(m);
                    if (slot < sectionsPerChunk)
                        toRelink.add(Library.setSectionY(sckBase, minSectionY + slot));
                }
            }
            if (any != 0) dirtyChunkIdsScratch[batch++] = id;
        }
        dirtyCk.reset();
        if (batch == 0) return;
        new RebuildDirtyChunkBatchTask(
                        dirtyChunkIdsScratch,
                        dirtyChunkMasksScratchArr,
                        0,
                        batch,
                        getTaskThreshold(batch, 8))
                .invoke();

        if (backend != null) {
            for (int i = 0; i < batch; i++) {
                pushSourcesToBackend(dirtyChunkIdsScratch[i]);
                pushDiffusivityToBackend(dirtyChunkIdsScratch[i]);
            }
        }
        if (!toRelink.isEmpty()) relinkKeys(toRelink.elements(), toRelink.size());
    }

    void pushSourcesToBackend(int id) {
        if (backend == null || id < 0) return;
        int total = 0;
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            RadiationSystemNT.SectionSources s = sectionSources[id * sectionsPerChunk + slot];
            if (s != null) total += s.size;
        }
        if (sourceKeyScratch.length < sectionsPerChunk) {
            sourceKeyScratch = new long[sectionsPerChunk];
            sourceCountScratch = new int[sectionsPerChunk];
        }
        if (sourcePocketScratch.length < total) {
            sourcePocketScratch = new short[Math.max(total, 16)];
            sourceMultScratch = new int[Math.max(total, 16)];
            sourceEmissionScratch = new double[Math.max(total, 16)];
            sourceSaturationScratch = new double[Math.max(total, 16)];
        }
        int at = 0;
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            sourceKeyScratch[slot] =
                    RadsimKeys.sectionKey(ChunkPos.getX(cks[id]), slot, ChunkPos.getZ(cks[id]));
            RadiationSystemNT.SectionSources s = sectionSources[id * sectionsPerChunk + slot];
            int n = s == null ? 0 : s.size;
            sourceCountScratch[slot] = n;
            for (int e = 0; e < n; e++, at++) {
                sourcePocketScratch[at] = s.pocket[e];
                sourceMultScratch[at] = s.count[e];
                sourceEmissionScratch[at] = s.emission[e];
                sourceSaturationScratch[at] = s.saturation[e];
            }
        }
        backend.submitSectionSources(
                sourceKeyScratch,
                sourceCountScratch,
                sourcePocketScratch,
                sourceMultScratch,
                sourceEmissionScratch,
                sourceSaturationScratch,
                sectionsPerChunk,
                total);
    }

    void pushDiffusivityToBackend(int id) {
        if (backend == null || !diffusivityTransport || id < 0) return;
        int total = 0;
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            total += pocketCountOf(id, slot);
        }
        if (diffKeyScratch.length < sectionsPerChunk) {
            diffKeyScratch = new long[sectionsPerChunk];
            diffCountScratch = new int[sectionsPerChunk];
        }
        if (diffValueScratch.length < total) diffValueScratch = new float[Math.max(total, 16)];

        int at = 0;
        int cx = ChunkPos.getX(cks[id]);
        int cz = ChunkPos.getZ(cks[id]);
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            diffKeyScratch[slot] = RadsimKeys.sectionKey(cx, slot, cz);
            int secIdx = id * sectionsPerChunk + slot;
            int n = pocketCountOf(id, slot);
            diffCountScratch[slot] = n;
            if (n == 0) continue;
            SectionRef ref = complexSecs[secIdx];
            if (ref instanceof MultiSectionRef multi) {
                System.arraycopy(multi.pocketDiffusivity, 0, diffValueScratch, at, n);
            } else if (ref instanceof SingleMaskedSectionRef single) {
                diffValueScratch[at] = single.diffusivity;
            } else {
                diffValueScratch[at] = uniformDiffusivity[secIdx];
            }
            at += n;
        }
        backend.submitSectionDiffusivity(
                diffKeyScratch, diffCountScratch, diffValueScratch, sectionsPerChunk, total);
    }

    private int pocketCountOf(int id, int slot) {
        int kind = getKind(id, slot);
        if (kind == KIND_NONE) return 0;
        if (kind != KIND_MULTI) return 1;
        SectionRef ref = complexSecs[id * sectionsPerChunk + slot];
        return ref == null ? 0 : (ref.pocketCount & 0xFFFF);
    }

    EditTable editsFor(long ck) {
        int id = ensureId(ck);
        EditTable t = editsById[id];
        if (t != null) return t;
        t = editTablePool.borrow();
        editsById[id] = t;
        editedChunkIds.add(id);
        return t;
    }

    void queueSet(long sck, int local, double density) {
        int slot = slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        long ck = Library.sectionToChunkLong(sck);
        long seq = ++setSeq;
        editsFor(ck).putSet(editKey(slot, local), density, seq, slot);
        if (touchedSectionWords != null)
            touchedSectionWords[wordIndex(getId(ck), slot)] |= 1 << (slot & 31);
    }

    void queueAdd(long sck, int local, double delta) {
        int slot = slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        long ck = Library.sectionToChunkLong(sck);
        editsFor(ck).addTo(editKey(slot, local), delta, slot);
        if (touchedSectionWords != null)
            touchedSectionWords[wordIndex(getId(ck), slot)] |= 1 << (slot & 31);
    }

    void queueEmit(long sck, int local, double emission, double saturation) {
        int slot = slotOf(SectionPos.y(sck));
        if (slot < 0) return;
        long ck = Library.sectionToChunkLong(sck);
        editsFor(ck).addSource(editKey(slot, local), emission, saturation, slot);
        if (touchedSectionWords != null)
            touchedSectionWords[wordIndex(getId(ck), slot)] |= 1 << (slot & 31);
    }

    void clearTouchedSections(int id) {
        if (touchedSectionWords != null) {
            int base = wordBase(id);
            Arrays.fill(touchedSectionWords, base, base + wordsPerChunk, 0);
        }
    }

    void clearQueuedWrites() {
        IntArrayList ids = editedChunkIds;
        int[] elements = ids.elements();
        for (int i = 0, n = ids.size(); i < n; i++) {
            int id = elements[i];
            EditTable t = editsById[id];
            if (t == null) continue;
            clearTouchedSections(id);
            editTablePool.recycle(t);
            editsById[id] = null;
        }
        ids.clear();
    }

    void applyQueuedWrites(
            long sectionKey,
            short @Nullable [] pocketData,
            int pocketCount,
            double[] densityOut,
            @Nullable EditTable edits) {
        if (edits == null || edits.isEmpty()) return;
        int slot = slotOf(SectionPos.y(sectionKey));
        if (slot < 0) return;
        int touchedIndex =
                touchedSectionWords == null
                        ? -1
                        : wordIndex(getId(Library.sectionToChunkLong(sectionKey)), slot);
        int touched =
                touchedSectionWords == null
                        ? edits.touchedSyMask
                        : touchedSectionWords[touchedIndex];
        if ((touched & (1 << (slot & 31))) == 0) return;
        double[] addPocket = TL_ADD.get();
        double[] setPocket = TL_SET.get();
        long[] bestSeq = TL_BEST_SET_SEQ.get();
        double[] srcWeight = TL_SRC_WEIGHT.get();
        double[] srcNumerator = TL_SRC_NUMERATOR.get();

        Arrays.fill(addPocket, 0, pocketCount, 0.0d);
        Arrays.fill(setPocket, 0, pocketCount, 0.0d);
        Arrays.fill(bestSeq, 0, pocketCount, 0L);
        Arrays.fill(srcWeight, 0, pocketCount, 0.0d);
        Arrays.fill(srcNumerator, 0, pocketCount, 0.0d);

        int e = edits.epoch;
        int n = edits.slotSize;
        int[] slots = edits.slots;
        int[] st = edits.stamps;
        long[] keys = edits.keys;
        double[] addAcc = edits.addAcc;
        double[] setV = edits.setVal;
        long[] setS = edits.setSeq;
        byte[] flags = edits.flags;

        for (int i = 0; i < n; i++) {
            int pos = slots[i];
            assert st[pos] == e;
            long k = keys[pos];
            if (editKeySlot(k) != slot) continue;
            int local = editKeyLocal(k);
            int pi;
            if (pocketData == null) {
                pi = 0;
            } else {
                pi = pocketData[local];
                if (pi < 0 || pi >= pocketCount) continue;
            }
            double dAdd = addAcc[pos];
            if (dAdd != 0.0d) addPocket[pi] += dAdd;
            if ((flags[pos] & EditTable.HAS_SET) != 0) {
                long seq = setS[pos];
                if (Long.compareUnsigned(seq, bestSeq[pi]) > 0) {
                    bestSeq[pi] = seq;
                    setPocket[pi] = setV[pos];
                }
            }
        }

        if (edits.srcCount != 0) {
            int[] srcHead = edits.srcHead;
            int[] srcNext = edits.srcNext;
            double[] srcEmission = edits.srcEmission;
            double[] srcSaturation = edits.srcSaturation;
            for (int i = 0; i < n; i++) {
                int pos = slots[i];
                int src = srcHead[pos];
                if (src == EditTable.NO_SOURCE) continue;
                long k = keys[pos];
                if (editKeySlot(k) != slot) continue;
                int local = editKeyLocal(k);
                int pi;
                if (pocketData == null) {
                    pi = 0;
                } else {
                    pi = pocketData[local];
                    if (pi < 0 || pi >= pocketCount) continue;
                }
                double c = bestSeq[pi] != 0L ? setPocket[pi] : densityOut[pi];
                for (; src != EditTable.NO_SOURCE; src = srcNext[src]) {
                    double emission = srcEmission[src];
                    double saturation = srcSaturation[src];
                    if (!Emission.active(emission, saturation, c)) continue;
                    srcWeight[pi] += Emission.weight(emission, saturation);
                    srcNumerator[pi] += Emission.numeratorTerm(emission, saturation);
                }
            }
        }

        for (int p = 0; p < pocketCount; p++) {
            double base = bestSeq[p] != 0L ? setPocket[p] : densityOut[p];
            densityOut[p] =
                    sanitize(Emission.relax(base, srcWeight[p], srcNumerator[p]) + addPocket[p]);
        }

        if (touchedSectionWords == null) edits.touchedSyMask &= ~(1 << slot);
        else touchedSectionWords[touchedIndex] &= ~(1 << (slot & 31));
    }

    void applyAndClearQueuedWrites() {
        IntArrayList ids = editedChunkIds;
        int[] elements = ids.elements();
        for (int i = 0, n = ids.size(); i < n; i++) {
            int id = elements[i];
            EditTable t = editsById[id];
            if (t == null) continue;

            editsById[id] = null;
            if (!t.isEmpty()) applyChunkQueuedWrites(id, t);
            clearTouchedSections(id);
            editTablePool.recycle(t);
        }
        ids.clear();
    }

    void applyAndClearQueuedWrites(int id) {
        EditTable t = editsById[id];
        if (t == null) return;
        editsById[id] = null;
        if (!t.isEmpty()) applyChunkQueuedWrites(id, t);
        clearTouchedSections(id);
        editTablePool.recycle(t);
    }

    private void applyChunkQueuedWrites(int id, EditTable edits) {
        long ck = cks[id];
        int cx = ChunkPos.getX(ck);
        int cz = ChunkPos.getZ(ck);
        double[] densities = TL_TEMP_DENSITIES.get();

        int metadataBase = wordBase(id);
        for (int word = 0; word < wordsPerChunk; word++) {
            int m =
                    touchedSectionWords == null
                            ? edits.touchedSyMask
                            : touchedSectionWords[metadataBase + word];
            while (m != 0) {
                int slot = (word << 5) + Integer.numberOfTrailingZeros(m);
                m &= (m - 1);
                if (slot >= sectionsPerChunk) continue;
                int kind = getKind(id, slot);

                if (kind == KIND_NONE) continue;

                int secIdx = id * sectionsPerChunk + slot;
                int pocketCount;
                short[] pocketData;
                MultiSectionRef multi = null;
                if (kind < KIND_MULTI) {
                    pocketCount = 1;
                    pocketData = kind == KIND_UNI ? null : complexSecs[secIdx].pocketData;
                    densities[0] = uniformRads[secIdx];
                } else {
                    multi = (MultiSectionRef) complexSecs[secIdx];
                    pocketCount = multi.pocketCount & 0xFFFF;
                    pocketData = multi.pocketData;
                    for (int p = 0; p < pocketCount; p++) densities[p] = multi.data[p << 1];
                }

                applyQueuedWrites(
                        SectionPos.asLong(cx, minSectionY + slot, cz),
                        pocketData,
                        pocketCount,
                        densities,
                        edits);

                boolean active = false;
                if (multi == null) {
                    uniformRads[secIdx] = densities[0];
                    active = densities[0] != 0.0D;
                } else {
                    for (int p = 0; p < pocketCount; p++) {
                        double d = densities[p];
                        multi.data[p << 1] = d;
                        active |= d != 0.0D;
                    }
                }

                if (active) markActive(id, slot);
                setChunkDirty(id);
            }
        }
    }

    double sanitize(double v) {
        return RadiationSystemNT.sanitize(v, minBound);
    }

    int onChunkLoaded(int cx, int cz, LevelChunk chunk) {
        assert ((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) == 0;
        long ck = ChunkPos.pack(cx, cz);
        int id = ensureId(ck);
        boolean wasLoaded = mcChunks[id] != null;
        mcChunks[id] = chunk;
        if (!wasLoaded) linkLoadedNeighbors(id);
        if (myBucketIndex[id] < 0) addLoadedToBucket(id);
        else assert myBucket[id] >= 0 && myBucket[id] < 4;
        return id;
    }

    void ensureDirtyChunkRefCapacity(int need) {
        if (dirtyChunkIdsScratch.length >= need) return;
        int n = grow(dirtyChunkIdsScratch.length, need);
        dirtyChunkIdsScratch = Arrays.copyOf(dirtyChunkIdsScratch, n);
        if (dirtyChunkMasksScratchArr.length < n * wordsPerChunk) {
            dirtyChunkMasksScratchArr = Arrays.copyOf(dirtyChunkMasksScratchArr, n * wordsPerChunk);
        }
    }

    int nextWorkEpoch() {
        int e = ++workEpoch == 0 ? ++workEpoch : workEpoch;
        workEpochSalt = HashCommon.murmurHash3(worldSalt + (long) e * 0x9E3779B97F4A7C15L);
        return e;
    }

    void unloadChunk(int cx, int cz) {
        long ck = ChunkPos.pack(cx, cz);
        int id = getId(ck);
        if (id < 0) return;
        if (mcChunks[id] == null) return;
        removeLoadedFromBucket(id);
        unlinkLoadedNeighbors(id);
        mcChunks[id] = null;
        int metadataBase = wordBase(id);
        Arrays.fill(chunkActiveDirty, metadataBase, metadataBase + wordsPerChunk, 0L);
    }

    void removeChunkRef(long ck) {
        int id = coordToId.remove(ck);
        if (id < 0) return;
        assert mcChunks[id] == null : "removeChunkRef called for loaded chunk; must unload first";
        assert myBucketIndex[id] < 0 : "Bucket membership leaked across unload/remove";
        removeRadiationDirty(id);
        int si = id * sectionsPerChunk;
        for (int sy = 0; sy < sectionsPerChunk; sy++) {
            SectionRef old = complexSecs[si + sy];
            if (old != null) pocketDataPool.recycle(old.pocketData);
        }
        clearResidentState(id);
        pushFreeId(id);
    }

    private static final ThreadLocal<int[]> TL_ENTRY_SYPI =
            ThreadLocal.withInitial(() -> new int[512]);
    private static final ThreadLocal<double[]> TL_ENTRY_DENSITY =
            ThreadLocal.withInitial(() -> new double[512]);

    void pushDensitiesToBackend(long ck, int id) {
        if (backend == null || id < 0) return;
        int[] entrySypi = TL_ENTRY_SYPI.get();
        double[] entryDensity = TL_ENTRY_DENSITY.get();
        int rows = 0;
        for (int slot = 0; slot < sectionsPerChunk; slot++) {
            int kind = getKind(id, slot);
            if (kind == KIND_NONE) continue;
            int secIdx = id * sectionsPerChunk + slot;
            if (kind == KIND_MULTI && complexSecs[secIdx] instanceof MultiSectionRef m) {
                for (int pocket = 0, count = m.pocketCount & 0xFFFF; pocket < count; pocket++) {
                    if (rows == entrySypi.length) {
                        TL_ENTRY_SYPI.set(entrySypi = Arrays.copyOf(entrySypi, rows * 2));
                        TL_ENTRY_DENSITY.set(entryDensity = Arrays.copyOf(entryDensity, rows * 2));
                    }
                    entrySypi[rows] = (slot << 11) | pocket;
                    entryDensity[rows++] = m.data[pocket << 1];
                }
            } else {
                if (rows == entrySypi.length) {
                    TL_ENTRY_SYPI.set(entrySypi = Arrays.copyOf(entrySypi, rows * 2));
                    TL_ENTRY_DENSITY.set(entryDensity = Arrays.copyOf(entryDensity, rows * 2));
                }
                entrySypi[rows] = slot << 11;
                entryDensity[rows++] = uniformRads[secIdx];
            }
        }
        backend.loadChunk(ck, rows, entrySypi, entryDensity);
        if (rows != 0) {

            int metadataBase = wordBase(id);
            for (int word = 0; word < wordsPerChunk; word++) {
                if (chunkSourceMask[metadataBase + word] != 0L) {
                    pushSourcesToBackend(id);
                    break;
                }
            }
            pushDiffusivityToBackend(id);
        }
    }

    private void syncDensitiesFromBackend(long ck, int id) {
        if (backend == null) return;
        int[] entrySypi = TL_ENTRY_SYPI.get();
        double[] entryDensity = TL_ENTRY_DENSITY.get();
        int rows;
        while ((rows = backend.dumpChunk(ck, entrySypi, entryDensity)) >= entrySypi.length) {
            TL_ENTRY_SYPI.set(entrySypi = new int[entrySypi.length * 2]);
            TL_ENTRY_DENSITY.set(entryDensity = new double[entryDensity.length * 2]);
        }
        for (int i = 0; i < rows; i++) {
            int sypi = entrySypi[i];
            int slot = sypi >>> 11;
            int pocket = sypi & 0x7FF;
            if (slot >= sectionsPerChunk) continue;
            int secIdx = id * sectionsPerChunk + slot;
            if (getKind(id, slot) == KIND_MULTI
                    && complexSecs[secIdx] instanceof MultiSectionRef m) {
                int dataIdx = pocket << 1;
                if (dataIdx < m.data.length) m.data[dataIdx] = entryDensity[i];
            } else {
                uniformRads[secIdx] = entryDensity[i];
            }
        }
    }

    byte @Nullable [] tryEncodePayload(long ck) {
        int id = getId(ck);
        if (id < 0) return null;
        syncDensitiesFromBackend(ck, id);

        ByteBuffer buf = TL_ENCODE_BUF.get();
        buf.clear();
        buf.put((byte) 0);
        int sectionCount = 0;
        double[] densities = TL_TEMP_DENSITIES.get();

        for (int sy = 0; sy < sectionsPerChunk; sy++) {
            int secIdx = id * sectionsPerChunk + sy;
            int kind = getKind(id, sy);
            if (kind == KIND_NONE) continue;

            int pocketCount;
            short[] pocketData;
            if (kind < KIND_MULTI) {
                pocketCount = 1;
                pocketData = kind == KIND_UNI ? null : complexSecs[secIdx].pocketData;
                densities[0] = uniformRads[secIdx];
            } else {
                MultiSectionRef section = (MultiSectionRef) complexSecs[secIdx];
                pocketCount = section.pocketCount & 0xFFFF;
                pocketData = section.pocketData;
                for (int pocket = 0; pocket < pocketCount; pocket++) {
                    densities[pocket] = section.data[pocket << 1];
                }
            }

            int nonZeroCount = 0;
            for (int pocket = 0; pocket < pocketCount; pocket++) {
                if (sanitize(densities[pocket]) != 0.0D) nonZeroCount++;
            }
            if (nonZeroCount == 0) continue;
            int bytes = 9 + (pocketData == null ? 0 : 512) + nonZeroCount * 10;
            if (buf.remaining() < bytes) {
                int required = buf.position() + bytes;
                ByteBuffer grown =
                        ByteBuffer.allocate(
                                Math.min(
                                        FMT8_MAX_BYTES - 4,
                                        Math.max(required, buf.capacity() * 2)));
                buf.flip();
                grown.put(buf);
                TL_ENCODE_BUF.set(buf = grown);
            }
            writeSectionRecord(
                    buf, minSectionY + sy, pocketCount, pocketData, densities, nonZeroCount);
            sectionCount++;
        }

        if (sectionCount == 0) return null;
        buf.put(0, (byte) sectionCount);
        buf.flip();

        byte[] out = new byte[4 + buf.limit()];
        out[0] = MAGIC_0;
        out[1] = MAGIC_1;
        out[2] = MAGIC_2;
        out[3] = FMT;
        buf.get(out, 4, buf.limit());
        return out;
    }

    boolean writeSectionRecord(
            ByteBuffer buf,
            int absoluteSectionY,
            int pocketCount,
            short @Nullable [] pocketData,
            double[] densities) {
        int nonZeroCount = 0;
        for (int pocket = 0; pocket < pocketCount; pocket++) {
            if (sanitize(densities[pocket]) != 0.0D) nonZeroCount++;
        }
        if (nonZeroCount == 0) return false;
        writeSectionRecord(buf, absoluteSectionY, pocketCount, pocketData, densities, nonZeroCount);
        return true;
    }

    private void writeSectionRecord(
            ByteBuffer buf,
            int absoluteSectionY,
            int pocketCount,
            short @Nullable [] pocketData,
            double[] densities,
            int nonZeroCount) {
        buf.putInt(absoluteSectionY);
        buf.put((byte) (pocketData == null ? 0 : 1));
        buf.putShort((short) pocketCount);
        buf.putShort((short) nonZeroCount);
        if (pocketData != null) {
            for (int word = 0; word < 64; word++) {
                long resistant = 0L;
                int base = word << 6;
                for (int bit = 0; bit < 64; bit++) {
                    if (pocketData[base + bit] == NO_POCKET) resistant |= 1L << bit;
                }
                buf.putLong(resistant);
            }
        }
        for (int pocket = 0; pocket < pocketCount; pocket++) {
            double density = sanitize(densities[pocket]);
            if (density == 0.0D) continue;
            buf.putShort((short) pocket);
            buf.putDouble(density);
        }
    }

    void readPayload(int cx, int cz, byte[] raw) throws DecodeException {
        ByteBuffer buf = ByteBuffer.wrap(raw, 4, raw.length - 4);
        int recordCount = buf.get() & 0xFF;
        if (recordCount == 0) throw new DecodeException("Empty FMT 8 payload");
        if (recordCount > MAX_SECTIONS_PER_CHUNK) {
            throw new DecodeException("Too many section records: " + recordCount);
        }

        PendingRad decoded = new PendingRad(sectionsPerChunk);
        int previousSectionY = Integer.MIN_VALUE;
        for (int record = 0; record < recordCount; record++) {
            int absoluteSectionY = buf.getInt();
            if (absoluteSectionY <= previousSectionY) {
                throw new DecodeException("Section records are not strictly ordered");
            }
            previousSectionY = absoluteSectionY;

            int topology = buf.get() & 0xFF;
            if (topology > 1) throw new DecodeException("Unknown topology: " + topology);
            int pocketCount = buf.getShort() & 0xFFFF;
            int nonZeroCount = buf.getShort() & 0xFFFF;
            if (pocketCount == 0 || pocketCount > MAX_POCKETS) {
                throw new DecodeException("Invalid pocket count: " + pocketCount);
            }
            if (nonZeroCount == 0 || nonZeroCount > pocketCount) {
                throw new DecodeException("Invalid nonzero count: " + nonZeroCount);
            }

            short[] pocketData = null;
            int[] volumes = new int[pocketCount];
            if (topology == 0) {
                if (pocketCount != 1) {
                    throw new DecodeException("Uniform topology has " + pocketCount + " pockets");
                }
                volumes[0] = SectionPos.SECTION_BLOCK_COUNT;
            } else {
                SectionMask resistant = new SectionMask();
                for (int word = 0; word < 64; word++) resistant.words[word] = buf.getLong();
                pocketData = new short[SectionPos.SECTION_BLOCK_COUNT];
                Arrays.fill(pocketData, NO_POCKET);
                int[] volumeScratch = TL_VOL_COUNTS.get();
                int actualCount =
                        floodFillPockets(
                                resistant, pocketData, TL_FF_QUEUE.get(), volumeScratch, null);
                if (actualCount != pocketCount) {
                    throw new DecodeException(
                            "Topology pocket count " + actualCount + " != " + pocketCount);
                }
                System.arraycopy(volumeScratch, 0, volumes, 0, pocketCount);
            }

            double[] densities = new double[pocketCount];
            int previousPocket = -1;
            for (int i = 0; i < nonZeroCount; i++) {
                int pocket = buf.getShort() & 0xFFFF;
                if (pocket <= previousPocket || pocket >= pocketCount) {
                    throw new DecodeException("Invalid pocket index: " + pocket);
                }
                previousPocket = pocket;
                double stored = buf.getDouble();
                double canonical = sanitize(stored);
                if (!Double.isFinite(stored)
                        || canonical == 0.0D
                        || Double.doubleToRawLongBits(canonical)
                                != Double.doubleToRawLongBits(stored)) {
                    throw new DecodeException("Non-canonical density in pocket " + pocket);
                }
                densities[pocket] = stored;
            }

            int sy = slotOf(absoluteSectionY);
            if (sy < 0) throw new DecodeException("Section outside dimension: " + absoluteSectionY);
            decoded.put(
                    sy, new PendingRad.SavedSection(pocketCount, pocketData, volumes, densities));
        }
        if (buf.hasRemaining())
            throw new DecodeException("Trailing payload bytes: " + buf.remaining());

        long ck = ChunkPos.pack(cx, cz);
        int id = ensureId(ck);
        int secBase = id * sectionsPerChunk;
        for (int sy = 0; sy < sectionsPerChunk; sy++) {
            SectionRef previous = complexSecs[secBase + sy];
            if (previous != null) pocketDataPool.recycle(previous.pocketData);
            complexSecs[secBase + sy] = null;
        }
        pending[id] = decoded.isEmpty() ? null : decoded;
        int metadataBase = wordBase(id);
        Arrays.fill(chunkKinds, metadataBase, metadataBase + wordsPerChunk, 0L);
        Arrays.fill(chunkActiveDirty, metadataBase, metadataBase + wordsPerChunk, 0L);
        Arrays.fill(chunkSourceMask, metadataBase, metadataBase + wordsPerChunk, 0L);
        clearTouchedSections(id);
        Arrays.fill(uniformRads, secBase, secBase + sectionsPerChunk, 0.0D);
        dirtyCk.add(ck, id);
    }

    void rebuildChunkPocketsLoaded(
            int ownerId,
            int sy,
            long sectionKey,
            @Nullable LevelChunkSection section,
            @Nullable EditTable edits) {
        int secIdx = (ownerId * sectionsPerChunk) + sy;
        int metadataIndex = wordIndex(ownerId, sy);
        int lane = sy & 31;
        chunkActiveDirty[metadataIndex] &= ~(1L << lane);
        long kinds = chunkKinds[metadataIndex];
        int oldKind = (int) ((kinds >>> (lane << 1)) & 3);
        SectionRef old = complexSecs[secIdx];
        PendingRad pr = pending[ownerId];
        assert (oldKind == KIND_NONE || oldKind == KIND_UNI) == (old == null);
        assert pr == null || !pr.hasSy(sy) || oldKind == KIND_NONE;
        short[] pocketData;

        int[] vols = TL_VOL_COUNTS.get();
        long[] sumXYZ = TL_SUM_XYZ.get();

        short[][] pOut = new short[1][];
        int pocketCount = computePocketMappingForRebuild(section, pOut, vols, sumXYZ);
        pocketData = pOut[0];
        assert pocketCount >= 0 && pocketCount <= MAX_POCKETS;
        assert pocketData != null || pocketCount == 1;

        if (pocketCount == 0) {
            if (old != null) pocketDataPool.recycle(old.pocketData);
            complexSecs[secIdx] = null;
            sectionSources[secIdx] = null;
            chunkSourceMask[metadataIndex] &= ~(1L << lane);
            chunkKinds[metadataIndex] = (chunkKinds[metadataIndex] & ~(3L << (lane << 1)));

            if (pr != null && pr.hasSy(sy)) {
                pr.clearSy(sy);
                if (pr.isEmpty()) pending[ownerId] = null;
            }
            return;
        }

        RadiationSystemNT.SectionSources sources =
                RadiationSystemNT.scanSectionSources(section, pocketData, pocketCount, vols);
        sectionSources[secIdx] = sources;
        if (sources == null) chunkSourceMask[metadataIndex] &= ~(1L << lane);
        else chunkSourceMask[metadataIndex] |= (1L << lane);

        int singleVolume0 = SectionPos.SECTION_BLOCK_COUNT;
        long singleFaceCounts = 0L;
        if (pocketCount == 1 && pocketData != null) {
            singleVolume0 = Math.max(1, vols[0]);
            for (int face = 0; face < 6; face++) {
                int base = face << 8;
                int c = 0;
                for (int t = 0; t < 256; t++) {
                    int idx = FACE_PLANE[base + t];
                    if (pocketData[idx] == 0) c++;
                }
                singleFaceCounts |= ((long) c & 0x1FFL) << (face * 9);
            }
        }

        double[] newMass = TL_NEW_MASS.get();
        Arrays.fill(newMass, 0, pocketCount, 0.0d);

        if (oldKind != KIND_NONE) {
            remapPocketMass(secIdx, oldKind, old, pocketCount, pocketData, vols, newMass);
        }

        double[] densities = TL_DENSITIES.get();
        for (int p = 0; p < pocketCount; p++) {
            int v = (pocketData == null) ? SectionPos.SECTION_BLOCK_COUNT : Math.max(1, vols[p]);
            double d = newMass[p] / (double) v;
            densities[p] = sanitize(d);
        }

        if (pr != null && pr.hasSy(sy)) {
            PendingRad.SavedSection saved = pr.take(sy);
            if (saved.hasSameTopology(pocketCount, pocketData)) {
                System.arraycopy(saved.densities(), 0, densities, 0, pocketCount);
            } else {
                remapSavedPocketMass(saved, pocketCount, pocketData, newMass);
                for (int p = 0; p < pocketCount; p++) {
                    int volume =
                            pocketData == null
                                    ? SectionPos.SECTION_BLOCK_COUNT
                                    : Math.max(1, vols[p]);
                    densities[p] = sanitize(newMass[p] / (double) volume);
                }
            }
            if (pr.isEmpty()) pending[ownerId] = null;
        }

        assert pr == null || !pr.hasSy(sy);

        applyQueuedWrites(sectionKey, pocketData, pocketCount, densities, edits);

        if (old != null) pocketDataPool.recycle(old.pocketData);

        if (pocketCount == 1 && pocketData == null) {
            double d = densities[0];
            uniformRads[secIdx] = d;
            if (uniformDiffusivity != null) {
                uniformDiffusivity[secIdx] =
                        RadiationSystemNT.scanSectionDiffusivity(section, null, 1, vols, null);
            }
            chunkKinds[metadataIndex] =
                    (chunkKinds[metadataIndex] & ~(3L << (lane << 1))) | (1L << (lane << 1));
            complexSecs[secIdx] = null;
            if (d != 0.0D) chunkActiveDirty[metadataIndex] |= (1L << lane);
            return;
        }

        if (pocketCount == 1) {
            double density = densities[0];

            double inv = 1.0d / (double) singleVolume0;
            double cx = sumXYZ[0] * inv;
            double cy = sumXYZ[1] * inv;
            double cz = sumXYZ[2] * inv;

            SingleMaskedSectionRef masked =
                    new SingleMaskedSectionRef(
                            secIdx, pocketData, singleVolume0, singleFaceCounts, cx, cy, cz);
            if (diffusivityTransport) {
                masked.diffusivity =
                        RadiationSystemNT.scanSectionDiffusivity(
                                section, pocketData, 1, vols, null);
            }
            uniformRads[secIdx] = density;

            complexSecs[secIdx] = masked;
            chunkKinds[metadataIndex] =
                    (chunkKinds[metadataIndex] & ~(3L << (lane << 1))) | (2L << (lane << 1));

            if (density != 0.0D) chunkActiveDirty[metadataIndex] |= (1L << lane);
            return;
        }

        float[] faceDists = new float[pocketCount * 6];
        for (int p = 0; p < pocketCount; p++) {
            int v = Math.max(1, vols[p]);
            double inv = 1.0d / (double) v;

            int sBase = p * 3;
            double cx = sumXYZ[sBase] * inv;
            double cy = sumXYZ[sBase + 1] * inv;
            double cz = sumXYZ[sBase + 2] * inv;

            int base = p * 6;
            faceDists[base] = (float) (cy + 0.5d);
            faceDists[base + 1] = (float) (15.5d - cy);
            faceDists[base + 2] = (float) (cz + 0.5d);
            faceDists[base + 3] = (float) (15.5d - cz);
            faceDists[base + 4] = (float) (cx + 0.5d);
            faceDists[base + 5] = (float) (15.5d - cx);
        }

        MultiSectionRef sc =
                new MultiSectionRef(secIdx, (short) pocketCount, pocketData, faceDists);
        if (sc.pocketDiffusivity != null) {
            RadiationSystemNT.scanSectionDiffusivity(
                    section, pocketData, pocketCount, vols, sc.pocketDiffusivity);
        }
        boolean active = false;
        for (int p = 0; p < pocketCount; p++) {
            int v = Math.max(1, vols[p]);
            int i2 = p << 1;
            double d = densities[p];
            sc.data[i2] = d;
            sc.data[i2 + 1] = 1.0d / (double) v;
            sc.volume[p] = v;
            if (d != 0.0D) active = true;
        }

        if (active) chunkActiveDirty[metadataIndex] |= (1L << lane);
        complexSecs[secIdx] = sc;
        chunkKinds[metadataIndex] =
                (chunkKinds[metadataIndex] & ~(3L << (lane << 1))) | (3L << (lane << 1));
    }

    int computePocketMappingForRebuild(
            @Nullable LevelChunkSection section,
            short[][] outPocketData,
            int[] vols,
            long[] sumXYZ) {
        outPocketData[0] = null;

        if (section == null || section.hasOnlyAir()) {
            Arrays.fill(vols, 0);
            vols[0] = SectionPos.SECTION_BLOCK_COUNT;
            Arrays.fill(sumXYZ, 0);
            return 1;
        }

        SectionMask resistant = scanResistantMask(section);
        if (resistant == null || resistant.isEmpty()) {
            Arrays.fill(vols, 0);
            vols[0] = SectionPos.SECTION_BLOCK_COUNT;
            Arrays.fill(sumXYZ, 0);
            return 1;
        }

        short[] scratch = pocketDataPool.borrow();
        int[] queue = TL_FF_QUEUE.get();

        int pc = floodFillPockets(resistant, scratch, queue, vols, sumXYZ);
        if (pc <= 0) {
            pocketDataPool.recycle(scratch);
            return 0;
        }

        outPocketData[0] = scratch;
        return pc;
    }

    void relinkKeys(long[] dirtyKeys, int hi) {
        if (hi <= 0) return;
        ensureLinkScratch(hi << 2);
        long[] keys = linkScratch;
        int n = 0;
        for (int i = 0; i < hi; i++) {
            long k = dirtyKeys[i];
            int absY = SectionPos.y(k);
            int slot = slotOf(absY);
            if (slot < 0) continue;
            int yzBase = absY << 4;
            keys[n++] = Library.setSectionY(k, yzBase | 7);
            keys[n++] = Library.setSectionY(Library.shiftSectionX(k, -1), yzBase | 1);
            keys[n++] = Library.setSectionY(Library.shiftSectionZ(k, -1), yzBase | 2);
            if (slot != 0) {
                keys[n++] = Library.setSectionY(k, ((absY - 1) << 4) | 4);
            }
        }
        if (n == 0) return;
        if (n < 4096) LongArrays.radixSort(keys, 0, n);
        else LongArrays.parallelRadixSort(keys, 0, n);

        int u = 0;
        for (int i = 0; i < n; i++) {
            long k = keys[i];
            long base = k & ~0xFL;
            int dm = (int) (k & 0xFL);
            if (u == 0) {
                keys[u++] = base | (long) dm;
                continue;
            }
            long prev = keys[u - 1];
            long prevBase = prev & ~0xFL;
            if (base != prevBase) {
                keys[u++] = base | (long) dm;
            } else {
                int prevDm = (int) (prev & 0xFL);
                keys[u - 1] = prevBase | (long) (prevDm | dm);
            }
        }
        int threshold = getTaskThreshold(u, 256);
        if (u > 0) new LinkCanonicalKeysTask(keys, 0, u, threshold).invoke();
    }

    void ensureLinkScratch(int need) {
        long[] a = linkScratch;
        if (a.length >= need) return;
        int n = a.length;
        while (n < need) n = n + (n >>> 1) + 16;
        linkScratch = Arrays.copyOf(a, n);
    }

    void clearAllChunkRefs() {
        coordToId.clear();
        nextId = 0;
        freeTop = 0;
        Arrays.fill(mcChunks, null);
        Arrays.fill(pending, null);
        Arrays.fill(chunkKinds, 0L);
        Arrays.fill(chunkActiveDirty, 0L);
        Arrays.fill(chunkSourceMask, 0L);
        Arrays.fill(sectionSources, null);
        if (touchedSectionWords != null) Arrays.fill(touchedSectionWords, 0);
        Arrays.fill(eastNeighborId, -1);
        Arrays.fill(southNeighborId, -1);
        radiationDirtyCount = 0;
        Arrays.fill(radiationDirtyIndex, -1);
        Arrays.fill(uniformRads, 0.0D);
        Arrays.fill(complexSecs, null);
        IntArrayList edited = editedChunkIds;
        int[] editedArr = edited.elements();
        for (int i = 0, n = edited.size(); i < n; i++) {
            int id = editedArr[i];
            EditTable t = editsById[id];
            if (t == null) continue;
            clearTouchedSections(id);
            editTablePool.recycle(t);
            editsById[id] = null;
        }
        edited.clear();
        Arrays.fill(editsById, null);
        Arrays.fill(myBucket, (byte) -1);
        Arrays.fill(myBucketIndex, -1);
        for (int b = 0; b < 4; b++) {
            Arrays.fill(parityBucketIds[b], -1);
            xPairCounts[b] = 0;
            zPairCounts[b] = 0;
        }
        pairListsDirty = true;
        clearBuckets();
    }

    void linkLoadedNeighbors(int id) {
        long ck = cks[id];
        int cx = (int) ck;
        int cz = (int) (ck >>> 32);
        int east = coordToId.get(ChunkPos.pack(cx + 1, cz));
        eastNeighborId[id] = (east >= 0 && mcChunks[east] != null) ? east : -1;
        int south = coordToId.get(ChunkPos.pack(cx, cz + 1));
        southNeighborId[id] = (south >= 0 && mcChunks[south] != null) ? south : -1;

        int west = coordToId.get(ChunkPos.pack(cx - 1, cz));
        if (west >= 0 && mcChunks[west] != null) eastNeighborId[west] = id;
        int north = coordToId.get(ChunkPos.pack(cx, cz - 1));
        if (north >= 0 && mcChunks[north] != null) southNeighborId[north] = id;
        pairListsDirty = true;
    }

    void unlinkLoadedNeighbors(int id) {
        long ck = cks[id];
        int cx = (int) ck;
        int cz = (int) (ck >>> 32);
        int west = coordToId.get(ChunkPos.pack(cx - 1, cz));
        if (west >= 0 && eastNeighborId[west] == id) eastNeighborId[west] = -1;
        int north = coordToId.get(ChunkPos.pack(cx, cz - 1));
        if (north >= 0 && southNeighborId[north] == id) southNeighborId[north] = -1;
        eastNeighborId[id] = -1;
        southNeighborId[id] = -1;
        pairListsDirty = true;
    }

    void rebuildPairListsIfNeeded() {
        if (!pairListsDirty) return;
        for (int b = 0; b < 4; b++) {
            int[] ids = parityBucketIds[b];
            int n = parityCounts[b];
            int xCount = 0;
            int zCount = 0;
            for (int i = 0; i < n; i++) {
                int aId = ids[i];
                if (aId < 0) continue;

                int bId = eastNeighborId[aId];
                if (bId >= 0 && mcChunks[bId] != null) {
                    int need = xCount + 1;
                    if (need > xPairAByBucket[b].length)
                        ensurePairBucketCapacity(xPairAByBucket, xPairBByBucket, b, need);
                    xPairAByBucket[b][xCount] = aId;
                    xPairBByBucket[b][xCount] = bId;
                    xCount++;
                }

                bId = southNeighborId[aId];
                if (bId >= 0 && mcChunks[bId] != null) {
                    int need = zCount + 1;
                    if (need > zPairAByBucket[b].length)
                        ensurePairBucketCapacity(zPairAByBucket, zPairBByBucket, b, need);
                    zPairAByBucket[b][zCount] = aId;
                    zPairBByBucket[b][zCount] = bId;
                    zCount++;
                }
            }
            xPairCounts[b] = xCount;
            zPairCounts[b] = zCount;
        }
        pairListsDirty = false;
    }

    boolean hasActivePair(int[] pairA, int[] pairB, int lo, int hi) {
        for (int i = lo; i < hi; i++) {
            if (((chunkActiveDirty[pairA[i]] | chunkActiveDirty[pairB[i]]) & activeMaskAll) != 0L)
                return true;
        }
        return false;
    }

    private record SidecarPayload(
            int id,
            long ck,
            ChunkPos pos,
            byte @Nullable [] payload,
            @Nullable RuntimeException failure) {}

    final class DiffuseXTask extends RecursiveAction {
        final int[] pairA;
        final int[] pairB;
        final int lo, hi, threshold;

        DiffuseXTask(int[] pairA, int[] pairB, int lo, int hi, int threshold) {
            this.pairA = pairA;
            this.pairB = pairB;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (wordsPerChunk > 1) {
                    for (int i = lo; i < hi; i++) diffuseXZWide(pairA[i], pairB[i], 5, 4);
                } else {
                    if (sectionsPerChunk >= 16 && !hasActivePair(pairA, pairB, lo, hi)) return;
                    for (int i = lo; i < hi; i++) diffuseXZ(pairA[i], pairB[i], 5, 4);
                }
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseXTask(pairA, pairB, lo, mid, threshold).fork();
            new DiffuseXTask(pairA, pairB, mid, hi, threshold).compute();
            left.join();
        }
    }

    final class DiffuseZTask extends RecursiveAction {
        final int[] pairA;
        final int[] pairB;
        final int lo, hi, threshold;

        DiffuseZTask(int[] pairA, int[] pairB, int lo, int hi, int threshold) {
            this.pairA = pairA;
            this.pairB = pairB;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (wordsPerChunk > 1) {
                    for (int i = lo; i < hi; i++) diffuseXZWide(pairA[i], pairB[i], 3, 2);
                } else {
                    if (sectionsPerChunk >= 16 && !hasActivePair(pairA, pairB, lo, hi)) return;
                    for (int i = lo; i < hi; i++) diffuseXZ(pairA[i], pairB[i], 3, 2);
                }
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseZTask(pairA, pairB, lo, mid, threshold).fork();
            new DiffuseZTask(pairA, pairB, mid, hi, threshold).compute();
            left.join();
        }
    }

    final class DiffuseYTask extends RecursiveAction {
        final int[] chunks;
        final int lo, hi, parity, threshold;

        DiffuseYTask(int[] chunks, int lo, int hi, int parity, int threshold) {
            this.chunks = chunks;
            this.lo = lo;
            this.hi = hi;
            this.parity = parity;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (wordsPerChunk == 1) work(lo, hi);
                else workWide(lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseYTask(chunks, lo, mid, parity, threshold).fork();
            new DiffuseYTask(chunks, mid, hi, parity, threshold).compute();
            left.join();
        }

        void work(int start, int end) {
            int N = sectionsPerChunk;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                int off = id * N;
                long kinds = chunkKinds[id];
                long active = chunkActiveDirty[id];
                double[] u = uniformRads;
                boolean d = false;

                if (kinds == allUniKinds) {
                    for (int sy = parity; sy < N - 1; sy += 2) {
                        long actMask = 3L << sy;
                        if ((active & actMask) == 0L) continue;
                        int idx = off + sy;
                        int idxN = idx + 1;
                        if (exchangeUni(u, idx, idxN)) {
                            chunkActiveDirty[id] |= actMask;
                            d = true;
                        }
                    }
                    if (d) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
                    continue;
                }

                for (int sy = parity; sy < N - 1; sy += 2) {
                    long actMask = 3L << sy;
                    if ((active & actMask) == 0L) continue;

                    int idx = off + sy;
                    int idxN = idx + 1;
                    int kk = (int) ((kinds >>> (sy << 1)) & 0xF);
                    int k = kk & 3;
                    int kN = (kk >>> 2) & 3;

                    if (k == KIND_UNI && kN == KIND_UNI) {
                        if (exchangeUni(u, idx, idxN)) {
                            chunkActiveDirty[id] |= actMask;
                            d = true;
                        }
                    } else {
                        d |= exchangeFaceExactY(idx, k, idxN, kN);
                    }
                }
                if (d) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
            }
        }

        void workWide(int start, int end) {
            int n = sectionsPerChunk;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                int base = id * n;
                int metadataBase = id * wordsPerChunk;
                boolean dirty = false;
                for (int word = 0; word < wordsPerChunk; word++) {
                    int at = metadataBase + word;
                    int first = word << 5;
                    long active = chunkActiveDirty[at] & 0xffffffffL;
                    long kinds = chunkKinds[at];
                    int count = Math.min(32, n - first);
                    int off = base + first;
                    double[] u = uniformRads;
                    if (kinds == uniformKinds(count)) {
                        for (int lane = parity; lane < count - 1; lane += 2) {
                            long pairMask = 3L << lane;
                            if ((active & pairMask) == 0L) continue;
                            if (exchangeUni(u, off + lane, off + lane + 1)) {
                                chunkActiveDirty[at] |= pairMask;
                                dirty = true;
                            }
                        }
                    } else {
                        for (int lane = parity; lane < count - 1; lane += 2) {
                            long pairMask = 3L << lane;
                            if ((active & pairMask) == 0L) continue;
                            int pairKinds = (int) ((kinds >>> (lane * 2)) & 15);
                            int kind = pairKinds & 3;
                            int upperKind = pairKinds >>> 2;
                            if (kind == KIND_UNI && upperKind == KIND_UNI) {
                                if (exchangeUni(u, off + lane, off + lane + 1)) {
                                    chunkActiveDirty[at] |= pairMask;
                                    dirty = true;
                                }
                            } else {
                                dirty |=
                                        exchangeFaceExactY(
                                                off + lane, kind, off + lane + 1, upperKind);
                            }
                        }
                    }
                    if (parity == 1
                            && word + 1 < wordsPerChunk
                            && ((active >>> 31) | (chunkActiveDirty[at + 1] & 1L)) != 0L) {
                        int kind = (int) (kinds >>> 62);
                        int upperKind = (int) (chunkKinds[at + 1] & 3);
                        if (kind == KIND_UNI && upperKind == KIND_UNI) {
                            if (exchangeUni(u, off + 31, off + 32)) {
                                chunkActiveDirty[at] |= 1L << 31;
                                chunkActiveDirty[at + 1] |= 1L;
                                dirty = true;
                            }
                        } else {
                            dirty |= exchangeFaceExactY(off + 31, kind, off + 32, upperKind);
                        }
                    }
                }
                if (dirty) chunkActiveDirty[metadataBase] |= CHUNK_DIRTY_MASK;
            }
        }
    }

    abstract sealed class SectionRef permits MultiSectionRef, SingleMaskedSectionRef {
        final int secIdx;
        final int ownerId;
        final int slot;
        final short pocketCount;
        final short @NonNull [] pocketData;

        SectionRef(int secIdx, short pocketCount, short[] pocketData) {
            this.secIdx = secIdx;
            this.ownerId = secIdx / sectionsPerChunk;
            this.slot = secIdx - this.ownerId * sectionsPerChunk;
            this.pocketCount = pocketCount;
            this.pocketData = pocketData;
        }

        abstract boolean exchangeWithMulti(MultiSectionRef other, int myFace, int otherFace);

        abstract boolean exchangeWithUniform(int otherIdx, int myFace, int otherFace);

        abstract boolean exchangeWithSingle(
                SingleMaskedSectionRef other, int myFace, int otherFace);

        abstract int getPocketIndex(long pos);

        abstract int paletteIndexOrNeg(int blockIndex);

        abstract void clearFaceAllPockets(int faceOrdinal);

        abstract void linkFaceToMulti(MultiSectionRef other, int myFace);

        abstract void linkFaceToSingle(SingleMaskedSectionRef single, int faceA);

        abstract void linkFaceToUniform(int faceA);
    }

    final class SingleMaskedSectionRef extends SectionRef {
        static final long CONN_OFF = fieldOffset(SingleMaskedSectionRef.class, "connections");

        final int volume;
        final double invVolume, cx, cy, cz;
        final long packedFaceCounts;
        long connections;
        float diffusivity = RadiationDiffusivity.NEUTRAL;

        SingleMaskedSectionRef(
                int secIdx,
                short[] pocketData,
                int volume,
                long packedFaceCounts,
                double cx,
                double cy,
                double cz) {
            super(secIdx, (short) 1, pocketData);
            this.volume = volume;
            invVolume = 1.0d / volume;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.packedFaceCounts = packedFaceCounts;
        }

        @Override
        boolean exchangeWithSingle(SingleMaskedSectionRef other, int myFace, int otherFace) {
            long conns = connections;
            int area = (int) ((conns >>> (myFace * 9)) & 0x1FFL);
            if (area == 0) return false;
            double ra = uniformRads[secIdx];
            double rb = uniformRads[other.secIdx];
            if (ra == rb) return false;

            double invVa = invVolume;
            double invVb = other.invVolume;
            double denomInv = invVa + invVb;
            double myDist = getFaceDist(myFace);
            double otherDist = other.getFaceDist(otherFace);
            double distSum = myDist + otherDist;
            if (distSum <= 0.0D) return false;

            double dt = diffusionDt;
            if (diffusivityTransport) {
                double dEff =
                        RadiationDiffusivity.edge(
                                myDist, diffusivity, otherDist, other.diffusivity);
                if (!(dEff > 0.0D)) return false;
                dt *= dEff;
            }

            double e = Math.exp(-((area / distSum) * denomInv * dt));
            double rStar = (ra * invVb + rb * invVa) / denomInv;
            uniformRads[secIdx] = rStar + (ra - rStar) * e;
            uniformRads[other.secIdx] = rStar + (rb - rStar) * e;
            markActive(ownerId, slot);
            markActive(other.ownerId, other.slot);
            return true;
        }

        @Override
        boolean exchangeWithUniform(int otherIdx, int myFace, int otherFace) {
            int area = getFaceCount(myFace);
            double ra = uniformRads[otherIdx];
            double rb = uniformRads[secIdx];
            if (area <= 0 || ra == rb) return false;

            double myDist = getFaceDist(myFace);
            double distSum = 8.0d + myDist;
            if (distSum <= 0.0D) return false;

            double invVb = invVolume;
            double denomInv = 2.44140625E-4 + invVb;
            double dt = diffusionDt;
            if (diffusivityTransport) {
                double dEff =
                        RadiationDiffusivity.edge(
                                8.0d, uniformDiffusivity[otherIdx], myDist, diffusivity);
                if (!(dEff > 0.0D)) return false;
                dt *= dEff;
            }
            double e = Math.exp(-((area / distSum) * denomInv * dt));
            double rStar = (ra * invVb + rb * 2.44140625E-4) / denomInv;

            uniformRads[otherIdx] = rStar + (ra - rStar) * e;
            uniformRads[secIdx] = rStar + (rb - rStar) * e;

            int _otherOwner = otherIdx / sectionsPerChunk;
            markActive(_otherOwner, otherIdx - _otherOwner * sectionsPerChunk);
            markActive(ownerId, slot);
            return true;
        }

        @Override
        boolean exchangeWithMulti(MultiSectionRef other, int myFace, int otherFace) {
            int[] edges = other.edgesByFace[otherFace];
            int edgeCount = other.getEdgeCount(otherFace);
            if (edges == null || edgeCount == 0) return false;

            boolean changed = false;
            double ra = uniformRads[secIdx];
            double invVa = invVolume;

            for (int i = 0; i < edgeCount; i++) {
                int edge = edges[i];
                int neiPi = (edge >>> 20) & 0x7FF;
                int area = edge & 0x1FF;

                int idxB = neiPi << 1;
                double rb = other.data[idxB];
                if (ra == rb) continue;

                double invVb = other.data[idxB + 1];
                double denomInv = invVa + invVb;
                double myDist = getFaceDist(myFace);
                double otherDist = other.faceDist[neiPi * 6 + otherFace];
                double distSum = myDist + otherDist;
                if (distSum <= 0.0D) continue;

                double dt = diffusionDt;
                if (diffusivityTransport) {
                    double dEff =
                            RadiationDiffusivity.edge(
                                    myDist, diffusivity, otherDist, other.pocketDiffusivity[neiPi]);
                    if (!(dEff > 0.0D)) continue;
                    dt *= dEff;
                }

                double e = Math.exp(-((area / distSum) * denomInv * dt));
                double rStar = (ra * invVb + rb * invVa) / denomInv;

                ra = rStar + (ra - rStar) * e;
                other.data[idxB] = rStar + (rb - rStar) * e;
                changed = true;
            }

            if (changed) {
                uniformRads[secIdx] = ra;
                markActive(ownerId, slot);
                markActive(other.ownerId, other.slot);
            }
            return changed;
        }

        @Override
        void clearFaceAllPockets(int faceOrdinal) {
            updateConnections(faceOrdinal, 0);
        }

        double getFaceDist(int face) {
            return switch (face) {
                case 0 -> cy + 0.5d;
                case 1 -> 15.5d - cy;
                case 2 -> cz + 0.5d;
                case 3 -> 15.5d - cz;
                case 4 -> cx + 0.5d;
                case 5 -> 15.5d - cx;
                default -> throw new IllegalArgumentException("Invalid face ordinal: " + face);
            };
        }

        void updateConnections(int face, int value) {
            int shift = face * 9;
            long maskBits = 0x1FFL << shift;
            long bits = ((long) value & 0x1FFL) << shift;
            while (true) {
                long cur = U.getLongVolatile(this, CONN_OFF);
                long next = (cur & ~maskBits) | bits;
                if (cur == next) return;
                if (U.compareAndSetLong(this, CONN_OFF, cur, next)) return;
            }
        }

        int getFaceCount(int face) {
            return (int) ((packedFaceCounts >>> (face * 9)) & 0x1FFL);
        }

        @Override
        int getPocketIndex(long pos) {
            int blockIndex = Library.blockPosToLocal(pos);
            return pocketData[blockIndex] < 0 ? -1 : 0;
        }

        @Override
        int paletteIndexOrNeg(int blockIndex) {
            int pi = pocketData[blockIndex];
            if (pi < 0) return -1;
            return pi;
        }

        @Override
        void linkFaceToMulti(MultiSectionRef other, int myFace) {
            other.linkFaceToSingle(this, myFace ^ 1);
        }

        @Override
        void linkFaceToSingle(SingleMaskedSectionRef other, int faceA) {
            int faceB = faceA ^ 1;
            int baseA = faceA << 8;
            int baseB = faceB << 8;
            int count = 0;
            short[] myData = pocketData;
            short[] otherData = other.pocketData;
            for (int t = 0; t < 256; t++) {
                int idxA = FACE_PLANE[baseA + t];
                if (myData[idxA] < 0) continue;
                int idxB = FACE_PLANE[baseB + t];
                if (otherData[idxB] < 0) continue;
                count++;
            }
            other.updateConnections(faceB, count);
            updateConnections(faceA, count);
        }

        @Override
        void linkFaceToUniform(int faceA) {
            int area = getFaceCount(faceA);
            updateConnections(faceA, area);
        }
    }

    final class MultiSectionRef extends SectionRef {
        final double[] data;
        final float[] faceDist;
        final int[] volume;
        final float @Nullable [] pocketDiffusivity;
        final int[][] edgesByFace = new int[6][];
        final short[] edgeCounts = new short[6];

        MultiSectionRef(int secIdx, short pocketCount, short[] pocketData, float[] faceDist) {
            super(secIdx, pocketCount, pocketData);
            this.faceDist = faceDist;
            int count = pocketCount & 0xFFFF;
            data = new double[count << 1];
            volume = new int[count];
            pocketDiffusivity = diffusivityTransport ? new float[count] : null;
        }

        int getEdgeCount(int face) {
            return edgeCounts[face] & 0xFFFF;
        }

        int[] ensureFaceEdgeCapacity(int face, int need) {
            int[] arr = edgesByFace[face];
            if (arr != null && arr.length >= need) return arr;
            int n = (arr == null) ? 16 : arr.length;
            while (n < need) n = n + (n >>> 1) + 16;
            arr = new int[n];
            edgesByFace[face] = arr;
            return arr;
        }

        @Override
        boolean exchangeWithMulti(MultiSectionRef other, int myFace, int otherFace) {
            int[] edges = edgesByFace[myFace];
            int edgeCount = getEdgeCount(myFace);
            if (edges == null || edgeCount == 0) return false;

            boolean changed = false;
            double[] myData = data;
            double[] otherData = other.data;

            for (int i = 0; i < edgeCount; i++) {
                int edge = edges[i];
                int myPi = (edge >>> 20) & 0x7FF;
                int neiPi = (edge >>> 9) & 0x7FF;
                int area = edge & 0x1FF;

                int idxA = myPi << 1;
                int idxB = neiPi << 1;

                double ra = myData[idxA];
                double rb = otherData[idxB];
                if (ra == rb) continue;

                double invVa = myData[idxA + 1];
                double invVb = otherData[idxB + 1];
                double denomInv = invVa + invVb;

                double myDist = faceDist[myPi * 6 + myFace];
                double otherDist = other.faceDist[neiPi * 6 + otherFace];
                double distSum = myDist + otherDist;
                if (distSum <= 0.0D) continue;

                double dt = diffusionDt;
                if (diffusivityTransport) {
                    double dEff =
                            RadiationDiffusivity.edge(
                                    myDist,
                                    pocketDiffusivity[myPi],
                                    otherDist,
                                    other.pocketDiffusivity[neiPi]);
                    if (!(dEff > 0.0D)) continue;
                    dt *= dEff;
                }

                double e = Math.exp(-((area / distSum) * denomInv * dt));
                double rStar = (ra * invVb + rb * invVa) / denomInv;

                myData[idxA] = rStar + (ra - rStar) * e;
                otherData[idxB] = rStar + (rb - rStar) * e;
                changed = true;
            }

            if (changed) {
                markActive(ownerId, slot);
                markActive(other.ownerId, other.slot);
            }
            return changed;
        }

        @Override
        boolean exchangeWithUniform(int otherIdx, int myFace, int otherFace) {
            int[] edges = edgesByFace[myFace];
            int edgeCount = getEdgeCount(myFace);
            if (edges == null || edgeCount == 0) return false;

            boolean changed = false;
            double[] myData = data;
            double ra = uniformRads[otherIdx];

            for (int i = 0; i < edgeCount; i++) {
                int edge = edges[i];
                int myPi = (edge >>> 20) & 0x7FF;
                int area = edge & 0x1FF;

                int idx = myPi << 1;
                double rb = myData[idx];
                if (ra == rb) continue;

                double myDist = faceDist[myPi * 6 + myFace];
                double distSum = 8.0d + myDist;
                if (distSum <= 0.0D) continue;

                double invVb = myData[idx + 1];
                double denomInv = 2.44140625E-4 + invVb;
                double dt = diffusionDt;
                if (diffusivityTransport) {
                    double dEff =
                            RadiationDiffusivity.edge(
                                    8.0d,
                                    uniformDiffusivity[otherIdx],
                                    myDist,
                                    pocketDiffusivity[myPi]);
                    if (!(dEff > 0.0D)) continue;
                    dt *= dEff;
                }
                double e = Math.exp(-((area / distSum) * denomInv * dt));
                double rStar = (ra * invVb + rb * 2.44140625E-4) / denomInv;

                ra = rStar + (ra - rStar) * e;
                myData[idx] = rStar + (rb - rStar) * e;
                changed = true;
            }

            if (changed) {
                uniformRads[otherIdx] = ra;
                int _otherOwner = otherIdx / sectionsPerChunk;
                markActive(_otherOwner, otherIdx - _otherOwner * sectionsPerChunk);
                markActive(ownerId, slot);
            }
            return changed;
        }

        @Override
        boolean exchangeWithSingle(SingleMaskedSectionRef other, int myFace, int otherFace) {
            int[] edges = edgesByFace[myFace];
            int edgeCount = getEdgeCount(myFace);
            if (edges == null || edgeCount == 0) return false;

            boolean changed = false;
            int oIdx = other.secIdx;
            double invVb = other.invVolume;
            double rb = uniformRads[oIdx];

            for (int i = 0; i < edgeCount; i++) {
                int edge = edges[i];
                int myPi = (edge >>> 20) & 0x7FF;
                int area = edge & 0x1FF;

                int idxA = myPi << 1;
                double ra = data[idxA];
                if (ra == rb) continue;

                double invVa = data[idxA + 1];
                double denomInv = invVa + invVb;
                double myDist = faceDist[myPi * 6 + myFace];
                double otherDist = other.getFaceDist(otherFace);
                double distSum = myDist + otherDist;
                if (distSum <= 0.0D) continue;

                double dt = diffusionDt;
                if (diffusivityTransport) {
                    double dEff =
                            RadiationDiffusivity.edge(
                                    myDist, pocketDiffusivity[myPi], otherDist, other.diffusivity);
                    if (!(dEff > 0.0D)) continue;
                    dt *= dEff;
                }

                double e = Math.exp(-((area / distSum) * denomInv * dt));
                double rStar = (ra * invVb + rb * invVa) / denomInv;

                data[idxA] = rStar + (ra - rStar) * e;
                rb = rStar + (rb - rStar) * e;
                changed = true;
            }

            if (changed) {
                uniformRads[oIdx] = rb;
                markActive(ownerId, slot);
                markActive(other.ownerId, other.slot);
            }
            return changed;
        }

        @Override
        int getPocketIndex(long pos) {
            int blockIndex = Library.blockPosToLocal(pos);
            int pi = pocketData[blockIndex];
            if (pi < 0) return -1;
            return pi;
        }

        @Override
        int paletteIndexOrNeg(int blockIndex) {
            int pi = pocketData[blockIndex];
            if (pi < 0) return -1;
            return pi;
        }

        @Override
        void clearFaceAllPockets(int faceOrdinal) {
            edgeCounts[faceOrdinal] = 0;
        }

        void markSentinelPlane16x16(int faceA) {
            int[] tempArr = TL_TEMP_ARRAY.get();
            int[] touched = TL_TOUCHED.get();
            int touchCount = 0;

            int planeA = faceA << 8;
            for (int t = 0; t < 256; t++) {
                int pa = paletteIndexOrNeg(FACE_PLANE[planeA + t]);
                if (pa < 0) continue;

                if (tempArr[pa] == 0) touched[touchCount++] = pa;
                tempArr[pa]++;
            }

            if (touchCount == 0) {
                edgeCounts[faceA] = 0;
                return;
            }

            int[] aEdges = ensureFaceEdgeCapacity(faceA, touchCount);
            for (int i = 0; i < touchCount; i++) {
                int pa = touched[i];
                int area = tempArr[pa];
                tempArr[pa] = 0;
                aEdges[i] = (pa << 20) | area;
            }
            edgeCounts[faceA] = (short) touchCount;
        }

        @Override
        void linkFaceToMulti(MultiSectionRef multiB, int faceA) {
            int faceB = faceA ^ 1;
            Long2IntOpenHashMap edgeCountsMap = TL_EDGE_COUNTS.get();
            edgeCountsMap.clear();

            int planeA = faceA << 8;
            int planeB = faceB << 8;

            for (int t = 0; t < 256; t++) {
                int pa = paletteIndexOrNeg(FACE_PLANE[planeA + t]);
                int pb = multiB.paletteIndexOrNeg(FACE_PLANE[planeB + t]);
                if (pa < 0 || pb < 0) continue;
                long key = ((long) pa << 32) | (pb & 0xFFFF_FFFFL);
                edgeCountsMap.addTo(key, 1);
            }

            int touchCount = edgeCountsMap.size();
            if (touchCount == 0) {
                edgeCounts[faceA] = 0;
                multiB.edgeCounts[faceB] = 0;
                return;
            }

            int[] aEdges = ensureFaceEdgeCapacity(faceA, touchCount);
            int[] bEdges = multiB.ensureFaceEdgeCapacity(faceB, touchCount);

            int e = 0;
            var iterator = edgeCountsMap.long2IntEntrySet().fastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                long key = entry.getLongKey();
                int pa = (int) (key >>> 32);
                int pb = (int) key;
                int area = entry.getIntValue();

                aEdges[e] = (pa << 20) | (pb << 9) | area;
                bEdges[e] = (pb << 20) | (pa << 9) | area;
                e++;
            }
            edgeCounts[faceA] = (short) e;
            multiB.edgeCounts[faceB] = (short) e;
        }

        @Override
        void linkFaceToSingle(SingleMaskedSectionRef single, int faceA) {
            int faceB = faceA ^ 1;

            int[] tempArr = TL_TEMP_ARRAY.get();
            int[] touched = TL_TOUCHED.get();
            int touchCount = 0;

            int planeA = faceA << 8;
            int planeB = faceB << 8;
            int singleCount = 0;

            for (int t = 0; t < 256; t++) {
                int pa = paletteIndexOrNeg(FACE_PLANE[planeA + t]);
                if (pa < 0) continue;

                if (single.paletteIndexOrNeg(FACE_PLANE[planeB + t]) < 0) continue;

                if (tempArr[pa] == 0) touched[touchCount++] = pa;
                tempArr[pa]++;
                singleCount++;
            }

            if (touchCount == 0) {
                edgeCounts[faceA] = 0;
                single.updateConnections(faceB, 0);
                return;
            }

            int[] aEdges = ensureFaceEdgeCapacity(faceA, touchCount);
            for (int i = 0; i < touchCount; i++) {
                int pa = touched[i];
                int area = tempArr[pa];
                tempArr[pa] = 0;
                aEdges[i] = (pa << 20) | area;
            }
            edgeCounts[faceA] = (short) touchCount;
            single.updateConnections(faceB, singleCount);
        }

        @Override
        void linkFaceToUniform(int faceA) {
            int[] tempArr = TL_TEMP_ARRAY.get();
            int[] touched = TL_TOUCHED.get();
            int touchCount = 0;

            int planeA = faceA << 8;
            for (int t = 0; t < 256; t++) {
                int pa = paletteIndexOrNeg(FACE_PLANE[planeA + t]);
                if (pa < 0) continue;

                if (tempArr[pa] == 0) touched[touchCount++] = pa;
                tempArr[pa]++;
            }

            if (touchCount == 0) {
                edgeCounts[faceA] = 0;
                return;
            }

            int[] aEdges = ensureFaceEdgeCapacity(faceA, touchCount);
            for (int i = 0; i < touchCount; i++) {
                int pa = touched[i];
                int area = tempArr[pa];
                tempArr[pa] = 0;
                aEdges[i] = (pa << 20) | area;
            }
            edgeCounts[faceA] = (short) touchCount;
        }
    }

    final class LinkCanonicalKeysTask extends RecursiveAction {
        final long[] keys;
        final int lo, hi, threshold;

        LinkCanonicalKeysTask(long[] keys, int lo, int hi, int threshold) {
            this.keys = keys;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                work(lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            invokeAll(
                    new LinkCanonicalKeysTask(keys, lo, mid, threshold),
                    new LinkCanonicalKeysTask(keys, mid, hi, threshold));
        }

        void work(int start, int end) {
            long curCk = Long.MIN_VALUE;
            int currentWord = -1;
            int idA = -1;
            long kindsA = 0;
            int idE = -1, idS = -1;
            long kindsE = 0, kindsS = 0;

            for (int i = start; i < end; i++) {
                long k = keys[i];

                long packedY = SectionPos.y(k);
                int dm = (int) (packedY & 0xFL);
                int absY = (int) (packedY >> 4);
                int slot = slotOf(absY);

                long ck = Library.sectionToChunkLong(k);
                if (ck != curCk) {
                    curCk = ck;

                    idA = coordToId.get(ck);
                    if (idA < 0 || mcChunks[idA] == null) {
                        idA = -1;
                        continue;
                    }
                    currentWord = -1;
                    idE = eastNeighborId[idA];
                    if (idE >= 0 && mcChunks[idE] == null) idE = -1;
                    idS = southNeighborId[idA];
                    if (idS >= 0 && mcChunks[idS] == null) idS = -1;
                }

                if (idA < 0 || slot < 0) continue;

                int word = slot >>> 5;
                if (word != currentWord) {
                    currentWord = word;
                    kindsA = chunkKinds[wordIndex(idA, slot)];
                    kindsE = idE >= 0 ? chunkKinds[wordIndex(idE, slot)] : 0;
                    kindsS = idS >= 0 ? chunkKinds[wordIndex(idS, slot)] : 0;
                }
                int shift = (slot & 31) << 1;
                int kA = (int) ((kindsA >>> shift) & 3);
                int idxA = (idA * sectionsPerChunk) + slot;
                int idxE = (idE >= 0) ? ((idE * sectionsPerChunk) + slot) : -1;
                int idxS = (idS >= 0) ? ((idS * sectionsPerChunk) + slot) : -1;

                if ((dm & 1) != 0) {
                    if (kA == KIND_NONE) {
                        if (idE >= 0) {
                            int kB = (int) ((kindsE >>> shift) & 3);
                            if (kB == KIND_SINGLE || kB == KIND_MULTI) {
                                SectionRef b = complexSecs[idxE];
                                if (b != null) {
                                    b.clearFaceAllPockets(4);
                                    if (kB == KIND_MULTI)
                                        ((MultiSectionRef) b).markSentinelPlane16x16(4);
                                }
                            }
                        }
                    } else if (kA == KIND_UNI) {
                        if (idE >= 0) {
                            int kB = (int) ((kindsE >>> shift) & 3);
                            if (kB != KIND_NONE && kB != KIND_UNI) {
                                SectionRef b = complexSecs[idxE];
                                if (b != null) b.linkFaceToUniform(4);
                            }
                        }
                    } else {
                        SectionRef a = complexSecs[idxA];
                        if (a != null) linkNonUniFace(a, kA, 5, idxE);
                    }
                }

                if ((dm & 2) != 0) {
                    if (kA == KIND_NONE) {
                        if (idS >= 0) {
                            int kB = (int) ((kindsS >>> shift) & 3);
                            if (kB == KIND_SINGLE || kB == KIND_MULTI) {
                                SectionRef b = complexSecs[idxS];
                                if (b != null) {
                                    b.clearFaceAllPockets(2);
                                    if (kB == KIND_MULTI)
                                        ((MultiSectionRef) b).markSentinelPlane16x16(2);
                                }
                            }
                        }
                    } else if (kA == KIND_UNI) {
                        if (idS >= 0) {
                            int kB = (int) ((kindsS >>> shift) & 3);
                            if (kB != KIND_NONE && kB != KIND_UNI) {
                                SectionRef b = complexSecs[idxS];
                                if (b != null) b.linkFaceToUniform(2);
                            }
                        }
                    } else {
                        SectionRef a = complexSecs[idxA];
                        if (a != null) linkNonUniFace(a, kA, 3, idxS);
                    }
                }

                if ((dm & 4) != 0 && slot < sectionsPerChunk - 1) {
                    int kB = getKind(idA, slot + 1);
                    int idxU = idxA + 1;
                    if (kA == KIND_NONE) {
                        if (kB == KIND_SINGLE || kB == KIND_MULTI) {
                            SectionRef b = complexSecs[idxU];
                            if (b != null) {
                                b.clearFaceAllPockets(0);
                                if (kB == KIND_MULTI)
                                    ((MultiSectionRef) b).markSentinelPlane16x16(0);
                            }
                        }
                    } else if (kA == KIND_UNI) {
                        if (kB != KIND_NONE && kB != KIND_UNI) {
                            SectionRef b = complexSecs[idxU];
                            if (b != null) b.linkFaceToUniform(0);
                        }
                    } else {
                        SectionRef a = complexSecs[idxA];
                        if (a != null) linkNonUniFace(a, kA, 1, idxU);
                    }
                }
            }
        }
    }

    final class DecayEmissionTask extends RecursiveAction {
        final int[] chunks;
        final int lo, hi, threshold;
        final long[] decayMasks = new long[4];

        DecayEmissionTask(int[] chunks, int lo, int hi, int threshold) {
            this.chunks = chunks;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (wordsPerChunk == 1) work(lo, hi);
                else workWide(lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DecayEmissionTask(chunks, lo, mid, threshold);
            var right = new DecayEmissionTask(chunks, mid, hi, threshold);
            left.fork();
            right.compute();
            left.join();
        }

        void work(int start, int end) {
            int N = sectionsPerChunk;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                LevelChunk chunk = mcChunks[id];
                if (chunk == null) continue;
                boolean dirty = isChunkDirty(id);
                long baseSck = Library.sectionToLong(cks[id], minSectionY);
                int secBase = id * N;
                long kinds = chunkKinds[id];
                long active = chunkActiveDirty[id];

                if (kinds == allUniKinds && chunkSourceMask[id] == 0L) {
                    long[] masks = decayMasks;
                    UniformDecay.INSTANCE.decay(
                            uniformRads, secBase, N, retentionDt, minBound, fogRad, 5.0D, masks);
                    if (masks[0] != 0L) dirty = true;
                    chunkActiveDirty[id] &= ~(masks[1] & active);
                    for (long m = masks[2] & active; m != 0L; m &= m - 1L) {
                        int sy = Long.numberOfTrailingZeros(m);
                        maybeQueueFog(Library.setSectionY(baseSck, minSectionY + sy), 0, sy);
                    }
                    for (long m = masks[3] & active; m != 0L; m &= m - 1L) {
                        int sy = Long.numberOfTrailingZeros(m);
                        maybeQueueDestroy(Library.setSectionY(baseSck, minSectionY + sy), 0, sy);
                    }
                    clearChunkDirty(id);
                    if (dirty) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
                    continue;
                }

                long visit = active | chunkSourceMask[id];
                for (int sy = 0; sy < N; sy++) {
                    if ((visit & (1L << sy)) == 0L) continue;

                    int secIdx = secBase + sy;
                    RadiationSystemNT.SectionSources src = sectionSources[secIdx];
                    int kind = (int) ((kinds >>> (sy << 1)) & 3);

                    long sck = Library.setSectionY(baseSck, minSectionY + sy);

                    if (kind < KIND_MULTI) {
                        double prev = uniformRads[secIdx];
                        double next =
                                sanitize((src == null ? prev : src.relax(0, prev)) * retentionDt);
                        if (next != prev) {
                            uniformRads[secIdx] = next;
                            dirty = true;
                        }
                        if (next == 0.0D && src == null) {
                            chunkActiveDirty[id] &= ~(1L << sy);
                        } else if (next != 0.0D) {
                            if (next > fogRad) maybeQueueFog(sck, 0, sy);
                            if (next >= 5.0D) maybeQueueDestroy(sck, 0, sy);
                        }
                    } else {
                        MultiSectionRef multi = (MultiSectionRef) complexSecs[secIdx];
                        int pCount = multi.pocketCount & 0xFFFF;
                        boolean anyAlive = false;

                        for (int p = 0; p < pCount; p++) {
                            int dataIdx = p << 1;
                            double prev = multi.data[dataIdx];
                            if (prev == 0.0D && src == null) continue;

                            double next =
                                    sanitize(
                                            (src == null ? prev : src.relax(p, prev))
                                                    * retentionDt);
                            if (next != prev) {
                                multi.data[dataIdx] = next;
                                dirty = true;
                            }

                            if (next != 0.0D) {
                                anyAlive = true;
                                if (next > fogRad) maybeQueueFog(sck, p, sy);
                                if (next >= 5.0D) maybeQueueDestroy(sck, p, sy);
                            }
                        }

                        if (!anyAlive && src == null) {
                            chunkActiveDirty[id] &= ~(1L << sy);
                        }
                    }
                }
                chunkActiveDirty[id] &= ~CHUNK_DIRTY_MASK;
                if (dirty) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
            }
        }

        void workWide(int start, int end) {
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                LevelChunk chunk = mcChunks[id];
                if (chunk == null) continue;
                boolean dirty = isChunkDirty(id);
                long baseSck = Library.sectionToLong(cks[id], minSectionY);
                int metadataBase = id * wordsPerChunk;
                for (int word = 0; word < wordsPerChunk; word++) {
                    int first = word << 5;
                    int N = Math.min(32, sectionsPerChunk - first);
                    int secBase = id * sectionsPerChunk + first;
                    int at = metadataBase + word;
                    long kinds = chunkKinds[at];
                    long active = chunkActiveDirty[at] & 0xffffffffL;

                    if (kinds == uniformKinds(N) && chunkSourceMask[at] == 0L) {
                        long[] masks = decayMasks;
                        UniformDecay.INSTANCE.decay(
                                uniformRads,
                                secBase,
                                N,
                                retentionDt,
                                minBound,
                                fogRad,
                                5.0D,
                                masks);
                        if (masks[0] != 0L) dirty = true;
                        chunkActiveDirty[at] &= ~(masks[1] & active);
                        for (long m = masks[2] & active; m != 0L; m &= m - 1L) {
                            int sy = Long.numberOfTrailingZeros(m);
                            maybeQueueFog(
                                    Library.setSectionY(baseSck, minSectionY + first + sy),
                                    0,
                                    first + sy);
                        }
                        for (long m = masks[3] & active; m != 0L; m &= m - 1L) {
                            int sy = Long.numberOfTrailingZeros(m);
                            maybeQueueDestroy(
                                    Library.setSectionY(baseSck, minSectionY + first + sy),
                                    0,
                                    first + sy);
                        }
                        continue;
                    }

                    long visit = active | chunkSourceMask[at];
                    for (int sy = 0; sy < N; sy++) {
                        if ((visit & (1L << sy)) == 0L) continue;

                        int secIdx = secBase + sy;
                        RadiationSystemNT.SectionSources src = sectionSources[secIdx];
                        int kind = (int) ((kinds >>> (sy << 1)) & 3);

                        long sck = Library.setSectionY(baseSck, minSectionY + first + sy);

                        if (kind < KIND_MULTI) {
                            double prev = uniformRads[secIdx];
                            double next =
                                    sanitize(
                                            (src == null ? prev : src.relax(0, prev))
                                                    * retentionDt);
                            if (next != prev) {
                                uniformRads[secIdx] = next;
                                dirty = true;
                            }
                            if (next == 0.0D && src == null) {
                                chunkActiveDirty[at] &= ~(1L << sy);
                            } else if (next != 0.0D) {
                                if (next > fogRad) maybeQueueFog(sck, 0, first + sy);
                                if (next >= 5.0D) maybeQueueDestroy(sck, 0, first + sy);
                            }
                        } else {
                            MultiSectionRef multi = (MultiSectionRef) complexSecs[secIdx];
                            int pCount = multi.pocketCount & 0xFFFF;
                            boolean anyAlive = false;

                            for (int p = 0; p < pCount; p++) {
                                int dataIdx = p << 1;
                                double prev = multi.data[dataIdx];
                                if (prev == 0.0D && src == null) continue;

                                double next =
                                        sanitize(
                                                (src == null ? prev : src.relax(p, prev))
                                                        * retentionDt);
                                if (next != prev) {
                                    multi.data[dataIdx] = next;
                                    dirty = true;
                                }

                                if (next != 0.0D) {
                                    anyAlive = true;
                                    if (next > fogRad) maybeQueueFog(sck, p, first + sy);
                                    if (next >= 5.0D) maybeQueueDestroy(sck, p, first + sy);
                                }
                            }

                            if (!anyAlive && src == null) {
                                chunkActiveDirty[at] &= ~(1L << sy);
                            }
                        }
                    }
                }
                if (dirty) chunkActiveDirty[metadataBase] |= CHUNK_DIRTY_MASK;
            }
        }
    }

    final class RebuildDirtyChunkBatchTask extends RecursiveAction {
        final int[] ids;
        final int[] masks;
        final int lo, hi, threshold;

        RebuildDirtyChunkBatchTask(int[] ids, int[] masks, int lo, int hi, int threshold) {
            this.ids = ids;
            this.masks = masks;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            if (hi - lo <= threshold) {
                for (int i = lo; i < hi; i++) {
                    int id = ids[i];
                    LevelChunk chunk = mcChunks[id];
                    if (chunk == null) continue;
                    EditTable edits = editsById[id];
                    LevelChunkSection[] stor = chunk.getSections();
                    for (int word = 0; word < wordsPerChunk; word++) {
                        int m = masks[i * wordsPerChunk + word];
                        while (m != 0) {
                            int slot = (word << 5) + Integer.numberOfTrailingZeros(m);
                            m &= (m - 1);
                            if (slot >= sectionsPerChunk) continue;
                            long sck =
                                    SectionPos.asLong(
                                            chunk.getPos().x(),
                                            minSectionY + slot,
                                            chunk.getPos().z());
                            LevelChunkSection section = (slot < stor.length) ? stor[slot] : null;
                            rebuildChunkPocketsLoaded(id, slot, sck, section, edits);
                        }
                    }
                }
                return;
            }

            int mid = (lo + hi) >>> 1;
            var left = new RebuildDirtyChunkBatchTask(ids, masks, lo, mid, threshold);
            var right = new RebuildDirtyChunkBatchTask(ids, masks, mid, hi, threshold);
            left.fork();
            right.compute();
            left.join();
        }
    }
}
