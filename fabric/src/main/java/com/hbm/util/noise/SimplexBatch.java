// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

import com.hbm.lib.internal.FusedMath;
import com.hbm.lib.internal.VectorApi;
import com.hbm.lib.internal.VectorBackend;
import com.hbm.lib.internal.natives.NativeLibrary;
import java.util.Arrays;
import net.minecraft.util.Mth;
import org.slf4j.LoggerFactory;

public interface SimplexBatch {

    double SQRT_3 = Math.sqrt(3.0D);
    double F2 = 0.5D * (SQRT_3 - 1.0D);
    double G2 = (3.0D - SQRT_3) / 6.0D;
    int TABLE_SIZE = 256;

    SimplexBatch INSTANCE = choose();

    default void accumulate(
            int[] perm,
            double[] gradX,
            double[] gradY,
            double[] xs,
            double[] ys,
            double scale,
            double weight,
            double[] acc,
            int len) {
        accumulate(perm, gradX, gradY, 0, xs, ys, scale, weight, acc, len);
    }

    void accumulate(
            int[] perm,
            double[] gradX,
            double[] gradY,
            int tableOffset,
            double[] xs,
            double[] ys,
            double scale,
            double weight,
            double[] acc,
            int len);

    default void fractal(
            int[] perm,
            double[] gradX,
            double[] gradY,
            double[] xs,
            double[] ys,
            double[] out,
            int octaves,
            int len) {
        if (octaves < 0) throw new IllegalArgumentException();
        Arrays.fill(out, 0, len, 0.0D);
        double scale = 1.0D;
        for (int octave = 0; octave < octaves; octave++) {
            accumulate(
                    perm, gradX, gradY, octave * TABLE_SIZE, xs, ys, scale, 1.0D / scale, out, len);
            scale *= 0.5D;
        }
    }

    default void fractalGrid(
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
        int len = Math.multiplyExact(sizeX, sizeZ);
        SimplexGridBatch.Scratch scratch = SimplexGridBatch.scratch();
        for (int base = 0; base < len; base += SimplexGridBatch.TILE) {
            int count = Math.min(SimplexGridBatch.TILE, len - base);
            for (int i = 0; i < count; i++) {
                int index = base + i;
                scratch.xs[i] = (double) (firstX + index / sizeZ) * scaleX;
                scratch.zs[i] = (double) (firstZ + index % sizeZ) * scaleZ;
            }
            fractal(perm, gradX, gradY, scratch.xs, scratch.zs, scratch.out, octaves, count);
            System.arraycopy(scratch.out, 0, out, base, count);
        }
    }

    static double value(int[] perm, double[] gradX, double[] gradY, double xin, double yin) {
        return value(perm, gradX, gradY, 0, xin, yin);
    }

    static double value(
            int[] perm, double[] gradX, double[] gradY, int tableOffset, double xin, double yin) {
        double skew = xin + yin;
        int i = Mth.floor(Math.fma(skew, F2, xin));
        int j = Mth.floor(Math.fma(skew, F2, yin));
        double unskew = -(double) (i + j);
        double x0 = xin - Math.fma(unskew, G2, i);
        double y0 = yin - Math.fma(unskew, G2, j);

        int i1;
        int j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;

        int ii = i & 0xFF;
        int jj = j & 0xFF;
        int k0 = (ii + perm[tableOffset + jj]) & 0xFF;
        int k1 = (ii + i1 + perm[tableOffset + ((jj + j1) & 0xFF)]) & 0xFF;
        int k2 = (ii + 1 + perm[tableOffset + ((jj + 1) & 0xFF)]) & 0xFF;

        double c =
                cornerWeight(x0, y0)
                        * cornerDot(gradX[tableOffset + k0], gradY[tableOffset + k0], x0, y0);
        c =
                Math.fma(
                        cornerWeight(x1, y1),
                        cornerDot(gradX[tableOffset + k1], gradY[tableOffset + k1], x1, y1),
                        c);
        c =
                Math.fma(
                        cornerWeight(x2, y2),
                        cornerDot(gradX[tableOffset + k2], gradY[tableOffset + k2], x2, y2),
                        c);
        return 70.0D * c;
    }

    private static double cornerWeight(double x, double y) {
        double t0 = Math.max(Math.fma(-y, y, Math.fma(-x, x, 0.5D)), 0.0D);
        double squared = t0 * t0;
        return squared * squared;
    }

    private static double cornerDot(double gx, double gy, double x, double y) {
        return Math.fma(gy, y, gx * x);
    }

    static double[][] gradientTables(int[] perm) {
        double[] x = new double[256];
        double[] y = new double[256];
        gradientTables(perm, 0, x, y, 0);
        return new double[][] {x, y};
    }

    static void gradientTables(
            int[] perm, int permOffset, double[] x, double[] y, int tableOffset) {
        for (int k = 0; k < TABLE_SIZE; k++) {
            int g = perm[permOffset + k] % 12;
            x[tableOffset + k] = g < 8 ? ((g & 1) == 0 ? 1.0D : -1.0D) : 0.0D;
            y[tableOffset + k] =
                    g < 4
                            ? ((g & 2) == 0 ? 1.0D : -1.0D)
                            : g < 8 ? 0.0D : ((g & 1) == 0 ? 1.0D : -1.0D);
        }
    }

    private static SimplexBatch choose() {
        boolean enabled = Boolean.parseBoolean(System.getProperty("hbm.vector.simplex", "true"));
        if (!enabled) return new SimplexBatchScalar();

        try {

            if (NativeLibrary.AVAILABLE && nativeOptIn()) return new SimplexBatchNative();
        } catch (Throwable t) {
            LoggerFactory.getLogger("NTM")
                    .warn("Native simplex kernel unavailable; using a Java kernel", t);
        }

        if (!FusedMath.AVAILABLE) return new SimplexBatchScalar();

        VectorBackend backend = VectorApi.backend();
        try {
            if (gatherOptIn() && backend.fma() && backend.converts() && backend.gathers()) {
                return new SimplexBatchGather();
            }
            if (backend.arithmetic() && backend.fma()) return new SimplexBatchVector();
        } catch (Throwable t) {
            LoggerFactory.getLogger("NTM")
                    .warn("Vector simplex kernel unavailable; using scalar", t);
        }
        return new SimplexBatchScalar();
    }

    private static boolean nativeOptIn() {
        return Boolean.parseBoolean(System.getProperty("hbm.natives.simplex", "true"));
    }

    private static boolean gatherOptIn() {
        return Boolean.getBoolean("hbm.vector.simplex.gather");
    }
}

final class SimplexGridBatch {

    static final int TILE = 64;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private SimplexGridBatch() {}

    static Scratch scratch() {
        return SCRATCH.get();
    }

    static final class Scratch {
        final double[] xs = new double[TILE];
        final double[] zs = new double[TILE];
        final double[] out = new double[TILE];
    }
}
