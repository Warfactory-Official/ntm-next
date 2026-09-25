// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemDemonCore extends Item implements IItemEntityUpdate {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (entity.level().isClientSide() || !entity.onGround()) return false;

        entity.setItem(new ItemStack(ModItems.DEMON_CORE_CLOSED));
        entity.level()
                .addFreshEntity(
                        new ItemEntity(
                                entity.level(),
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                new ItemStack(ModItems.SCREWDRIVER)));
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
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
