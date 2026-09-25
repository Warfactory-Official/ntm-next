// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

public record GunStateData(CompoundTag tag) {

    public static final GunStateData EMPTY = new GunStateData(new CompoundTag());
    public static final Codec<GunStateData> CODEC =
            CompoundTag.CODEC.xmap(GunStateData::new, GunStateData::tag);
}
