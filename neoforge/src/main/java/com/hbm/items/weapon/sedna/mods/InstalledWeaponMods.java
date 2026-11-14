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

import com.hbm.items.ModDataComponents;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InstalledWeaponMods {
    public static final InstalledWeaponMods NONE = new InstalledWeaponMods(null, List.of());
    private static final int SHARED_LIMIT = 1_024;
    private static final ConcurrentHashMap<Key, InstalledWeaponMods> SHARED =
            new ConcurrentHashMap<>();

    private final @Nullable Item gun;
    private final List<Identifier> ids;
    final int[] bindings;
    private final ConcurrentHashMap<Object, Object> values = new ConcurrentHashMap<>();

    private InstalledWeaponMods(@Nullable Item gun, List<Identifier> ids) {
        this.gun = gun;
        this.ids = ids;
        var bound = new int[ids.size()];
        int count = 0;
        for (var id : ids) {
            int binding = XWeaponModManager.binding(gun, id);
            if (binding >= 0) bound[count++] = binding;
        }
        bindings = count == ids.size() ? bound : Arrays.copyOf(bound, count);
    }

    public static InstalledWeaponMods of(ItemStack stack, int cfg) {
        var state = stack.get(ModDataComponents.GUN_STATE.get());
        return state == null ? NONE : state.installedMods(stack.getItem(), cfg);
    }

    public static InstalledWeaponMods of(Item gun, List<Identifier> ids) {
        if (ids.isEmpty()) return NONE;
        var shared = SHARED.get(new Key(gun, ids));
        if (shared != null) return shared;
        if (SHARED.size() >= SHARED_LIMIT) SHARED.clear();
        return SHARED.computeIfAbsent(
                new Key(gun, List.copyOf(ids)), key -> new InstalledWeaponMods(key.gun, key.ids));
    }

    public List<Identifier> ids() {
        return ids;
    }

    public boolean appliesTo(Item gun) {
        return this.gun == null || this.gun == gun;
    }

    @SuppressWarnings("unchecked")
    public <O, V> V resolved(
            O owner, BiFunction<? super O, InstalledWeaponMods, ? extends V> resolve) {
        var resolved = values.get(owner);
        if (resolved == null)
            resolved =
                    values.computeIfAbsent(
                            owner, _ -> resolve.apply(owner, this));
        return (V) resolved;
    }

    private record Key(Item gun, List<Identifier> ids) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key(var otherGun, var otherIds)
                    && otherGun == gun
                    && otherIds.equals(ids);
        }

        @Override
        public int hashCode() {
            return 31 * gun.hashCode() + ids.hashCode();
        }
    }
}
