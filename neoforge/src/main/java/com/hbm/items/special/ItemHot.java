// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemHot extends Item {

    private final int maxHeat;

    public ItemHot(Properties properties, int maxHeat) {
        super(properties);
        this.maxHeat = maxHeat;
    }

    public final int maxHeat() {
        return maxHeat;
    }

    public int maxHeat(ItemStack stack) {
        return maxHeat;
    }

    public static ItemStack heatUp(ItemStack stack, long gameTime) {
        return heatUp(stack, gameTime, 1.0D);
    }

    public static ItemStack heatUp(ItemStack stack, long gameTime, double multiplier) {
        if (stack.getItem() instanceof ItemHot hot) {
            stack.remove(ModDataComponents.HOT_ITEM_HEAT.get());
            stack.set(
                    ModDataComponents.HOT_UNTIL.get(),
                    gameTime + (long) (multiplier * hot.maxHeat(stack)));
        }
        return stack;
    }

    public static double getHeat(ItemStack stack, long gameTime) {
        if (!(stack.getItem() instanceof ItemHot hot)) return 0.0D;
        Long until = stack.get(ModDataComponents.HOT_UNTIL.get());
        if (until != null) return Math.max(0L, until - gameTime) / (double) hot.maxHeat(stack);
        return stack.getOrDefault(ModDataComponents.HOT_ITEM_HEAT.get(), 0)
                / (double) hot.maxHeat(stack);
    }

    public static int getMaxHeat(ItemStack stack) {
        return stack.getItem() instanceof ItemHot hot ? hot.maxHeat(stack) : 0;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        Integer duration = stack.get(ModDataComponents.HOT_ITEM_HEAT.get());
        if (duration != null) {
            stack.remove(ModDataComponents.HOT_ITEM_HEAT.get());

            if (duration > 0)
                stack.set(ModDataComponents.HOT_UNTIL.get(), level.getGameTime() + duration - 1);
            else stack.remove(ModDataComponents.HOT_UNTIL.get());
            return;
        }
        Long until = stack.get(ModDataComponents.HOT_UNTIL.get());
        if (until != null && level.getGameTime() >= until)
            stack.remove(ModDataComponents.HOT_UNTIL.get());
    }
}
