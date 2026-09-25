// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine.upgrade;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemMachineUpgrade extends Item {

    public final UpgradeType type;
    public final int tier;
    private final Component[] description;

    public ItemMachineUpgrade(Properties props, UpgradeType type, int tier) {
        super(props);
        this.type = type;
        this.tier = tier;
        this.description = null;
    }

    public ItemMachineUpgrade(Properties props, Component... description) {
        super(props);
        this.type = UpgradeType.SPECIAL;
        this.tier = 0;
        this.description = description;
    }

    public static boolean isUpgrade(ItemStack stack) {
        return stack.getItem() instanceof ItemMachineUpgrade;
    }

    public static int getLevel(ItemStack stack, UpgradeType type) {
        return stack.getItem() instanceof ItemMachineUpgrade upgrade && upgrade.type == type
                ? upgrade.tier
                : 0;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (description != null) for (Component line : description) tooltip.accept(line);
    }
}
