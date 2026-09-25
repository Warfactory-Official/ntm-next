// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.special.ItemKitCustom;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class KitColorTintSource implements ItemTintSource {

    public static final MapCodec<KitColorTintSource> CODEC_1 =
            MapCodec.unit(new KitColorTintSource(1));
    public static final MapCodec<KitColorTintSource> CODEC_2 =
            MapCodec.unit(new KitColorTintSource(2));

    private final int index;

    private KitColorTintSource(int index) {
        this.index = index;
    }

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return ARGB.opaque(ItemKitCustom.getColor(stack, index));
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return index == 1 ? CODEC_1 : CODEC_2;
    }
}
