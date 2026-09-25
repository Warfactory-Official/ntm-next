// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemSwordMeteorite extends ItemSwordAbility {

    public ItemSwordMeteorite(Properties properties) {
        super(properties, AvailableAbilities.EMPTY);
    }

    public static void upgrade(
            NonNullList<ItemStack> inventory,
            int slot,
            RegistryHandle<? extends Item> from,
            RegistryHandle<? extends Item> to) {
        if (inventory.get(slot).is(from.get())) inventory.set(slot, new ItemStack(to.get()));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        for (String line : I18nUtil.loreLines(getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.ITALIC));
        }
    }
}
