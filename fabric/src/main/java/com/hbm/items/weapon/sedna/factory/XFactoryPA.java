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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorPAWeapons;
import com.hbm.items.armor.PowerArmorMeleeWeapon;
import com.hbm.items.armor.PowerArmorWeapon;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class XFactoryPA {

    public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA =
            (stack, ctx) -> {
                PowerArmorMeleeWeapon component = ArmorPAWeapons.melee(ctx.getPlayer());
                if (component != null) component.cuePass(stack, ctx);
            };
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MELEE_ANIMS =
            (stack, type) -> {
                PowerArmorMeleeWeapon component = ArmorPAWeapons.localMelee();
                if (component != null) return component.animation(stack, type);
                return null;
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY =
            (stack, ctx) -> {
                PowerArmorMeleeWeapon component = ArmorPAWeapons.melee(ctx.getPlayer());
                if (component != null) component.primary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SENONDARY =
            (stack, ctx) -> {
                PowerArmorMeleeWeapon component = ArmorPAWeapons.melee(ctx.getPlayer());
                if (component != null) component.secondary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY =
            (stack, ctx) -> {
                PowerArmorWeapon component = ArmorPAWeapons.ranged(ctx.getPlayer());
                if (component != null) component.primary(stack, ctx);
            };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SENONDARY =
            (stack, ctx) -> {
                PowerArmorWeapon component = ArmorPAWeapons.ranged(ctx.getPlayer());
                if (component != null) component.secondary(stack, ctx);
            };

    public static void init() {

        ModItems.GUN_PA_MELEE =
                Registrar.registerItem(
                        "gun_pa_melee",
                        props ->
                                new ItemGunPA(
                                        WeaponQuality.UTILITY,
                                        props,
                                        GunConfig.builder()
                                                .draw(10)
                                                .crosshair(Crosshair.NONE)
                                                .rec(Receiver.builder())
                                                .pp(LAMBDA_CLICK_MELEE_PRIMARY)
                                                .ps(LAMBDA_CLICK_MELEE_SENONDARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                                                .anim(LAMBDA_MELEE_ANIMS)
                                                .orchestra(ORCHESTRA)),
                        Item.Properties::new);

        ModItems.GUN_PA_RANGED =
                Reg.item(
                        "gun_pa_ranged",
                        props ->
                                new ItemGunPA(
                                        WeaponQuality.UTILITY,
                                        props,
                                        GunConfig.builder()
                                                .draw(0)
                                                .crosshair(Crosshair.CROSS)
                                                .rec(Receiver.builder())
                                                .pp(LAMBDA_CLICK_RANGED_PRIMARY)
                                                .ps(LAMBDA_CLICK_RANGED_SENONDARY)
                                                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)),
                        Item.Properties::new);
    }

    public static void doSwing(
            ItemStack stack, LambdaContext ctx, GunAnimation anim, int cooldown) {

        Player player = ctx.getPlayer();
        int index = ctx.configIndex();
        GunState state = GunBaseNTItem.getState(stack, index);

        if (state == GunState.IDLE) {
            GunBaseNTItem.playAnimation(player, stack, anim, ctx.configIndex());
            GunBaseNTItem.setState(stack, index, GunState.COOLDOWN);
            GunBaseNTItem.setTimer(stack, index, cooldown);
        }
    }

    public static class ItemGunPA extends GunBaseNTItem {

        public ItemGunPA(WeaponQuality quality, Properties properties, GunConfig.Builder... cfg) {
            super(quality, properties, cfg);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> list,
                TooltipFlag ext) {}
    }
}
