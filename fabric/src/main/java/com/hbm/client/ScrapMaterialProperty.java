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

package com.hbm.client;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.SmeltingBehavior;
import com.hbm.items.machine.ScrapsItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
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
        Material material = ScrapsItem.material(stack);
        if (material == null) return null;

        if (ScrapsItem.isLiquid(stack)) {
            if (material.behaviour() == SmeltingBehavior.SMELTABLE) return LIQUID;
            if (material.behaviour() == SmeltingBehavior.ADDITIVE) return ADDITIVE;
            return liquidKey(material);
        }

        return material.name();
    }

    private static String[] liquidKeys = new String[0];

    private static String liquidKey(Material material) {
        String[] keys = liquidKeys;
        int id = material.id();
        if (id >= keys.length) liquidKeys = keys = Arrays.copyOf(keys, id + 1);
        String key = keys[id];
        if (key == null) keys[id] = key = material.name() + "_liquid";
        return key;
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
