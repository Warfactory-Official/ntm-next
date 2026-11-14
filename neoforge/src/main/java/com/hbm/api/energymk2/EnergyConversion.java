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

package com.hbm.api.energymk2;

import com.hbm.data.EnergyData;
import java.math.BigInteger;

public final class EnergyConversion {

    private EnergyConversion() {}

    public static long heRatio() {
        return EnergyData.ENERGY_RATIO_HE.get();
    }

    public static long feRatio() {
        return EnergyData.ENERGY_RATIO_FE.get();
    }

    public static long feFromHe(long he) {
        return he <= 0 ? 0 : scale(he, feRatio(), heRatio());
    }

    public static long heFromFe(long fe) {
        return fe <= 0 ? 0 : scale(fe, heRatio(), feRatio());
    }

    static long heFromFeCeil(long fe) {
        if (fe <= 0) return 0;
        long a = heRatio();
        long b = feRatio();
        if (fe <= Long.MAX_VALUE / a) {
            long numerator = fe * a;
            return numerator / b + (numerator % b == 0 ? 0 : 1);
        }
        BigInteger[] result =
                BigInteger.valueOf(fe)
                        .multiply(BigInteger.valueOf(a))
                        .divideAndRemainder(BigInteger.valueOf(b));
        return result[0]
                .add(result[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE)
                .min(BigInteger.valueOf(Long.MAX_VALUE))
                .longValueExact();
    }

    public static long feQuantum() {
        return feQuantum(heRatio(), feRatio());
    }

    static long feQuantum(long heRatio, long feRatio) {
        return feRatio / gcd(heRatio, feRatio);
    }

    private static long gcd(long x, long y) {
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    private static long scale(long value, long mul, long div) {
        if (value > Long.MAX_VALUE / mul) {
            BigInteger result =
                    BigInteger.valueOf(value)
                            .multiply(BigInteger.valueOf(mul))
                            .divide(BigInteger.valueOf(div));
            return result.min(BigInteger.valueOf(Long.MAX_VALUE)).longValueExact();
        }
        return value * mul / div;
    }

    static long convert(long amount, long mul, long div, long[] carry) {
        if (amount <= 0) return 0;
        long banked = carry[0];
        if (amount > (Long.MAX_VALUE - banked) / mul) {
            BigInteger[] result =
                    BigInteger.valueOf(amount)
                            .multiply(BigInteger.valueOf(mul))
                            .add(BigInteger.valueOf(banked))
                            .divideAndRemainder(BigInteger.valueOf(div));
            if (result[0].compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) return Long.MAX_VALUE;
            carry[0] = result[1].longValueExact();
            return result[0].longValueExact();
        }
        long numerator = amount * mul + banked;
        carry[0] = numerator % div;
        return numerator / div;
    }

    public static final class Carry {

        private long heToFe;
        private long feToHe;

        public long feFromHe(long he) {
            long[] cell = {heToFe};
            long out = convert(he, feRatio(), heRatio(), cell);
            heToFe = cell[0];
            return out;
        }

        public long heFromFe(long fe) {
            long[] cell = {feToHe};
            long out = convert(fe, heRatio(), feRatio(), cell);
            feToHe = cell[0];
            return out;
        }

        public long refundFe(long fe, long maxHe) {
            if (fe <= 0 || maxHe <= 0) return 0;
            long a = heRatio();
            long b = feRatio();
            if (fe > (Long.MAX_VALUE - heToFe) / a) return 0;
            heToFe += fe * a;
            long he = Math.min(heToFe / b, maxHe);
            heToFe -= he * b;
            return he;
        }

        public long heToFeRemainder() {
            return heToFe;
        }

        public long feToHeRemainder() {
            return feToHe;
        }

        public void absorb(Carry other) {
            heToFe += other.heToFe;
            feToHe += other.feToHe;
        }

        public void restore(long heToFe, long feToHe) {
            this.heToFe = heToFe;
            this.feToHe = feToHe;
        }
    }
}
