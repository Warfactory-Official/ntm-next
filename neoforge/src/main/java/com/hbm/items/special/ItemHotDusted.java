// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemHotDusted extends ItemHot {

    public static final int FOLDS = 10;
    private static final int HEAT_PER_FOLD = 10;

    public ItemHotDusted(Properties properties, int maxHeat) {
        super(properties, maxHeat);
    }

    public static int folds(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FORGE_COUNT.get(), 0);
    }

    public int maxHeat(int folds) {
        return maxHeat() - folds * HEAT_PER_FOLD;
    }

    @Override
    public int maxHeat(ItemStack stack) {
        return maxHeat(folds(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.hot_dusted.forged", folds(stack)));
    }
}
