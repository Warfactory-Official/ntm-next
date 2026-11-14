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

import java.math.BigInteger;

public final class NumberFormatter {

    private static final double[] UNITS = {1e3, 1e6, 1e9, 1e12, 1e15, 1e18};
    private static final String[] SUFFIXES = {"k", "M", "G", "T", "P", "E"};
    private static final long TICKS_PER_DAY = 48_000;
    private static final long TICKS_PER_YEAR = 100 * TICKS_PER_DAY;

    private NumberFormatter() {}

    public static String formatCompact(long value) {
        double magnitude = Math.abs(value);
        for (var unit = UNITS.length - 1; unit >= 0; unit--) {
            if (magnitude >= UNITS[unit]) {
                var mantissa = value / UNITS[unit];
                var rounded =
                        mantissa <= -100
                                ? Math.round(mantissa * 10) / 10.0
                                : Math.round(mantissa * 100) / 100.0;
                return rounded + SUFFIXES[unit];
            }
        }
        return Long.toString(value);
    }

    public static String formatGrouped(long value) {
        return group(Long.toString(value));
    }

    public static String formatGrouped(BigInteger value) {
        return group(value.toString());
    }

    public static String[] formatLifetime(long ticks) {
        var years = Math.floorDiv(ticks, TICKS_PER_YEAR);
        var ofYear = Math.floorMod(ticks, TICKS_PER_YEAR);
        var days = ofYear / TICKS_PER_DAY;
        var ofDay = ofYear % TICKS_PER_DAY;
        var tenths = (float) ((double) (10 * ofDay) / TICKS_PER_DAY);
        return new String[] {Long.toString(years), Long.toString(days), Float.toString(tenths)};
    }

    private static String group(String digits) {
        var sign = digits.charAt(0) == '-' ? 1 : 0;
        var text = new StringBuilder(digits);
        for (var at = digits.length() - 3; at > sign; at -= 3) text.insert(at, ',');
        return text.toString();
    }
}
