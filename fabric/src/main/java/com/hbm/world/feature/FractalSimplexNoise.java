// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.util.noise.SimplexBatch;
import java.util.Objects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class FractalSimplexNoise {

    private final int[] perms;
    private final double[] gradsX;
    private final double[] gradsY;
    private final int octaveCount;

    public FractalSimplexNoise(long seed, int octaveCount) {
        if (octaveCount < 0) throw new IllegalArgumentException();
        RandomSource rand = new WorldgenRandom(new LegacyRandomSource(seed));
        int tableLength = Math.multiplyExact(octaveCount, SimplexBatch.TABLE_SIZE);
        this.perms = new int[tableLength];
        this.gradsX = new double[tableLength];
        this.gradsY = new double[tableLength];
        this.octaveCount = octaveCount;
        for (int i = 0; i < octaveCount; i++) {
            SimplexNoise octave = new SimplexNoise(rand);
            int tableOffset = i * SimplexBatch.TABLE_SIZE;
            System.arraycopy(octave.p, 0, perms, tableOffset, SimplexBatch.TABLE_SIZE);
            SimplexBatch.gradientTables(perms, tableOffset, gradsX, gradsY, tableOffset);
        }
    }

    public double get(double x, double z) {
        double sum = 0.0D;
        double d = 1.0D;
        for (int i = 0; i < octaveCount; i++) {
            sum +=
                    SimplexBatch.value(
                                    perms,
                                    gradsX,
                                    gradsY,
                                    i * SimplexBatch.TABLE_SIZE,
                                    x * d,
                                    z * d)
                            / d;
            d /= 2.0D;
        }
        return sum;
    }

    public void get(double[] xs, double[] zs, double[] out, int len) {
        Objects.checkFromIndexSize(0, len, xs.length);
        Objects.checkFromIndexSize(0, len, zs.length);
        Objects.checkFromIndexSize(0, len, out.length);
        SimplexBatch.INSTANCE.fractal(perms, gradsX, gradsY, xs, zs, out, octaveCount, len);
    }

    public void getGrid(
            int firstX,
            int firstZ,
            int sizeX,
            int sizeZ,
            double scaleX,
            double scaleZ,
            double[] out) {
        if (sizeX < 0 || sizeZ < 0) throw new IllegalArgumentException();
        int len = Math.multiplyExact(sizeX, sizeZ);
        Objects.checkFromIndexSize(0, len, out.length);
        SimplexBatch.INSTANCE.fractalGrid(
                perms,
                gradsX,
                gradsY,
                firstX,
                firstZ,
                sizeX,
                sizeZ,
                scaleX,
                scaleZ,
                out,
                octaveCount);
    }
}
