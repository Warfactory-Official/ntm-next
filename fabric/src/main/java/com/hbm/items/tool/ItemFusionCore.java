// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemFusionCore extends Item {

    private final long charge;

    public ItemFusionCore(Properties properties, long charge) {
        super(properties);
        this.charge = charge;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate)
                || !plate.isPowered()
                || !ArmorSuitEffects.hasFullSet(player, plate.suit(), false)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.getItem() instanceof ModArmorItem armor) {
                    armor.setCharge(worn, armor.getCharge(worn) + charge);
                } else if (worn.getItem() instanceof IBatteryItem battery) {
                    battery.chargeBattery(worn, charge);
                }
            }
            stack.shrink(1);
        }
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_BATTERY.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> lines,
            TooltipFlag flag) {

        lines.accept(
                Component.translatable(
                                "desc.item.fusionCore.charges", BobMathUtil.getShortNumber(charge))
                        .withStyle(ChatFormatting.YELLOW));
        lines.accept(Component.translatable("desc.item.fusionCore.requiresSet"));
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
