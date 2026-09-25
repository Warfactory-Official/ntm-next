// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GrassColor;
import org.jspecify.annotations.Nullable;

public final class PlantGrassTintSource implements ItemTintSource {

    public static final PlantGrassTintSource INSTANCE = new PlantGrassTintSource();
    public static final MapCodec<PlantGrassTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private PlantGrassTintSource() {}

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return GrassColor.getDefaultColor();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
