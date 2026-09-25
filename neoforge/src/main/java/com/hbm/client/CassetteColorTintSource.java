// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.machine.ItemCassette;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CassetteColorTintSource implements ItemTintSource {

    public static final CassetteColorTintSource INSTANCE = new CassetteColorTintSource();
    public static final MapCodec<CassetteColorTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private CassetteColorTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        int colour = ItemCassette.typeOf(stack).getColor();
        return ARGB.opaque(colour < 0 ? 16777215 : colour);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
