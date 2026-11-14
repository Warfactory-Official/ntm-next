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

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public abstract class WeaponModBase {

    public static final int PRIORITY_SET = 1_000_000;
    public static final int PRIORITY_MULTIPLICATIVE = 1_000;
    public static final int PRIORITY_ADDITIVE = 500;
    public static final int PRIORITY_MULT_FINAL = -1;

    private final int priority;
    private final List<Slot> slots;

    protected WeaponModBase(int priority, Slot... slots) {
        this.priority = priority;
        this.slots = List.of(slots);
    }

    public int priority() {
        return priority;
    }

    public List<Slot> slots() {
        return slots;
    }

    boolean sharesSlot(WeaponModBase other) {
        for (var slot : slots) if (other.slots.contains(slot)) return true;
        return false;
    }

    protected abstract void operations(Operations ops);

    protected boolean revalidatesMagazine() {
        return false;
    }

    protected void onInstall(ServerLevel level, ItemStack gun) {}

    protected void onUninstall(ServerLevel level, ItemStack gun) {}

    public enum Slot {
        FIRERATE,
        DAMAGE,
        MULTI,
        OVERRIDE,
        GENERIC_DAMAGE,
        GENERIC_DURABILITY,
        SPEEDLOADER,
        SILENCER,
        SCOPE,
        BARREL,
        SHIELD,
        FURNITURE,
        SPEED,
        BAYONET,
        MAG,
        UNDERBARREL,
        RECEIVER,
        COIN1,
        COIN2,
        DRILL,
        ENGINE,
        MAGNET,
        SIFTER,
        CANISTERS,
        CALIBER
    }

    public static final class Operations {
        private final int binding;

        Operations(int binding) {
            this.binding = binding;
        }

        public Operations set(GunAttribute.OfFloat attr, float value) {
            attr.put(binding, _ -> value);
            return this;
        }

        public Operations scale(GunAttribute.OfFloat attr, float factor) {
            attr.put(binding, v -> v * factor);
            return this;
        }

        public Operations add(GunAttribute.OfFloat attr, float amount) {
            attr.put(binding, v -> v + amount);
            return this;
        }

        public Operations map(GunAttribute.OfFloat attr, GunAttribute.FloatOp op) {
            attr.put(binding, op);
            return this;
        }

        public Operations set(GunAttribute.OfInt attr, int value) {
            attr.put(binding, _ -> value);
            return this;
        }

        public Operations map(GunAttribute.OfInt attr, IntUnaryOperator op) {
            attr.put(binding, op);
            return this;
        }

        public Operations set(GunAttribute.OfBool attr, boolean value) {
            attr.put(binding, _ -> value);
            return this;
        }

        public Operations scale(GunAttribute.OfDouble attr, double factor) {
            attr.put(binding, v -> v * factor);
            return this;
        }

        public <T> Operations set(GunAttribute.Ref<T> attr, T value) {
            attr.put(binding, _ -> value);
            return this;
        }

        public <T> Operations map(GunAttribute.Ref<T> attr, UnaryOperator<T> op) {
            attr.put(binding, op);
            return this;
        }

        public Operations clear(GunAttribute<?> attr) {
            attr.clear(binding);
            return this;
        }
    }
}
