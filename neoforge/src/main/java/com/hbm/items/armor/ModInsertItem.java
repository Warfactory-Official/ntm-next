/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.armor;

import com.hbm.extprop.LivingHazards;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ModInsertItem extends ArmorModItem {

    private final float damageMod;
    private final float projectileMod;
    private final float explosionMod;
    private final float speed;

    public ModInsertItem(
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

    public static final OverridePoint<BiConsumer<ItemStack, Consumer<Component>>> EXTRA_LINES =
            OverridePoint.of(RegistryUtil.id("armor_insert_extra_lines"), (stack, adder) -> {});

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        EXTRA_LINES.get().accept(stack, adder);
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
    public boolean ticksServer() {
        return this == ModItems.INSERT_POLONIUM.get();
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide() && this == ModItems.INSERT_POLONIUM.get())
            LivingHazards.addRadiation(entity, 100D);
    }

    @Override
    public boolean hasAttributes() {
        return true;
    }

    @Override
    public void collectModifiers(
            EquipmentSlot slot,
            Identifier id,
            BiConsumer<Holder<Attribute>, AttributeModifier> adder) {
        if (speed == 1F) return;
        adder.accept(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        id, -1F + speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
