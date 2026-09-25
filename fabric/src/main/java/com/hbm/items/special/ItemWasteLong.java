// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemWasteLong extends ItemNuclearWaste {

    public final WasteClass type;

    public ItemWasteLong(Properties props, WasteClass type) {
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
        URANIUM235("Uranium-235", 0, 0),
        URANIUM233("Uranium-233", 0, 50),
        NEPTUNIUM("Neptunium-237", 0, 100),
        THORIUM("Thorium-232", 0, 0),
        SCHRABIDIUM("Schrabidium-326", 0, 250);

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
