// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemTemFlakes extends Item {

    private static final float HEAL = 2F;

    public final Tier tier;

    public ItemTemFlakes(Properties props, Tier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) entity.heal(HEAL);
        return rest;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines("item.hbm.tem_flakes.desc." + tier.ordinal())) {
            adder.accept(Component.literal(line));
        }
    }

    public enum Tier {
        CHEAP,
        NORMAL,
        EXPENSIVE
    }
}
