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

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

public class KitCustomItem extends KitNBTItem {

    public KitCustomItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(
            String name, @Nullable String lore, int color1, int color2, ItemStack... contents) {
        ItemStack stack = KitNBTItem.create(contents);

        setColor(stack, color1, 1);
        setColor(stack, color2, 2);

        if (lore != null) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore.split("\\$")) {
                lines.add(
                        Component.literal(line)
                                .withStyle(ChatFormatting.RESET, ChatFormatting.GRAY));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }

        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(name).withStyle(ChatFormatting.RESET));

        return stack;
    }

    public static void setColor(ItemStack stack, int color, int index) {
        stack.set(
                index == 1
                        ? ModDataComponents.KIT_COLOR_1.get()
                        : ModDataComponents.KIT_COLOR_2.get(),
                color);
    }

    public static int getColor(ItemStack stack, int index) {
        return stack.getOrDefault(
                index == 1
                        ? ModDataComponents.KIT_COLOR_1.get()
                        : ModDataComponents.KIT_COLOR_2.get(),
                0);
    }
}
