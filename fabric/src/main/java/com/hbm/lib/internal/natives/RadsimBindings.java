// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import org.lwjgl.system.NativeType;

import static com.hbm.lib.internal.natives.NativeLibrary.bind;
import static com.hbm.lib.internal.natives.NativeLibrary.bindLeaf;

public final class RadsimBindings {

    private static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG;
    private static final ValueLayout.OfInt INT = ValueLayout.JAVA_INT;
    private static final ValueLayout.OfDouble DOUBLE = ValueLayout.JAVA_DOUBLE;

    private static final MethodHandle BUILD_FLAGS =
            bindLeaf("rad_build_flags", FunctionDescriptor.of(INT));
    private static final MethodHandle CONFIGURE_RUNTIME =
            bind("rad_configure_runtime", FunctionDescriptor.ofVoid(INT, INT));
    private static final MethodHandle WORLD_CREATE =
            bind("rad_world_create", FunctionDescriptor.of(LONG, INT, LONG, DOUBLE, INT));
    private static final MethodHandle WORLD_DESTROY =
            bind("rad_world_destroy", FunctionDescriptor.ofVoid(LONG));
    private static final MethodHandle WORLD_SET_PARAMS =
            bindLeaf(
                    "rad_world_set_params",
                    FunctionDescriptor.ofVoid(
                            LONG, DOUBLE, DOUBLE, DOUBLE, LONG, LONG, DOUBLE, DOUBLE, DOUBLE));
    private static final MethodHandle CHUNK_LOADED =
            bind("rad_chunk_loaded", FunctionDescriptor.ofVoid(LONG, LONG));
    private static final MethodHandle CHUNK_UNLOADED =
            bind("rad_chunk_unloaded", FunctionDescriptor.ofVoid(LONG, LONG));
    private static final MethodHandle CHUNK_REMOVED =
            bind("rad_chunk_removed", FunctionDescriptor.ofVoid(LONG, LONG));
    private static final MethodHandle STEP =
            bind(
                    "rad_step",
                    FunctionDescriptor.of(INT, LONG, LONG, INT, INT, ValueLayout.ADDRESS, LONG));
    private static final MethodHandle SUBMIT_EDITS =
            bind(
                    "rad_submit_edits",
                    FunctionDescriptor.ofVoid(LONG, INT, ValueLayout.ADDRESS, LONG));
    private static final MethodHandle SUBMIT_DIRTY_SECTIONS =
            bind(
                    "rad_submit_dirty_sections",
                    FunctionDescriptor.ofVoid(LONG, INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle SUBMIT_SECTION_SOURCES =
            bind(
                    "rad_submit_section_sources",
                    FunctionDescriptor.ofVoid(
                            LONG,
                            INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS));
    private static final MethodHandle SUBMIT_SECTION_DIFFUSIVITY =
            bind(
                    "rad_submit_section_diffusivity",
                    FunctionDescriptor.ofVoid(
                            LONG,
                            INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS));
    private static final MethodHandle WORLD_SET_FEATURE_FLAGS =
            bindLeaf("rad_world_set_feature_flags", FunctionDescriptor.ofVoid(LONG, INT));
    private static final MethodHandle QUERY_LOCAL_DENSITY =
            bindLeaf("rad_query_local_density", FunctionDescriptor.of(DOUBLE, LONG, LONG, INT));
    private static final MethodHandle DUMP_CHUNK_ENTRIES =
            bind(
                    "rad_dump_chunk_entries",
                    FunctionDescriptor.of(
                            INT, LONG, LONG, ValueLayout.ADDRESS, LONG, ValueLayout.ADDRESS, LONG));
    private static final MethodHandle LOAD_PENDING_ENTRIES =
            bind(
                    "rad_load_pending_entries",
                    FunctionDescriptor.ofVoid(
                            LONG, LONG, INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
    private static final MethodHandle CHUNK_ID =
            bindLeaf("rad_chunk_id", FunctionDescriptor.of(INT, LONG, LONG));
    private static final MethodHandle BUFFER_PTR =
            bindLeaf("rad_buffer_ptr", FunctionDescriptor.of(LONG, LONG, INT));
    private static final MethodHandle BUFFER_COUNT =
            bindLeaf("rad_buffer_count", FunctionDescriptor.of(LONG, LONG, INT));
    private static final MethodHandle BUFFER_STRIDE =
            bindLeaf("rad_buffer_stride", FunctionDescriptor.of(INT, LONG, INT));
    private static final MethodHandle BUFFER_GENERATION =
            bindLeaf("rad_buffer_generation", FunctionDescriptor.of(INT, LONG));
    private static final MethodHandle SECTIONS_PER_CHUNK =
            bindLeaf("rad_sections_per_chunk", FunctionDescriptor.of(INT));
    private static final MethodHandle WORLD_SECTIONS_PER_CHUNK =
            bindLeaf("rad_world_sections_per_chunk", FunctionDescriptor.of(INT, LONG));

    private static final class Debug {
        private static final MethodHandle SECTION_SOURCE_COUNT =
                bindLeaf("rad_debug_section_source_count", FunctionDescriptor.of(INT, LONG, LONG));
        private static final MethodHandle CPU_PROFILE =
                bindLeaf("rad_cpu_profile", FunctionDescriptor.of(INT));
        private static final MethodHandle FORCE_SCALAR_COLUMNS =
                bindLeaf("rad_debug_force_scalar_columns", FunctionDescriptor.ofVoid(LONG, INT));
        private static final MethodHandle BYPASS_COEFF_CACHE =
                bindLeaf(
                        "rad_debug_bypass_coefficient_cache", FunctionDescriptor.ofVoid(LONG, INT));
        private static final MethodHandle LAST_STEP_PROFILE =
                bindLeaf(
                        "rad_debug_get_last_step_profile",
                        FunctionDescriptor.ofVoid(LONG, ValueLayout.ADDRESS, LONG));
        private static final MethodHandle DIFFUSIVITY_FAILURES =
                bindLeaf("rad_debug_diffusivity_failures", FunctionDescriptor.of(LONG, LONG));
        private static final MethodHandle VALIDATION_FAILURES =
                bindLeaf(
                        "rad_debug_get_validation_failures",
                        FunctionDescriptor.ofVoid(LONG, ValueLayout.ADDRESS, LONG));
    }

    private static void debug() {
        if (!hasDebugDump())
            throw new IllegalStateException(
                    "the loaded ntm_next_native has no debug surface (rad_build_flags lacks RAD_BUILD_DEBUG_DUMP);"
                            + " the dump and debug bindings exist only on overlay-patched builds");
        if (Debug.CPU_PROFILE == null) throw new AssertionError("unreachable");
    }

    private RadsimBindings() {}

    @NativeType("int32_t")
    public static int buildFlags() {
        try {
            return (int) BUILD_FLAGS.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static boolean reproducesJavaMath() {
        return (buildFlags() & 1) != 0;
    }

    public static boolean validatesEveryStep() {
        return (buildFlags() & 2) != 0;
    }

    public static boolean hasDebugDump() {
        return (buildFlags() & 4) != 0;
    }

    public static void validationFailures(
            @NativeType("uint64_t") long handle,
            @NativeType("uint64_t *") MemorySegment out,
            @NativeType("size_t") long count) {
        debug();
        try {
            Debug.VALIDATION_FAILURES.invokeExact(handle, out, count);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int sectionSourceCount(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long sectionKey) {
        debug();
        try {
            return (int) Debug.SECTION_SOURCE_COUNT.invokeExact(handle, sectionKey);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int64_t")
    public static long diffusivityFailures(@NativeType("uint64_t") long handle) {
        debug();
        try {
            return (long) Debug.DIFFUSIVITY_FAILURES.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int cpuProfile() {
        debug();
        try {
            return (int) Debug.CPU_PROFILE.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void ensureBound() {}

    public static void configureRuntime(
            @NativeType("int32_t") boolean parallel, @NativeType("int32_t") int threads) {
        invokeVoid(CONFIGURE_RUNTIME, parallel ? 1 : 0, threads);
    }

    @NativeType("uint64_t")
    public static long worldCreate(
            @NativeType("int32_t") int dim,
            @NativeType("int64_t") long seed,
            @NativeType("double") double minBound,
            @NativeType("int32_t") int sectionsPerChunk) {
        try {
            return (long) WORLD_CREATE.invokeExact(dim, seed, minBound, sectionsPerChunk);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void worldSetParams(
            @NativeType("uint64_t") long handle,
            @NativeType("double") double diffusionDt,
            @NativeType("double") double uniformExchange,
            @NativeType("double") double retentionDt,
            @NativeType("uint64_t") long fogProbU64,
            @NativeType("uint64_t") long destroyProbU64,
            @NativeType("double") double fogThreshold,
            @NativeType("double") double eps,
            @NativeType("double") double maxValue) {
        try {
            WORLD_SET_PARAMS.invokeExact(
                    handle,
                    diffusionDt,
                    uniformExchange,
                    retentionDt,
                    fogProbU64,
                    destroyProbU64,
                    fogThreshold,
                    eps,
                    maxValue);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void worldDestroy(@NativeType("uint64_t") long handle) {
        try {
            WORLD_DESTROY.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void chunkLoaded(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        invokeVoidLL(CHUNK_LOADED, handle, ck);
    }

    public static void chunkUnloaded(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        invokeVoidLL(CHUNK_UNLOADED, handle, ck);
    }

    public static void chunkRemoved(
            @NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        invokeVoidLL(CHUNK_REMOVED, handle, ck);
    }

    @NativeType("int32_t")
    public static int step(
            @NativeType("uint64_t") long handle,
            @NativeType("uint64_t") long epochSalt,
            @NativeType("int32_t") int epoch,
            @NativeType("int32_t") int permBits,
            @NativeType("void *") MemorySegment events,
            @NativeType("size_t") long eventsCapacityBytes) {
        try {
            return (int)
                    STEP.invokeExact(
                            handle, epochSalt, epoch, permBits, events, eventsCapacityBytes);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void submitEdits(
            @NativeType("uint64_t") long handle,
            @NativeType("int32_t") int count,
            @NativeType("const void *") MemorySegment edits,
            @NativeType("size_t") long editsBytes) {
        try {
            SUBMIT_EDITS.invokeExact(handle, count, edits, editsBytes);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void submitSectionSources(
            @NativeType("uint64_t") long handle,
            @NativeType("int32_t") int count,
            @NativeType("const int64_t *") MemorySegment keys,
            @NativeType("const int32_t *") MemorySegment entryCounts,
            @NativeType("const uint16_t *") MemorySegment pockets,
            @NativeType("const int32_t *") MemorySegment multiplicities,
            @NativeType("const double *") MemorySegment emissions,
            @NativeType("const double *") MemorySegment saturations) {
        try {
            SUBMIT_SECTION_SOURCES.invokeExact(
                    handle,
                    count,
                    keys,
                    entryCounts,
                    pockets,
                    multiplicities,
                    emissions,
                    saturations);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void submitSectionDiffusivity(
            @NativeType("uint64_t") long handle,
            @NativeType("int32_t") int count,
            @NativeType("const int64_t *") MemorySegment keys,
            @NativeType("const int32_t *") MemorySegment pocketCounts,
            @NativeType("const float *") MemorySegment values) {
        try {
            SUBMIT_SECTION_DIFFUSIVITY.invokeExact(handle, count, keys, pocketCounts, values);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void worldSetFeatureFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        try {
            WORLD_SET_FEATURE_FLAGS.invokeExact(handle, flags);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void submitDirtySections(
            @NativeType("uint64_t") long handle,
            @NativeType("int32_t") int count,
            @NativeType("const int64_t *") MemorySegment packedKeys,
            @NativeType("const uint64_t *") MemorySegment masks) {
        try {
            SUBMIT_DIRTY_SECTIONS.invokeExact(handle, count, packedKeys, masks);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int dumpChunkEntries(
            @NativeType("uint64_t") long handle,
            @NativeType("int64_t") long ck,
            @NativeType("void *") MemorySegment sypi,
            @NativeType("size_t") long sypiBytes,
            @NativeType("void *") MemorySegment density,
            @NativeType("size_t") long densityBytes) {
        try {
            return (int)
                    DUMP_CHUNK_ENTRIES.invokeExact(
                            handle, ck, sypi, sypiBytes, density, densityBytes);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void loadPendingEntries(
            @NativeType("uint64_t") long handle,
            @NativeType("int64_t") long ck,
            @NativeType("int32_t") int count,
            @NativeType("const uint32_t *") MemorySegment sypi,
            @NativeType("const double *") MemorySegment density) {
        try {
            LOAD_PENDING_ENTRIES.invokeExact(handle, ck, count, sypi, density);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("double")
    public static double queryLocalDensity(
            @NativeType("uint64_t") long handle,
            @NativeType("int64_t") long sectionKey,
            @NativeType("int32_t") int local) {
        try {
            return (double) QUERY_LOCAL_DENSITY.invokeExact(handle, sectionKey, local);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int chunkId(@NativeType("uint64_t") long handle, @NativeType("int64_t") long ck) {
        try {
            return (int) CHUNK_ID.invokeExact(handle, ck);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("uint64_t")
    public static long bufferPtr(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        try {
            return (long) BUFFER_PTR.invokeExact(handle, which);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int64_t")
    public static long bufferCount(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        try {
            return (long) BUFFER_COUNT.invokeExact(handle, which);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int bufferStride(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int which) {
        try {
            return (int) BUFFER_STRIDE.invokeExact(handle, which);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int bufferGeneration(@NativeType("uint64_t") long handle) {
        try {
            return (int) BUFFER_GENERATION.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int sectionsPerChunk() {
        try {
            return (int) SECTIONS_PER_CHUNK.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @NativeType("int32_t")
    public static int worldSectionsPerChunk(@NativeType("uint64_t") long handle) {
        try {
            return (int) WORLD_SECTIONS_PER_CHUNK.invokeExact(handle);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void lastStepProfileNanos(
            @NativeType("uint64_t") long handle,
            @NativeType("uint64_t *") MemorySegment out,
            @NativeType("size_t") long count) {
        debug();
        try {
            Debug.LAST_STEP_PROFILE.invokeExact(handle, out, count);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void debugKernelFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        debug();
        try {
            Debug.BYPASS_COEFF_CACHE.invokeExact(handle, flags);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void columnPathFlags(
            @NativeType("uint64_t") long handle, @NativeType("int32_t") int flags) {
        debug();
        try {
            Debug.FORCE_SCALAR_COLUMNS.invokeExact(handle, flags);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static final String[] PHASE_NAMES = {
        "rebuild", "edits", "relink", "sweeps", "decay", "validate", "serialize", "total"
    };

    private static void invokeVoid(MethodHandle handle, int a, int b) {
        try {
            handle.invokeExact(a, b);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private static void invokeVoidLL(MethodHandle handle, long a, long b) {
        try {
            handle.invokeExact(a, b);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
