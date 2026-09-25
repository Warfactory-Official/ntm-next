// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemModCard extends ItemArmorMod {

    private final boolean queen;

    public ItemModCard(Properties properties, boolean queen) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, true, false, false);
        this.queen = queen;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable(
                                queen
                                        ? "desc.item.armorMod.cardQos.1"
                                        : "desc.item.armorMod.cardAos.1")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable(
                                queen
                                        ? "desc.item.armorMod.cardQos.2"
                                        : "desc.item.armorMod.cardAos.2")
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(stack.getHoverName().copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (queen && entity.getRandom().nextInt(3) == 0 && entity instanceof Player player) {
            HbmPlayerProps.plink(
                    player,
                    SoundEvents.ITEM_BREAK.value(),
                    0.5F,
                    1.0F + entity.getRandom().nextFloat() * 0.5F);
            return 0;
        }
        return amount;
    }
}
