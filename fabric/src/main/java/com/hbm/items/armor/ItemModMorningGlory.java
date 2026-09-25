// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ItemModMorningGlory extends ItemArmorMod {
    public ItemModMorningGlory(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modMorningGlory.5ChanceToApply")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(" (5% for resistance, wither immunity)")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (entity.level() instanceof ServerLevel && entity.getRandom().nextInt(20) == 0) {
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.RESISTANCE, 5 * SharedConstants.TICKS_PER_SECOND, 4));
        }
        return amount;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.hasEffect(MobEffects.WITHER)) entity.removeEffect(MobEffects.WITHER);
    }
}
