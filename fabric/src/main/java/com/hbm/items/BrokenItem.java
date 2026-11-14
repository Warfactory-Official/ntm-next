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

package com.hbm.items;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class BrokenItem extends Item {

    public BrokenItem(Properties properties) {
        super(properties);
    }

    public static ItemStack make(ItemLike item) {
        return make(item, 1);
    }

    public static ItemStack make(ItemStack stack) {
        return make(stack.getItem(), stack.getCount());
    }

    public static ItemStack make(ItemLike item, int count) {
        ItemStack stack = new ItemStack(ModItems.BROKEN_ITEM.get(), count);
        stack.set(
                ModDataComponents.BROKEN_ITEM_SOURCE.get(),
                BuiltInRegistries.ITEM.getKey(item.asItem()));
        return stack;
    }

    public static @Nullable Item sourceOf(ItemStack stack) {
        Identifier id = stack.get(ModDataComponents.BROKEN_ITEM_SOURCE.get());
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    @Override
    public Component getName(ItemStack stack) {
        Item source = sourceOf(stack);
        if (source == null) return super.getName(stack);
        return Component.translatable(
                getDescriptionId() + ".prefix", new ItemStack(source).getHoverName());
    }
}
