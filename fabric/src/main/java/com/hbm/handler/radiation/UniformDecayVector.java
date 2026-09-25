// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

final class UniformDecayVector implements UniformDecay {

    private static final VectorSpecies<Double> D = DoubleVector.SPECIES_PREFERRED;

    @Override
    public void decay(
            double[] uniform,
            int off,
            int n,
            double retention,
            double minBound,
            double fogRad,
            double destroyRad,
            long[] out) {
        long changed = 0L, zeroed = 0L, fog = 0L, destroy = 0L;
        int bound = D.loopBound(n);
        int i = 0;

        for (; i < bound; i += D.length()) {
            DoubleVector prev = DoubleVector.fromArray(D, uniform, off + i);
            DoubleVector decayed = prev.mul(retention);

            VectorMask<Double> nan = decayed.compare(VectorOperators.NE, decayed);
            VectorMask<Double> tiny =
                    decayed.abs()
                            .compare(VectorOperators.LT, RadiationSystemNT.RAD_EPSILON)
                            .and(decayed.compare(VectorOperators.GT, minBound));
            VectorMask<Double> toZero = nan.or(tiny);

            DoubleVector clamped =
                    decayed.lanewise(VectorOperators.MIN, RadiationSystemNT.RAD_MAX)
                            .lanewise(VectorOperators.MAX, minBound);
            DoubleVector next = clamped.blend(0.0D, toZero);
            next.intoArray(uniform, off + i);

            VectorMask<Double> isZero = next.compare(VectorOperators.EQ, 0.0D);
            VectorMask<Double> nonZero = isZero.not();
            changed |= next.compare(VectorOperators.NE, prev).toLong() << i;
            zeroed |= isZero.toLong() << i;
            fog |= next.compare(VectorOperators.GT, fogRad).and(nonZero).toLong() << i;
            destroy |= next.compare(VectorOperators.GE, destroyRad).and(nonZero).toLong() << i;
        }

        for (; i < n; i++) {
            double prev = uniform[off + i];
            double next = RadiationSystemNT.sanitize(prev * retention, minBound);
            if (next != prev) {
                uniform[off + i] = next;
                changed |= 1L << i;
            }
            if (next == 0.0D) {
                zeroed |= 1L << i;
            } else {
                if (next > fogRad) fog |= 1L << i;
                if (next >= destroyRad) destroy |= 1L << i;
            }
        }

        out[CHANGED] = changed;
        out[ZEROED] = zeroed;
        out[FOG] = fog;
        out[DESTROY] = destroy;
    }
}
