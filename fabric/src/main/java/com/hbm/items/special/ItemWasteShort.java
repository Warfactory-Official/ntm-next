// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemWasteShort extends Item {

    public final WasteClass type;

    public ItemWasteShort(Properties props, WasteClass type) {
        super(props);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.literal(type.name).withStyle(ChatFormatting.ITALIC));
    }

    public enum WasteClass {
        URANIUM235("Uranium-235", 0, 100),
        URANIUM233("Uranium-233", 50, 100),
        NEPTUNIUM("Neptunium-237", 150, 500),
        PLUTONIUM239("Plutonium-239", 250, 1000),
        PLUTONIUM240("Plutonium-240", 350, 1000),
        PLUTONIUM241("Plutonium-241", 500, 1000),
        AMERICIUM242("Americium-242", 750, 1000),
        SCHRABIDIUM("Schrabidium-326", 1000, 1000);

        public static final WasteClass[] VALUES = values();

        public final String name;
        public final int liquid;
        public final int gas;

        WasteClass(String name, int liquid, int gas) {
            this.name = name;
            this.liquid = liquid;
            this.gas = gas;
        }
    }
}
