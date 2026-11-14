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

import com.hbm.items.AmmoTypes;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem.WeaponQuality;
import com.hbm.items.weapon.sedna.factory.GunAnimationActions;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.items.weapon.sedna.factory.XFactoryPile;
import com.hbm.items.weapon.sedna.factory.XFactoryWarhead;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GunFactory {

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_DEBUG_DECIDER =
            (stack, ctx) -> {
                int index = ctx.configIndex();
                GunState lastState = GunBaseNTItem.getState(stack, index);
                GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
                GunStateDecider.deciderStandardClearJam(stack, lastState, index);
                GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
                GunStateDecider.deciderStandardReload(stack, ctx, lastState, 1, index);
                GunStateDecider.deciderAutoRefire(
                        stack,
                        ctx,
                        lastState,
                        0,
                        index,
                        () ->
                                GunBaseNTItem.getPrimary(stack, index)
                                        && GunBaseNTItem.getMode(stack, index) == 0);
                GunStateDecider.deciderAutoRefire(
                        stack,
                        ctx,
                        lastState,
                        1,
                        index,
                        () ->
                                GunBaseNTItem.getSecondary(stack, index)
                                        && GunBaseNTItem.getMode(stack, index) == 0);
            };
    public static BulletConfig ammo_debug;
    public static BulletConfig ammo_debug_shot;

    public static void init() {

        ModItems.AMMO_DEBUG = Registrar.registerItem("ammo_debug", Item::new, Item.Properties::new);
        ModItems.AMMO_STANDARD =
                ModItems.dotted(
                        "ammo_standard",
                        EnumAmmo.class,
                        (props, type) -> new Item(props),
                        Item.Properties::new);
        ModItems.AMMO_SHELL =
                Reg.family(
                        AmmoTypes.Ammo240Shell.class,
                        type ->
                                type == AmmoTypes.Ammo240Shell.STOCK
                                        ? "ammo_shell"
                                        : "ammo_shell_" + type.name().toLowerCase(Locale.ROOT),
                        (props, type) -> new Item(props),
                        Item.Properties::new);
        ModItems.AMMO_SECRET =
                ModItems.dotted(
                        "ammo_secret",
                        EnumAmmoSecret.class,
                        (props, type) -> new Item(props),
                        Item.Properties::new);

        ModItems.WEAPON_MOD_TEST =
                ModItems.dotted(
                        "weapon_mod_test",
                        EnumModTest.class,
                        (props, type) -> new Item(props),
                        () -> new Item.Properties().stacksTo(1));
        ModItems.WEAPON_MOD_GENERIC =
                ModItems.dotted(
                        "weapon_mod_generic",
                        EnumModGeneric.class,
                        (props, type) -> new Item(props),
                        () -> new Item.Properties().stacksTo(1));
        ModItems.WEAPON_MOD_SPECIAL =
                ModItems.dotted(
                        "weapon_mod_special",
                        EnumModSpecial.class,
                        (props, type) -> new Item(props),
                        () -> new Item.Properties().stacksTo(1));
        ModItems.WEAPON_MOD_CALIBER =
                ModItems.dotted(
                        "weapon_mod_caliber",
                        EnumModCaliber.class,
                        (props, type) -> new Item(props),
                        () -> new Item.Properties().stacksTo(1));

        Casing.Template casing44 =
                Casing.template(CasingShape.STRAIGHT)
                        .scale(1.5F, 1.0F, 1.5F)
                        .tints(Casing.MAGNUM_44);
        ammo_debug =
                new BulletConfig()
                        .setItem(ModItems.AMMO_DEBUG::get)
                        .setSpread(0.01F)
                        .setRicochetAngle(45)
                        .setCasing(casing44.copy().register("DEBUG0"));
        ammo_debug_shot =
                new BulletConfig()
                        .setItem(ModItems.AMMO_DEBUG::get)
                        .setSpread(0.05F)
                        .setProjectiles(6)
                        .setRicochetAngle(45)
                        .setCasing(casing44.copy().register("DEBUG1"));

        ModItems.GUN_DEBUG =
                Registrar.registerItem(
                        "gun_debug",
                        props ->
                                new GunBaseNTItem(
                                        WeaponQuality.DEBUG,
                                        props,
                                        GunConfig.builder()
                                                .dura(600F)
                                                .draw(15)
                                                .inspect(23)
                                                .crosshair(Crosshair.L_CLASSIC)
                                                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                                                .orchestra(GunAnimationActions.DEBUG)
                                                .rec(
                                                        Receiver.builder()
                                                                .dmg(10F)
                                                                .delay(14)
                                                                .reload(46)
                                                                .jam(23)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_HEAVY_REVOLVER_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(
                                                                                        0, 12)
                                                                                .addConfigs(
                                                                                        ammo_debug))
                                                                .offset(0.75, -0.0625, -0.3125D)
                                                                .canFire(
                                                                        Lego
                                                                                .LAMBDA_STANDARD_CAN_FIRE)
                                                                .fire(Lego.LAMBDA_STANDARD_FIRE),
                                                        Receiver.builder()
                                                                .dmg(5F)
                                                                .delay(14)
                                                                .reload(46)
                                                                .jam(23)
                                                                .sound(
                                                                        () ->
                                                                                ModSounds
                                                                                        .GUN_HEAVY_REVOLVER_FIRE
                                                                                        .get(),
                                                                        1.0F,
                                                                        1.0F)
                                                                .mag(
                                                                        new MagazineFullReload(
                                                                                        1, 12)
                                                                                .addConfigs(
                                                                                        ammo_debug_shot))
                                                                .offset(0.75, -0.0625, -0.3125D)
                                                                .canFire(
                                                                        Lego.LAMBDA_SECOND_CAN_FIRE)
                                                                .fire(Lego.LAMBDA_SECOND_FIRE))
                                                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                                                .ps(
                                                        (stack, ctx) ->
                                                                Lego.clickReceiver(stack, ctx, 1))
                                                .pr(Lego.LAMBDA_STANDARD_RELOAD)
                                                .pt(Lego.LAMBDA_TOGGLE_AIM)
                                                .decider(LAMBDA_DEBUG_DECIDER)
                                                .anim(Lego.LAMBDA_DEBUG_ANIMS)),
                        Item.Properties::new);

        XFactoryBlackPowder.init();
        XFactory357.init();
        XFactory44.init();
        XFactory9mm.init();
        XFactory12ga.init();
        XFactory40mm.init();
        XFactory762mm.init();
        XFactory22lr.init();
        XFactoryFlamer.init();
        XFactoryRocket.init();
        XFactory556mm.init();
        XFactory50.init();
        XFactoryEnergy.init();
        XFactoryAccelerator.init();
        XFactoryCatapult.init();
        XFactory75Bolt.init();
        XFactoryFolly.init();
        XFactoryTurret.init();
        XFactoryWarhead.init();
        XFactoryPile.init();
        XFactoryNPC.init();
        XFactory10ga.init();
        XFactory35800.init();
        XFactory45.init();
        XFactoryTool.init();
        XFactoryDrill.init();
        XFactoryPA.init();
    }

    public enum EnumAmmo {
        STONE,
        STONE_AP,
        STONE_IRON,
        STONE_SHOT,
        M357_BP,
        M357_SP,
        M357_FMJ,
        M357_JHP,
        M357_AP,
        M357_EXPRESS,
        M44_BP,
        M44_SP,
        M44_FMJ,
        M44_JHP,
        M44_AP,
        M44_EXPRESS,
        P22_SP,
        P22_FMJ,
        P22_JHP,
        P22_AP,
        P9_SP,
        P9_FMJ,
        P9_JHP,
        P9_AP,
        R556_SP,
        R556_FMJ,
        R556_JHP,
        R556_AP,
        R762_SP,
        R762_FMJ,
        R762_JHP,
        R762_AP,
        R762_DU,
        BMG50_SP,
        BMG50_FMJ,
        BMG50_JHP,
        BMG50_AP,
        BMG50_DU,
        B75,
        B75_INC,
        B75_EXP,
        G12_BP,
        G12_BP_MAGNUM,
        G12_BP_SLUG,
        G12,
        G12_SLUG,
        G12_FLECHETTE,
        G12_MAGNUM,
        G12_EXPLOSIVE,
        G12_PHOSPHORUS,
        G26_FLARE,
        G26_FLARE_SUPPLY,
        G26_FLARE_WEAPON,
        G40_HE,
        G40_HEAT,
        G40_DEMO,
        G40_INC,
        G40_PHOSPHORUS,
        ROCKET_HE,
        ROCKET_HEAT,
        ROCKET_DEMO,
        ROCKET_INC,
        ROCKET_PHOSPHORUS,
        FLAME_DIESEL,
        FLAME_GAS,
        FLAME_NAPALM,
        FLAME_BALEFIRE,
        CAPACITOR,
        CAPACITOR_OVERCHARGE,
        CAPACITOR_IR,
        TAU_URANIUM,
        COIL_TUNGSTEN,
        COIL_FERROURANIUM,
        NUKE_STANDARD,
        NUKE_DEMO,
        NUKE_HIGH,
        NUKE_TOTS,
        NUKE_HIVE,
        G10,
        G10_SHRAPNEL,
        G10_DU,
        G10_SLUG,
        R762_HE,
        BMG50_HE,
        G10_EXPLOSIVE,
        P45_SP,
        P45_FMJ,
        P45_JHP,
        P45_AP,
        P45_DU,
        CT_HOOK,
        CT_MORTAR,
        CT_MORTAR_CHARGE,
        NUKE_BALEFIRE,
        BMG50_SM,
        ;

        public static EnumAmmo[] order =
                new EnumAmmo[] {
                    STONE,
                    STONE_AP,
                    STONE_IRON,
                    STONE_SHOT,
                    M357_BP,
                    M357_SP,
                    M357_FMJ,
                    M357_JHP,
                    M357_AP,
                    M357_EXPRESS,
                    M44_BP,
                    M44_SP,
                    M44_FMJ,
                    M44_JHP,
                    M44_AP,
                    M44_EXPRESS,
                    P22_SP,
                    P22_FMJ,
                    P22_JHP,
                    P22_AP,
                    P9_SP,
                    P9_FMJ,
                    P9_JHP,
                    P9_AP,
                    P45_SP,
                    P45_FMJ,
                    P45_JHP,
                    P45_AP,
                    P45_DU,
                    R556_SP,
                    R556_FMJ,
                    R556_JHP,
                    R556_AP,
                    R762_SP,
                    R762_FMJ,
                    R762_JHP,
                    R762_AP,
                    R762_DU,
                    R762_HE,
                    BMG50_SP,
                    BMG50_FMJ,
                    BMG50_JHP,
                    BMG50_AP,
                    BMG50_DU,
                    BMG50_SM,
                    BMG50_HE,
                    B75,
                    B75_INC,
                    B75_EXP,
                    G12_BP,
                    G12_BP_MAGNUM,
                    G12_BP_SLUG,
                    G12,
                    G12_SLUG,
                    G12_FLECHETTE,
                    G12_MAGNUM,
                    G12_EXPLOSIVE,
                    G12_PHOSPHORUS,
                    G10,
                    G10_SHRAPNEL,
                    G10_DU,
                    G10_SLUG,
                    G10_EXPLOSIVE,
                    G26_FLARE,
                    G26_FLARE_SUPPLY,
                    G26_FLARE_WEAPON,
                    G40_HE,
                    G40_HEAT,
                    G40_DEMO,
                    G40_INC,
                    G40_PHOSPHORUS,
                    ROCKET_HE,
                    ROCKET_HEAT,
                    ROCKET_DEMO,
                    ROCKET_INC,
                    ROCKET_PHOSPHORUS,
                    FLAME_DIESEL,
                    FLAME_GAS,
                    FLAME_NAPALM,
                    FLAME_BALEFIRE,
                    CAPACITOR,
                    CAPACITOR_OVERCHARGE,
                    CAPACITOR_IR,
                    TAU_URANIUM,
                    COIL_TUNGSTEN,
                    COIL_FERROURANIUM,
                    NUKE_STANDARD,
                    NUKE_DEMO,
                    NUKE_HIGH,
                    NUKE_TOTS,
                    NUKE_HIVE,
                    NUKE_BALEFIRE,
                    CT_HOOK,
                    CT_MORTAR,
                    CT_MORTAR_CHARGE,
                };
    }

    public enum EnumAmmoSecret {
        FOLLY_SM,
        FOLLY_NUKE,
        M44_EQUESTRIAN,
        G12_EQUESTRIAN,
        BMG50_EQUESTRIAN,
        P35_800,
        BMG50_BLACK,
        P35_800_BL
    }

    public enum EnumModTest {
        FIRERATE,
        DAMAGE,
        MULTI,
        OVERRIDE_2_5,
        OVERRIDE_5,
        OVERRIDE_7_5,
        OVERRIDE_10,
        OVERRIDE_12_5,
        OVERRIDE_15,
        OVERRIDE_20
    }

    public enum EnumModGeneric {
        IRON_DAMAGE,
        IRON_DURA,
        STEEL_DAMAGE,
        STEEL_DURA,
        DURA_DAMAGE,
        DURA_DURA,
        DESH_DAMAGE,
        DESH_DURA,
        WSTEEL_DAMAGE,
        WSTEEL_DURA,
        FERRO_DAMAGE,
        FERRO_DURA,
        TCALLOY_DAMAGE,
        TCALLOY_DURA,
        BIGMT_DAMAGE,
        BIGMT_DURA,
        BRONZE_DAMAGE,
        BRONZE_DURA,
    }

    public enum EnumModSpecial {
        SILENCER,
        SCOPE,
        SAW,
        GREASEGUN,
        SLOWDOWN,
        SPEEDUP,
        CHOKE,
        SPEEDLOADER,
        FURNITURE_GREEN,
        FURNITURE_BLACK,
        BAYONET,
        STACK_MAG,
        SKIN_SATURNITE,
        LAS_SHOTGUN,
        LAS_CAPACITOR,
        LAS_AUTO,
        NICKEL,
        DOUBLOONS,
        DRILL_HSS,
        DRILL_WEAPONSTEEL,
        DRILL_TCALLOY,
        DRILL_SATURNITE,
        ENGINE_DIESEL,
        ENGINE_AVIATION,
        ENGINE_ELECTRIC,
        ENGINE_TURBO,
        MAGNET,
        SIFTER,
        CANISTERS
    }

    public enum EnumModCaliber {
        P9,
        P45,
        P22,
        M357,
        M44,
        R556,
        R762,
        BMG50,
    }
}
