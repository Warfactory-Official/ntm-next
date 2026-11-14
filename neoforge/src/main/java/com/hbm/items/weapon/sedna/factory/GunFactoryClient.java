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

import com.hbm.client.render.BeamRenderer;
import com.hbm.client.render.BulletMK4Renderer;
import com.hbm.client.weapon.GunClientProperties;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.grenade.GrenadeFillingItem;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryPile;
import com.hbm.items.weapon.sedna.hud.GunHudWidget;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.AberratorItemRenderer;
import com.hbm.render.item.weapon.sedna.Am180ItemRenderer;
import com.hbm.render.item.weapon.sedna.AmatItemRenderer;
import com.hbm.render.item.weapon.sedna.AtlasItemRenderer;
import com.hbm.render.item.weapon.sedna.BolterItemRenderer;
import com.hbm.render.item.weapon.sedna.CarbineItemRenderer;
import com.hbm.render.item.weapon.sedna.ChargeThrowerItemRenderer;
import com.hbm.render.item.weapon.sedna.ChemthrowerItemRenderer;
import com.hbm.render.item.weapon.sedna.CoilgunItemRenderer;
import com.hbm.render.item.weapon.sedna.CongoLakeItemRenderer;
import com.hbm.render.item.weapon.sedna.DANIItemRenderer;
import com.hbm.render.item.weapon.sedna.DebugItemRenderer;
import com.hbm.render.item.weapon.sedna.DoubleBarrelItemRenderer;
import com.hbm.render.item.weapon.sedna.DrillItemRenderer;
import com.hbm.render.item.weapon.sedna.EOTTItemRenderer;
import com.hbm.render.item.weapon.sedna.FatManItemRenderer;
import com.hbm.render.item.weapon.sedna.FlamerItemRenderer;
import com.hbm.render.item.weapon.sedna.FlaregunItemRenderer;
import com.hbm.render.item.weapon.sedna.FollyItemRenderer;
import com.hbm.render.item.weapon.sedna.G3ItemRenderer;
import com.hbm.render.item.weapon.sedna.GreasegunItemRenderer;
import com.hbm.render.item.weapon.sedna.HangmanItemRenderer;
import com.hbm.render.item.weapon.sedna.HeavyRevolverItemRenderer;
import com.hbm.render.item.weapon.sedna.HenryItemRenderer;
import com.hbm.render.item.weapon.sedna.LagItemRenderer;
import com.hbm.render.item.weapon.sedna.LaserPistolItemRenderer;
import com.hbm.render.item.weapon.sedna.LasrifleItemRenderer;
import com.hbm.render.item.weapon.sedna.LiberatorItemRenderer;
import com.hbm.render.item.weapon.sedna.M2ItemRenderer;
import com.hbm.render.item.weapon.sedna.MAS36ItemRenderer;
import com.hbm.render.item.weapon.sedna.MK108ItemRenderer;
import com.hbm.render.item.weapon.sedna.MareslegAkimboItemRenderer;
import com.hbm.render.item.weapon.sedna.MareslegItemRenderer;
import com.hbm.render.item.weapon.sedna.MinigunDualItemRenderer;
import com.hbm.render.item.weapon.sedna.MinigunItemRenderer;
import com.hbm.render.item.weapon.sedna.MissileLauncherItemRenderer;
import com.hbm.render.item.weapon.sedna.NI4NIItemRenderer;
import com.hbm.render.item.weapon.sedna.PAMeleeItemRenderer;
import com.hbm.render.item.weapon.sedna.PanzerschreckItemRenderer;
import com.hbm.render.item.weapon.sedna.PepperboxItemRenderer;
import com.hbm.render.item.weapon.sedna.QuadroItemRenderer;
import com.hbm.render.item.weapon.sedna.SPAS12ItemRenderer;
import com.hbm.render.item.weapon.sedna.SexyItemRenderer;
import com.hbm.render.item.weapon.sedna.ShredderItemRenderer;
import com.hbm.render.item.weapon.sedna.StarFAkimboItemRenderer;
import com.hbm.render.item.weapon.sedna.StarFItemRenderer;
import com.hbm.render.item.weapon.sedna.Stg77ItemRenderer;
import com.hbm.render.item.weapon.sedna.StingerItemRenderer;
import com.hbm.render.item.weapon.sedna.TauItemRenderer;
import com.hbm.render.item.weapon.sedna.TeslaCannonItemRenderer;
import com.hbm.render.item.weapon.sedna.UziAkimboItemRenderer;
import com.hbm.render.item.weapon.sedna.UziItemRenderer;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import java.util.List;

public class GunFactoryClient {

    public static void init() {
        WeaponItemRenderer.register(ModItems.GUN_DEBUG::get, new DebugItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_LIGHT_REVOLVER::get,
                new AtlasItemRenderer(ResourceManager.bio_revolver_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_LIGHT_REVOLVER_ATLAS::get,
                new AtlasItemRenderer(ResourceManager.bio_revolver_atlas_tex));
        WeaponItemRenderer.register(ModItems.GUN_LIGHT_REVOLVER_DANI::get, new DANIItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_HENRY::get, new HenryItemRenderer(ResourceManager.henry_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_HENRY_LINCOLN::get,
                new HenryItemRenderer(ResourceManager.henry_lincoln_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_HEAVY_REVOLVER::get,
                new HeavyRevolverItemRenderer(ResourceManager.heavy_revolver_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_HEAVY_REVOLVER_LILMAC::get,
                new HeavyRevolverItemRenderer(ResourceManager.lilmac_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_HEAVY_REVOLVER_PROTEGE::get,
                new HeavyRevolverItemRenderer(ResourceManager.heavy_revolver_protege_tex));
        WeaponItemRenderer.register(ModItems.GUN_HANGMAN::get, new HangmanItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_MARESLEG::get, new MareslegItemRenderer(ResourceManager.maresleg_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_MARESLEG_AKIMBO::get, new MareslegAkimboItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_MARESLEG_BROKEN::get,
                new MareslegItemRenderer(ResourceManager.maresleg_broken_tex));
        WeaponItemRenderer.register(ModItems.GUN_LIBERATOR::get, new LiberatorItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_SPAS12::get, new SPAS12ItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_AUTOSHOTGUN::get,
                new ShredderItemRenderer(ResourceManager.shredder_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_AUTOSHOTGUN_SHREDDER::get,
                new ShredderItemRenderer(ResourceManager.shredder_orig_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_AUTOSHOTGUN_SEXY::get, new SexyItemRenderer(ResourceManager.sexy_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_AUTOSHOTGUN_HERETIC::get,
                new SexyItemRenderer(ResourceManager.heretic_tex));
        WeaponItemRenderer.register(ModItems.GUN_GREASEGUN::get, new GreasegunItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_UZI::get, new UziItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_UZI_AKIMBO::get, new UziAkimboItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_CARBINE::get, new CarbineItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_MINIGUN::get, new MinigunItemRenderer(ResourceManager.minigun_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_MINIGUN_LACUNAE::get,
                new MinigunItemRenderer(ResourceManager.minigun_lacunae_tex));
        WeaponItemRenderer.register(ModItems.GUN_MINIGUN_DUAL::get, new MinigunDualItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_MAS36::get, new MAS36ItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_PANZERSCHRECK::get, new PanzerschreckItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_STINGER::get, new StingerItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_QUADRO::get, new QuadroItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_MISSILE_LAUNCHER::get, new MissileLauncherItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_TAU::get, new TauItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_COILGUN::get, new CoilgunItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_PEPPERBOX::get, new PepperboxItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_AM180::get, new Am180ItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_STAR_F::get, new StarFItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_STAR_F_AKIMBO::get, new StarFAkimboItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_BOLTER::get, new BolterItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_LAG::get, new LagItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_G3::get, new G3ItemRenderer(ResourceManager.g3_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_G3_ZEBRA::get, new G3ItemRenderer(ResourceManager.g3_zebra_tex));
        WeaponItemRenderer.register(ModItems.GUN_STG77::get, new Stg77ItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_AMAT::get, new AmatItemRenderer(ResourceManager.amat_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_AMAT_SUBTLETY::get,
                new AmatItemRenderer(ResourceManager.amat_subtlety_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_AMAT_PENANCE::get,
                new AmatItemRenderer(ResourceManager.amat_penance_tex));
        WeaponItemRenderer.register(ModItems.GUN_M2::get, new M2ItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_FLAREGUN::get, new FlaregunItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_CONGOLAKE::get, new CongoLakeItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_MK108::get, new MK108ItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_FATMAN::get, new FatManItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_FLAMER::get, new FlamerItemRenderer(ResourceManager.flamethrower_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_FLAMER_TOPAZ::get,
                new FlamerItemRenderer(ResourceManager.flamethrower_topaz_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_FLAMER_DAYBREAKER::get,
                new FlamerItemRenderer(ResourceManager.flamethrower_daybreaker_tex));
        WeaponItemRenderer.register(ModItems.GUN_CHEMTHROWER::get, new ChemthrowerItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_TESLA_CANNON::get, new TeslaCannonItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_LASER_PISTOL::get,
                new LaserPistolItemRenderer(ResourceManager.laser_pistol_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_LASER_PISTOL_PEW_PEW::get,
                new LaserPistolItemRenderer(ResourceManager.laser_pistol_pew_pew_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_LASER_PISTOL_MORNING_GLORY::get,
                new LaserPistolItemRenderer(ResourceManager.laser_pistol_morning_glory_tex));
        WeaponItemRenderer.register(ModItems.GUN_LASRIFLE::get, new LasrifleItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_CHARGE_THROWER::get, new ChargeThrowerItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_DRILL::get, new DrillItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_PA_MELEE::get, new PAMeleeItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_FOLLY::get, new FollyItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_ABERRATOR::get, new AberratorItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_ABERRATOR_EOTT::get, new EOTTItemRenderer());
        WeaponItemRenderer.register(ModItems.GUN_N_I_4_N_I::get, new NI4NIItemRenderer());
        WeaponItemRenderer.register(
                ModItems.GUN_DOUBLE_BARREL::get,
                new DoubleBarrelItemRenderer(ResourceManager.double_barrel_tex));
        WeaponItemRenderer.register(
                ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON::get,
                new DoubleBarrelItemRenderer(ResourceManager.double_barrel_sacred_dragon_tex));

        List<GunHudWidget> standard =
                List.of(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        List<GunHudWidget> mirrored =
                List.of(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        List<GunHudWidget> ammo = List.of(LegoClient.HUD_COMPONENT_AMMO);
        List<GunHudWidget> fuel =
                List.of(
                        LegoClient.HUD_COMPONENT_DURABILITY,
                        LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        for (var gun :
                List.of(
                        ModItems.GUN_LIGHT_REVOLVER,
                        ModItems.GUN_LIGHT_REVOLVER_ATLAS,
                        ModItems.GUN_HENRY,
                        ModItems.GUN_HENRY_LINCOLN,
                        ModItems.GUN_HEAVY_REVOLVER,
                        ModItems.GUN_HEAVY_REVOLVER_LILMAC,
                        ModItems.GUN_HEAVY_REVOLVER_PROTEGE,
                        ModItems.GUN_HANGMAN,
                        ModItems.GUN_MARESLEG,
                        ModItems.GUN_LIBERATOR,
                        ModItems.GUN_SPAS12,
                        ModItems.GUN_AUTOSHOTGUN,
                        ModItems.GUN_AUTOSHOTGUN_SHREDDER,
                        ModItems.GUN_AUTOSHOTGUN_SEXY,
                        ModItems.GUN_GREASEGUN,
                        ModItems.GUN_UZI,
                        ModItems.GUN_CARBINE,
                        ModItems.GUN_MINIGUN,
                        ModItems.GUN_MINIGUN_LACUNAE,
                        ModItems.GUN_MAS36,
                        ModItems.GUN_PANZERSCHRECK,
                        ModItems.GUN_STINGER,
                        ModItems.GUN_QUADRO,
                        ModItems.GUN_MISSILE_LAUNCHER,
                        ModItems.GUN_TAU,
                        ModItems.GUN_COILGUN,
                        ModItems.GUN_PEPPERBOX,
                        ModItems.GUN_AM180,
                        ModItems.GUN_STAR_F,
                        ModItems.GUN_BOLTER,
                        ModItems.GUN_LAG,
                        ModItems.GUN_G3,
                        ModItems.GUN_G3_ZEBRA,
                        ModItems.GUN_STG77,
                        ModItems.GUN_AMAT,
                        ModItems.GUN_AMAT_SUBTLETY,
                        ModItems.GUN_AMAT_PENANCE,
                        ModItems.GUN_M2,
                        ModItems.GUN_FLAREGUN,
                        ModItems.GUN_CONGOLAKE,
                        ModItems.GUN_MK108,
                        ModItems.GUN_FATMAN,
                        ModItems.GUN_CHEMTHROWER,
                        ModItems.GUN_TESLA_CANNON,
                        ModItems.GUN_LASER_PISTOL,
                        ModItems.GUN_LASER_PISTOL_PEW_PEW,
                        ModItems.GUN_LASER_PISTOL_MORNING_GLORY,
                        ModItems.GUN_LASRIFLE,
                        ModItems.GUN_FIREEXT,
                        ModItems.GUN_CHARGE_THROWER,
                        ModItems.GUN_DOUBLE_BARREL,
                        ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON))
            GunClientProperties.register(gun.get(), standard);
        for (var gun :
                List.of(
                        ModItems.GUN_LIGHT_REVOLVER_DANI,
                        ModItems.GUN_MARESLEG_AKIMBO,
                        ModItems.GUN_UZI_AKIMBO,
                        ModItems.GUN_STAR_F_AKIMBO,
                        ModItems.GUN_MINIGUN_DUAL))
            GunClientProperties.register(gun.get(), mirrored, standard);
        for (var gun :
                List.of(
                        ModItems.GUN_MARESLEG_BROKEN,
                        ModItems.GUN_AUTOSHOTGUN_HERETIC,
                        ModItems.GUN_FOLLY,
                        ModItems.GUN_ABERRATOR)) GunClientProperties.register(gun.get(), ammo);
        for (var gun :
                List.of(
                        ModItems.GUN_FLAMER,
                        ModItems.GUN_FLAMER_TOPAZ,
                        ModItems.GUN_FLAMER_DAYBREAKER))
            GunClientProperties.register(gun.get(), fuel);
        GunClientProperties.register(
                ModItems.GUN_DEBUG.get(),
                List.of(
                        LegoClient.HUD_COMPONENT_DURABILITY,
                        LegoClient.HUD_COMPONENT_AMMO,
                        LegoClient.HUD_COMPONENT_AMMO_SECOND));
        GunClientProperties.register(
                ModItems.GUN_ABERRATOR_EOTT.get(),
                List.of(LegoClient.HUD_COMPONENT_AMMO_MIRROR),
                ammo);

        BulletMK4Renderer.setRenderer(GunFactory.ammo_debug, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryPile.debris, LegoClient.RENDER_GRAPHITE);
        BulletMK4Renderer.setRenderer(
                GunFactory.ammo_debug_shot, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_bp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory357.m357_express, LegoClient.RENDER_EXPRESS_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.nuke_standard, LegoClient.RENDER_NUKE);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.nuke_demo, LegoClient.RENDER_NUKE);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.nuke_high, LegoClient.RENDER_NUKE);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.nuke_tots, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.nuke_hive, LegoClient.RENDER_HIVE);
        BulletMK4Renderer.setRenderer(XFactoryCatapult.cluster_submunition, LegoClient.RENDER_BOMB);
        BulletMK4Renderer.setRenderer(
                XFactoryCatapult.nuke_balefire, LegoClient.RENDER_NUKE_BALEFIRE);
        BulletMK4Renderer.setRenderer(XFactoryTurret.shell_normal, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactoryTurret.shell_explosive, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactoryTurret.shell_ap, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactoryTurret.shell_du, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactoryTurret.shell_w9, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(
                GrenadeFillingItem.fragmentation, LegoClient.RENDER_FRAGMENTATION);
        BulletMK4Renderer.setRenderer(GrenadeFillingItem.pellets, LegoClient.RENDER_FRAGMENTATION);
        BulletMK4Renderer.setRenderer(
                GrenadeFillingItem.pellets_heavy, LegoClient.RENDER_FRAGMENTATION);
        BeamRenderer.setRenderer(GrenadeFillingItem.laser, LegoClient.RENDER_LASER_RED);
        BulletMK4Renderer.setRenderer(XFactory40mm.g26_flare, LegoClient.RENDER_FLARE);
        BulletMK4Renderer.setRenderer(
                XFactory40mm.g26_flare_supply, LegoClient.RENDER_FLARE_SUPPLY);
        BulletMK4Renderer.setRenderer(
                XFactory40mm.g26_flare_weapon, LegoClient.RENDER_FLARE_WEAPON);
        BulletMK4Renderer.setRenderer(XFactory40mm.g40_he, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactory40mm.g40_heat, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactory40mm.g40_demo, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactory40mm.g40_inc, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactory40mm.g40_phosphorus, LegoClient.RENDER_GRENADE);
        BulletMK4Renderer.setRenderer(XFactory44.m44_bp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory44.m44_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory44.m44_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory44.m44_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory44.m44_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory44.m44_express, LegoClient.RENDER_EXPRESS_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory44.m44_equestrian_pip, LegoClient.RENDER_LEGENDARY_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory44.m44_equestrian_mn7, LegoClient.RENDER_LEGENDARY_BULLET);
        BulletMK4Renderer.setRenderer(XFactory9mm.p9_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory9mm.p9_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory9mm.p9_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory9mm.p9_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_bp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_bp_magnum, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_bp_slug, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_slug, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_flechette, LegoClient.RENDER_FLECHETTE_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_magnum, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_explosive, LegoClient.RENDER_EXPRESS_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_phosphorus, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_equestrian_bj, LegoClient.RENDER_LEGENDARY_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_equestrian_tkr, LegoClient.RENDER_LEGENDARY_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_sub, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_sub_slug, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_sub_flechette, LegoClient.RENDER_FLECHETTE_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_sub_magnum, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactory12ga.g12_sub_explosive, LegoClient.RENDER_EXPRESS_BULLET);
        BulletMK4Renderer.setRenderer(XFactory12ga.g12_sub_phosphorus, LegoClient.RENDER_AP_BULLET);
        for (BulletConfig rocket : XFactoryRocket.rocket_rpzb)
            BulletMK4Renderer.setRenderer(rocket, LegoClient.RENDER_RPZB);
        for (BulletConfig rocket : XFactoryRocket.rocket_qd)
            BulletMK4Renderer.setRenderer(rocket, LegoClient.RENDER_QD);
        for (BulletConfig rocket : XFactoryRocket.rocket_ml)
            BulletMK4Renderer.setRenderer(rocket, LegoClient.RENDER_ML);
        for (BulletConfig rocket : XFactoryRocket.rocket_ncrpa_steer)
            BulletMK4Renderer.setRenderer(rocket, LegoClient.RENDER_RPZB);
        for (BulletConfig rocket : XFactoryRocket.rocket_ncrpa)
            BulletMK4Renderer.setRenderer(rocket, LegoClient.RENDER_RPZB);
        BeamRenderer.setRenderer(XFactoryAccelerator.ni4ni_arc, LegoClient.RENDER_NI4NI_BOLT);
        BeamRenderer.setRenderer(XFactoryAccelerator.tau_uranium, LegoClient.RENDER_TAU);
        BeamRenderer.setRenderer(
                XFactoryAccelerator.tau_uranium_charge, LegoClient.RENDER_TAU_CHARGE);
        BulletMK4Renderer.setRenderer(
                XFactoryAccelerator.coil_tungsten, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(
                XFactoryAccelerator.coil_ferrouranium, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryBlackPowder.stone, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryBlackPowder.flint, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryBlackPowder.iron, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryBlackPowder.shot, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory22lr.p22_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory22lr.p22_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory22lr.p22_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory22lr.p22_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory45.p45_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory45.p45_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory45.p45_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory45.p45_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory45.p45_du, LegoClient.RENDER_DU_BULLET);
        BulletMK4Renderer.setRenderer(XFactory75Bolt.b75, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory75Bolt.b75_inc, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory75Bolt.b75_exp, LegoClient.RENDER_EXPRESS_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_inc_sp, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_inc_fmj, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_inc_jhp, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory556mm.r556_inc_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_du, LegoClient.RENDER_DU_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_he, LegoClient.RENDER_HE_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_sm, LegoClient.RENDER_SM_BULLET);
        BulletMK4Renderer.setRenderer(XFactory50.bmg50_black, LegoClient.RENDER_BLACK_BULLET);
        BulletMK4Renderer.setRenderer(XFactoryFolly.folly_nuke, LegoClient.RENDER_BIG_NUKE);
        BulletMK4Renderer.setRenderer(XFactoryTool.ct_hook, LegoClient.RENDER_CT_HOOK);
        BeamRenderer.setRenderer(XFactoryFolly.folly_sm, LegoClient.RENDER_FOLLY);
        BeamRenderer.setRenderer(XFactory35800.p35800, LegoClient.RENDER_CRACKLE);
        BeamRenderer.setRenderer(XFactory35800.p35800_bl, LegoClient.RENDER_BLACK_LIGHTNING);
        BulletMK4Renderer.setRenderer(XFactoryTool.ct_mortar, LegoClient.RENDER_CT_MORTAR);
        BulletMK4Renderer.setRenderer(
                XFactoryTool.ct_mortar_charge, LegoClient.RENDER_CT_MORTAR_CHARGE);
        BulletMK4Renderer.setRenderer(XFactory10ga.g10, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory10ga.g10_shrapnel, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory10ga.g10_du, LegoClient.RENDER_DU_BULLET);
        BulletMK4Renderer.setRenderer(XFactory10ga.g10_slug, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory10ga.g10_explosive, LegoClient.RENDER_HE_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_sp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_fmj, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_jhp, LegoClient.RENDER_STANDARD_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_ap, LegoClient.RENDER_AP_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_du, LegoClient.RENDER_DU_BULLET);
        BulletMK4Renderer.setRenderer(XFactory762mm.r762_he, LegoClient.RENDER_HE_BULLET);

        BeamRenderer.setRenderer(XFactoryEnergy.energy_tesla, LegoClient.RENDER_LIGHTNING);
        BeamRenderer.setRenderer(
                XFactoryEnergy.energy_tesla_overcharge, LegoClient.RENDER_LIGHTNING);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_tesla_ir, LegoClient.RENDER_LIGHTNING);
        BeamRenderer.setRenderer(
                XFactoryEnergy.energy_tesla_ir_sub, LegoClient.RENDER_LIGHTNING_SUB);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_las, LegoClient.RENDER_LASER_RED);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_las_overcharge, LegoClient.RENDER_LASER_RED);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_las_ir, LegoClient.RENDER_LASER_RED);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_emerald, LegoClient.RENDER_LASER_EMERALD);
        BeamRenderer.setRenderer(
                XFactoryEnergy.energy_emerald_overcharge, LegoClient.RENDER_LASER_EMERALD);
        BeamRenderer.setRenderer(XFactoryEnergy.energy_emerald_ir, LegoClient.RENDER_LASER_EMERALD);
        BeamRenderer.setRenderer(XFactory762mm.energy_lacunae, LegoClient.RENDER_LASER_PURPLE);
        BeamRenderer.setRenderer(
                XFactory762mm.energy_lacunae_overcharge, LegoClient.RENDER_LASER_PURPLE);
        BeamRenderer.setRenderer(XFactory762mm.energy_lacunae_ir, LegoClient.RENDER_LASER_PURPLE);
        BeamRenderer.setRenderer(XFactory12ga.g12_shredder, LegoClient.RENDER_LASER_CYAN);
        BeamRenderer.setRenderer(XFactory12ga.g12_shredder_slug, LegoClient.RENDER_LASER_CYAN);
        BeamRenderer.setRenderer(XFactory12ga.g12_shredder_flechette, LegoClient.RENDER_LASER_CYAN);
        BeamRenderer.setRenderer(XFactory12ga.g12_shredder_magnum, LegoClient.RENDER_LASER_CYAN);
        BeamRenderer.setRenderer(XFactory12ga.g12_shredder_explosive, LegoClient.RENDER_LASER_CYAN);
        BeamRenderer.setRenderer(
                XFactory12ga.g12_shredder_phosphorus, LegoClient.RENDER_LASER_CYAN);
    }
}
