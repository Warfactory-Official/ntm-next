/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModTest;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory22lr;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory45;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.items.weapon.sedna.mags.Magazine;
import com.hbm.items.weapon.sedna.mods.WeaponModBase.Operations;
import com.hbm.items.weapon.sedna.mods.WeaponModBase.Slot;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.RegistryHandle;
import com.hbm.sound.ModSounds;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class XWeaponModManager {

    public static final String KEY_MOD_LIST = "KEY_MOD_LIST_";
    public static final Codec<List<Identifier>> MOD_LIST_CODEC = Identifier.CODEC.listOf();
    public static final RegistryHandle<Declaration> ID_SILENCER =
            handle("weapon_mod_special_silencer");
    public static final RegistryHandle<Declaration> ID_SCOPE = handle("weapon_mod_special_scope");
    public static final RegistryHandle<Declaration> ID_SAWED_OFF =
            handle("weapon_mod_special_saw/sawed_off");
    public static final RegistryHandle<Declaration> ID_NO_SHIELD =
            handle("weapon_mod_special_saw/no_shield");
    public static final RegistryHandle<Declaration> ID_NO_STOCK =
            handle("weapon_mod_special_saw/no_stock");
    public static final RegistryHandle<Declaration> ID_GREASEGUN_CLEAN =
            handle("weapon_mod_special_greasegun");
    public static final RegistryHandle<Declaration> ID_MINIGUN_SPEED =
            handle("weapon_mod_special_speedup/minigun");
    public static final RegistryHandle<Declaration> ID_SLOWDOWN =
            handle("weapon_mod_special_slowdown");
    public static final RegistryHandle<Declaration> ID_FURNITURE_GREEN =
            handle("weapon_mod_special_furniture_green");
    public static final RegistryHandle<Declaration> ID_FURNITURE_BLACK =
            handle("weapon_mod_special_furniture_black");
    public static final RegistryHandle<Declaration> ID_MAS_BAYONET =
            handle("weapon_mod_special_bayonet/mas36");
    public static final RegistryHandle<Declaration> ID_UZI_SATURN =
            handle("weapon_mod_special_skin_saturnite");
    public static final RegistryHandle<Declaration> ID_LAS_SHOTGUN =
            handle("weapon_mod_special_las_shotgun");
    public static final RegistryHandle<Declaration> ID_LAS_CAPACITOR =
            handle("weapon_mod_special_las_capacitor");
    public static final RegistryHandle<Declaration> ID_LAS_AUTO =
            handle("weapon_mod_special_las_auto");
    public static final RegistryHandle<Declaration> ID_CARBINE_BAYONET =
            handle("weapon_mod_special_bayonet/carbine");
    public static final RegistryHandle<Declaration> ID_NI4NI_NICKEL =
            handle("weapon_mod_special_nickel");
    public static final RegistryHandle<Declaration> ID_NI4NI_DOUBLOONS =
            handle("weapon_mod_special_doubloons");
    public static final RegistryHandle<Declaration> ID_DRILL_HSS =
            handle("weapon_mod_special_drill_hss");
    public static final RegistryHandle<Declaration> ID_DRILL_WSTEEL =
            handle("weapon_mod_special_drill_weaponsteel");
    public static final RegistryHandle<Declaration> ID_DRILL_TCALLOY =
            handle("weapon_mod_special_drill_tcalloy");
    public static final RegistryHandle<Declaration> ID_DRILL_SATURN =
            handle("weapon_mod_special_drill_saturnite");
    public static final RegistryHandle<Declaration> ID_ENGINE_DIESEL =
            handle("weapon_mod_special_engine_diesel");
    public static final RegistryHandle<Declaration> ID_ENGINE_AVIATION =
            handle("weapon_mod_special_engine_aviation");
    public static final RegistryHandle<Declaration> ID_ENGINE_ELECTRIC =
            handle("weapon_mod_special_engine_electric");
    public static final RegistryHandle<Declaration> ID_ENGINE_TURBO =
            handle("weapon_mod_special_engine_turbo");

    private static final Comparator<Declaration> INSTALL_ORDER =
            Comparator.comparingInt((Declaration declaration) -> declaration.modifier.priority())
                    .reversed();
    private static final Map<Identifier, Declaration> BY_ID = new LinkedHashMap<>();
    private static final Map<Item, ModItem> BY_ITEM = new LinkedHashMap<>();
    private static @Nullable List<Runnable> addonVariants = new ArrayList<>();
    private static int nextBinding;

    private XWeaponModManager() {}

    private static RegistryHandle<Declaration> handle(String path) {
        var id = Identifier.fromNamespaceAndPath("hbm", path);
        return RegistryHandle.of(id, () -> BY_ID.get(id));
    }

    public static void init() {
        var test = ModItems.WEAPON_MOD_TEST;
        mod(test, EnumModTest.FIRERATE).anyGun(new WeaponModTestFirerate());
        mod(test, EnumModTest.DAMAGE).anyGun(new WeaponModTestDamage());
        mod(test, EnumModTest.MULTI).anyGun(new WeaponModTestMulti());
        mod(test, EnumModTest.OVERRIDE_2_5).anyGun(new WeaponModOverride(2.5F));
        mod(test, EnumModTest.OVERRIDE_5).anyGun(new WeaponModOverride(5F));
        mod(test, EnumModTest.OVERRIDE_7_5).anyGun(new WeaponModOverride(7.5F));
        mod(test, EnumModTest.OVERRIDE_10).anyGun(new WeaponModOverride(10F));
        mod(test, EnumModTest.OVERRIDE_12_5).anyGun(new WeaponModOverride(125F));
        mod(test, EnumModTest.OVERRIDE_15).anyGun(new WeaponModOverride(15F));
        mod(test, EnumModTest.OVERRIDE_20).anyGun(new WeaponModOverride(20F));

        var generic = ModItems.WEAPON_MOD_GENERIC;
        mod(generic, EnumModGeneric.IRON_DAMAGE)
                .guns(new WeaponModGenericDamage(), ModItems.GUN_PEPPERBOX);
        mod(generic, EnumModGeneric.IRON_DURA)
                .guns(new WeaponModGenericDurability(), ModItems.GUN_PEPPERBOX);
        List<RegistryHandle<? extends GunBaseNTItem>> steel =
                List.of(
                        ModItems.GUN_LIGHT_REVOLVER,
                        ModItems.GUN_LIGHT_REVOLVER_ATLAS,
                        ModItems.GUN_HENRY,
                        ModItems.GUN_HENRY_LINCOLN,
                        ModItems.GUN_GREASEGUN,
                        ModItems.GUN_MARESLEG,
                        ModItems.GUN_MARESLEG_AKIMBO,
                        ModItems.GUN_FLAREGUN);
        List<RegistryHandle<? extends GunBaseNTItem>> dura =
                List.of(
                        ModItems.GUN_AM180,
                        ModItems.GUN_LIBERATOR,
                        ModItems.GUN_CONGOLAKE,
                        ModItems.GUN_FLAMER,
                        ModItems.GUN_FLAMER_TOPAZ);
        List<RegistryHandle<? extends GunBaseNTItem>> desh =
                List.of(
                        ModItems.GUN_HEAVY_REVOLVER,
                        ModItems.GUN_CARBINE,
                        ModItems.GUN_UZI,
                        ModItems.GUN_UZI_AKIMBO,
                        ModItems.GUN_SPAS12,
                        ModItems.GUN_PANZERSCHRECK);
        List<RegistryHandle<? extends GunBaseNTItem>> wsteel =
                List.of(
                        ModItems.GUN_STAR_F,
                        ModItems.GUN_STAR_F_AKIMBO,
                        ModItems.GUN_G3,
                        ModItems.GUN_G3_ZEBRA,
                        ModItems.GUN_MK108,
                        ModItems.GUN_CHEMTHROWER);
        List<RegistryHandle<? extends GunBaseNTItem>> ferro =
                List.of(
                        ModItems.GUN_AMAT,
                        ModItems.GUN_M2,
                        ModItems.GUN_AUTOSHOTGUN,
                        ModItems.GUN_AUTOSHOTGUN_SHREDDER,
                        ModItems.GUN_QUADRO);
        List<RegistryHandle<? extends GunBaseNTItem>> tcalloy =
                List.of(
                        ModItems.GUN_LAG,
                        ModItems.GUN_MINIGUN,
                        ModItems.GUN_MISSILE_LAUNCHER,
                        ModItems.GUN_TESLA_CANNON);
        List<RegistryHandle<? extends GunBaseNTItem>> bigmt =
                List.of(
                        ModItems.GUN_LASER_PISTOL,
                        ModItems.GUN_LASER_PISTOL_PEW_PEW,
                        ModItems.GUN_STG77,
                        ModItems.GUN_FATMAN,
                        ModItems.GUN_TAU);
        List<RegistryHandle<? extends GunBaseNTItem>> bronze = List.of(ModItems.GUN_LASRIFLE);
        mod(generic, EnumModGeneric.STEEL_DAMAGE).guns(new WeaponModGenericDamage(), steel);
        mod(generic, EnumModGeneric.STEEL_DURA).guns(new WeaponModGenericDurability(), steel);
        mod(generic, EnumModGeneric.DURA_DAMAGE).guns(new WeaponModGenericDamage(), dura);
        mod(generic, EnumModGeneric.DURA_DURA).guns(new WeaponModGenericDurability(), dura);
        mod(generic, EnumModGeneric.DESH_DAMAGE).guns(new WeaponModGenericDamage(), desh);
        mod(generic, EnumModGeneric.DESH_DURA).guns(new WeaponModGenericDurability(), desh);
        mod(generic, EnumModGeneric.WSTEEL_DAMAGE).guns(new WeaponModGenericDamage(), wsteel);
        mod(generic, EnumModGeneric.WSTEEL_DURA).guns(new WeaponModGenericDurability(), wsteel);
        mod(generic, EnumModGeneric.FERRO_DAMAGE).guns(new WeaponModGenericDamage(), ferro);
        mod(generic, EnumModGeneric.FERRO_DURA).guns(new WeaponModGenericDurability(), ferro);
        mod(generic, EnumModGeneric.TCALLOY_DAMAGE).guns(new WeaponModGenericDamage(), tcalloy);
        mod(generic, EnumModGeneric.TCALLOY_DURA).guns(new WeaponModGenericDurability(), tcalloy);
        mod(generic, EnumModGeneric.BIGMT_DAMAGE).guns(new WeaponModGenericDamage(), bigmt);
        mod(generic, EnumModGeneric.BIGMT_DURA).guns(new WeaponModGenericDurability(), bigmt);
        mod(generic, EnumModGeneric.BRONZE_DAMAGE).guns(new WeaponModGenericDamage(), bronze);
        mod(generic, EnumModGeneric.BRONZE_DURA).guns(new WeaponModGenericDurability(), bronze);

        var special = ModItems.WEAPON_MOD_SPECIAL;
        mod(special, EnumModSpecial.SPEEDLOADER)
                .guns(new WeaponModLiberatorSpeedloader(), ModItems.GUN_LIBERATOR);
        mod(special, EnumModSpecial.SILENCER)
                .guns(
                        new WeaponModSilencer(),
                        ModItems.GUN_AM180,
                        ModItems.GUN_UZI,
                        ModItems.GUN_UZI_AKIMBO,
                        ModItems.GUN_STAR_F,
                        ModItems.GUN_STAR_F_AKIMBO,
                        ModItems.GUN_G3,
                        ModItems.GUN_AMAT)
                .variant(
                        ModItems.GUN_AMAT.get(),
                        ops -> ops.set(Receiver.FIRE_SOUND, ModSounds.GUN_AMAT_SILENCER));
        mod(special, EnumModSpecial.SCOPE)
                .guns(
                        new WeaponModScope(),
                        ModItems.GUN_HEAVY_REVOLVER,
                        ModItems.GUN_CARBINE,
                        ModItems.GUN_G3,
                        ModItems.GUN_MAS36,
                        ModItems.GUN_CHARGE_THROWER)
                .variant(
                        ModItems.GUN_HEAVY_REVOLVER.get(),
                        ops -> ops.set(GunConfig.SCOPE_TEXTURE, XFactory44.scope_lilmac))
                .variant(
                        ModItems.GUN_CHARGE_THROWER.get(),
                        ops -> ops.set(GunConfig.SCOPE_TEXTURE, XFactoryTool.scope));
        var saw = mod(special, EnumModSpecial.SAW);
        saw.guns(
                        "sawed_off",
                        new WeaponModSawedOff(),
                        ModItems.GUN_MARESLEG,
                        ModItems.GUN_DOUBLE_BARREL)
                .variant(
                        ModItems.GUN_MARESLEG.get(),
                        ops ->
                                ops.set(
                                                GunConfig.ANIMATIONS,
                                                XFactory12ga.LAMBDA_MARESLEG_SHORT_ANIMS)
                                        .set(GunConfig.DRAW_DURATION, 5));
        saw.guns("no_shield", new WeaponModPanzerschreckSawedOff(), ModItems.GUN_PANZERSCHRECK);
        saw.guns("no_stock", new WeapnModG3SawedOff(), ModItems.GUN_G3, ModItems.GUN_G3_ZEBRA);
        mod(special, EnumModSpecial.GREASEGUN)
                .guns(new WeaponModGreasegun(), ModItems.GUN_GREASEGUN);
        mod(special, EnumModSpecial.SLOWDOWN)
                .guns(new WeaponModSlowdown(), ModItems.GUN_MINIGUN, ModItems.GUN_MINIGUN_DUAL);
        var speedup = mod(special, EnumModSpecial.SPEEDUP);
        speedup.guns(
                "minigun",
                new WeaponModMinigunSpeedup(),
                ModItems.GUN_MINIGUN,
                ModItems.GUN_MINIGUN_DUAL);
        speedup.guns(
                "shredder",
                new WeaponModShredderSpeedup(),
                ModItems.GUN_AUTOSHOTGUN,
                ModItems.GUN_AUTOSHOTGUN_SHREDDER,
                ModItems.GUN_MK108);
        mod(special, EnumModSpecial.CHOKE)
                .guns(
                        new WeaponModChoke(),
                        ModItems.GUN_PEPPERBOX,
                        ModItems.GUN_MARESLEG,
                        ModItems.GUN_DOUBLE_BARREL,
                        ModItems.GUN_LIBERATOR,
                        ModItems.GUN_SPAS12,
                        ModItems.GUN_AUTOSHOTGUN_SEXY,
                        ModItems.GUN_AUTOSHOTGUN_HERETIC);
        mod(special, EnumModSpecial.FURNITURE_GREEN)
                .guns(new WeaponModPolymerFurniture(), ModItems.GUN_G3);
        mod(special, EnumModSpecial.FURNITURE_BLACK)
                .guns(new WeaponModPolymerFurniture(), ModItems.GUN_G3);
        var bayonet = mod(special, EnumModSpecial.BAYONET);
        bayonet.guns("mas36", new WeaponModMASBayonet(), ModItems.GUN_MAS36);
        bayonet.guns("carbine", new WeaponModCarbineBayonet(), ModItems.GUN_CARBINE);
        mod(special, EnumModSpecial.STACK_MAG)
                .guns(
                        new WeaponModStackMag(),
                        ModItems.GUN_GREASEGUN,
                        ModItems.GUN_UZI,
                        ModItems.GUN_UZI_AKIMBO,
                        ModItems.GUN_ABERRATOR,
                        ModItems.GUN_ABERRATOR_EOTT);
        mod(special, EnumModSpecial.SKIN_SATURNITE)
                .guns(new WeaponModUziSaturnite(), ModItems.GUN_UZI, ModItems.GUN_UZI_AKIMBO);
        mod(special, EnumModSpecial.LAS_SHOTGUN)
                .guns(new WeaponModLasShotgun(), ModItems.GUN_LASRIFLE);
        mod(special, EnumModSpecial.LAS_CAPACITOR)
                .guns(new WeaponModLasCapacitor(), ModItems.GUN_LASRIFLE);
        mod(special, EnumModSpecial.LAS_AUTO).guns(new WeaponModLasAuto(), ModItems.GUN_LASRIFLE);
        mod(special, EnumModSpecial.NICKEL)
                .guns(new WeaponModNickel(Slot.COIN1), ModItems.GUN_N_I_4_N_I);
        mod(special, EnumModSpecial.DOUBLOONS)
                .guns(new WeaponModNickel(Slot.COIN2), ModItems.GUN_N_I_4_N_I);
        mod(special, EnumModSpecial.DRILL_HSS)
                .guns(new WeaponModDrill(1.25F, 3F, 0.15F, 1D, -1, 3), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.DRILL_WEAPONSTEEL)
                .guns(new WeaponModDrill(1.5F, 5F, 0.2F, 1D, 2, 3), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.DRILL_TCALLOY)
                .guns(new WeaponModDrill(2F, 7.5F, 0.2F, 2D, 3, 4), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.DRILL_SATURNITE)
                .guns(new WeaponModDrill(3F, 10F, 0.25F, 2D, 3, 5), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.ENGINE_DIESEL)
                .guns(new WeaponModEngine(WeaponModEngine.ENGINE_DIESEL, 15), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.ENGINE_AVIATION)
                .guns(new WeaponModEngine(WeaponModEngine.ENGINE_AVIATION, 10), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.ENGINE_ELECTRIC)
                .guns(new WeaponModEngine(WeaponModEngine.ENGINE_ELECTRIC, 15), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.ENGINE_TURBO)
                .guns(new WeaponModEngine(WeaponModEngine.ENGINE_TURBO, 5), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.MAGNET)
                .guns(new WeaponModDrillFortune(Slot.MAGNET, 2), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.SIFTER)
                .guns(new WeaponModDrillFortune(Slot.SIFTER, 1), ModItems.GUN_DRILL);
        mod(special, EnumModSpecial.CANISTERS).guns(new WeaponModCanisters(), ModItems.GUN_DRILL);

        var p9 =
                new BulletConfig[] {
                    XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap
                };
        var p45 =
                new BulletConfig[] {
                    XFactory45.p45_sp,
                    XFactory45.p45_fmj,
                    XFactory45.p45_jhp,
                    XFactory45.p45_ap,
                    XFactory45.p45_du
                };
        var p22 =
                new BulletConfig[] {
                    XFactory22lr.p22_sp,
                    XFactory22lr.p22_fmj,
                    XFactory22lr.p22_jhp,
                    XFactory22lr.p22_ap
                };
        var m357 =
                new BulletConfig[] {
                    XFactory357.m357_sp,
                    XFactory357.m357_fmj,
                    XFactory357.m357_jhp,
                    XFactory357.m357_ap,
                    XFactory357.m357_express
                };
        var m44 =
                new BulletConfig[] {
                    XFactory44.m44_sp,
                    XFactory44.m44_fmj,
                    XFactory44.m44_jhp,
                    XFactory44.m44_ap,
                    XFactory44.m44_express
                };
        var r556 =
                new BulletConfig[] {
                    XFactory556mm.r556_sp,
                    XFactory556mm.r556_fmj,
                    XFactory556mm.r556_jhp,
                    XFactory556mm.r556_ap
                };
        var r762 =
                new BulletConfig[] {
                    XFactory762mm.r762_sp,
                    XFactory762mm.r762_fmj,
                    XFactory762mm.r762_jhp,
                    XFactory762mm.r762_ap,
                    XFactory762mm.r762_du,
                    XFactory762mm.r762_he
                };
        var bmg50 =
                new BulletConfig[] {
                    XFactory50.bmg50_sp,
                    XFactory50.bmg50_fmj,
                    XFactory50.bmg50_jhp,
                    XFactory50.bmg50_ap,
                    XFactory50.bmg50_du,
                    XFactory50.bmg50_he
                };
        var caliber = ModItems.WEAPON_MOD_CALIBER;
        var p9Mod = mod(caliber, EnumModCaliber.P9);
        p9Mod.guns("henry", new WeaponModCaliber(28, 10F, p9), ModItems.GUN_HENRY);
        p9Mod.guns("star_f", new WeaponModCaliber(12, 15F, p9), ModItems.GUN_STAR_F);
        p9Mod.guns("star_f_akimbo", new WeaponModCaliber(12, 15F, p9), ModItems.GUN_STAR_F_AKIMBO);
        var p45Mod = mod(caliber, EnumModCaliber.P45);
        p45Mod.guns("henry", new WeaponModCaliber(28, 10F, p45), ModItems.GUN_HENRY);
        p45Mod.guns("greasegun", new WeaponModCaliber(24, 3F, p45), ModItems.GUN_GREASEGUN);
        p45Mod.guns("uzi", new WeaponModCaliber(24, 3F, p45), ModItems.GUN_UZI);
        p45Mod.guns("uzi_akimbo", new WeaponModCaliber(24, 3F, p45), ModItems.GUN_UZI_AKIMBO);
        p45Mod.guns("lag", new WeaponModCaliber(15, 25F, p45), ModItems.GUN_LAG);
        var p22Mod = mod(caliber, EnumModCaliber.P22);
        p22Mod.guns("henry", new WeaponModCaliber(28, 10F, p22), ModItems.GUN_HENRY);
        p22Mod.guns("uzi", new WeaponModCaliber(40, 3F, p22), ModItems.GUN_UZI);
        p22Mod.guns("uzi_akimbo", new WeaponModCaliber(40, 3F, p22), ModItems.GUN_UZI_AKIMBO);
        var m357Mod = mod(caliber, EnumModCaliber.M357);
        m357Mod.guns("henry", new WeaponModCaliber(20, 10F, m357), ModItems.GUN_HENRY);
        m357Mod.guns("lag", new WeaponModCaliber(15, 25F, m357), ModItems.GUN_LAG);
        mod(caliber, EnumModCaliber.M44)
                .guns("lag", new WeaponModCaliber(13, 25F, m44), ModItems.GUN_LAG);
        var r556Mod = mod(caliber, EnumModCaliber.R556);
        r556Mod.guns("henry", new WeaponModCaliber(10, 10F, r556), ModItems.GUN_HENRY);
        r556Mod.guns("carbine", new WeaponModCaliber(20, 15F, r556), ModItems.GUN_CARBINE);
        r556Mod.guns(
                "minigun",
                new WeaponModCaliber(0, 6F, r556),
                ModItems.GUN_MINIGUN,
                ModItems.GUN_MINIGUN_DUAL);
        var r762Mod = mod(caliber, EnumModCaliber.R762);
        r762Mod.guns("henry", new WeaponModCaliber(8, 10F, r762), ModItems.GUN_HENRY);
        r762Mod.guns("g3", new WeaponModCaliber(24, 5F, r762), ModItems.GUN_G3);
        var bmg50Mod = mod(caliber, EnumModCaliber.BMG50);
        bmg50Mod.guns("henry", new WeaponModCaliber(5, 10F, bmg50), ModItems.GUN_HENRY);
        bmg50Mod.guns(
                "minigun",
                new WeaponModCaliber(0, 6F, bmg50),
                ModItems.GUN_MINIGUN,
                ModItems.GUN_MINIGUN_DUAL);

        List<Runnable> variants;
        synchronized (XWeaponModManager.class) {
            variants = addonVariants;
            addonVariants = null;
        }
        for (var variant : variants) variant.run();
    }

    public static synchronized void variant(
            RegistryHandle<Declaration> id,
            Supplier<? extends GunBaseNTItem> gun,
            Consumer<Operations> adjust) {
        if (addonVariants == null)
            throw new IllegalStateException(
                    "weapon mod variant " + id.id() + " declared after common setup");
        addonVariants.add(
                () -> {
                    var declaration = id.get();
                    var target = gun.get();
                    BY_ITEM.get(declaration.item).bind(target, declaration);
                    declaration.variant(target, adjust);
                });
    }

    private static <E extends Enum<E>> ModItem mod(ItemFamily<E, ?> family, E type) {
        var mod = new ModItem(family.handle(type), () -> family.stack(type));
        BY_ITEM.put(mod.item, mod);
        return mod;
    }

    public static List<Identifier> installed(ItemStack stack, int cfg) {
        return InstalledWeaponMods.of(stack, cfg).ids();
    }

    static int binding(Item gun, Identifier id) {
        var declaration = BY_ID.get(id);
        return declaration == null ? -1 : declaration.bindings.getInt(gun);
    }

    public static Collection<Declaration> declarations() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static Set<Item> modItems() {
        return Collections.unmodifiableSet(BY_ITEM.keySet());
    }

    public static @Nullable Declaration declarationFor(Item gun, Item modItem) {
        var mod = BY_ITEM.get(modItem);
        if (mod == null) return null;
        var bound = mod.guns.get(gun);
        return bound != null ? bound : mod.anyGun;
    }

    public static Map<Item, Declaration> boundGuns(Item modItem) {
        return Collections.unmodifiableMap(BY_ITEM.get(modItem).guns);
    }

    public static @Nullable Declaration anyGun(Item modItem) {
        return BY_ITEM.get(modItem).anyGun;
    }

    public static ItemStack[] getUpgradeItems(ItemStack stack, int cfg) {
        var ids = installed(stack, cfg);
        var items = new ItemStack[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            var declaration = BY_ID.get(ids.get(i));
            if (declaration != null) items[i] = declaration.stack();
        }
        return items;
    }

    public static boolean hasUpgrade(ItemStack stack, int cfg, RegistryHandle<Declaration> mod) {
        return installed(stack, cfg).contains(mod.id());
    }

    public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
        var declaration = declarationFor(gun.getItem(), mod.getItem());
        if (declaration == null) return false;
        if (checkMutex)
            for (var id : installed(gun, cfg)) {
                var other = BY_ID.get(id);
                if (other != null && other.modifier.sharesSlot(declaration.modifier)) return false;
            }
        return true;
    }

    public static void install(ServerLevel level, ItemStack stack, int cfg, ItemStack... mods) {
        var loaded = Loaded.of(stack, cfg);
        boolean revalidate = removeAll(level, stack, cfg);
        var gun = stack.getItem();
        var chosen = new ArrayList<Declaration>(mods.length);
        for (var mod : mods) {
            var declaration = declarationFor(gun, mod.getItem());
            if (declaration != null) chosen.add(declaration);
        }
        if (!chosen.isEmpty()) {
            chosen.sort(INSTALL_ORDER);
            var ids = new ArrayList<Identifier>(chosen.size());
            for (int i = 0; i < chosen.size(); i++) {
                var declaration = chosen.get(i);
                ids.add(declaration.id);
                declaration.modifier.onInstall(level, stack);
                revalidate |= declaration.modifier.revalidatesMagazine();
            }
            GunBaseNTItem.setValue(stack, KEY_MOD_LIST + cfg, MOD_LIST_CODEC, ids);
        }
        if (revalidate && loaded != null) loaded.restore(stack, cfg);
    }

    public static void uninstall(ServerLevel level, ItemStack stack, int cfg) {
        var loaded = Loaded.of(stack, cfg);
        if (removeAll(level, stack, cfg) && loaded != null) loaded.restore(stack, cfg);
    }

    private static boolean removeAll(ServerLevel level, ItemStack stack, int cfg) {
        boolean revalidate = false;
        for (var id : installed(stack, cfg)) {
            var declaration = BY_ID.get(id);
            if (declaration == null) continue;
            declaration.modifier.onUninstall(level, stack);
            revalidate |= declaration.modifier.revalidatesMagazine();
        }
        GunBaseNTItem.removeValue(stack, KEY_MOD_LIST + cfg);
        return revalidate;
    }

    private static @Nullable Magazine<?> magazine(ItemStack stack, int cfg) {
        return ((GunBaseNTItem) stack.getItem())
                .getConfig(stack, cfg)
                .getReceivers(stack)
                .get(0)
                .getMagazine(stack);
    }

    public static final class Declaration {
        public final Identifier id;
        public final WeaponModBase modifier;
        public final Item item;
        private final Supplier<ItemStack> stack;
        private final Reference2IntOpenHashMap<Item> bindings = new Reference2IntOpenHashMap<>();

        private Declaration(Identifier id, WeaponModBase modifier, ModItem mod) {
            this.id = id;
            this.modifier = modifier;
            this.item = mod.item;
            this.stack = mod.stack;
            bindings.defaultReturnValue(declare());
        }

        public ItemStack stack() {
            return stack.get();
        }

        private int declare() {
            int binding = nextBinding++;
            modifier.operations(new Operations(binding));
            return binding;
        }

        private Declaration variant(Item gun, Consumer<Operations> adjust) {
            int binding = declare();
            adjust.accept(new Operations(binding));
            bindings.put(gun, binding);
            return this;
        }
    }

    private static final class ModItem {
        final RegistryHandle<? extends Item> handle;
        final Item item;
        final Supplier<ItemStack> stack;
        final Map<Item, Declaration> guns = new LinkedHashMap<>();
        @Nullable Declaration anyGun;

        ModItem(RegistryHandle<? extends Item> handle, Supplier<ItemStack> stack) {
            this.handle = handle;
            this.item = handle.get();
            this.stack = stack;
        }

        private Declaration declare(Identifier id, WeaponModBase modifier) {
            var declaration = new Declaration(id, modifier, this);
            BY_ID.put(id, declaration);
            return declaration;
        }

        void anyGun(WeaponModBase modifier) {
            anyGun = declare(handle.id(), modifier);
        }

        @SafeVarargs
        final Declaration guns(
                WeaponModBase modifier, RegistryHandle<? extends GunBaseNTItem>... guns) {
            return guns(modifier, List.of(guns));
        }

        Declaration guns(
                WeaponModBase modifier, List<RegistryHandle<? extends GunBaseNTItem>> guns) {
            return guns(handle.id(), modifier, guns);
        }

        @SafeVarargs
        final Declaration guns(
                String variant,
                WeaponModBase modifier,
                RegistryHandle<? extends GunBaseNTItem>... guns) {
            return guns(handle.id().withSuffix("/" + variant), modifier, List.of(guns));
        }

        private Declaration guns(
                Identifier id,
                WeaponModBase modifier,
                List<RegistryHandle<? extends GunBaseNTItem>> guns) {
            var declaration = declare(id, modifier);
            for (var gun : guns) bind(gun.get(), declaration);
            return declaration;
        }

        void bind(GunBaseNTItem gun, Declaration declaration) {
            this.guns.put(gun, declaration);
            if (!gun.recognizedMods.contains(stack)) gun.recognizedMods.add(stack);
        }
    }

    private record Loaded(Object type, int count) {
        static @Nullable Loaded of(ItemStack stack, int cfg) {
            var mag = magazine(stack, cfg);
            return mag == null
                    ? null
                    : new Loaded(mag.getType(stack, null), mag.getAmount(stack, null));
        }

        void restore(ItemStack stack, int cfg) {
            var mag = magazine(stack, cfg);
            if (mag.getType(stack, null) == type)
                mag.setAmount(stack, Mth.clamp(count, 0, mag.getCapacity(stack)));
            else mag.setAmount(stack, 0);
        }
    }
}
