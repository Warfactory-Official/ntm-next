// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

import com.hbm.lib.internal.natives.NativeBindings;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

final class SimplexBatchNative implements SimplexBatch {

    private static final int MAX_CRITICAL_WORK = 256;
    private static final int MAX_CRITICAL_SAMPLES = 64;
    private static final double MAX_GRID_COORDINATE = 1.0E9D;
    private static final SimplexBatchScalar FALLBACK = new SimplexBatchScalar();

    SimplexBatchNative() {
        NativeBindings.ensureBound();
    }

    @Override
    public void accumulate(
            int[] perm,
            double[] gradX,
            double[] gradY,
            int tableOffset,
            double[] xs,
            double[] ys,
            double scale,
            double weight,
            double[] acc,
            int len) {
        Objects.checkFromIndexSize(tableOffset, TABLE_SIZE, perm.length);
        Objects.checkFromIndexSize(tableOffset, TABLE_SIZE, gradX.length);
        Objects.checkFromIndexSize(tableOffset, TABLE_SIZE, gradY.length);
        Objects.checkFromIndexSize(0, len, xs.length);
        Objects.checkFromIndexSize(0, len, ys.length);
        Objects.checkFromIndexSize(0, len, acc.length);
        MemorySegment permSegment =
                MemorySegment.ofArray(perm).asSlice((long) tableOffset * Integer.BYTES);
        MemorySegment gradXSegment =
                MemorySegment.ofArray(gradX).asSlice((long) tableOffset * Double.BYTES);
        MemorySegment gradYSegment =
                MemorySegment.ofArray(gradY).asSlice((long) tableOffset * Double.BYTES);
        MemorySegment xsSegment = MemorySegment.ofArray(xs);
        MemorySegment ysSegment = MemorySegment.ofArray(ys);
        MemorySegment accSegment = MemorySegment.ofArray(acc);
        for (int start = 0; start < len; start += MAX_CRITICAL_SAMPLES) {
            int count = Math.min(MAX_CRITICAL_SAMPLES, len - start);
            long byteOffset = (long) start * Double.BYTES;
            NativeBindings.simplexAccumulate(
                    permSegment,
                    gradXSegment,
                    gradYSegment,
                    xsSegment.asSlice(byteOffset),
                    ysSegment.asSlice(byteOffset),
                    scale,
                    weight,
                    accSegment.asSlice(byteOffset),
                    count);
        }
    }

    @Override
    public void fractal(
            int[] perm,
            double[] gradX,
            double[] gradY,
            double[] xs,
            double[] ys,
            double[] out,
            int octaves,
            int len) {
        if (octaves < 0) throw new IllegalArgumentException();
        checkFractalTables(perm, gradX, gradY, octaves);
        Objects.checkFromIndexSize(0, len, xs.length);
        Objects.checkFromIndexSize(0, len, ys.length);
        Objects.checkFromIndexSize(0, len, out.length);
        if (octaves > MAX_CRITICAL_WORK) {
            FALLBACK.fractal(perm, gradX, gradY, xs, ys, out, octaves, len);
            return;
        }

        MemorySegment permSegment = MemorySegment.ofArray(perm);
        MemorySegment gradXSegment = MemorySegment.ofArray(gradX);
        MemorySegment gradYSegment = MemorySegment.ofArray(gradY);
        MemorySegment xsSegment = MemorySegment.ofArray(xs);
        MemorySegment ysSegment = MemorySegment.ofArray(ys);
        MemorySegment outSegment = MemorySegment.ofArray(out);
        int batchSize =
                octaves == 0
                        ? MAX_CRITICAL_SAMPLES
                        : Math.min(MAX_CRITICAL_SAMPLES, MAX_CRITICAL_WORK / octaves);
        for (int start = 0; start < len; start += batchSize) {
            NativeBindings.simplexFractal(
                    permSegment,
                    gradXSegment,
                    gradYSegment,
                    xsSegment,
                    ysSegment,
                    outSegment,
                    octaves,
                    start,
                    Math.min(batchSize, len - start));
        }
    }

    @Override
    public void fractalGrid(
            int[] perm,
            double[] gradX,
            double[] gradY,
            int firstX,
            int firstZ,
            int sizeX,
            int sizeZ,
            double scaleX,
            double scaleZ,
            double[] out,
            int octaves) {
        if (octaves < 0 || sizeX < 0 || sizeZ < 0) throw new IllegalArgumentException();
        checkFractalTables(perm, gradX, gradY, octaves);
        int len = Math.multiplyExact(sizeX, sizeZ);
        Objects.checkFromIndexSize(0, len, out.length);
        if (octaves > MAX_CRITICAL_WORK
                || !gridCoordinatesSafe(firstX, firstZ, sizeX, sizeZ, scaleX, scaleZ)) {
            FALLBACK.fractalGrid(
                    perm, gradX, gradY, firstX, firstZ, sizeX, sizeZ, scaleX, scaleZ, out, octaves);
            return;
        }

        MemorySegment permSegment = MemorySegment.ofArray(perm);
        MemorySegment gradXSegment = MemorySegment.ofArray(gradX);
        MemorySegment gradYSegment = MemorySegment.ofArray(gradY);
        MemorySegment outSegment = MemorySegment.ofArray(out);
        int batchSize =
                octaves == 0
                        ? MAX_CRITICAL_SAMPLES
                        : Math.min(MAX_CRITICAL_SAMPLES, MAX_CRITICAL_WORK / octaves);
        for (int start = 0; start < len; start += batchSize) {
            NativeBindings.simplexFractalGrid(
                    permSegment,
                    gradXSegment,
                    gradYSegment,
                    outSegment,
                    octaves,
                    firstX,
                    firstZ,
                    sizeZ,
                    scaleX,
                    scaleZ,
                    start,
                    Math.min(batchSize, len - start));
        }
    }

    private static boolean gridCoordinatesSafe(
            int firstX, int firstZ, int sizeX, int sizeZ, double scaleX, double scaleZ) {
        if (sizeX == 0 || sizeZ == 0) return true;
        long lastX = (long) firstX + sizeX - 1L;
        long lastZ = (long) firstZ + sizeZ - 1L;
        if (lastX < Integer.MIN_VALUE
                || lastX > Integer.MAX_VALUE
                || lastZ < Integer.MIN_VALUE
                || lastZ > Integer.MAX_VALUE) return false;
        return coordinateSafe(firstX, scaleX)
                && coordinateSafe(lastX, scaleX)
                && coordinateSafe(firstZ, scaleZ)
                && coordinateSafe(lastZ, scaleZ);
    }

    private static boolean coordinateSafe(long coordinate, double scale) {
        return Math.abs((double) coordinate * scale) <= MAX_GRID_COORDINATE;
    }

    private static void checkFractalTables(
            int[] perm, double[] gradX, double[] gradY, int octaves) {
        if (octaves == 0) return;
        int tableLength = Math.multiplyExact(octaves, TABLE_SIZE);
        Objects.checkFromIndexSize(0, tableLength, perm.length);
        Objects.checkFromIndexSize(0, tableLength, gradX.length);
        Objects.checkFromIndexSize(0, tableLength, gradY.length);
    }
}
