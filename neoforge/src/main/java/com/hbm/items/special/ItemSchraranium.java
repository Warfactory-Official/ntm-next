// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.config.BalanceConfig;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemSchraranium extends MaterialShapeItem {

    public ItemSchraranium(Properties properties, NTMMaterial material, MaterialShapes shape) {
        super(properties, material, shape);
    }

    public static boolean nikonium() {
        return BalanceConfig.enableLBSM && BalanceConfig.enableLBSMFullSchrab;
    }

    @Override
    public Component getName(ItemStack stack) {
        return nikonium()
                ? Component.translatable("item.hbm.ingot_schraranium.nikonium")
                : super.getName(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (nikonium())
            adder.accept(Component.translatable("item.hbm.ingot_schraranium.nikonium.desc"));
    }
}
