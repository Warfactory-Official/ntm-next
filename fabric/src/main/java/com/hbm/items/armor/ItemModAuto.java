// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ItemModAuto extends ItemArmorMod {

    private static final float TRIGGER = 5F;
    private static final int STABILITY_DURATION = 60 * SharedConstants.TICKS_PER_SECOND;

    public ItemModAuto(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.auto").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.auto.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || HbmLivingProps.getDigamma(entity) < TRIGGER) return;

        ArmorModHandler.removeMod(armor, ArmorModHandler.EXTRA);
        entity.level()
                .playSound(
                        null,
                        entity.blockPosition(),
                        ModSounds.ITEM_SYRINGE.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
        HbmLivingProps.setDigamma(entity, HbmLivingProps.getDigamma(entity) - TRIGGER);
        entity.addEffect(new MobEffectInstance(HbmPotion.stability(), STABILITY_DURATION, 0));
        entity.heal(20F);
    }
}
