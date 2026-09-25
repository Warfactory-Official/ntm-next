// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.factory;

import com.hbm.client.render.RenderBeam;
import com.hbm.client.render.RenderBulletMK4;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderAberrator;
import com.hbm.render.item.weapon.sedna.ItemRenderAm180;
import com.hbm.render.item.weapon.sedna.ItemRenderAmat;
import com.hbm.render.item.weapon.sedna.ItemRenderAtlas;
import com.hbm.render.item.weapon.sedna.ItemRenderBolter;
import com.hbm.render.item.weapon.sedna.ItemRenderCarbine;
import com.hbm.render.item.weapon.sedna.ItemRenderChargeThrower;
import com.hbm.render.item.weapon.sedna.ItemRenderChemthrower;
import com.hbm.render.item.weapon.sedna.ItemRenderCoilgun;
import com.hbm.render.item.weapon.sedna.ItemRenderCongoLake;
import com.hbm.render.item.weapon.sedna.ItemRenderDANI;
import com.hbm.render.item.weapon.sedna.ItemRenderDebug;
import com.hbm.render.item.weapon.sedna.ItemRenderDoubleBarrel;
import com.hbm.render.item.weapon.sedna.ItemRenderDrill;
import com.hbm.render.item.weapon.sedna.ItemRenderEOTT;
import com.hbm.render.item.weapon.sedna.ItemRenderFatMan;
import com.hbm.render.item.weapon.sedna.ItemRenderFlamer;
import com.hbm.render.item.weapon.sedna.ItemRenderFlaregun;
import com.hbm.render.item.weapon.sedna.ItemRenderFolly;
import com.hbm.render.item.weapon.sedna.ItemRenderG3;
import com.hbm.render.item.weapon.sedna.ItemRenderGreasegun;
import com.hbm.render.item.weapon.sedna.ItemRenderHangman;
import com.hbm.render.item.weapon.sedna.ItemRenderHeavyRevolver;
import com.hbm.render.item.weapon.sedna.ItemRenderHenry;
import com.hbm.render.item.weapon.sedna.ItemRenderLag;
import com.hbm.render.item.weapon.sedna.ItemRenderLaserPistol;
import com.hbm.render.item.weapon.sedna.ItemRenderLasrifle;
import com.hbm.render.item.weapon.sedna.ItemRenderLiberator;
import com.hbm.render.item.weapon.sedna.ItemRenderM2;
import com.hbm.render.item.weapon.sedna.ItemRenderMAS36;
import com.hbm.render.item.weapon.sedna.ItemRenderMK108;
import com.hbm.render.item.weapon.sedna.ItemRenderMaresleg;
import com.hbm.render.item.weapon.sedna.ItemRenderMareslegAkimbo;
import com.hbm.render.item.weapon.sedna.ItemRenderMinigun;
import com.hbm.render.item.weapon.sedna.ItemRenderMinigunDual;
import com.hbm.render.item.weapon.sedna.ItemRenderMissileLauncher;
import com.hbm.render.item.weapon.sedna.ItemRenderNI4NI;
import com.hbm.render.item.weapon.sedna.ItemRenderPAMelee;
import com.hbm.render.item.weapon.sedna.ItemRenderPanzerschreck;
import com.hbm.render.item.weapon.sedna.ItemRenderPepperbox;
import com.hbm.render.item.weapon.sedna.ItemRenderQuadro;
import com.hbm.render.item.weapon.sedna.ItemRenderSPAS12;
import com.hbm.render.item.weapon.sedna.ItemRenderSexy;
import com.hbm.render.item.weapon.sedna.ItemRenderShredder;
import com.hbm.render.item.weapon.sedna.ItemRenderStarF;
import com.hbm.render.item.weapon.sedna.ItemRenderStarFAkimbo;
import com.hbm.render.item.weapon.sedna.ItemRenderStg77;
import com.hbm.render.item.weapon.sedna.ItemRenderStinger;
import com.hbm.render.item.weapon.sedna.ItemRenderTau;
import com.hbm.render.item.weapon.sedna.ItemRenderTeslaCannon;
import com.hbm.render.item.weapon.sedna.ItemRenderUzi;
import com.hbm.render.item.weapon.sedna.ItemRenderUziAkimbo;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;

public class GunFactoryClient {

    public static void init() {
        ItemRenderWeaponBase.register(ModItems.GUN_DEBUG::get, new ItemRenderDebug());
        ItemRenderWeaponBase.register(
                ModItems.GUN_LIGHT_REVOLVER::get,
                new ItemRenderAtlas(ResourceManager.bio_revolver_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_LIGHT_REVOLVER_ATLAS::get,
                new ItemRenderAtlas(ResourceManager.bio_revolver_atlas_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_LIGHT_REVOLVER_DANI::get, new ItemRenderDANI());
        ItemRenderWeaponBase.register(
                ModItems.GUN_HENRY::get, new ItemRenderHenry(ResourceManager.henry_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_HENRY_LINCOLN::get,
                new ItemRenderHenry(ResourceManager.henry_lincoln_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_HEAVY_REVOLVER::get,
                new ItemRenderHeavyRevolver(ResourceManager.heavy_revolver_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_HEAVY_REVOLVER_LILMAC::get,
                new ItemRenderHeavyRevolver(ResourceManager.lilmac_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_HEAVY_REVOLVER_PROTEGE::get,
                new ItemRenderHeavyRevolver(ResourceManager.heavy_revolver_protege_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_HANGMAN::get, new ItemRenderHangman());
        ItemRenderWeaponBase.register(
                ModItems.GUN_MARESLEG::get, new ItemRenderMaresleg(ResourceManager.maresleg_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_MARESLEG_AKIMBO::get, new ItemRenderMareslegAkimbo());
        ItemRenderWeaponBase.register(
                ModItems.GUN_MARESLEG_BROKEN::get,
                new ItemRenderMaresleg(ResourceManager.maresleg_broken_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_LIBERATOR::get, new ItemRenderLiberator());
        ItemRenderWeaponBase.register(ModItems.GUN_SPAS12::get, new ItemRenderSPAS12());
        ItemRenderWeaponBase.register(
                ModItems.GUN_AUTOSHOTGUN::get,
                new ItemRenderShredder(ResourceManager.shredder_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_AUTOSHOTGUN_SHREDDER::get,
                new ItemRenderShredder(ResourceManager.shredder_orig_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_AUTOSHOTGUN_SEXY::get, new ItemRenderSexy(ResourceManager.sexy_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_AUTOSHOTGUN_HERETIC::get,
                new ItemRenderSexy(ResourceManager.heretic_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_GREASEGUN::get, new ItemRenderGreasegun());
        ItemRenderWeaponBase.register(ModItems.GUN_UZI::get, new ItemRenderUzi());
        ItemRenderWeaponBase.register(ModItems.GUN_UZI_AKIMBO::get, new ItemRenderUziAkimbo());
        ItemRenderWeaponBase.register(ModItems.GUN_CARBINE::get, new ItemRenderCarbine());
        ItemRenderWeaponBase.register(
                ModItems.GUN_MINIGUN::get, new ItemRenderMinigun(ResourceManager.minigun_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_MINIGUN_LACUNAE::get,
                new ItemRenderMinigun(ResourceManager.minigun_lacunae_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_MINIGUN_DUAL::get, new ItemRenderMinigunDual());
        ItemRenderWeaponBase.register(ModItems.GUN_MAS36::get, new ItemRenderMAS36());
        ItemRenderWeaponBase.register(
                ModItems.GUN_PANZERSCHRECK::get, new ItemRenderPanzerschreck());
        ItemRenderWeaponBase.register(ModItems.GUN_STINGER::get, new ItemRenderStinger());
        ItemRenderWeaponBase.register(ModItems.GUN_QUADRO::get, new ItemRenderQuadro());
        ItemRenderWeaponBase.register(
                ModItems.GUN_MISSILE_LAUNCHER::get, new ItemRenderMissileLauncher());
        ItemRenderWeaponBase.register(ModItems.GUN_TAU::get, new ItemRenderTau());
        ItemRenderWeaponBase.register(ModItems.GUN_COILGUN::get, new ItemRenderCoilgun());
        ItemRenderWeaponBase.register(ModItems.GUN_PEPPERBOX::get, new ItemRenderPepperbox());
        ItemRenderWeaponBase.register(ModItems.GUN_AM180::get, new ItemRenderAm180());
        ItemRenderWeaponBase.register(ModItems.GUN_STAR_F::get, new ItemRenderStarF());
        ItemRenderWeaponBase.register(ModItems.GUN_STAR_F_AKIMBO::get, new ItemRenderStarFAkimbo());
        ItemRenderWeaponBase.register(ModItems.GUN_BOLTER::get, new ItemRenderBolter());
        ItemRenderWeaponBase.register(ModItems.GUN_LAG::get, new ItemRenderLag());
        ItemRenderWeaponBase.register(
                ModItems.GUN_G3::get, new ItemRenderG3(ResourceManager.g3_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_G3_ZEBRA::get, new ItemRenderG3(ResourceManager.g3_zebra_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_STG77::get, new ItemRenderStg77());
        ItemRenderWeaponBase.register(
                ModItems.GUN_AMAT::get, new ItemRenderAmat(ResourceManager.amat_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_AMAT_SUBTLETY::get,
                new ItemRenderAmat(ResourceManager.amat_subtlety_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_AMAT_PENANCE::get,
                new ItemRenderAmat(ResourceManager.amat_penance_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_M2::get, new ItemRenderM2());
        ItemRenderWeaponBase.register(ModItems.GUN_FLAREGUN::get, new ItemRenderFlaregun());
        ItemRenderWeaponBase.register(ModItems.GUN_CONGOLAKE::get, new ItemRenderCongoLake());
        ItemRenderWeaponBase.register(ModItems.GUN_MK108::get, new ItemRenderMK108());

        ItemRenderWeaponBase.register(ModItems.GUN_FATMAN::get, new ItemRenderFatMan());
        ItemRenderWeaponBase.register(
                ModItems.GUN_FLAMER::get, new ItemRenderFlamer(ResourceManager.flamethrower_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_FLAMER_TOPAZ::get,
                new ItemRenderFlamer(ResourceManager.flamethrower_topaz_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_FLAMER_DAYBREAKER::get,
                new ItemRenderFlamer(ResourceManager.flamethrower_daybreaker_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_CHEMTHROWER::get, new ItemRenderChemthrower());
        ItemRenderWeaponBase.register(ModItems.GUN_TESLA_CANNON::get, new ItemRenderTeslaCannon());
        ItemRenderWeaponBase.register(
                ModItems.GUN_LASER_PISTOL::get,
                new ItemRenderLaserPistol(ResourceManager.laser_pistol_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_LASER_PISTOL_PEW_PEW::get,
                new ItemRenderLaserPistol(ResourceManager.laser_pistol_pew_pew_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_LASER_PISTOL_MORNING_GLORY::get,
                new ItemRenderLaserPistol(ResourceManager.laser_pistol_morning_glory_tex));
        ItemRenderWeaponBase.register(ModItems.GUN_LASRIFLE::get, new ItemRenderLasrifle());

        ItemRenderWeaponBase.register(
                ModItems.GUN_CHARGE_THROWER::get, new ItemRenderChargeThrower());

        ItemRenderWeaponBase.register(ModItems.GUN_DRILL::get, new ItemRenderDrill());

        ItemRenderWeaponBase.register(ModItems.GUN_PA_MELEE::get, new ItemRenderPAMelee());
        ItemRenderWeaponBase.register(ModItems.GUN_FOLLY::get, new ItemRenderFolly());
        ItemRenderWeaponBase.register(ModItems.GUN_ABERRATOR::get, new ItemRenderAberrator());
        ItemRenderWeaponBase.register(ModItems.GUN_ABERRATOR_EOTT::get, new ItemRenderEOTT());
        ItemRenderWeaponBase.register(ModItems.GUN_N_I_4_N_I::get, new ItemRenderNI4NI());
        ItemRenderWeaponBase.register(
                ModItems.GUN_DOUBLE_BARREL::get,
                new ItemRenderDoubleBarrel(ResourceManager.double_barrel_tex));
        ItemRenderWeaponBase.register(
                ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON::get,
                new ItemRenderDoubleBarrel(ResourceManager.double_barrel_sacred_dragon_tex));

        ModItems.GUN_LIGHT_REVOLVER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LIGHT_REVOLVER_ATLAS
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LIGHT_REVOLVER_DANI
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_LIGHT_REVOLVER_DANI
                .get()
                .getConfig(null, 1)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HENRY
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HENRY_LINCOLN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HEAVY_REVOLVER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HEAVY_REVOLVER_LILMAC
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HEAVY_REVOLVER_PROTEGE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_HANGMAN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MARESLEG
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MARESLEG_AKIMBO
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_MARESLEG_AKIMBO
                .get()
                .getConfig(null, 1)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MARESLEG_BROKEN.get().getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LIBERATOR
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_SPAS12
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AUTOSHOTGUN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AUTOSHOTGUN_SHREDDER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AUTOSHOTGUN_SEXY
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AUTOSHOTGUN_HERETIC
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_GREASEGUN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_UZI
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_UZI_AKIMBO
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_UZI_AKIMBO
                .get()
                .getConfig(null, 1)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_CARBINE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MINIGUN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MINIGUN_LACUNAE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MAS36
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_PANZERSCHRECK
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_STINGER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_QUADRO
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MISSILE_LAUNCHER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_TAU
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_COILGUN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_PEPPERBOX
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AM180
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_STAR_F
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_STAR_F_AKIMBO
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_STAR_F_AKIMBO
                .get()
                .getConfig(null, 1)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_BOLTER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LAG
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_G3
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_G3_ZEBRA
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_STG77
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AMAT
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AMAT_SUBTLETY
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_AMAT_PENANCE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_M2
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_FLAREGUN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_CONGOLAKE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MK108
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_FATMAN
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_FLAMER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ModItems.GUN_FLAMER_TOPAZ
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ModItems.GUN_FLAMER_DAYBREAKER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO_NOCOUNTER);
        ModItems.GUN_CHEMTHROWER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_TESLA_CANNON
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LASER_PISTOL
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LASER_PISTOL_PEW_PEW
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LASER_PISTOL_MORNING_GLORY
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_LASRIFLE
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_DEBUG
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY,
                        LegoClient.HUD_COMPONENT_AMMO,
                        LegoClient.HUD_COMPONENT_AMMO_SECOND);
        ModItems.GUN_FIREEXT
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_CHARGE_THROWER
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_FOLLY.get().getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_ABERRATOR.get().getConfig(null, 0).hud(LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_ABERRATOR_EOTT
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_ABERRATOR_EOTT.get().getConfig(null, 1).hud(LegoClient.HUD_COMPONENT_AMMO);

        ModItems.GUN_DOUBLE_BARREL
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_DOUBLE_BARREL_SACRED_DRAGON
                .get()
                .getConfig(null, 0)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);
        ModItems.GUN_MINIGUN_DUAL
                .get()
                .getConfig(null, 0)
                .hud(
                        LegoClient.HUD_COMPONENT_DURABILITY_MIRROR,
                        LegoClient.HUD_COMPONENT_AMMO_MIRROR);
        ModItems.GUN_MINIGUN_DUAL
                .get()
                .getConfig(null, 1)
                .hud(LegoClient.HUD_COMPONENT_DURABILITY, LegoClient.HUD_COMPONENT_AMMO);

        RenderBulletMK4.setRenderer(GunFactory.ammo_debug, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactoryPile.debris, LegoClient.RENDER_GRAPHITE);
        RenderBulletMK4.setRenderer(GunFactory.ammo_debug_shot, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_bp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory357.m357_express, LegoClient.RENDER_EXPRESS_BULLET);
        RenderBulletMK4.setRenderer(XFactoryCatapult.nuke_standard, LegoClient.RENDER_NUKE);
        RenderBulletMK4.setRenderer(XFactoryCatapult.nuke_demo, LegoClient.RENDER_NUKE);
        RenderBulletMK4.setRenderer(XFactoryCatapult.nuke_high, LegoClient.RENDER_NUKE);
        RenderBulletMK4.setRenderer(XFactoryCatapult.nuke_tots, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactoryCatapult.nuke_hive, LegoClient.RENDER_HIVE);
        RenderBulletMK4.setRenderer(XFactoryCatapult.cluster_submunition, LegoClient.RENDER_BOMB);
        RenderBulletMK4.setRenderer(
                XFactoryCatapult.nuke_balefire, LegoClient.RENDER_NUKE_BALEFIRE);

        RenderBulletMK4.setRenderer(XFactoryTurret.shell_normal, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactoryTurret.shell_explosive, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactoryTurret.shell_ap, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactoryTurret.shell_du, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactoryTurret.shell_w9, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory40mm.g26_flare, LegoClient.RENDER_FLARE);
        RenderBulletMK4.setRenderer(XFactory40mm.g26_flare_supply, LegoClient.RENDER_FLARE_SUPPLY);
        RenderBulletMK4.setRenderer(XFactory40mm.g26_flare_weapon, LegoClient.RENDER_FLARE_WEAPON);
        RenderBulletMK4.setRenderer(XFactory40mm.g40_he, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory40mm.g40_heat, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory40mm.g40_demo, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory40mm.g40_inc, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory40mm.g40_phosphorus, LegoClient.RENDER_GRENADE);
        RenderBulletMK4.setRenderer(XFactory44.m44_bp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory44.m44_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory44.m44_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory44.m44_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory44.m44_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory44.m44_express, LegoClient.RENDER_EXPRESS_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory44.m44_equestrian_pip, LegoClient.RENDER_LEGENDARY_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory44.m44_equestrian_mn7, LegoClient.RENDER_LEGENDARY_BULLET);
        RenderBulletMK4.setRenderer(XFactory9mm.p9_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory9mm.p9_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory9mm.p9_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory9mm.p9_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_bp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_bp_magnum, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_bp_slug, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_slug, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_flechette, LegoClient.RENDER_FLECHETTE_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_magnum, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_explosive, LegoClient.RENDER_EXPRESS_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_phosphorus, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory12ga.g12_equestrian_bj, LegoClient.RENDER_LEGENDARY_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory12ga.g12_equestrian_tkr, LegoClient.RENDER_LEGENDARY_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_sub, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_sub_slug, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory12ga.g12_sub_flechette, LegoClient.RENDER_FLECHETTE_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_sub_magnum, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(
                XFactory12ga.g12_sub_explosive, LegoClient.RENDER_EXPRESS_BULLET);
        RenderBulletMK4.setRenderer(XFactory12ga.g12_sub_phosphorus, LegoClient.RENDER_AP_BULLET);
        for (BulletConfig rocket : XFactoryRocket.rocket_rpzb)
            RenderBulletMK4.setRenderer(rocket, LegoClient.RENDER_RPZB);
        for (BulletConfig rocket : XFactoryRocket.rocket_qd)
            RenderBulletMK4.setRenderer(rocket, LegoClient.RENDER_QD);
        for (BulletConfig rocket : XFactoryRocket.rocket_ml)
            RenderBulletMK4.setRenderer(rocket, LegoClient.RENDER_ML);
        for (BulletConfig rocket : XFactoryRocket.rocket_ncrpa_steer)
            RenderBulletMK4.setRenderer(rocket, LegoClient.RENDER_RPZB);
        for (BulletConfig rocket : XFactoryRocket.rocket_ncrpa)
            RenderBulletMK4.setRenderer(rocket, LegoClient.RENDER_RPZB);
        RenderBeam.setRenderer(XFactoryAccelerator.ni4ni_arc, LegoClient.RENDER_NI4NI_BOLT);
        RenderBeam.setRenderer(XFactoryAccelerator.tau_uranium, LegoClient.RENDER_TAU);
        RenderBeam.setRenderer(
                XFactoryAccelerator.tau_uranium_charge, LegoClient.RENDER_TAU_CHARGE);
        RenderBulletMK4.setRenderer(XFactoryAccelerator.coil_tungsten, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(
                XFactoryAccelerator.coil_ferrouranium, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactoryBlackPowder.stone, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactoryBlackPowder.flint, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactoryBlackPowder.iron, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactoryBlackPowder.shot, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory22lr.p22_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory22lr.p22_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory22lr.p22_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory22lr.p22_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory45.p45_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory45.p45_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory45.p45_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory45.p45_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory45.p45_du, LegoClient.RENDER_DU_BULLET);
        RenderBulletMK4.setRenderer(XFactory75Bolt.b75, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory75Bolt.b75_inc, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory75Bolt.b75_exp, LegoClient.RENDER_EXPRESS_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_inc_sp, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_inc_fmj, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_inc_jhp, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory556mm.r556_inc_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_du, LegoClient.RENDER_DU_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_he, LegoClient.RENDER_HE_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_sm, LegoClient.RENDER_SM_BULLET);
        RenderBulletMK4.setRenderer(XFactory50.bmg50_black, LegoClient.RENDER_BLACK_BULLET);
        RenderBulletMK4.setRenderer(XFactoryFolly.folly_nuke, LegoClient.RENDER_BIG_NUKE);
        RenderBulletMK4.setRenderer(XFactoryTool.ct_hook, LegoClient.RENDER_CT_HOOK);
        RenderBeam.setRenderer(XFactoryFolly.folly_sm, LegoClient.RENDER_FOLLY);
        RenderBeam.setRenderer(XFactory35800.p35800, LegoClient.RENDER_CRACKLE);
        RenderBeam.setRenderer(XFactory35800.p35800_bl, LegoClient.RENDER_BLACK_LIGHTNING);
        RenderBulletMK4.setRenderer(XFactoryTool.ct_mortar, LegoClient.RENDER_CT_MORTAR);
        RenderBulletMK4.setRenderer(
                XFactoryTool.ct_mortar_charge, LegoClient.RENDER_CT_MORTAR_CHARGE);
        RenderBulletMK4.setRenderer(XFactory10ga.g10, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory10ga.g10_shrapnel, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory10ga.g10_du, LegoClient.RENDER_DU_BULLET);
        RenderBulletMK4.setRenderer(XFactory10ga.g10_slug, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory10ga.g10_explosive, LegoClient.RENDER_HE_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_sp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_fmj, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_jhp, LegoClient.RENDER_STANDARD_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_ap, LegoClient.RENDER_AP_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_du, LegoClient.RENDER_DU_BULLET);
        RenderBulletMK4.setRenderer(XFactory762mm.r762_he, LegoClient.RENDER_HE_BULLET);

        RenderBeam.setRenderer(XFactoryEnergy.energy_tesla, LegoClient.RENDER_LIGHTNING);
        RenderBeam.setRenderer(XFactoryEnergy.energy_tesla_overcharge, LegoClient.RENDER_LIGHTNING);
        RenderBeam.setRenderer(XFactoryEnergy.energy_tesla_ir, LegoClient.RENDER_LIGHTNING);
        RenderBeam.setRenderer(XFactoryEnergy.energy_tesla_ir_sub, LegoClient.RENDER_LIGHTNING_SUB);
        RenderBeam.setRenderer(XFactoryEnergy.energy_las, LegoClient.RENDER_LASER_RED);
        RenderBeam.setRenderer(XFactoryEnergy.energy_las_overcharge, LegoClient.RENDER_LASER_RED);
        RenderBeam.setRenderer(XFactoryEnergy.energy_las_ir, LegoClient.RENDER_LASER_RED);
        RenderBeam.setRenderer(XFactoryEnergy.energy_emerald, LegoClient.RENDER_LASER_EMERALD);
        RenderBeam.setRenderer(
                XFactoryEnergy.energy_emerald_overcharge, LegoClient.RENDER_LASER_EMERALD);
        RenderBeam.setRenderer(XFactoryEnergy.energy_emerald_ir, LegoClient.RENDER_LASER_EMERALD);

        RenderBeam.setRenderer(XFactory762mm.energy_lacunae, LegoClient.RENDER_LASER_PURPLE);
        RenderBeam.setRenderer(
                XFactory762mm.energy_lacunae_overcharge, LegoClient.RENDER_LASER_PURPLE);
        RenderBeam.setRenderer(XFactory762mm.energy_lacunae_ir, LegoClient.RENDER_LASER_PURPLE);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder, LegoClient.RENDER_LASER_CYAN);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder_slug, LegoClient.RENDER_LASER_CYAN);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder_flechette, LegoClient.RENDER_LASER_CYAN);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder_magnum, LegoClient.RENDER_LASER_CYAN);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder_explosive, LegoClient.RENDER_LASER_CYAN);
        RenderBeam.setRenderer(XFactory12ga.g12_shredder_phosphorus, LegoClient.RENDER_LASER_CYAN);
    }
}
