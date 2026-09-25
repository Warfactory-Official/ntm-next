// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ItemModServos extends ItemArmorMod {

    public enum Tier {
        STANDARD(0, 1, 0.5D, 0.25D, "standard"),
        DESH(2, 2, 1.5D, 0.5D, "desh");

        final int haste;
        final int jump;
        final double damage;
        final double speed;
        final String key;

        Tier(int haste, int jump, double damage, double speed, String key) {
            this.haste = haste;
            this.jump = jump;
            this.damage = damage;
            this.speed = speed;
            this.key = key;
        }
    }

    private static final int EFFECT_DURATION = 60;

    private final Tier tier;

    public ItemModServos(Properties properties, Tier tier) {
        super(properties, ArmorModHandler.SERVOS, false, true, true, false);
        this.tier = tier;
    }

    private static EquipmentSlot slotOf(ItemStack armor) {
        var equippable = armor.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable == null ? EquipmentSlot.MAINHAND : equippable.slot();
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.servos." + tier.key + ".chest")
                        .withStyle(ChatFormatting.DARK_PURPLE));
        adder.accept(
                Component.translatable("desc.item.armorMod.servos." + tier.key + ".legs")
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        String half =
                switch (slotOf(armor)) {
                    case CHEST -> "chest";
                    case LEGS -> "legs";
                    default -> null;
                };
        if (half == null) return;
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.servos." + tier.key + "." + half + ".installed",
                                stack.getHoverName())
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        switch (slotOf(armor)) {
            case CHEST ->
                    entity.addEffect(
                            new MobEffectInstance(MobEffects.HASTE, EFFECT_DURATION, tier.haste));
            case LEGS ->
                    entity.addEffect(
                            new MobEffectInstance(
                                    MobEffects.JUMP_BOOST, EFFECT_DURATION, tier.jump));
            default -> {}
        }
    }

    @Override
    public void collectModifiers(
            ItemStack armor,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        switch (slotOf(armor)) {
            case CHEST ->
                    adder.accept(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    id,
                                    tier.damage,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            case LEGS ->
                    adder.accept(
                            Attributes.MOVEMENT_SPEED,
                            new AttributeModifier(
                                    id,
                                    tier.speed,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            default -> {}
        }
    }
}
