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

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public sealed interface ResponseFunction {

    Component MEDIUM_LOGARITHMIC =
            Component.literal("MEDIUM / LOGARITHMIC").withStyle(ChatFormatting.YELLOW);
    Component MEDIUM_SQUARE_ROOT =
            Component.literal("MEDIUM / SQUARE ROOT").withStyle(ChatFormatting.YELLOW);
    Component DANGEROUS_LINEAR =
            Component.literal("DANGEROUS / LINEAR").withStyle(ChatFormatting.RED);
    Component DANGEROUS_QUADRATIC =
            Component.literal("DANGEROUS / QUADRATIC").withStyle(ChatFormatting.RED);

    double apply(double x);

    String formula();

    Component danger();

    ResponseFunction withDivisor(double divisor);

    ResponseFunction withOffset(double offset);

    static Logarithmic logarithmic(double scale) {
        return new Logarithmic(scale, 1, 1);
    }

    static DampedRoot root(double scale) {
        return new DampedRoot(scale, 1, 0);
    }

    static DampedRoot fallingRoot(double falloff) {
        return new DampedRoot(1 / falloff, 1, falloff * falloff);
    }

    static Linear linear(double scale) {
        return new Linear(scale, 1, 0);
    }

    static Quadratic quadratic(double scale, double constant) {
        return new Quadratic(scale, constant, 1, 0);
    }

    record Logarithmic(double scale, double divisor, double offset) implements ResponseFunction {
        @Override
        public double apply(double x) {
            return Math.log10(x / divisor + offset) * scale;
        }

        @Override
        public String formula() {
            return "log10(" + input(divisor, offset) + ") * " + tenths(scale);
        }

        @Override
        public Component danger() {
            return MEDIUM_LOGARITHMIC;
        }

        @Override
        public Logarithmic withDivisor(double divisor) {
            return new Logarithmic(scale, divisor, offset);
        }

        @Override
        public Logarithmic withOffset(double offset) {
            return new Logarithmic(scale, divisor, offset);
        }
    }

    record DampedRoot(double scale, double divisor, double offset) implements ResponseFunction {
        @Override
        public double apply(double x) {
            return NtmMath.dampedSqrt(x / divisor + offset) * scale;
        }

        @Override
        public String formula() {
            return "sqrt("
                    + input(divisor, offset)
                    + ") * "
                    + String.format(Locale.ROOT, "%,.3f", scale);
        }

        @Override
        public Component danger() {
            return MEDIUM_SQUARE_ROOT;
        }

        @Override
        public DampedRoot withDivisor(double divisor) {
            return new DampedRoot(scale, divisor, offset);
        }

        @Override
        public DampedRoot withOffset(double offset) {
            return new DampedRoot(scale, divisor, offset);
        }
    }

    record Linear(double scale, double divisor, double offset) implements ResponseFunction {
        @Override
        public double apply(double x) {
            return (x / divisor + offset) * scale;
        }

        @Override
        public String formula() {
            return input(divisor, offset) + " * " + tenths(scale);
        }

        @Override
        public Component danger() {
            return DANGEROUS_LINEAR;
        }

        @Override
        public Linear withDivisor(double divisor) {
            return new Linear(scale, divisor, offset);
        }

        @Override
        public Linear withOffset(double offset) {
            return new Linear(scale, divisor, offset);
        }
    }

    record Quadratic(double scale, double constant, double divisor, double offset)
            implements ResponseFunction {
        @Override
        public double apply(double x) {
            double u = x / divisor + offset;
            return u * u * scale + constant;
        }

        @Override
        public String formula() {
            String formula = input(divisor, offset) + "\u00b2 * " + tenths(scale);
            return constant != 0 ? formula + " + " + tenths(constant) : formula;
        }

        @Override
        public Component danger() {
            return DANGEROUS_QUADRATIC;
        }

        @Override
        public Quadratic withDivisor(double divisor) {
            return new Quadratic(scale, constant, divisor, offset);
        }

        @Override
        public Quadratic withOffset(double offset) {
            return new Quadratic(scale, constant, divisor, offset);
        }
    }

    private static String input(double divisor, double offset) {
        String input = divisor != 1 ? "x / " + tenths(divisor) : "x";
        return offset != 0 ? input + " + " + tenths(offset) : input;
    }

    private static String tenths(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }
}
