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

package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.mods.InstalledWeaponMods;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class GunStateData {

    public static final GunStateData EMPTY = new GunStateData(new CompoundTag());
    public static final Codec<GunStateData> CODEC =
            CompoundTag.CODEC.xmap(GunStateData::new, GunStateData::tag);

    private final CompoundTag tag;
    private volatile @Nullable InstalledWeaponMods[] views;

    public GunStateData(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag tag() {
        return tag;
    }

    public InstalledWeaponMods installedMods(Item gun, int cfg) {
        var memo = views;
        var view = memo != null && cfg < memo.length ? memo[cfg] : null;
        if (view != null && view.appliesTo(gun)) return view;
        view =
                InstalledWeaponMods.of(
                        gun,
                        tag.read(
                                        XWeaponModManager.KEY_MOD_LIST + cfg,
                                        XWeaponModManager.MOD_LIST_CODEC)
                                .orElse(List.of()));
        var next =
                memo == null
                        ? new InstalledWeaponMods[cfg + 1]
                        : Arrays.copyOf(memo, Math.max(memo.length, cfg + 1));
        next[cfg] = view;
        views = next;
        return view;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GunStateData data && tag.equals(data.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return "GunStateData[tag=" + tag + "]";
    }
}
