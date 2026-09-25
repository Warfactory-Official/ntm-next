// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.NuclearTech;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public final class TooltipStyle {

    private TooltipStyle() {}

    public static boolean isNtm(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(NuclearTech.MOD_ID);
    }

    public static Component defaultColor(Component line, ChatFormatting color) {
        return line.getStyle().getColor() != null
                ? line
                : line.copy().withStyle(style -> style.withColor(color));
    }

    public static Consumer<Component> defaultGray(Consumer<Component> adder) {
        return line -> adder.accept(defaultColor(line, ChatFormatting.GRAY));
    }

    public static Component holdLshift() {
        return Component.translatable(
                "desc.tooltip.hold",
                Component.literal("LSHIFT")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
