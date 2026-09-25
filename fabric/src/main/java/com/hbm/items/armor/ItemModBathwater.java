// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ItemModBathwater extends ItemArmorMod {

    private static final int DURATION = 200;

    private final Grade grade;

    public ItemModBathwater(Properties properties, Grade grade) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.grade = grade;
    }

    private ChatFormatting color() {
        boolean early = System.currentTimeMillis() % 1000L < 500L;
        return grade == Grade.MK2
                ? (early ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                : (early ? ChatFormatting.BLUE : ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(Component.translatable("desc.item.armorMod.bathwater").withStyle(color()));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.bathwater.installed", stack.getHoverName())
                        .withStyle(color()));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (entity.level().isClientSide()) return amount;
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(grade.effect, DURATION, grade.amplifier));
        return amount;
    }

    public enum Grade {
        STANDARD(MobEffects.POISON, 2),
        MK2(MobEffects.WITHER, 4);

        final Holder<MobEffect> effect;
        final int amplifier;

        Grade(Holder<MobEffect> effect, int amplifier) {
            this.effect = effect;
            this.amplifier = amplifier;
        }
    }
}
