// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ScrapsColorTintSource implements ItemTintSource {

    public static final ScrapsColorTintSource INSTANCE = new ScrapsColorTintSource();
    public static final MapCodec<ScrapsColorTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private ScrapsColorTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        MaterialStack contents = ItemScraps.getMats(stack);
        if (contents == null) return CommonColors.WHITE;
        return ARGB.opaque(contents.material.moltenColor);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
