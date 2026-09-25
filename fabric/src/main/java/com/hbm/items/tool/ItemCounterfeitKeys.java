// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemCounterfeitKeys extends Item {

    public ItemCounterfeitKeys(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof BlockEntityLockableBase lock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (lock.isLocked() && lock.cheesable) {
            ItemStack key = new ItemStack(ModItems.KEY_FAKE);
            ItemKeyPin.setPins(key, lock.getPins());

            player.setItemInHand(context.getHand(), key.copy());
            player.getInventory().placeItemBackInInventory(key.copy());
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }
        if (!lock.cheesable) {
            player.sendSystemMessage(
                    Component.translatable("desc.item.counterfeitKeys.thisLockIsToo")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(
                    Component.translatable("desc.item.counterfeitKeys.perhapsThereIsAnother")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.translatable("desc.item.counterfeitKeys.useOnALocked"));
    }
}
