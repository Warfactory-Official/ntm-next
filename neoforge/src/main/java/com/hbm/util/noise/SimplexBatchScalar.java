// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

final class SimplexBatchScalar implements SimplexBatch {

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
        for (int i = 0; i < len; i++) {
            acc[i] =
                    Math.fma(
                            SimplexBatch.value(
                                    perm, gradX, gradY, tableOffset, xs[i] * scale, ys[i] * scale),
                            weight,
                            acc[i]);
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
        Math.multiplyExact(sizeX, sizeZ);
        int index = 0;
        for (int ox = 0; ox < sizeX; ox++) {
            double x = (double) (firstX + ox) * scaleX;
            for (int oz = 0; oz < sizeZ; oz++) {
                double z = (double) (firstZ + oz) * scaleZ;
                double sum = 0.0D;
                double scale = 1.0D;
                for (int octave = 0; octave < octaves; octave++) {
                    sum +=
                            SimplexBatch.value(
                                            perm,
                                            gradX,
                                            gradY,
                                            octave * TABLE_SIZE,
                                            x * scale,
                                            z * scale)
                                    / scale;
                    scale *= 0.5D;
                }
                out[index++] = sum;
            }
        }
    }
}
