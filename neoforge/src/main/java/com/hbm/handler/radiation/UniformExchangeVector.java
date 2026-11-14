/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler.radiation;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

final class UniformExchangeVector implements UniformExchange {

    private static final VectorSpecies<Double> D = DoubleVector.SPECIES_PREFERRED;

    @Override
    public long exchange(double[] uniform, int offA, int offB, int n, double uuE) {
        long changed = 0L;
        int bound = D.loopBound(n);
        int i = 0;

        for (; i < bound; i += D.length()) {
            DoubleVector ra = DoubleVector.fromArray(D, uniform, offA + i);
            DoubleVector rb = DoubleVector.fromArray(D, uniform, offB + i);
            DoubleVector avg = ra.add(rb).mul(0.5D);
            DoubleVector halfDiff = ra.sub(rb).mul(0.5D);
            halfDiff.lanewise(VectorOperators.FMA, uuE, avg).intoArray(uniform, offA + i);
            halfDiff.neg().lanewise(VectorOperators.FMA, uuE, avg).intoArray(uniform, offB + i);
            changed |= ra.compare(VectorOperators.NE, rb).toLong() << i;
        }

        for (; i < n; i++) {
            if (RadiationSystemNT.exchangeUniExactXZ(uniform, offA + i, offB + i, uuE))
                changed |= 1L << i;
        }
        return changed;
    }
}
