// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ItemModWD40 extends ItemArmorMod {

    private static final double HEALTH = 4D;

    private static final int SPRAY_COLOR = ARGB.colorFromFloat(1F, 0.01F, 0.5F, 0.8F);

    public ItemModWD40(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    private static ChatFormatting color() {
        return System.currentTimeMillis() % 1000L < 500L
                ? ChatFormatting.BLUE
                : ChatFormatting.YELLOW;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(Component.translatable("desc.item.armorMod.wd40").withStyle(color()));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.wd40.installed", stack.getHoverName())
                        .withStyle(color()));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (!entity.level().isClientSide()
                && armor.getDamageValue() > 0
                && entity.getRandom().nextInt(5) != 0)
            armor.setDamageValue(armor.getDamageValue() - 1);
        return amount;
    }

    @Override
    public void modUpdateClient(LivingEntity entity, ItemStack armor) {
        if (entity.hurtTime <= 0) return;
        var random = entity.getRandom();
        entity.level()
                .addParticle(
                        new DustParticleOptions(SPRAY_COLOR, 1F),
                        entity.getX() + (random.nextDouble() - 0.5D) * entity.getBbWidth() * 2D,
                        entity.getY() + random.nextDouble() * entity.getBbHeight(),
                        entity.getZ() + (random.nextDouble() - 0.5D) * entity.getBbWidth() * 2D,
                        0D,
                        0D,
                        0D);
    }

    @Override
    public void collectModifiers(
            ItemStack armor,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        adder.accept(
                Attributes.MAX_HEALTH,
                new AttributeModifier(id, HEALTH, AttributeModifier.Operation.ADD_VALUE));
    }
}
