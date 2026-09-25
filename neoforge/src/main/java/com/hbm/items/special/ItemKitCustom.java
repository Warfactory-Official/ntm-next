// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

public class ItemKitCustom extends ItemKitNBT {

    public ItemKitCustom(Properties properties) {
        super(properties);
    }

    public static ItemStack create(
            String name, @Nullable String lore, int color1, int color2, ItemStack... contents) {
        ItemStack stack = ItemKitNBT.create(contents);

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
