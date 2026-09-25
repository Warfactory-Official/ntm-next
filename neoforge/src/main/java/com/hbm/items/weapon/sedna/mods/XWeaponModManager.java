// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.mods;

import com.google.common.collect.HashBiMap;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.*;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModCaliber;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModGeneric;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModTest;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.RegistryHandle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class XWeaponModManager {

    public static final String KEY_MOD_LIST = "KEY_MOD_LIST_";
    public static final int ID_SILENCER = 201;
    public static final int ID_SCOPE = 202;
    public static final int ID_SAWED_OFF = 203;
    public static final int ID_NO_SHIELD = 204;
    public static final int ID_NO_STOCK = 205;
    public static final int ID_GREASEGUN_CLEAN = 206;
    public static final int ID_MINIGUN_SPEED = 208;
    public static final int ID_FURNITURE_GREEN = 211;
    public static final int ID_FURNITURE_BLACK = 212;
    public static final int ID_MAS_BAYONET = 213;
    public static final int ID_UZI_SATURN = 215;
    public static final int ID_LAS_SHOTGUN = 216;
    public static final int ID_LAS_CAPACITOR = 217;
    public static final int ID_LAS_AUTO = 218;
    public static final int ID_CARBINE_BAYONET = 219;
    public static final int ID_NI4NI_NICKEL = 220;
    public static final int ID_NI4NI_DOUBLOONS = 221;
    public static final int ID_DRILL_HSS = 222;
    public static final int ID_DRILL_WSTEEL = 223;
    public static final int ID_DRILL_TCALLOY = 224;
    public static final int ID_DRILL_SATURN = 225;
    public static final int ID_ENGINE_DIESEL = 226;
    public static final int ID_ENGINE_AVIATION = 227;
    public static final int ID_ENGINE_ELECTRIC = 228;
    public static final int ID_ENGINE_TURBO = 229;

    public static HashBiMap<Integer, IWeaponMod> idToMod = HashBiMap.create();

    public static HashMap<Item, WeaponModDefinition> stackToMod = new HashMap<>();

    public static HashMap<IWeaponMod, Supplier<ItemStack>> modToStack = new HashMap<>();
    public static Comparator<IWeaponMod> modSorter =
            new Comparator<IWeaponMod>() {

                @Override
                public int compare(IWeaponMod o1, IWeaponMod o2) {
                    return o2.getModPriority() - o1.getModPriority();
                }
            };
    private static Object prevMagType;
    private static int prevMagCount;
    private static boolean changedMagState = false;

    public static void init() {
        if (!idToMod.isEmpty()) return;

        def(ModItems.WEAPON_MOD_TEST, EnumModTest.FIRERATE)
                .addDefault(new WeaponModTestFirerate(0, "FIRERATE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.DAMAGE)
                .addDefault(new WeaponModTestDamage(1, "DAMAGE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.MULTI)
                .addDefault(new WeaponModTestMulti(2, "MULTI"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_2_5)
                .addDefault(new WeaponModOverride(3, 2.5F, "OVERRIDE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_5)
                .addDefault(new WeaponModOverride(4, 5F, "OVERRIDE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_7_5)
                .addDefault(new WeaponModOverride(5, 7.5F, "OVERRIDE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_10)
                .addDefault(new WeaponModOverride(6, 10F, "OVERRIDE"));

        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_12_5)
                .addDefault(new WeaponModOverride(7, 125F, "OVERRIDE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_15)
                .addDefault(new WeaponModOverride(8, 15F, "OVERRIDE"));
        def(ModItems.WEAPON_MOD_TEST, EnumModTest.OVERRIDE_20)
                .addDefault(new WeaponModOverride(9, 20F, "OVERRIDE"));

        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.IRON_DAMAGE)
                .addMod(ModItems.GUN_PEPPERBOX.get(), new WeaponModGenericDamage(100));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.IRON_DURA)
                .addMod(ModItems.GUN_PEPPERBOX.get(), new WeaponModGenericDurability(101));
        Item[] steel =
                items(
                        ModItems.GUN_LIGHT_REVOLVER,
                        ModItems.GUN_LIGHT_REVOLVER_ATLAS,
                        ModItems.GUN_HENRY,
                        ModItems.GUN_HENRY_LINCOLN,
                        ModItems.GUN_GREASEGUN,
                        ModItems.GUN_MARESLEG,
                        ModItems.GUN_MARESLEG_AKIMBO,
                        ModItems.GUN_FLAREGUN);
        Item[] dura =
                items(
                        ModItems.GUN_AM180,
                        ModItems.GUN_LIBERATOR,
                        ModItems.GUN_CONGOLAKE,
                        ModItems.GUN_FLAMER,
                        ModItems.GUN_FLAMER_TOPAZ);
        Item[] desh =
                items(
                        ModItems.GUN_HEAVY_REVOLVER,
                        ModItems.GUN_CARBINE,
                        ModItems.GUN_UZI,
                        ModItems.GUN_UZI_AKIMBO,
                        ModItems.GUN_SPAS12,
                        ModItems.GUN_PANZERSCHRECK);
        Item[] wsteel =
                items(
                        ModItems.GUN_STAR_F,
                        ModItems.GUN_STAR_F_AKIMBO,
                        ModItems.GUN_G3,
                        ModItems.GUN_G3_ZEBRA,
                        ModItems.GUN_MK108,
                        ModItems.GUN_CHEMTHROWER);
        Item[] ferro =
                items(
                        ModItems.GUN_AMAT,
                        ModItems.GUN_M2,
                        ModItems.GUN_AUTOSHOTGUN,
                        ModItems.GUN_AUTOSHOTGUN_SHREDDER,
                        ModItems.GUN_QUADRO);
        Item[] tcalloy =
                items(
                        ModItems.GUN_LAG,
                        ModItems.GUN_MINIGUN,
                        ModItems.GUN_MISSILE_LAUNCHER,
                        ModItems.GUN_TESLA_CANNON);
        Item[] bigmt =
                items(
                        ModItems.GUN_LASER_PISTOL,
                        ModItems.GUN_LASER_PISTOL_PEW_PEW,
                        ModItems.GUN_STG77,
                        ModItems.GUN_FATMAN,
                        ModItems.GUN_TAU);
        Item[] bronze = items(ModItems.GUN_LASRIFLE);
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.STEEL_DAMAGE)
                .addMod(steel, new WeaponModGenericDamage(102));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.STEEL_DURA)
                .addMod(steel, new WeaponModGenericDurability(103));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.DURA_DAMAGE)
                .addMod(dura, new WeaponModGenericDamage(104));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.DURA_DURA)
                .addMod(dura, new WeaponModGenericDurability(105));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.DESH_DAMAGE)
                .addMod(desh, new WeaponModGenericDamage(106));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.DESH_DURA)
                .addMod(desh, new WeaponModGenericDurability(107));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.WSTEEL_DAMAGE)
                .addMod(wsteel, new WeaponModGenericDamage(108));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.WSTEEL_DURA)
                .addMod(wsteel, new WeaponModGenericDurability(109));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.FERRO_DAMAGE)
                .addMod(ferro, new WeaponModGenericDamage(110));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.FERRO_DURA)
                .addMod(ferro, new WeaponModGenericDurability(111));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.TCALLOY_DAMAGE)
                .addMod(tcalloy, new WeaponModGenericDamage(112));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.TCALLOY_DURA)
                .addMod(tcalloy, new WeaponModGenericDurability(113));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.BIGMT_DAMAGE)
                .addMod(bigmt, new WeaponModGenericDamage(114));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.BIGMT_DURA)
                .addMod(bigmt, new WeaponModGenericDurability(115));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.BRONZE_DAMAGE)
                .addMod(bronze, new WeaponModGenericDamage(116));
        def(ModItems.WEAPON_MOD_GENERIC, EnumModGeneric.BRONZE_DURA)
                .addMod(bronze, new WeaponModGenericDurability(117));

        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SPEEDLOADER)
                .addMod(ModItems.GUN_LIBERATOR.get(), new WeaponModLiberatorSpeedloader(200));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SILENCER)
                .addMod(
                        items(
                                ModItems.GUN_AM180,
                                ModItems.GUN_UZI,
                                ModItems.GUN_UZI_AKIMBO,
                                ModItems.GUN_STAR_F,
                                ModItems.GUN_STAR_F_AKIMBO,
                                ModItems.GUN_G3,
                                ModItems.GUN_AMAT),
                        new WeaponModSilencer(ID_SILENCER));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SCOPE)
                .addMod(
                        items(
                                ModItems.GUN_HEAVY_REVOLVER,
                                ModItems.GUN_CARBINE,
                                ModItems.GUN_G3,
                                ModItems.GUN_MAS36,
                                ModItems.GUN_CHARGE_THROWER),
                        new WeaponModScope(ID_SCOPE));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SAW)
                .addMod(
                        items(ModItems.GUN_MARESLEG, ModItems.GUN_DOUBLE_BARREL),
                        new WeaponModSawedOff(ID_SAWED_OFF))
                .addMod(
                        ModItems.GUN_PANZERSCHRECK.get(),
                        new WeaponModPanzerschreckSawedOff(ID_NO_SHIELD))
                .addMod(
                        items(ModItems.GUN_G3, ModItems.GUN_G3_ZEBRA),
                        new WeapnModG3SawedOff(ID_NO_STOCK));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.GREASEGUN)
                .addMod(ModItems.GUN_GREASEGUN.get(), new WeaponModGreasegun(ID_GREASEGUN_CLEAN));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SLOWDOWN)
                .addMod(
                        items(ModItems.GUN_MINIGUN, ModItems.GUN_MINIGUN_DUAL),
                        new WeaponModSlowdown(207));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SPEEDUP)
                .addMod(
                        items(ModItems.GUN_MINIGUN, ModItems.GUN_MINIGUN_DUAL),
                        new WeaponModMinigunSpeedup(ID_MINIGUN_SPEED))
                .addMod(
                        items(
                                ModItems.GUN_AUTOSHOTGUN,
                                ModItems.GUN_AUTOSHOTGUN_SHREDDER,
                                ModItems.GUN_MK108),
                        new WeaponModShredderSpeedup(209));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.CHOKE)
                .addMod(
                        items(
                                ModItems.GUN_PEPPERBOX,
                                ModItems.GUN_MARESLEG,
                                ModItems.GUN_DOUBLE_BARREL,
                                ModItems.GUN_LIBERATOR,
                                ModItems.GUN_SPAS12,
                                ModItems.GUN_AUTOSHOTGUN_SEXY,
                                ModItems.GUN_AUTOSHOTGUN_HERETIC),
                        new WeaponModChoke(210));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.FURNITURE_GREEN)
                .addMod(ModItems.GUN_G3.get(), new WeaponModPolymerFurniture(ID_FURNITURE_GREEN));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.FURNITURE_BLACK)
                .addMod(ModItems.GUN_G3.get(), new WeaponModPolymerFurniture(ID_FURNITURE_BLACK));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.BAYONET)
                .addMod(ModItems.GUN_MAS36.get(), new WeaponModMASBayonet(ID_MAS_BAYONET))
                .addMod(
                        ModItems.GUN_CARBINE.get(),
                        new WeaponModCarbineBayonet(ID_CARBINE_BAYONET));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.STACK_MAG)
                .addMod(
                        items(
                                ModItems.GUN_GREASEGUN,
                                ModItems.GUN_UZI,
                                ModItems.GUN_UZI_AKIMBO,
                                ModItems.GUN_ABERRATOR,
                                ModItems.GUN_ABERRATOR_EOTT),
                        new WeaponModStackMag(214));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SKIN_SATURNITE)
                .addMod(
                        items(ModItems.GUN_UZI, ModItems.GUN_UZI_AKIMBO),
                        new WeaponModUziSaturnite(ID_UZI_SATURN));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.LAS_SHOTGUN)
                .addMod(ModItems.GUN_LASRIFLE.get(), new WeaponModLasShotgun(ID_LAS_SHOTGUN));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.LAS_CAPACITOR)
                .addMod(ModItems.GUN_LASRIFLE.get(), new WeaponModLasCapacitor(ID_LAS_CAPACITOR));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.LAS_AUTO)
                .addMod(ModItems.GUN_LASRIFLE.get(), new WeaponModLasAuto(ID_LAS_AUTO));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.NICKEL)
                .addMod(
                        ModItems.GUN_N_I_4_N_I.get(),
                        new WeaponModNickel(ID_NI4NI_NICKEL, "COIN1"));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.DOUBLOONS)
                .addMod(
                        ModItems.GUN_N_I_4_N_I.get(),
                        new WeaponModNickel(ID_NI4NI_DOUBLOONS, "COIN2"));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.DRILL_HSS)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModDrill(ID_DRILL_HSS)
                                .damage(1.25F)
                                .dt(3F)
                                .pierce(0.15F)
                                .harvest(3));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.DRILL_WEAPONSTEEL)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModDrill(ID_DRILL_WSTEEL)
                                .damage(1.5F)
                                .dt(5F)
                                .pierce(0.2F)
                                .aoe(2)
                                .harvest(3));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.DRILL_TCALLOY)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModDrill(ID_DRILL_TCALLOY)
                                .damage(2F)
                                .dt(7.5F)
                                .pierce(0.2F)
                                .reach(2)
                                .aoe(3)
                                .harvest(4));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.DRILL_SATURNITE)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModDrill(ID_DRILL_SATURN)
                                .damage(3F)
                                .dt(10F)
                                .pierce(0.25F)
                                .reach(2)
                                .aoe(3)
                                .harvest(5));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.ENGINE_DIESEL)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModEngine(ID_ENGINE_DIESEL)
                                .mag(WeaponModEngine.ENGINE_DIESEL)
                                .delay(15));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.ENGINE_AVIATION)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModEngine(ID_ENGINE_AVIATION)
                                .mag(WeaponModEngine.ENGINE_AVIATION)
                                .delay(10));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.ENGINE_ELECTRIC)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModEngine(ID_ENGINE_ELECTRIC)
                                .mag(WeaponModEngine.ENGINE_ELECTRIC)
                                .delay(15));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.ENGINE_TURBO)
                .addMod(
                        ModItems.GUN_DRILL.get(),
                        new WeaponModEngine(ID_ENGINE_TURBO)
                                .mag(WeaponModEngine.ENGINE_TURBO)
                                .delay(5));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.MAGNET)
                .addMod(ModItems.GUN_DRILL.get(), new WeaponModDrillFortune(230, "MAGNET", 2));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.SIFTER)
                .addMod(ModItems.GUN_DRILL.get(), new WeaponModDrillFortune(231, "SIFTER", 1));
        def(ModItems.WEAPON_MOD_SPECIAL, EnumModSpecial.CANISTERS)
                .addMod(ModItems.GUN_DRILL.get(), new WeaponModCanisters(232));

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
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.P9)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(300, 28, 10F, p9))
                .addMod(ModItems.GUN_STAR_F.get(), new WeaponModCaliber(301, 12, 15F, p9))
                .addMod(ModItems.GUN_STAR_F_AKIMBO.get(), new WeaponModCaliber(302, 12, 15F, p9));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.P45)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(310, 28, 10F, p45))
                .addMod(ModItems.GUN_GREASEGUN.get(), new WeaponModCaliber(311, 24, 3F, p45))
                .addMod(ModItems.GUN_UZI.get(), new WeaponModCaliber(312, 24, 3F, p45))
                .addMod(ModItems.GUN_UZI_AKIMBO.get(), new WeaponModCaliber(313, 24, 3F, p45))
                .addMod(ModItems.GUN_LAG.get(), new WeaponModCaliber(314, 15, 25F, p45));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.P22)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(320, 28, 10F, p22))
                .addMod(ModItems.GUN_UZI.get(), new WeaponModCaliber(321, 40, 3F, p22))
                .addMod(ModItems.GUN_UZI_AKIMBO.get(), new WeaponModCaliber(322, 40, 3F, p22));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.M357)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(330, 20, 10F, m357))
                .addMod(ModItems.GUN_LAG.get(), new WeaponModCaliber(331, 15, 25F, m357));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.M44)
                .addMod(ModItems.GUN_LAG.get(), new WeaponModCaliber(340, 13, 25F, m44));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.R556)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(350, 10, 10F, r556))
                .addMod(ModItems.GUN_CARBINE.get(), new WeaponModCaliber(351, 20, 15F, r556))
                .addMod(
                        items(ModItems.GUN_MINIGUN, ModItems.GUN_MINIGUN_DUAL),
                        new WeaponModCaliber(352, 0, 6F, r556));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.R762)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(360, 8, 10F, r762))
                .addMod(ModItems.GUN_G3.get(), new WeaponModCaliber(361, 24, 5F, r762));
        def(ModItems.WEAPON_MOD_CALIBER, EnumModCaliber.BMG50)
                .addMod(ModItems.GUN_HENRY.get(), new WeaponModCaliber(370, 5, 10F, bmg50))
                .addMod(
                        items(ModItems.GUN_MINIGUN, ModItems.GUN_MINIGUN_DUAL),
                        new WeaponModCaliber(371, 0, 6F, bmg50));
    }

    private static <E extends Enum<E>> WeaponModDefinition def(ItemFamily<E, ?> family, E type) {
        return new WeaponModDefinition(() -> family.stack(type), family.get(type));
    }

    @SafeVarargs
    private static Item[] items(RegistryHandle<? extends Item>... handles) {
        Item[] items = new Item[handles.length];
        for (int i = 0; i < handles.length; i++) items[i] = handles[i].get();
        return items;
    }

    public static ItemStack[] getUpgradeItems(ItemStack stack, int cfg) {
        int[] modIds = ItemGunBaseNT.getValueIntArray(stack, KEY_MOD_LIST + cfg);
        if (modIds.length == 0) return new ItemStack[0];
        ItemStack[] mods = new ItemStack[modIds.length];
        for (int i = 0; i < mods.length; i++) {
            IWeaponMod mod = idToMod.get(modIds[i]);
            if (mod != null) {
                Supplier<ItemStack> supplier = modToStack.get(mod);
                mods[i] = supplier != null ? supplier.get() : null;
            }
        }
        return mods;
    }

    public static boolean hasUpgrade(ItemStack stack, int cfg, int id) {
        int[] modIds = ItemGunBaseNT.getValueIntArray(stack, KEY_MOD_LIST + cfg);
        for (int i = 0; i < modIds.length; i++) {
            if (modIds[i] == id) return true;
        }
        return false;
    }

    public static void changedMagState() {
        changedMagState = true;
    }

    private static void saveMagState(ItemStack stack, int cfg) {
        IMagazine<?> mag =
                ((ItemGunBaseNT) stack.getItem())
                        .getConfig(stack, cfg)
                        .getReceivers(stack)[0]
                        .getMagazine(stack);
        if (mag == null) return;
        prevMagType = mag.getType(stack, null);
        prevMagCount = mag.getAmount(stack, null);
    }

    private static void restoreMagState(ItemStack stack, int cfg) {
        if (!changedMagState) return;
        changedMagState = false;

        IMagazine<?> mag =
                ((ItemGunBaseNT) stack.getItem())
                        .getConfig(stack, cfg)
                        .getReceivers(stack)[0]
                        .getMagazine(stack);
        if (mag == null) return;
        if (mag.getType(stack, null) == prevMagType) {
            mag.setAmount(stack, Mth.clamp(prevMagCount, 0, mag.getCapacity(stack)));
        } else {
            mag.setAmount(stack, 0);
        }
    }

    public static void install(ItemStack stack, int cfg, ItemStack... mods) {
        install(null, stack, cfg, mods);
    }

    public static void install(ServerLevel level, ItemStack stack, int cfg, ItemStack... mods) {
        saveMagState(stack, cfg);

        uninstall(level, stack, cfg);

        List<IWeaponMod> toInstall = new ArrayList<>();
        Item gun = stack.getItem();

        for (ItemStack mod : mods) {
            if (mod == null || mod.isEmpty()) continue;
            WeaponModDefinition def = stackToMod.get(mod.getItem());
            if (def != null) {
                IWeaponMod forGun = def.modByGun.get(gun);
                if (forGun != null) {
                    toInstall.add(forGun);
                } else {
                    forGun = def.modByGun.get(null);
                    if (forGun != null) toInstall.add(forGun);
                }
            }
        }
        if (toInstall.isEmpty()) return;
        toInstall.sort(modSorter);
        int[] modIds = new int[toInstall.size()];
        for (int i = 0; i < modIds.length; i++) {
            IWeaponMod mod = toInstall.get(i);
            modIds[i] = idToMod.inverse().get(mod);
            Supplier<ItemStack> supplier = modToStack.get(mod);
            onInstallStack(level, stack, supplier != null ? supplier.get() : ItemStack.EMPTY, cfg);
        }
        ItemGunBaseNT.setValueIntArray(stack, KEY_MOD_LIST + cfg, modIds);
        restoreMagState(stack, cfg);
    }

    public static void uninstall(ItemStack stack, int cfg) {
        uninstall(null, stack, cfg);
    }

    public static void uninstall(ServerLevel level, ItemStack stack, int cfg) {
        if (!stack.isEmpty()) {
            for (ItemStack mod : getUpgradeItems(stack, cfg)) {
                XWeaponModManager.onUninstallStack(level, stack, mod, cfg);
            }
            ItemGunBaseNT.removeValue(stack, KEY_MOD_LIST + cfg);
        }
    }

    public static void onInstallStack(ItemStack gun, ItemStack mod, int cfg) {
        onInstallStack(null, gun, mod, cfg);
    }

    public static void onInstallStack(ServerLevel level, ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        if (level == null) newMod.onInstall(gun, mod, cfg);
        else newMod.onInstall(level, gun, mod, cfg);
    }

    public static void onUninstallStack(ItemStack gun, ItemStack mod, int cfg) {
        onUninstallStack(null, gun, mod, cfg);
    }

    public static void onUninstallStack(ServerLevel level, ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return;
        if (level == null) newMod.onUninstall(gun, mod, cfg);
        else newMod.onUninstall(level, gun, mod, cfg);
    }

    public static IWeaponMod modFromStack(ItemStack gun, ItemStack mod, int cfg) {
        if (gun == null || gun.isEmpty() || mod == null || mod.isEmpty()) return null;
        WeaponModDefinition def = stackToMod.get(mod.getItem());
        if (def == null) return null;
        IWeaponMod newMod = def.modByGun.get(gun.getItem());
        if (newMod == null) newMod = def.modByGun.get(null);
        return newMod;
    }

    public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
        IWeaponMod newMod = modFromStack(gun, mod, cfg);
        if (newMod == null) return false;

        if (checkMutex)
            for (int i : ItemGunBaseNT.getValueIntArray(gun, KEY_MOD_LIST + cfg)) {
                IWeaponMod iMod = idToMod.get(i);
                if (iMod != null)
                    for (String mutex0 : newMod.getSlots())
                        for (String mutex1 : iMod.getSlots()) {
                            if (mutex0.equals(mutex1)) return false;
                        }
            }

        return true;
    }

    public static <T> T eval(T base, ItemStack stack, String key, Object parent, int cfg) {
        if (stack == null || stack.isEmpty()) return base;

        for (int i : ItemGunBaseNT.getValueIntArray(stack, KEY_MOD_LIST + cfg)) {
            IWeaponMod mod = idToMod.get(i);
            if (mod != null) base = mod.eval(base, stack, key, parent);
        }

        return base;
    }

    public static class WeaponModDefinition {

        public HashMap<Item, IWeaponMod> modByGun = new HashMap<>();
        public Supplier<ItemStack> stack;

        public WeaponModDefinition(Supplier<ItemStack> stack, Item key) {
            this.stack = stack;
            stackToMod.put(key, this);
        }

        public WeaponModDefinition addMod(Item gun, IWeaponMod mod) {
            if (gun instanceof ItemGunBaseNT weapon && !weapon.recognizedMods.contains(stack))
                weapon.recognizedMods.add(stack);
            modByGun.put(gun, mod);
            modToStack.put(mod, stack);
            return this;
        }

        public WeaponModDefinition addMod(Item[] guns, IWeaponMod mod) {
            for (Item gun : guns) addMod(gun, mod);
            return this;
        }

        public WeaponModDefinition addDefault(IWeaponMod mod) {
            modByGun.put(null, mod);
            modToStack.put(mod, stack);
            return this;
        }
    }
}
