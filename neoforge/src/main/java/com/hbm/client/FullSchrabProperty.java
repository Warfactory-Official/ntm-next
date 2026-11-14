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

import com.hbm.items.special.SchraraniumItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record FullSchrabProperty() implements SelectItemModelProperty<String> {

    public static final Codec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<FullSchrabProperty, String> TYPE =
            SelectItemModelProperty.Type.create(
                    MapCodec.unit(new FullSchrabProperty()), VALUE_CODEC);

    @Override
    public String get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext) {
        return String.valueOf(SchraraniumItem.nikonium());
    }

    @Override
    public SelectItemModelProperty.Type<FullSchrabProperty, String> type() {
        return TYPE;
    }

    @Override
    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}
