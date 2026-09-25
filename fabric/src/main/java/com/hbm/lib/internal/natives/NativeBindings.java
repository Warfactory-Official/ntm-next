// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import org.lwjgl.system.NativeType;

public final class NativeBindings {

    private static final MethodHandle MH_SIMPLEX_ACCUMULATE =
            NativeLibrary.bindCritical(
                    "ntm_natives_simplex_accumulate",
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_DOUBLE,
                            ValueLayout.JAVA_DOUBLE,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

    private static final MethodHandle MH_SIMPLEX_FRACTAL =
            NativeLibrary.bindCritical(
                    "ntm_natives_simplex_fractal",
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT));

    private static final MethodHandle MH_SIMPLEX_FRACTAL_GRID =
            NativeLibrary.bindCritical(
                    "ntm_natives_simplex_fractal_grid",
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_DOUBLE,
                            ValueLayout.JAVA_DOUBLE,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT));

    private NativeBindings() {}

    public static void ensureBound() {}

    public static void simplexAccumulate(
            @NativeType("const uint32_t *") MemorySegment perm,
            @NativeType("const double *") MemorySegment gradX,
            @NativeType("const double *") MemorySegment gradY,
            @NativeType("const double *") MemorySegment xs,
            @NativeType("const double *") MemorySegment ys,
            @NativeType("double") double scale,
            @NativeType("double") double weight,
            @NativeType("double *") MemorySegment acc,
            @NativeType("uint32_t") int len) {
        try {
            MH_SIMPLEX_ACCUMULATE.invokeExact(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void simplexFractal(
            MemorySegment perm,
            MemorySegment gradX,
            MemorySegment gradY,
            MemorySegment xs,
            MemorySegment ys,
            MemorySegment out,
            int octaves,
            int start,
            int len) {
        try {
            MH_SIMPLEX_FRACTAL.invokeExact(perm, gradX, gradY, xs, ys, out, octaves, start, len);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    public static void simplexFractalGrid(
            MemorySegment perm,
            MemorySegment gradX,
            MemorySegment gradY,
            MemorySegment out,
            int octaves,
            int firstX,
            int firstZ,
            int sizeZ,
            double scaleX,
            double scaleZ,
            int start,
            int len) {
        try {
            MH_SIMPLEX_FRACTAL_GRID.invokeExact(
                    perm, gradX, gradY, out, octaves, firstX, firstZ, sizeZ, scaleX, scaleZ, start,
                    len);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
