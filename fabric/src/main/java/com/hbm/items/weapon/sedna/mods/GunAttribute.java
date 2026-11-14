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

package com.hbm.items.weapon.sedna.mods;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

public abstract sealed class GunAttribute<T>
        permits GunAttribute.Ref,
                GunAttribute.OfFloat,
                GunAttribute.OfInt,
                GunAttribute.OfBool,
                GunAttribute.OfDouble {

    abstract void clear(int binding);

    public static final class Ref<T> extends GunAttribute<T> {
        private final Int2ObjectOpenHashMap<UnaryOperator<T>> ops = new Int2ObjectOpenHashMap<>();

        public T resolve(T value, InstalledWeaponMods mods) {
            if (ops.isEmpty()) return value;
            for (int binding : mods.bindings) {
                var op = ops.get(binding);
                if (op != null) value = op.apply(value);
            }
            return value;
        }

        void put(int binding, UnaryOperator<T> op) {
            ops.put(binding, op);
        }

        @Override
        void clear(int binding) {
            ops.remove(binding);
        }
    }

    public static final class OfFloat extends GunAttribute<Float> {
        private final Int2ObjectOpenHashMap<FloatOp> ops = new Int2ObjectOpenHashMap<>();

        public float resolve(float value, InstalledWeaponMods mods) {
            if (ops.isEmpty()) return value;
            for (int binding : mods.bindings) {
                var op = ops.get(binding);
                if (op != null) value = op.apply(value);
            }
            return value;
        }

        void put(int binding, FloatOp op) {
            ops.put(binding, op);
        }

        @Override
        void clear(int binding) {
            ops.remove(binding);
        }
    }

    public static final class OfInt extends GunAttribute<Integer> {
        private final Int2ObjectOpenHashMap<IntUnaryOperator> ops = new Int2ObjectOpenHashMap<>();

        public int resolve(int value, InstalledWeaponMods mods) {
            if (ops.isEmpty()) return value;
            for (int binding : mods.bindings) {
                var op = ops.get(binding);
                if (op != null) value = op.applyAsInt(value);
            }
            return value;
        }

        void put(int binding, IntUnaryOperator op) {
            ops.put(binding, op);
        }

        @Override
        void clear(int binding) {
            ops.remove(binding);
        }
    }

    public static final class OfBool extends GunAttribute<Boolean> {
        private final Int2ObjectOpenHashMap<BoolOp> ops = new Int2ObjectOpenHashMap<>();

        public boolean resolve(boolean value, InstalledWeaponMods mods) {
            if (ops.isEmpty()) return value;
            for (int binding : mods.bindings) {
                var op = ops.get(binding);
                if (op != null) value = op.apply(value);
            }
            return value;
        }

        void put(int binding, BoolOp op) {
            ops.put(binding, op);
        }

        @Override
        void clear(int binding) {
            ops.remove(binding);
        }
    }

    public static final class OfDouble extends GunAttribute<Double> {
        private final Int2ObjectOpenHashMap<DoubleUnaryOperator> ops =
                new Int2ObjectOpenHashMap<>();

        public double resolve(double value, InstalledWeaponMods mods) {
            if (ops.isEmpty()) return value;
            for (int binding : mods.bindings) {
                var op = ops.get(binding);
                if (op != null) value = op.applyAsDouble(value);
            }
            return value;
        }

        void put(int binding, DoubleUnaryOperator op) {
            ops.put(binding, op);
        }

        @Override
        void clear(int binding) {
            ops.remove(binding);
        }
    }

    @FunctionalInterface
    public interface FloatOp {
        float apply(float value);
    }

    @FunctionalInterface
    public interface BoolOp {
        boolean apply(boolean value);
    }
}
