// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.loot;

import com.hbm.config.ConfigSchema;
import com.hbm.data.WorldData;
import com.hbm.platform.Services;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public record ConfigScaledNumberProvider(NumberProvider value) implements NumberProvider {

    public static final MapCodec<ConfigScaledNumberProvider> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            NumberProviders.CODEC
                                                    .fieldOf("value")
                                                    .forGetter(ConfigScaledNumberProvider::value))
                                    .apply(i, ConfigScaledNumberProvider::new));

    public static int scaledRolls(double base, double factor) {
        return (int) Math.max(1, Math.floor(base * factor));
    }

    private static double factor() {
        return WorldData.LOOT_AMOUNT_FACTOR.get();
    }

    @Override
    public MapCodec<ConfigScaledNumberProvider> codec() {
        return MAP_CODEC;
    }

    @Override
    public float getFloat(LootContext context) {
        return scaledRolls(value.getFloat(context), factor());
    }

    @Override
    public int getInt(LootContext context) {
        return scaledRolls(value.getFloat(context), factor());
    }
}
