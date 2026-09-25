// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemScraps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record ScrapMaterialProperty() implements SelectItemModelProperty<String> {

    public static final String LIQUID = "liquid";
    public static final String ADDITIVE = "additive";

    public static final Codec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<ScrapMaterialProperty, String> TYPE =
            SelectItemModelProperty.Type.create(
                    MapCodec.unit(new ScrapMaterialProperty()), VALUE_CODEC);

    @Override
    public @Nullable String get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext) {
        MaterialStack contents = ItemScraps.getMats(stack);
        if (contents == null) return null;

        if (ItemScraps.isLiquid(stack)) {
            if (contents.material.smeltable == SmeltingBehavior.SMELTABLE) return LIQUID;
            if (contents.material.smeltable == SmeltingBehavior.ADDITIVE) return ADDITIVE;
        }

        return contents.material.tagPath;
    }

    @Override
    public SelectItemModelProperty.Type<ScrapMaterialProperty, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
