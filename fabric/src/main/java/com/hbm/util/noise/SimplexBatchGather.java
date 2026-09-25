// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util.noise;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

final class SimplexBatchGather implements SimplexBatch {

    private static final VectorSpecies<Double> D = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> I =
            VectorSpecies.of(int.class, VectorShape.forBitSize(D.length() * Integer.SIZE));
    private static final int LANES = D.length();
    private static final int TILE = 64;

    private static final ThreadLocal<Tile> TL_TILE = ThreadLocal.withInitial(Tile::new);

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
            Tile tile = TL_TILE.get();
            while (i < bound) {
                int width = Math.min(TILE, bound - i);
                tile.skew(xs, ys, scale, i, width);
                tile.offsets(width);
                tile.index0(perm, tableOffset, width);
                tile.index1(perm, tableOffset, width);
                tile.index2(perm, tableOffset, width);
                tile.corner0(gradX, gradY, tableOffset, width);
                tile.corner1(gradX, gradY, tableOffset, width);
                tile.corner2(gradX, gradY, tableOffset, width);
                tile.emit(weight, acc, i, width);
                i += width;
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

    private static final double ROUND_MAGIC = 6755399441055744.0D;

    private static DoubleVector floor(DoubleVector v) {
        DoubleVector rounded = v.add(ROUND_MAGIC).sub(ROUND_MAGIC);
        return rounded.sub(1.0D, v.compare(VectorOperators.LT, rounded));
    }

    private static IntVector toInt(DoubleVector v) {
        return (IntVector) v.convertShape(VectorOperators.D2I, I, 0);
    }

    private static IntVector lookup(
            int[] table, int tableOffset, IntVector index, int[] scratch, int at) {
        index.lanewise(VectorOperators.AND, 0xFF).intoArray(scratch, at);
        return IntVector.fromArray(I, table, tableOffset, scratch, at);
    }

    private static final class Tile {

        final double[] fi = new double[TILE];
        final double[] fj = new double[TILE];
        final double[] xin = new double[TILE];
        final double[] yin = new double[TILE];
        final int[] i1 = new int[TILE];
        final int[] j1 = new int[TILE];
        final double[] x0 = new double[TILE];
        final double[] y0 = new double[TILE];
        final double[] x1 = new double[TILE];
        final double[] y1 = new double[TILE];
        final double[] x2 = new double[TILE];
        final double[] y2 = new double[TILE];
        final double[] sum = new double[TILE];
        final int[] ii = new int[TILE];
        final int[] jj = new int[TILE];
        final int[] k0 = new int[TILE];
        final int[] k1 = new int[TILE];
        final int[] k2 = new int[TILE];

        void skew(double[] xs, double[] ys, double scale, int base, int width) {
            for (int o = 0; o < width; o += LANES) {
                DoubleVector vx = DoubleVector.fromArray(D, xs, base + o).mul(scale);
                DoubleVector vy = DoubleVector.fromArray(D, ys, base + o).mul(scale);
                DoubleVector skew = vx.add(vy);
                vx.intoArray(xin, o);
                vy.intoArray(yin, o);
                DoubleVector vfi = floor(skew.lanewise(VectorOperators.FMA, F2, vx));
                DoubleVector vfj = floor(skew.lanewise(VectorOperators.FMA, F2, vy));
                vfi.intoArray(fi, o);
                vfj.intoArray(fj, o);
                toInt(vfi).lanewise(VectorOperators.AND, 0xFF).intoArray(ii, o);
                toInt(vfj).lanewise(VectorOperators.AND, 0xFF).intoArray(jj, o);
            }
        }

        void offsets(int width) {
            for (int o = 0; o < width; o += LANES) {
                DoubleVector vfi = DoubleVector.fromArray(D, fi, o);
                DoubleVector vfj = DoubleVector.fromArray(D, fj, o);
                DoubleVector unskew = vfi.add(vfj).neg();
                DoubleVector px =
                        DoubleVector.fromArray(D, xin, o)
                                .sub(unskew.lanewise(VectorOperators.FMA, G2, vfi));
                DoubleVector py =
                        DoubleVector.fromArray(D, yin, o)
                                .sub(unskew.lanewise(VectorOperators.FMA, G2, vfj));
                px.intoArray(x0, o);
                py.intoArray(y0, o);
                px.sub(1.0D).add(2.0D * G2).intoArray(x2, o);
                py.sub(1.0D).add(2.0D * G2).intoArray(y2, o);

                DoubleVector vi1 =
                        DoubleVector.zero(D).blend(1.0D, px.compare(VectorOperators.GT, py));
                DoubleVector vj1 = DoubleVector.broadcast(D, 1.0D).sub(vi1);

                toInt(vi1).intoArray(i1, o);
                toInt(vj1).intoArray(j1, o);
                px.sub(vi1).add(G2).intoArray(x1, o);
                py.sub(vj1).add(G2).intoArray(y1, o);
            }
        }

        void index0(int[] perm, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                IntVector.fromArray(I, ii, o)
                        .add(lookup(perm, tableOffset, IntVector.fromArray(I, jj, o), k0, o))
                        .lanewise(VectorOperators.AND, 0xFF)
                        .intoArray(k0, o);
            }
        }

        void index1(int[] perm, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                IntVector base = IntVector.fromArray(I, jj, o).add(IntVector.fromArray(I, j1, o));
                IntVector.fromArray(I, ii, o)
                        .add(IntVector.fromArray(I, i1, o))
                        .add(lookup(perm, tableOffset, base, k1, o))
                        .lanewise(VectorOperators.AND, 0xFF)
                        .intoArray(k1, o);
            }
        }

        void index2(int[] perm, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                IntVector.fromArray(I, ii, o)
                        .add(1)
                        .add(lookup(perm, tableOffset, IntVector.fromArray(I, jj, o).add(1), k2, o))
                        .lanewise(VectorOperators.AND, 0xFF)
                        .intoArray(k2, o);
            }
        }

        void corner0(double[] gradX, double[] gradY, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                cornerWeight(x0, y0, o)
                        .mul(cornerDot(gradX, gradY, tableOffset, k0, x0, y0, o))
                        .intoArray(sum, o);
            }
        }

        void corner1(double[] gradX, double[] gradY, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                cornerWeight(x1, y1, o)
                        .lanewise(
                                VectorOperators.FMA,
                                cornerDot(gradX, gradY, tableOffset, k1, x1, y1, o),
                                DoubleVector.fromArray(D, sum, o))
                        .intoArray(sum, o);
            }
        }

        void corner2(double[] gradX, double[] gradY, int tableOffset, int width) {
            for (int o = 0; o < width; o += LANES) {
                cornerWeight(x2, y2, o)
                        .lanewise(
                                VectorOperators.FMA,
                                cornerDot(gradX, gradY, tableOffset, k2, x2, y2, o),
                                DoubleVector.fromArray(D, sum, o))
                        .intoArray(sum, o);
            }
        }

        void emit(double weight, double[] acc, int base, int width) {
            for (int o = 0; o < width; o += LANES) {
                DoubleVector.fromArray(D, sum, o)
                        .mul(70.0D)
                        .lanewise(
                                VectorOperators.FMA,
                                weight,
                                DoubleVector.fromArray(D, acc, base + o))
                        .intoArray(acc, base + o);
            }
        }

        private static DoubleVector cornerWeight(double[] xs, double[] ys, int o) {
            DoubleVector x = DoubleVector.fromArray(D, xs, o);
            DoubleVector y = DoubleVector.fromArray(D, ys, o);
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

        private static DoubleVector cornerDot(
                double[] gradX,
                double[] gradY,
                int tableOffset,
                int[] idx,
                double[] xs,
                double[] ys,
                int o) {
            DoubleVector x = DoubleVector.fromArray(D, xs, o);
            return DoubleVector.fromArray(D, gradY, tableOffset, idx, o)
                    .lanewise(
                            VectorOperators.FMA,
                            DoubleVector.fromArray(D, ys, o),
                            DoubleVector.fromArray(D, gradX, tableOffset, idx, o).mul(x));
        }
    }
}
