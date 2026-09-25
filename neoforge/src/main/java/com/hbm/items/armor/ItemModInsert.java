// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemModInsert extends ItemArmorMod {

    private final float damageMod;
    private final float projectileMod;
    private final float explosionMod;
    private final float speed;

    public ItemModInsert(
            Properties properties,
            int durability,
            float damageMod,
            float projectileMod,
            float explosionMod,
            float speed) {
        super(properties.durability(durability), ArmorModHandler.KEVLAR, false, true, false, false);
        this.damageMod = damageMod;
        this.projectileMod = projectileMod;
        this.explosionMod = explosionMod;
        this.speed = speed;
    }

    private static String signedPercent(float mod) {
        return (mod < 1F ? "-" : "+") + Math.abs(Math.round((1F - mod) * 100F));
    }

    private static String percent(float mod) {
        return String.valueOf(Math.round((1F - mod) * 100F));
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        if (damageMod != 1F)
            adder.accept(
                    Component.translatable(
                                    "desc.item.armorMod.insert.damage", signedPercent(damageMod))
                            .withStyle(ChatFormatting.RED));
        if (projectileMod != 1F)
            adder.accept(
                    Component.translatable(
                                    "desc.item.armorMod.insert.projectile", percent(projectileMod))
                            .withStyle(ChatFormatting.YELLOW));
        if (explosionMod != 1F)
            adder.accept(
                    Component.translatable(
                                    "desc.item.armorMod.insert.explosion", percent(explosionMod))
                            .withStyle(ChatFormatting.YELLOW));
        if (speed != 1F)
            adder.accept(
                    Component.translatable("desc.item.armorMod.insert.speed", percent(speed))
                            .withStyle(ChatFormatting.BLUE));
        if (this == ModItems.INSERT_POLONIUM.get())
            adder.accept(
                    Component.translatable("desc.item.armorMod.insert.rads")
                            .withStyle(ChatFormatting.DARK_RED));
        adder.accept(
                Component.translatable(
                        "desc.item.armorMod.insert.hp",
                        stack.getMaxDamage() - stack.getDamageValue(),
                        stack.getMaxDamage()));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        MutableComponent body = Component.empty();
        if (damageMod != 1F)
            append(
                    body,
                    Component.translatable(
                            "desc.item.armorMod.insert.short.damage", signedPercent(damageMod)));
        if (projectileMod != 1F)
            append(
                    body,
                    Component.translatable(
                            "desc.item.armorMod.insert.short.projectile", percent(projectileMod)));
        if (explosionMod != 1F)
            append(
                    body,
                    Component.translatable(
                            "desc.item.armorMod.insert.short.explosion", percent(explosionMod)));

        if (speed != 1F)
            append(
                    body,
                    Component.translatable(
                            "desc.item.armorMod.insert.short.speed", percent(speed)));
        if (this == ModItems.INSERT_POLONIUM.get())
            append(body, Component.translatable("desc.item.armorMod.insert.rads"));
        append(
                body,
                Component.translatable(
                        "desc.item.armorMod.insert.short.hp",
                        stack.getMaxDamage() - stack.getDamageValue()));

        tooltip.add(
                Component.translatable("desc.item.armorMod.installed", stack.getHoverName(), body)
                        .withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void append(MutableComponent body, Component part) {
        if (!body.getSiblings().isEmpty()) body.append(Component.literal(" / "));
        body.append(part);
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        amount *= damageMod;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= projectileMod;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= explosionMod;

        ItemStack insert = ArmorModHandler.pryMod(armor, ArmorModHandler.KEVLAR);
        if (insert.isEmpty()) return amount;

        insert.setDamageValue(insert.getDamageValue() + 1);

        if (this == ModItems.INSERT_ERA.get() && entity.level() instanceof ServerLevel level) {
            level.explode(
                    entity,
                    null,
                    null,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D,
                    entity.getZ(),
                    0.05F,
                    false,
                    Level.ExplosionInteraction.NONE);
        }

        if (insert.getDamageValue() >= insert.getMaxDamage())
            ArmorModHandler.removeMod(armor, ArmorModHandler.KEVLAR);
        else ArmorModHandler.applyMod(armor, insert);
        return amount;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide() && this == ModItems.INSERT_POLONIUM.get())
            HbmLivingProps.incrementRadiation(entity, 100D);
    }

    @Override
    public void collectModifiers(
            ItemStack armor,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        if (speed == 1F) return;
        adder.accept(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        id, -1F + speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
