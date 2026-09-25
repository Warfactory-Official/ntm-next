// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import net.minecraft.util.Mth;

final class SimplexBatchVector implements SimplexBatch {

    private static final VectorSpecies<Double> D = DoubleVector.SPECIES_PREFERRED;
    private static final int LANES = D.length();

    private static final ThreadLocal<Lattice> TL_LATTICE = ThreadLocal.withInitial(Lattice::new);

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
        int bound = D.loopBound(len);
        int i = 0;

        if (bound > 0) {
            Lattice l = TL_LATTICE.get();
            for (; i < bound; i += LANES) {
                l.resolve(perm, gradX, gradY, tableOffset, xs, ys, scale, i);

                DoubleVector n = cornerWeight(l.x0, l.y0).mul(cornerDot(l.gx0, l.gy0, l.x0, l.y0));
                n =
                        cornerWeight(l.x1, l.y1)
                                .lanewise(
                                        VectorOperators.FMA,
                                        cornerDot(l.gx1, l.gy1, l.x1, l.y1),
                                        n);
                n =
                        cornerWeight(l.x2, l.y2)
                                .lanewise(
                                        VectorOperators.FMA,
                                        cornerDot(l.gx2, l.gy2, l.x2, l.y2),
                                        n);
                n.mul(70.0D)
                        .lanewise(VectorOperators.FMA, weight, DoubleVector.fromArray(D, acc, i))
                        .intoArray(acc, i);
            }
        }

        for (; i < len; i++) {
            acc[i] =
                    Math.fma(
                            SimplexBatch.value(
                                    perm, gradX, gradY, tableOffset, xs[i] * scale, ys[i] * scale),
                            weight,
                            acc[i]);
        }
    }

    private static DoubleVector cornerWeight(double[] xs, double[] ys) {
        DoubleVector x = DoubleVector.fromArray(D, xs, 0);
        DoubleVector y = DoubleVector.fromArray(D, ys, 0);
        DoubleVector t0 =
                y.neg()
                        .lanewise(
                                VectorOperators.FMA,
                                y,
                                x.neg().lanewise(VectorOperators.FMA, x, 0.5D))
                        .lanewise(VectorOperators.MAX, 0.0D);
        DoubleVector squared = t0.mul(t0);
        return squared.mul(squared);
    }

    private static DoubleVector cornerDot(double[] gxs, double[] gys, double[] xs, double[] ys) {
        DoubleVector x = DoubleVector.fromArray(D, xs, 0);
        return DoubleVector.fromArray(D, gys, 0)
                .lanewise(
                        VectorOperators.FMA,
                        DoubleVector.fromArray(D, ys, 0),
                        DoubleVector.fromArray(D, gxs, 0).mul(x));
    }

    private static final class Lattice {

        final double[] x0 = new double[LANES];
        final double[] y0 = new double[LANES];
        final double[] x1 = new double[LANES];
        final double[] y1 = new double[LANES];
        final double[] x2 = new double[LANES];
        final double[] y2 = new double[LANES];
        final double[] gx0 = new double[LANES];
        final double[] gy0 = new double[LANES];
        final double[] gx1 = new double[LANES];
        final double[] gy1 = new double[LANES];
        final double[] gx2 = new double[LANES];
        final double[] gy2 = new double[LANES];

        void resolve(
                int[] perm,
                double[] gradX,
                double[] gradY,
                int tableOffset,
                double[] xs,
                double[] ys,
                double scale,
                int base) {
            for (int l = 0; l < LANES; l++) {
                double xin = xs[base + l] * scale;
                double yin = ys[base + l] * scale;

                double skew = xin + yin;
                int i = Mth.floor(Math.fma(skew, F2, xin));
                int j = Mth.floor(Math.fma(skew, F2, yin));

                double unskew = -(double) (i + j);
                double px = xin - Math.fma(unskew, G2, i);
                double py = yin - Math.fma(unskew, G2, j);

                int i1;
                int j1;
                if (px > py) {
                    i1 = 1;
                    j1 = 0;
                } else {
                    i1 = 0;
                    j1 = 1;
                }

                x0[l] = px;
                y0[l] = py;
                x1[l] = px - i1 + G2;
                y1[l] = py - j1 + G2;
                x2[l] = px - 1.0D + 2.0D * G2;
                y2[l] = py - 1.0D + 2.0D * G2;

                int ii = i & 0xFF;
                int jj = j & 0xFF;
                int k0 = (ii + perm[tableOffset + jj]) & 0xFF;
                int k1 = (ii + i1 + perm[tableOffset + ((jj + j1) & 0xFF)]) & 0xFF;
                int k2 = (ii + 1 + perm[tableOffset + ((jj + 1) & 0xFF)]) & 0xFF;

                gx0[l] = gradX[tableOffset + k0];
                gy0[l] = gradY[tableOffset + k0];
                gx1[l] = gradX[tableOffset + k1];
                gy1[l] = gradY[tableOffset + k1];
                gx2[l] = gradX[tableOffset + k2];
                gy2[l] = gradY[tableOffset + k2];
            }
        }
    }
}
