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

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.registration.OverridePoint;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ModArmorItem extends Item {

    private final Suit suit;
    private final boolean powered;
    private final long maxPower;
    private final long consumption;

    public ModArmorItem(Properties properties, Suit suit) {
        super(properties);
        this.suit = suit;
        this.powered = false;
        this.maxPower = 0L;
        this.consumption = 0L;
    }

    protected ModArmorItem(Properties properties, Suit suit, long maxPower, long consumption) {
        super(properties);
        this.suit = suit;
        this.powered = true;
        this.maxPower = maxPower;
        this.consumption = consumption;
    }

    public Suit suit() {
        return suit;
    }

    public boolean hasCustomItemRenderer() {
        return suit.objMesh;
    }

    public boolean isPowered() {
        return powered;
    }

    public long maxPower() {
        return maxPower;
    }

    public long consumption() {
        return consumption;
    }

    public long maxPower(ItemStack stack) {
        return ArmorModHandler.modItem(stack, ArmorModHandler.BATTERY)
                        instanceof ModBatteryItem battery
                ? (long) (maxPower * battery.mod)
                : maxPower;
    }

    public long getCharge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), maxPower(stack));
    }

    public void setCharge(ItemStack stack, long charge) {
        stack.set(ModDataComponents.BATTERY_CHARGE.get(), Math.clamp(charge, 0L, maxPower(stack)));
    }

    public boolean isEnabled(ItemStack stack) {
        return !powered || getCharge(stack) > 0L;
    }

    public boolean drainsOnWear() {
        return powered;
    }

    public void drainForWear(ItemStack stack, int wear) {
        setCharge(stack, getCharge(stack) - wear * consumption);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return powered ? getCharge(stack) < maxPower(stack) : super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return powered
                ? Math.round(getCharge(stack) * 13F / maxPower(stack))
                : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return powered
                ? Mth.hsvToRgb((float) getCharge(stack) / maxPower(stack) / 3.0F, 1.0F, 1.0F)
                : super.getBarColor(stack);
    }

    public static final OverridePoint<BiConsumer<ItemStack, Consumer<Component>>> EXTRA_LINES =
            OverridePoint.of(RegistryUtil.id("armor_extra_lines"), (stack, adder) -> {});

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (powered) {
            adder.accept(
                    Component.translatable(
                                    "desc.item.modArmorItem.charge",
                                    NumberFormatter.formatCompact(getCharge(stack))
                                            + " / "
                                            + NumberFormatter.formatCompact(maxPower(stack)))
                            .withStyle(ChatFormatting.GOLD));
        }

        EXTRA_LINES.get().accept(stack, adder);
        fullSetBonus(stack, adder);
    }

    private void fullSetBonus(ItemStack stack, Consumer<Component> adder) {
        ArmorFullSetBonus bonus = ArmorFullSetBonus.get(suit);
        if (bonus == null) return;

        List<Component> lines = new ArrayList<>();

        if (!bonus.effects().isEmpty()) {
            MutableComponent joined = Component.empty();
            for (int i = 0; i < bonus.effects().size(); i++) {
                if (i > 0) joined.append(", ");
                joined.append(bonus.effects().get(i).effect().value().getDisplayName());
            }
            lines.add(joined.withStyle(ChatFormatting.AQUA));
        }

        if (bonus.geigerSound()) lines.add(bullet("armor.geigerSound", ChatFormatting.GOLD));
        if (bonus.geigerHUD()) lines.add(bullet("armor.geigerHUD", ChatFormatting.GOLD));
        if (bonus.vats()) lines.add(bullet("armor.vats", ChatFormatting.RED));
        if (bonus.thermal()) lines.add(bullet("armor.thermal", ChatFormatting.RED));
        if (bonus.hardLanding()) lines.add(bullet("armor.hardLanding", ChatFormatting.RED));
        if (bonus.stepSize() != 0) {
            lines.add(
                    Component.literal("  ")
                            .append(Component.translatable("armor.stepSize", bonus.stepSize()))
                            .withStyle(ChatFormatting.BLUE));
        }
        if (bonus.dashCount() > 0) {
            lines.add(
                    Component.literal("  ")
                            .append(Component.translatable("armor.dash", bonus.dashCount()))
                            .withStyle(ChatFormatting.AQUA));
        }
        if (bonus.rocketBoots()) lines.add(bullet("armor.rocketBoots", ChatFormatting.AQUA));
        if (bonus.fastFall()) lines.add(bullet("armor.fastFall", ChatFormatting.AQUA));
        if (bonus.sprintBoost()) lines.add(bullet("armor.sprintBoost", ChatFormatting.AQUA));
        if (bonus.moreAmmo()) lines.add(bullet("armor.moreAmmo", ChatFormatting.RED));
        if (stack.is(ModItems.BJ_PLATE_JETPACK.get())) {
            lines.add(plus("armor.electricJetpack", ChatFormatting.RED));
            lines.add(plus("armor.glider", ChatFormatting.GRAY));
        }

        if (lines.isEmpty()) return;
        adder.accept(Component.translatable("armor.fullSetBonus").withStyle(ChatFormatting.GOLD));
        lines.forEach(adder);
    }

    private static Component bullet(String key, ChatFormatting style) {
        return Component.literal("  ").append(Component.translatable(key)).withStyle(style);
    }

    private static Component plus(String key, ChatFormatting style) {
        return Component.literal("  + ").append(Component.translatable(key)).withStyle(style);
    }

    public enum Suit {
        NONE(false),
        STEEL(false),
        TITANIUM(false),
        ALLOY(false),
        COBALT(false),
        STARMETAL(false),
        ROBES(false),
        SECURITY(false),
        T51(true),
        TAURUN(true),
        ASBESTOS(false),
        HAZMAT(false),
        HAZMAT_RED(false),
        HAZMAT_GREY(false),
        HAZMAT_PAA(false),
        LIQUIDATOR(false),
        SCHRABIDIUM(false),
        CMB(false),
        PAA(false),
        DNT(false),
        EUPHEMIUM(false),
        BISMUTH(true),
        AJR(true),
        AJRO(true),
        RPA(true),
        NCRPA(true),
        BJ(true),
        ENVSUIT(true),
        HEV(true),
        DIGAMMA(true),
        DNS(true),
        TRENCHMASTER(true),
        DESH(true),
        DIESEL(true);

        private final boolean objMesh;

        Suit(boolean objMesh) {
            this.objMesh = objMesh;
        }

        public boolean objMesh() {
            return objMesh;
        }

        private static final EnumSet<Suit> AJR_MATERIAL = EnumSet.of(AJR, AJRO, RPA, NCRPA);

        public boolean sharesMaterial(Suit other) {
            return this == other || (AJR_MATERIAL.contains(this) && AJR_MATERIAL.contains(other));
        }
    }
}
