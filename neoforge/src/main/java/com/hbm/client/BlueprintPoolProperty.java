// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record BlueprintPoolProperty() implements SelectItemModelProperty<String> {

    public static final String BUCKET_DISCOVER = "discover";
    public static final String BUCKET_SECRET = "secret";
    public static final String BUCKET_528 = "528";
    public static final String BUCKET_DEFAULT = "default";

    public static final Codec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<BlueprintPoolProperty, String> TYPE =
            SelectItemModelProperty.Type.create(
                    MapCodec.unit(new BlueprintPoolProperty()), VALUE_CODEC);

    @Override
    public @Nullable String get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext) {
        String pool = ItemBlueprints.grabPool(stack);
        if (pool == null) return BUCKET_DEFAULT;
        if (pool.startsWith(GenericRecipes.POOL_PREFIX_DISCOVER)) return BUCKET_DISCOVER;
        if (pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return BUCKET_SECRET;
        if (pool.startsWith(GenericRecipes.POOL_PREFIX_528)) return BUCKET_528;
        return BUCKET_DEFAULT;
    }

    @Override
    public SelectItemModelProperty.Type<BlueprintPoolProperty, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
