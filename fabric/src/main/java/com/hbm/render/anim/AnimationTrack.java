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

package com.hbm.render.anim;

import com.hbm.render.anim.BusAnimationKeyframe.EType;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class AnimationTrack {

    private static final double NEAR = 0.000001;
    private static final double ROOT_MIN = 0.000001;
    private static final double ROOT_MAX = 1.000001;
    private static final int CURVE = 9;

    private static final AnimationTrack ZERO = new AnimationTrack(0);
    private static final AnimationTrack ONE = new AnimationTrack(1);

    private final double rest;
    private final int[] reach;
    private final double[] start;
    private final double[] span;
    private final double[] from;
    private final double[] to;
    private final Shape[] shape;
    private final double[] curve;
    private volatile @Nullable GpuAnimationTrack gpu;

    private AnimationTrack(double rest) {
        this.rest = rest;
        reach = new int[0];
        start = span = from = to = curve = new double[0];
        shape = new Shape[0];
    }

    private AnimationTrack(List<BusAnimationKeyframe> keys, double timeScale) {
        int n = keys.size();
        rest = 0;
        reach = new int[n];
        start = new double[n];
        span = new double[n];
        from = new double[n];
        to = new double[n];
        shape = new Shape[n];
        curve = new double[keys.stream().anyMatch(AnimationTrack::shapesWithCurve) ? n * CURVE : 0];
        int end = 0, furthest = Integer.MIN_VALUE, authored = 0;
        BusAnimationKeyframe previous = null;
        for (int i = 0; i < n; i++) {
            var key = keys.get(i);
            int duration = (int) (key.duration * timeScale);
            start[i] = end;
            span[i] = duration;
            end += duration;
            furthest = Math.max(furthest, end);
            reach[i] = furthest;
            from[i] = previous == null ? 0 : previous.value;
            to[i] = key.value;
            shape[i] = shape(previous, key, duration, from[i], to[i]);
            int at = i * CURVE;
            switch (shape[i]) {
                case BACK_OUT -> curve[at] = previous.back;
                case BEZIER -> bezier(previous, key, authored, duration, at);
                default -> {}
            }
            authored += key.authoredDuration;
            previous = key;
        }
    }

    public static AnimationTrack compile(
            List<BusAnimationKeyframe> keys, double timeScale, double rest) {
        return keys.isEmpty() ? rest == 0 ? ZERO : ONE : new AnimationTrack(keys, timeScale);
    }

    public GpuAnimationTrack getGpuTrack() {
        var compiled = gpu;
        if (compiled != null) return compiled;
        int n = reach.length;
        int[] ends = new int[n];
        float[] values = new float[n];
        int[] shapes = new int[n];
        for (int i = 0; i < n; i++) {
            if (span[i] < 0) throw new IllegalStateException("negative key duration " + span[i]);
            ends[i] = reach[i];
            values[i] = (float) to[i];
            shapes[i] =
                    switch (shape[i]) {
                        case HOLD_TO -> 0;
                        case HOLD_FROM -> 1;
                        case LINEAR -> 2;
                        case SIN_UP -> 3;
                        case SIN_DOWN -> 4;
                        case SIN_FULL -> 5;
                        default ->
                                throw new IllegalStateException("no shader form for " + shape[i]);
                    };
        }
        gpu = compiled = new GpuAnimationTrack(ends, values, shapes, (float) rest);
        return compiled;
    }

    private static boolean shapesWithCurve(BusAnimationKeyframe key) {
        return switch (key.interpolationType) {
            case BEZIER, BACK -> true;
            default -> false;
        };
    }

    public double getValue(int millis) {
        int low = 0, high = reach.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (reach[mid] > millis) high = mid;
            else low = mid + 1;
        }
        if (low == reach.length) return low == 0 ? rest : to[low - 1];
        int i = low, at = i * CURVE;
        double from = this.from[i], to = this.to[i], x = millis - start[i], d = span[i];
        return switch (shape[i]) {
            case HOLD_TO -> to;
            case HOLD_FROM -> from;
            case LINEAR -> (to - from) * (x / d) + from;
            case SIN_UP -> (to - from) * (-Math.sin((x / d * Math.PI + Math.PI) / 2) + 1) + from;
            case SIN_DOWN -> (to - from) * Math.sin(x / d * Math.PI / 2) + from;
            case SIN_FULL -> (to - from) * ((-Math.cos(x / d * Math.PI) + 1) / 2D) + from;
            case BEZIER -> {
                double origin = curve[at];
                double u =
                        parameter(
                                origin - (origin + x * curve[at + 1]),
                                curve[at + 2],
                                curve[at + 3],
                                curve[at + 4]);
                yield curve[at + 5]
                        + u * curve[at + 6]
                        + u * u * curve[at + 7]
                        + u * u * u * curve[at + 8];
            }
            case BACK_OUT -> AnimationEasing.backOut(x, from, to - from, d, curve[at]);
            case BOUNCE_OUT -> AnimationEasing.bounceOut(x, from, to - from, d);
            case CUBIC_IN -> AnimationEasing.cubicIn(x, from, to - from, d);
            case EXPO_IN -> AnimationEasing.expoIn(x, from, to - from, d);
            case QUAD_IN -> AnimationEasing.quadIn(x, from, to - from, d);
            case QUAD_OUT -> AnimationEasing.quadOut(x, from, to - from, d);
            case QUAD_IN_OUT -> AnimationEasing.quadInOut(x, from, to - from, d);
            case SINE_IN -> AnimationEasing.sineIn(x, from, to - from, d);
            case SINE_OUT -> AnimationEasing.sineOut(x, from, to - from, d);
        };
    }

    private static Shape shape(
            BusAnimationKeyframe previous,
            BusAnimationKeyframe key,
            int duration,
            double from,
            double to) {
        if (duration == 0) return Shape.HOLD_TO;
        if (previous != null && previous.interpolationType == IType.CONSTANT)
            return Shape.HOLD_FROM;
        if (Math.abs(from - to) < NEAR) return Shape.HOLD_TO;
        if (previous == null) return timed(key.interpolationType);
        return switch (previous.interpolationType) {
            case BEZIER -> Shape.BEZIER;
            case CONSTANT, LINEAR, SIN_UP, SIN_DOWN, SIN_FULL -> timed(key.interpolationType);
            default -> eased(previous.interpolationType, previous.easingType);
        };
    }

    private static Shape timed(IType type) {
        return switch (type) {
            case SIN_UP -> Shape.SIN_UP;
            case SIN_DOWN -> Shape.SIN_DOWN;
            case SIN_FULL -> Shape.SIN_FULL;
            default -> Shape.LINEAR;
        };
    }

    static void requireCurve(IType type, EType easing) {
        switch (type) {
            case CONSTANT, LINEAR, SIN_UP, SIN_DOWN, SIN_FULL, BEZIER -> {}
            default -> eased(type, easing);
        }
    }

    private static Shape eased(IType type, EType easing) {
        boolean in = easing == EType.AUTO || easing == EType.EASE_IN;
        boolean out = easing == EType.AUTO || easing == EType.EASE_OUT;
        Shape shape =
                switch (type) {
                    case BACK -> out ? Shape.BACK_OUT : null;
                    case BOUNCE -> out ? Shape.BOUNCE_OUT : null;
                    case CUBIC -> in ? Shape.CUBIC_IN : null;
                    case EXPO -> in ? Shape.EXPO_IN : null;
                    case QUAD ->
                            in
                                    ? Shape.QUAD_IN
                                    : easing == EType.EASE_OUT ? Shape.QUAD_OUT : Shape.QUAD_IN_OUT;
                    case SINE ->
                            in ? Shape.SINE_IN : easing == EType.EASE_OUT ? Shape.SINE_OUT : null;
                    default -> null;
                };
        if (shape == null)
            throw new IllegalArgumentException("no easing curve for " + type + " " + easing);
        return shape;
    }

    private void bezier(
            BusAnimationKeyframe previous,
            BusAnimationKeyframe key,
            int authoredStart,
            int duration,
            int at) {
        double x1 = authoredStart, y1 = previous.value;
        double x2 = previous.rightX, y2 = previous.rightY;
        double x3 = key.leftX, y3 = key.leftY;
        double x4 = x1 + key.authoredDuration, y4 = key.value;
        double length = x4 - x1;
        double reach1 = Math.abs(x1 - x2), reach2 = Math.abs(x4 - x3);
        if (reach1 != 0 || reach2 != 0) {
            if (reach1 > length) {
                double f = length / reach1;
                y2 = y1 - f * (y1 - y2);
                x2 = x1 - f * (x1 - x2);
            }
            if (reach2 > length) {
                double f = length / reach2;
                y3 = y4 - f * (y4 - y3);
                x3 = x4 - f * (x4 - x3);
            }
        }
        curve[at] = x1;
        curve[at + 1] = (double) key.authoredDuration / duration;
        curve[at + 2] = 3.0 * (x2 - x1);
        curve[at + 3] = 3.0 * (x1 - 2.0 * x2 + x3);
        curve[at + 4] = x4 - x1 + 3.0 * (x2 - x3);
        curve[at + 5] = y1;
        curve[at + 6] = 3.0 * (y2 - y1);
        curve[at + 7] = 3.0 * (y1 - 2.0 * y2 + y3);
        curve[at + 8] = y4 - y1 + 3.0 * (y2 - y3);
    }

    private static double parameter(double c0, double c1, double c2, double c3) {
        if (Math.abs(c3) > NEAR) {
            double s = c2 / c3 / 3, linear = c1 / c3, constant = c0 / c3;
            double p = linear / 3 - s * s;
            double q = (2 * s * s * s - s * linear + constant) / 2;
            double discriminant = q * q + p * p * p;
            if (discriminant > NEAR) {
                double w = Math.sqrt(discriminant);
                return cubeRoot(-q + w) + cubeRoot(-q - w) - s;
            }
            if (discriminant > -NEAR) {
                double r = cubeRoot(-q);
                double single = 2 * r - s;
                return outside(single) ? -r - s : single;
            }
            double phi = Math.acos(-q / Math.sqrt(-(p * p * p)));
            double m = Math.sqrt(-p);
            double cos = Math.cos(phi / 3);
            double sin = Math.sqrt(3 - 3 * cos * cos);
            double root = 2 * m * cos - s;
            if (!outside(root)) return root;
            root = -m * (cos + sin) - s;
            return outside(root) ? -m * (cos - sin) - s : root;
        }
        if (c2 > NEAR) {
            double discriminant = c1 * c1 - 4 * c2 * c0;
            if (discriminant > NEAR) {
                double w = Math.sqrt(discriminant);
                double root = (-c1 - w) / (2 * c2);
                return outside(root) ? (-c1 + w) / (2 * c2) : root;
            }
            if (discriminant > -NEAR) return -c1 / (2 * c2);
        }
        return c1 > NEAR ? -c0 / c1 : 0;
    }

    private static boolean outside(double u) {
        return u < ROOT_MIN || u > ROOT_MAX;
    }

    private static double cubeRoot(double v) {
        double magnitude = Math.exp(Math.log(Math.abs(v)) / 3.0);
        return v > NEAR ? magnitude : v > -NEAR ? 0 : -magnitude;
    }

    private enum Shape {
        HOLD_TO,
        HOLD_FROM,
        LINEAR,
        SIN_UP,
        SIN_DOWN,
        SIN_FULL,
        BEZIER,
        BACK_OUT,
        BOUNCE_OUT,
        CUBIC_IN,
        EXPO_IN,
        QUAD_IN,
        QUAD_OUT,
        QUAD_IN_OUT,
        SINE_IN,
        SINE_OUT
    }
}
