// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemModCharm extends ItemArmorMod {
    public ItemModCharm(Item.Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modCharm.youFeelBlessed")
                        .withStyle(ChatFormatting.AQUA));
        if (this == ModItems.PROTECTION_CHARM.get()) {
            adder.accept(
                    Component.translatable("desc.item.modCharm.divertsMeteorsAwayFrom")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.meteorsNoLongerDestroy")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.halvesBroadcasterDamage")
                            .withStyle(ChatFormatting.AQUA));
        }
        if (this == ModItems.METEOR_CHARM.get()) {
            adder.accept(
                    Component.translatable("desc.item.modCharm.disablesMeteoriteSpawning")
                            .withStyle(ChatFormatting.AQUA));
            adder.accept(
                    Component.translatable("desc.item.modCharm.negatesBroadcasterDamage")
                            .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    protected boolean hasTooltipSpacer() {
        return false;
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (!source.is(ModDamageTypes.BROADCAST)) return amount;
        if (this == ModItems.PROTECTION_CHARM.get()) return amount * 0.5F;
        if (this == ModItems.METEOR_CHARM.get()) return 0F;
        return amount;
    }
}
