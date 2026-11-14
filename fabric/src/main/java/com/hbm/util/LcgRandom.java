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

package com.hbm.util;

public final class LcgRandom {

    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long INCREMENT = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    private LcgRandom() {}

    public static long seeded(long seed) {
        return (seed ^ MULTIPLIER) & MASK;
    }

    public static long afterInt(long state, int bound) {
        long next = step(state);
        if (powerOfTwo(bound)) return next;
        while (retries(top31(next), bound)) next = step(next);
        return next;
    }

    public static int intDrawn(long state, int bound) {
        int bits = top31(state);
        return powerOfTwo(bound) ? (int) ((bound * (long) bits) >> 31) : bits % bound;
    }

    private static long step(long state) {
        return (state * MULTIPLIER + INCREMENT) & MASK;
    }

    private static int top31(long state) {
        return (int) (state >>> 17);
    }

    private static boolean powerOfTwo(int bound) {
        return (bound & (bound - 1)) == 0;
    }

    private static boolean retries(int bits, int bound) {
        return bits - bits % bound + (bound - 1) < 0;
    }
}
