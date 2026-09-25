// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.interfaces.IItemEntityUpdate;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ItemHat extends ModArmorItem implements IItemEntityUpdate {

    public static final float DAMAGE_THRESHOLD = 2F;

    public ItemHat(Properties properties) {
        super(properties, Suit.NONE);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.item.armor.damageThreshold", (int) DAMAGE_THRESHOLD)
                        .withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
