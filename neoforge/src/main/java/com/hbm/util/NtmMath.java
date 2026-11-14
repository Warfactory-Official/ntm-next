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

import net.minecraft.world.phys.Vec3;

public final class NtmMath {

    private NtmMath() {}

    public static float roundFloat(double number, int decimals) {
        var scale = Math.pow(10, decimals);
        return (float) (Math.round(number * scale) / scale);
    }

    public static double lerp(double from, double to, double weight) {
        return Math.fma(to - from, weight, from);
    }

    public static double flatSine(double x) {
        return Math.sin(Math.PI / 2 * Math.cos(x));
    }

    public static double angleBetween(Vec3 u, Vec3 v) {
        var degrees = Math.acos(u.dot(v) / (u.length() * v.length())) * 180 / Math.PI;
        return degrees >= 180 ? degrees - 180 : degrees;
    }

    public static double angleDelta(double from, double to) {
        var delta = (to - from + 180) % 360 - 180;
        return delta < -180 ? delta + 360 : delta;
    }

    public static double dampedSqrt(double x) {
        var offset = x + 2;
        return Math.sqrt(x + 1 / (offset * offset)) - 1 / offset;
    }

    public static int min(int first, int second, int third) {
        return Math.min(first, Math.min(second, third));
    }

    public static int min(int first, int... rest) {
        var min = first;
        for (var value : rest) min = Math.min(min, value);
        return min;
    }

    public static long min(long... values) {
        var min = Long.MAX_VALUE;
        for (var value : values) min = Math.min(min, value);
        return min;
    }

    public static long max(long... values) {
        var max = Long.MIN_VALUE;
        for (var value : values) max = Math.max(max, value);
        return max;
    }
}
