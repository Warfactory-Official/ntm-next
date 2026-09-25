// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.main;

import com.hbm.animloader.AnimatedModel;
import com.hbm.animloader.Animation;
import com.hbm.animloader.ColladaLoader;
import com.hbm.client.model.Meshes;
import com.hbm.client.render.RenderTextures;
import com.hbm.lib.Library;
import com.hbm.render.anim.AnimationLoader;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.HashMap;
import net.minecraft.resources.Identifier;

public final class ResourceManager {

    public static final HFRWavefrontObject conveyor_press =
            load("models/machines/conveyor_press.obj");
    public static final Identifier conveyor_press_tex =
            Library.id("textures/block/models/machines/conveyor_press.png");
    public static final Identifier conveyor_press_belt_tex =
            Library.id("textures/block/models/machines/conveyor_press_belt.png");
    public static final HFRWavefrontObject armor_t51 = load("models/armor/t51.obj");
    public static final HFRWavefrontObject armor_taurun = load("models/armor/taurun.obj");
    public static final HFRWavefrontObject armor_no9 = load("models/armor/no9.obj");
    public static final HFRWavefrontObject armor_goggles = load("models/armor/goggles.obj");
    public static final HFRWavefrontObject armor_mod_tesla = load("models/armor/mod_tesla.obj");
    public static final Identifier mod_tesla_tex = Library.id("textures/armor/mod_tesla.png");
    public static final HFRWavefrontObject armor_wings = load("models/armor/murk.obj");
    public static final Identifier wings_murk = Library.id("textures/armor/wings_murk.png");
    public static final Identifier goggles_tex = Library.id("textures/armor/goggles.png");
    public static final Identifier no9_tex = Library.id("textures/armor/no9.png");
    public static final Identifier no9_insignia_tex = Library.id("textures/armor/no9_insignia.png");
    public static final Identifier t51_helmet_tex = Library.id("textures/armor/t51_helmet.png");
    public static final Identifier t51_chest_tex = Library.id("textures/armor/t51_chest.png");
    public static final Identifier t51_arm_tex = Library.id("textures/armor/t51_arm.png");
    public static final Identifier t51_leg_tex = Library.id("textures/armor/t51_leg.png");
    public static final Identifier taurun_helmet_tex =
            Library.id("textures/armor/taurun_helmet.png");
    public static final Identifier taurun_chest_tex = Library.id("textures/armor/taurun_chest.png");
    public static final Identifier taurun_arm_tex = Library.id("textures/armor/taurun_arm.png");
    public static final Identifier taurun_leg_tex = Library.id("textures/armor/taurun_leg.png");

    public static final HFRWavefrontObject armor_ajr = load("models/armor/ajr.obj");
    public static final HFRWavefrontObject armor_remnant = load("models/armor/remnant.obj");
    public static final HFRWavefrontObject armor_ncr = load("models/armor/ncrpa.obj");
    public static final HFRWavefrontObject armor_bismuth = load("models/armor/bismuth.obj");
    public static final HFRWavefrontObject armor_bj = load("models/armor/bj.obj");
    public static final HFRWavefrontObject armor_envsuit = load("models/armor/envsuit.obj");
    public static final HFRWavefrontObject armor_fau = load("models/armor/fau.obj");
    public static final HFRWavefrontObject armor_dnt = load("models/armor/dnt.obj");
    public static final HFRWavefrontObject armor_trenchmaster =
            load("models/armor/trenchmaster.obj");

    public static final HFRWavefrontObject armor_steamsuit = load("models/armor/steamsuit.obj");
    public static final HFRWavefrontObject armor_bnuuy = load("models/armor/bnuuy.obj");

    public static final Identifier ajr_helmet_tex = Library.id("textures/armor/ajr_helmet.png");
    public static final Identifier ajr_chest_tex = Library.id("textures/armor/ajr_chest.png");
    public static final Identifier ajr_arm_tex = Library.id("textures/armor/ajr_arm.png");
    public static final Identifier ajr_leg_tex = Library.id("textures/armor/ajr_leg.png");
    public static final Identifier ajro_helmet_tex = Library.id("textures/armor/ajro_helmet.png");
    public static final Identifier ajro_chest_tex = Library.id("textures/armor/ajro_chest.png");
    public static final Identifier ajro_arm_tex = Library.id("textures/armor/ajro_arm.png");
    public static final Identifier ajro_leg_tex = Library.id("textures/armor/ajro_leg.png");
    public static final Identifier rpa_helmet_tex = Library.id("textures/armor/rpa_helmet.png");
    public static final Identifier rpa_chest_tex = Library.id("textures/armor/rpa_chest.png");
    public static final Identifier rpa_arm_tex = Library.id("textures/armor/rpa_arm.png");
    public static final Identifier rpa_leg_tex = Library.id("textures/armor/rpa_leg.png");
    public static final Identifier ncrpa_helmet_tex = Library.id("textures/armor/ncrpa_helmet.png");
    public static final Identifier ncrpa_chest_tex = Library.id("textures/armor/ncrpa_chest.png");
    public static final Identifier ncrpa_arm_tex = Library.id("textures/armor/ncrpa_arm.png");
    public static final Identifier ncrpa_leg_tex = Library.id("textures/armor/ncrpa_leg.png");
    public static final Identifier bismuth_tex = Library.id("textures/armor/bismuth.png");
    public static final Identifier bj_eyepatch_tex = Library.id("textures/armor/bj_eyepatch.png");
    public static final Identifier bj_chest_tex = Library.id("textures/armor/bj_chest.png");
    public static final Identifier bj_arm_tex = Library.id("textures/armor/bj_arm.png");
    public static final Identifier bj_leg_tex = Library.id("textures/armor/bj_leg.png");

    public static final Identifier bj_jetpack_tex = Library.id("textures/armor/bj_jetpack.png");
    public static final Identifier envsuit_helmet_tex =
            Library.id("textures/armor/envsuit_helmet.png");
    public static final Identifier envsuit_chest_tex =
            Library.id("textures/armor/envsuit_chest.png");
    public static final Identifier envsuit_arm_tex = Library.id("textures/armor/envsuit_arm.png");
    public static final Identifier envsuit_leg_tex = Library.id("textures/armor/envsuit_leg.png");
    public static final Identifier hev_chest_tex = Library.id("textures/armor/hev_chest.png");
    public static final Identifier hev_arm_tex = Library.id("textures/armor/hev_arm.png");
    public static final Identifier hev_leg_tex = Library.id("textures/armor/hev_leg.png");
    public static final Identifier fau_helmet_tex = Library.id("textures/armor/fau_helmet.png");
    public static final Identifier fau_chest_tex = Library.id("textures/armor/fau_chest.png");
    public static final Identifier fau_arm_tex = Library.id("textures/armor/fau_arm.png");
    public static final Identifier fau_leg_tex = Library.id("textures/armor/fau_leg.png");

    public static final Identifier fau_cassette_tex = Library.id("textures/armor/fau_cassette.png");

    public static final Identifier dns_helmet_tex = Library.id("textures/armor/dnt_helmet.png");
    public static final Identifier dns_chest_tex = Library.id("textures/armor/dnt_chest.png");
    public static final Identifier dns_arm_tex = Library.id("textures/armor/dnt_arm.png");
    public static final Identifier dns_leg_tex = Library.id("textures/armor/dnt_leg.png");
    public static final Identifier trenchmaster_helmet_tex =
            Library.id("textures/armor/trenchmaster_helmet.png");
    public static final Identifier trenchmaster_chest_tex =
            Library.id("textures/armor/trenchmaster_chest.png");
    public static final Identifier trenchmaster_arm_tex =
            Library.id("textures/armor/trenchmaster_arm.png");
    public static final Identifier trenchmaster_leg_tex =
            Library.id("textures/armor/trenchmaster_leg.png");
    public static final Identifier steamsuit_helmet_tex =
            Library.id("textures/armor/steamsuit_helmet.png");
    public static final Identifier steamsuit_chest_tex =
            Library.id("textures/armor/steamsuit_chest.png");
    public static final Identifier steamsuit_arm_tex =
            Library.id("textures/armor/steamsuit_arm.png");
    public static final Identifier steamsuit_leg_tex =
            Library.id("textures/armor/steamsuit_leg.png");
    public static final Identifier bnuuy_helmet_tex = Library.id("textures/armor/bnuuy_helmet.png");
    public static final Identifier bnuuy_chest_tex = Library.id("textures/armor/bnuuy_chest.png");
    public static final Identifier bnuuy_arm_tex = Library.id("textures/armor/bnuuy_arm.png");
    public static final Identifier bnuuy_leg_tex = Library.id("textures/armor/bnuuy_leg.png");

    public static final HFRWavefrontObject crucible_sword = load("models/weapons/crucible.obj");
    public static final Identifier crucible_hilt =
            Library.id("textures/models/weapons/crucible_hilt.png");
    public static final Identifier crucible_guard =
            Library.id("textures/models/weapons/crucible_guard.png");
    public static final Identifier crucible_blade =
            Library.id("textures/models/weapons/crucible_blade.png");
    public static final HFRWavefrontObject boltgun = load("models/weapons/boltgun.obj");
    public static final Identifier boltgun_tex = Library.id("textures/models/weapons/boltgun.png");

    public static final HFRWavefrontObject chainsaw =
            load("models/weapons/chainsaw.obj").noSmooth();
    public static final Identifier chainsaw_tex =
            Library.id("textures/models/weapons/chainsaw.png");

    public static final HFRWavefrontObject b92 = load("models/weapons/b92.obj");
    public static final Identifier b92_tex = Library.id("textures/models/model_b92_sm.png");

    public static final HFRWavefrontObject detonator_laser =
            load("models/weapons/detonator_laser.obj");
    public static final Identifier detonator_laser_tex =
            Library.id("textures/models/weapons/detonator_laser.png");

    public static final HFRWavefrontObject press_body = load("models/press_body.obj");
    public static final HFRWavefrontObject press_head = load("models/press_head.obj");
    public static final HFRWavefrontObject epress_body = load("models/epress_body.obj");
    public static final HFRWavefrontObject epress_head = load("models/epress_head.obj");
    public static final HFRWavefrontObject black_hole = load("models/sphere.obj");

    public static final HFRWavefrontObject sphere_new = load("models/sphere_new.obj");
    public static final HFRWavefrontObject sphere_uv = load("models/sphere_uv.obj");

    public static final HFRWavefrontObject sphere_ruv = load("models/sphere_ruv.obj");

    public static final HFRWavefrontObject assembly_machine =
            load("models/machines/assembly_machine.obj");
    public static final HFRWavefrontObject assembly_factory =
            load("models/machines/assembly_factory.obj");
    public static final HFRWavefrontObject chemical_plant =
            load("models/machines/chemical_plant.obj");
    public static final HFRWavefrontObject rock_mill = load("models/machines/rockmill.obj");
    public static final HFRWavefrontObject fusion_torus = load("models/fusion/torus.obj");
    public static final HFRWavefrontObject fusion_klystron = load("models/fusion/klystron.obj");
    public static final HFRWavefrontObject fusion_breeder = load("models/fusion/breeder.obj");
    public static final HFRWavefrontObject fusion_collector = load("models/fusion/collector.obj");
    public static final HFRWavefrontObject fusion_boiler = load("models/fusion/boiler.obj");
    public static final HFRWavefrontObject fusion_mhdt = load("models/fusion/mhdt.obj");
    public static final HFRWavefrontObject fusion_coupler = load("models/fusion/coupler.obj");
    public static final HFRWavefrontObject fusion_plasma_forge =
            load("models/fusion/plasma_forge.obj");
    public static final HFRWavefrontObject chemical_factory =
            load("models/machines/chemical_factory.obj");
    public static final HFRWavefrontObject purex = load("models/machines/purex.obj");
    public static final HFRWavefrontObject mixer = load("models/machines/mixer.obj");
    public static final HFRWavefrontObject ore_slopper = load("models/machines/ore_slopper.obj");
    public static final HFRWavefrontObject annihilator = load("models/machines/annihilator.obj");
    public static final HFRWavefrontObject hephaestus = load("models/machines/hephaestus.obj");
    public static final HFRWavefrontObject compressor = load("models/machines/compressor.obj");
    public static final HFRWavefrontObject charger = load("models/blocks/charger.obj");
    public static final HFRWavefrontObject refueler = load("models/blocks/refueler.obj");
    public static final HFRWavefrontObject condenser = load("models/machines/condenser.obj");
    public static final HFRWavefrontObject flare_stack = load("models/machines/flare_stack.obj");
    public static final HFRWavefrontObject pumpjack = load("models/machines/pumpjack.obj");
    public static final HFRWavefrontObject mining_laser = load("models/machines/mining_laser.obj");
    public static final HFRWavefrontObject mining_drill = load("models/machines/mining_drill.obj");
    public static final HFRWavefrontObject forcefield_top =
            load("models/machines/forcefield_top.obj");
    public static final HFRWavefrontObject reactor_small_base =
            load("models/reactors/reactor_small_base.obj");
    public static final HFRWavefrontObject reactor_small_rods =
            load("models/reactors/reactor_small_rods.obj");
    public static final HFRWavefrontObject breeder = load("models/reactors/breeder.obj");
    public static final HFRWavefrontObject cyclotron = load("models/machines/cyclotron.obj");
    public static final HFRWavefrontObject exposure_chamber =
            load("models/machines/exposure_chamber.obj");
    public static final HFRWavefrontObject radgen = load("models/machines/radgen.obj");
    public static final HFRWavefrontObject radar = load("models/machines/radar.obj");
    public static final HFRWavefrontObject radar_large = load("models/machines/radar_large.obj");
    public static final HFRWavefrontObject satlink = load("models/machines/satlink.obj");
    public static final HFRWavefrontObject tower_small = load("models/machines/tower_small.obj");
    public static final HFRWavefrontObject tower_large = load("models/machines/tower_large.obj");
    public static final HFRWavefrontObject dieselgen = load("models/machines/dieselgen.obj");
    public static final HFRWavefrontObject turbine = load("models/machines/turbine.obj");
    public static final HFRWavefrontObject turbofan = load("models/machines/turbofan.obj");
    public static final HFRWavefrontObject industrial_turbine =
            load("models/machines/industrial_turbine.obj");
    public static final HFRWavefrontObject lpw2 = load("models/machines/lpw2.obj");
    public static final HFRWavefrontObject chungus = load("models/machines/chungus.obj");

    public static final HFRWavefrontObject boiler = load("models/machines/boiler.obj");
    public static final HFRWavefrontObject boiler_burst = load("models/machines/boiler_burst.obj");
    public static final HFRWavefrontObject steam_engine = load("models/machines/steam_engine.obj");

    public static final HFRWavefrontObject fensu = load("models/machines/fensu.obj");
    public static final HFRWavefrontObject battery_redd = load("models/machines/fensu2.obj");
    public static final HFRWavefrontObject combustion_engine =
            load("models/machines/combustion_engine.obj");

    public static final HFRWavefrontObject pump = load("models/machines/pump.obj");
    public static final HFRWavefrontObject solar_mirror = load("models/machines/solar_mirror.obj");
    public static final HFRWavefrontObject heater_firebox = load("models/machines/firebox.obj");
    public static final HFRWavefrontObject crucible = load("models/machines/crucible.obj");
    public static final HFRWavefrontObject strand_caster =
            load("models/machines/strand_caster.obj");
    public static final HFRWavefrontObject rotary_furnace =
            load("models/machines/rotary_furnace.obj");
    public static final HFRWavefrontObject arc_furnace = load("models/machines/arc_furnace.obj");
    public static final HFRWavefrontObject furnace_iron = load("models/machines/furnace_iron.obj");
    public static final HFRWavefrontObject heater_oven = load("models/machines/heating_oven.obj");
    public static final HFRWavefrontObject ashpit = load("models/machines/heating_oven.obj");
    public static final HFRWavefrontObject crystallizer = load("models/machines/crystallizer.obj");
    public static final HFRWavefrontObject pa_beamline =
            load("models/particleaccelerator/beamline.obj");
    public static final HFRWavefrontObject coker = load("models/machines/coker.obj");
    public static final HFRWavefrontObject liquefactor = load("models/machines/liquefactor.obj");
    public static final HFRWavefrontObject solidifier = load("models/machines/solidifier.obj");
    public static final HFRWavefrontObject pyrooven = load("models/machines/pyrooven.obj");
    public static final HFRWavefrontObject intake = load("models/machines/intake.obj");
    public static final HFRWavefrontObject drain = load("models/machines/drain.obj");
    public static final HFRWavefrontObject bigasstank = load("models/machines/bigasstank.obj");
    public static final HFRWavefrontObject microwave = load("models/machines/microwave.obj");
    public static final HFRWavefrontObject supercomputer =
            load("models/machines/supercomputer.obj");
    public static final HFRWavefrontObject tape_drive = load("models/machines/tape_drive.obj");
    public static final HFRWavefrontObject thresher = load("models/machines/thresher.obj");
    public static final HFRWavefrontObject autosaw = load("models/machines/autosaw.obj");
    public static final HFRWavefrontObject sawmill = load("models/machines/sawmill.obj");
    public static final HFRWavefrontObject stirling = load("models/machines/stirling.obj");
    public static final HFRWavefrontObject ammo_press = load("models/machines/ammo_press.obj");

    public static final HFRWavefrontObject deb_zirnox_blank = load("models/zirnox/deb_blank.obj");
    public static final HFRWavefrontObject deb_zirnox_concrete =
            flat("models/zirnox/deb_concrete.obj");
    public static final HFRWavefrontObject deb_zirnox_element =
            load("models/zirnox/deb_element.obj");
    public static final HFRWavefrontObject deb_zirnox_exchanger =
            load("models/zirnox/deb_exchanger.obj");
    public static final HFRWavefrontObject deb_zirnox_shrapnel =
            load("models/zirnox/deb_shrapnel.obj");
    public static final HFRWavefrontObject battery_socket = load("models/machines/battery.obj");
    public static final HFRWavefrontObject blast_door_base =
            load("models/machines/blast_door_base.obj");
    public static final HFRWavefrontObject blast_door_block =
            load("models/machines/blast_door_block.obj");
    public static final HFRWavefrontObject blast_door_tooth =
            load("models/machines/blast_door_tooth.obj");
    public static final HFRWavefrontObject blast_door_slider =
            load("models/machines/blast_door_slider.obj");
    public static final HFRWavefrontObject horse = load("models/mobs/horse.obj");
    public static final HFRWavefrontObject glyphid = load("models/mobs/glyphid.obj");
    public static final HFRWavefrontObject fluidtank = load("models/fluidtank.obj");
    public static final HFRWavefrontObject fluidtank_exploded =
            load("models/fluidtank_exploded.obj");
    public static final HFRWavefrontObject floodlight = load("models/blocks/floodlight.obj");
    public static final HFRWavefrontObject pipe_anchor = load("models/network/pipe_anchor.obj");
    public static final HFRWavefrontObject fan = load("models/machines/fan.obj");
    public static final HFRWavefrontObject piston_inserter =
            load("models/machines/piston_inserter.obj");
    public static final HFRWavefrontObject pile_loader = load("models/pile/pile_loader.obj");
    public static final HFRWavefrontObject pile_vent = load("models/pile/pile_vent.obj");
    public static final HFRWavefrontObject pile_control = load("models/pile/pile_control.obj");
    public static final HFRWavefrontObject rbmk_element_rods =
            load("models/rbmk/rbmk_element_rods.obj");
    public static final HFRWavefrontObject rbmk_rods = load("models/rbmk/rbmk_rods.obj");
    public static final HFRWavefrontObject rbmk_crane_console =
            load("models/rbmk/crane_console.obj");
    public static final HFRWavefrontObject rbmk_crane = load("models/rbmk/crane.obj");
    public static final HFRWavefrontObject rbmk_gauge = load("models/rbmk/gauge.obj");
    public static final HFRWavefrontObject rbmk_numitron = load("models/rbmk/numitron.obj");
    public static final HFRWavefrontObject rbmk_indicator = load("models/rbmk/indicator.obj");
    public static final HFRWavefrontObject rbmk_lever = load("models/rbmk/lever.obj");
    public static final HFRWavefrontObject rbmk_button = load("models/rbmk/button.obj");
    public static final HFRWavefrontObject rbmk_terminal = load("models/rbmk/terminal.obj");
    public static final HFRWavefrontObject rbmk_autoloader = load("models/rbmk/autoloader.obj");
    public static final HFRWavefrontObject deb_blank = load("models/projectiles/deb_blank.obj");
    public static final HFRWavefrontObject deb_element = load("models/projectiles/deb_element.obj");
    public static final HFRWavefrontObject deb_fuel = load("models/projectiles/deb_fuel.obj");
    public static final HFRWavefrontObject deb_graphite =
            load("models/projectiles/deb_graphite.obj");
    public static final HFRWavefrontObject deb_lid = load("models/projectiles/deb_lid.obj");
    public static final HFRWavefrontObject deb_rod = load("models/projectiles/deb_rod.obj");
    public static final HFRWavefrontObject lance = load("models/weapons/lance.obj");
    public static final HFRWavefrontObject dud_balefire = load("models/bombs/dud_balefire.obj");
    public static final HFRWavefrontObject dud_conventional =
            load("models/bombs/dud_conventional.obj");
    public static final HFRWavefrontObject dud_nuke = load("models/bombs/dud_nuke.obj");
    public static final HFRWavefrontObject dud_salted = load("models/bombs/dud_salted.obj");

    public static final HFRWavefrontObject missileV2 = load("models/missile_v2.obj");
    public static final HFRWavefrontObject missileStrong = load("models/missile_strong.obj");
    public static final HFRWavefrontObject missileHuge = load("models/missile_huge.obj");
    public static final HFRWavefrontObject missileNuclear = load("models/missile_atlas.obj");
    public static final HFRWavefrontObject missileMicro = load("models/missile_micro.obj");

    public static final HFRWavefrontObject missileStealth =
            load("models/missile_stealth.obj").noSmooth();
    public static final HFRWavefrontObject turret_chekhov =
            load("models/turrets/turret_chekhov.obj");
    public static final HFRWavefrontObject turret_howard = load("models/turrets/turret_howard.obj");
    public static final HFRWavefrontObject turret_howard_damaged =
            load("models/turrets/turret_howard_damaged.obj");
    public static final HFRWavefrontObject turret_arty = load("models/turrets/turret_arty.obj");
    public static final HFRWavefrontObject turret_fritz = load("models/turrets/turret_fritz.obj");
    public static final HFRWavefrontObject turret_jeremy = load("models/turrets/turret_jeremy.obj");

    public static final HFRWavefrontObject turret_maxwell =
            load("models/turrets/turret_microwave.obj");
    public static final HFRWavefrontObject turret_richard =
            load("models/turrets/turret_richard.obj");
    public static final HFRWavefrontObject turret_sentry = load("models/turrets/turret_sentry.obj");
    public static final HFRWavefrontObject turret_tauon = load("models/turrets/turret_tauon.obj");
    public static final HFRWavefrontObject pheo_secure_door =
            load("models/pheodoors/secure_door.obj");
    public static final HFRWavefrontObject pheo_sliding_door =
            load("models/pheodoors/sliding_door.obj");
    public static final HFRWavefrontObject pheo_cargo_door =
            load("models/pheodoors/cargo_door.obj");
    public static final HFRWavefrontObject pheo_water_door =
            load("models/pheodoors/water_door.obj");
    public static final HFRWavefrontObject pheo_vault_door =
            load("models/pheodoors/vault_door.obj");
    public static final HFRWavefrontObject pheo_seal_door = load("models/pheodoors/seal_door.obj");
    public static final HFRWavefrontObject pheo_containment_door =
            load("models/pheodoors/containment_door.obj");
    public static final HFRWavefrontObject pheo_airlock_door =
            load("models/pheodoors/airlock_door.obj");
    public static final HFRWavefrontObject pheo_blast_door =
            load("models/pheodoors/blast_door.obj");
    public static final HFRWavefrontObject pheo_vehicle_door =
            load("models/pheodoors/vehicle_door.obj");
    public static final HFRWavefrontObject pheo_fire_door = load("models/pheodoors/fire_door.obj");
    public static final HFRWavefrontObject silo_hatch = load("models/doors/silo_hatch.obj");
    public static final HFRWavefrontObject silo_hatch_large =
            load("models/doors/silo_hatch_large.obj");
    public static final HFRWavefrontObject launch_pad_silo =
            load("models/weapons/launch_pad_silo.obj");

    public static final Identifier pipe_anchor_tex =
            Library.id("textures/block/models/network/pipe_anchor.png");
    public static final Identifier wire_tex = Library.id("textures/models/network/wire.png");
    public static final Identifier wire_greyscale_tex =
            Library.id("textures/models/network/wire_greyscale.png");
    public static final Identifier pile_loader_tex =
            Library.id("textures/block/models/pile/pile_loader.png");
    public static final Identifier pile_vent_tex =
            Library.id("textures/block/models/pile/pile_vent.png");
    public static final Identifier pile_control_tex =
            Library.id("textures/block/models/pile/pile_control.png");
    public static final Identifier fan_tex = Library.id("textures/block/models/machines/fan.png");
    public static final Identifier piston_inserter_tex =
            Library.id("textures/block/models/machines/piston_inserter.png");
    public static final Identifier press_body_tex =
            Library.id("textures/block/models/machines/press_body.png");
    public static final Identifier press_head_tex =
            Library.id("textures/models/machines/press_head.png");
    public static final Identifier epress_body_tex =
            Library.id("textures/block/models/machines/epress_body.png");
    public static final Identifier epress_head_tex =
            Library.id("textures/models/machines/epress_head.png");
    public static final Identifier black_hole_tex = Library.id("textures/models/black_hole.png");
    public static final Identifier vortex_tex = Library.id("textures/entity/bhole.png");

    public static final Identifier glyphid_tex = Library.id("textures/entity/glyphid.png");
    public static final Identifier glyphid_scout_tex =
            Library.id("textures/entity/glyphid_scout.png");
    public static final Identifier glyphid_blaster_tex =
            Library.id("textures/entity/glyphid_blaster.png");
    public static final Identifier glyphid_bombardier_tex =
            Library.id("textures/entity/glyphid_bombardier.png");
    public static final Identifier glyphid_brawler_tex =
            Library.id("textures/entity/glyphid_brawler.png");
    public static final Identifier glyphid_behemoth_tex =
            Library.id("textures/entity/glyphid_behemoth.png");
    public static final Identifier glyphid_brenda_tex =
            Library.id("textures/entity/glyphid_brenda.png");
    public static final Identifier glyphid_digger_tex =
            Library.id("textures/entity/glyphid_digger.png");
    public static final Identifier glyphid_nuclear_tex =
            Library.id("textures/entity/glyphid_nuclear.png");
    public static final Identifier glyphid_infestation_tex =
            Library.id("textures/entity/glyphid_infestation.png");

    public static final Identifier assembly_machine_tex =
            Library.id("textures/block/models/machines/assembly_machine.png");
    public static final Identifier assembly_factory_tex =
            Library.id("textures/block/models/machines/assembly_factory.png");
    public static final Identifier chemical_plant_tex =
            Library.id("textures/block/models/machines/chemical_plant.png");
    public static final Identifier rock_mill_tex =
            Library.id("textures/block/models/machines/rockmill.png");
    public static final Identifier fusion_torus_tex =
            Library.id("textures/block/models/fusion/torus.png");
    public static final Identifier fusion_klystron_tex =
            Library.id("textures/block/models/fusion/klystron.png");
    public static final Identifier fusion_klystron_creative_tex =
            Library.id("textures/block/models/fusion/klystron_creative.png");
    public static final Identifier fusion_breeder_tex =
            Library.id("textures/block/models/fusion/breeder.png");
    public static final Identifier fusion_collector_tex =
            Library.id("textures/block/models/fusion/collector.png");
    public static final Identifier fusion_boiler_tex =
            Library.id("textures/block/models/fusion/boiler.png");
    public static final Identifier fusion_mhdt_tex =
            Library.id("textures/block/models/fusion/mhdt.png");
    public static final Identifier fusion_coupler_tex =
            Library.id("textures/block/models/fusion/coupler.png");
    public static final Identifier fusion_plasma_forge_tex =
            Library.id("textures/block/models/fusion/plasma_forge.png");
    public static final Identifier fusion_plasma_tex =
            Library.id("textures/models/fusion/plasma.png");
    public static final Identifier fusion_plasma_glow_tex =
            Library.id("textures/models/fusion/plasma_glow.png");
    public static final Identifier fusion_plasma_sparkle_tex =
            Library.id("textures/models/fusion/plasma_sparkle.png");
    public static final Identifier chemical_factory_tex =
            Library.id("textures/block/models/machines/chemical_factory.png");
    public static final Identifier purex_tex =
            Library.id("textures/block/models/machines/purex.png");
    public static final Identifier mixer_tex =
            Library.id("textures/block/models/machines/mixer.png");
    public static final Identifier ore_slopper_tex =
            Library.id("textures/block/models/machines/ore_slopper.png");
    public static final Identifier annihilator_tex =
            Library.id("textures/block/models/machines/annihilator.png");
    public static final Identifier annihilator_belt_tex =
            Library.id("textures/block/models/machines/annihilator_belt.png");
    public static final Identifier hephaestus_tex =
            Library.id("textures/block/models/machines/hephaestus.png");
    public static final Identifier hephaestus_lava_tex =
            Library.id("textures/models/machines/lava.png");
    public static final Identifier compressor_tex =
            Library.id("textures/block/models/machines/compressor.png");
    public static final Identifier charger_tex =
            Library.id("textures/block/models/machines/charger.png");
    public static final Identifier refueler_tex =
            Library.id("textures/block/models/machines/refueler.png");
    public static final Identifier compressor_compact_tex =
            Library.id("textures/block/models/machines/compressor_compact.png");
    public static final Identifier flare_stack_tex =
            Library.id("textures/block/models/machines/flare_stack.png");
    public static final Identifier pumpjack_tex =
            Library.id("textures/block/models/machines/pumpjack.png");
    public static final Identifier mining_laser_base_tex =
            Library.id("textures/block/models/machines/mining_laser_base.png");
    public static final Identifier mining_laser_pivot_tex =
            Library.id("textures/block/models/machines/mining_laser_pivot.png");
    public static final Identifier mining_laser_laser_tex =
            Library.id("textures/block/models/machines/mining_laser_laser.png");
    public static final Identifier forcefield_top_tex =
            Library.id("textures/block/models/machines/forcefield_top.png");
    public static final Identifier reactor_small_base_tex =
            Library.id("textures/block/models/reactor_small_base.png");
    public static final Identifier reactor_small_rods_tex =
            Library.id("textures/block/models/reactor_small_rods.png");
    public static final Identifier breeder_tex =
            Library.id("textures/block/models/machines/breeder.png");
    public static final Identifier exposure_chamber_tex =
            Library.id("textures/block/models/machines/exposure_chamber.png");
    public static final Identifier radgen_tex =
            Library.id("textures/block/models/machines/radgen.png");
    public static final Identifier radar_base_tex =
            Library.id("textures/block/models/machines/radar_base.png");
    public static final Identifier radar_dish_tex =
            Library.id("textures/block/models/machines/radar_dish.png");
    public static final Identifier radar_large_tex =
            Library.id("textures/block/models/machines/radar_large.png");
    public static final Identifier satlink_tex =
            Library.id("textures/block/models/machines/satlink.png");
    public static final Identifier cyclotron_ashes_tex =
            Library.id("textures/block/models/machines/cyclotron_ashes.png");
    public static final Identifier cyclotron_ashes_filled_tex =
            Library.id("textures/block/models/machines/cyclotron_ashes_filled.png");
    public static final Identifier cyclotron_book_tex =
            Library.id("textures/block/models/machines/cyclotron_book.png");
    public static final Identifier cyclotron_book_filled_tex =
            Library.id("textures/block/models/machines/cyclotron_book_filled.png");
    public static final Identifier cyclotron_gavel_tex =
            Library.id("textures/block/models/machines/cyclotron_gavel.png");
    public static final Identifier cyclotron_gavel_filled_tex =
            Library.id("textures/block/models/machines/cyclotron_gavel_filled.png");
    public static final Identifier cyclotron_coin_tex =
            Library.id("textures/block/models/machines/cyclotron_coin.png");
    public static final Identifier cyclotron_coin_filled_tex =
            Library.id("textures/block/models/machines/cyclotron_coin_filled.png");

    public static final Identifier mining_drill_tex =
            Library.id("textures/block/models/machines/mining_drill.png");

    public static final Identifier mining_drill_cobble_tex =
            Library.id("textures/models/machines/cobblestone.png");
    public static final Identifier mining_drill_gravel_tex =
            Library.id("textures/models/machines/gravel.png");
    public static final Identifier dieselgen_tex =
            Library.id("textures/block/models/machines/dieselgen.png");
    public static final Identifier turbofan_blades_tex =
            Library.id("textures/block/models/machines/turbofan_blades.png");
    public static final Identifier turbofan_tex =
            Library.id("textures/block/models/machines/turbofan.png");
    public static final Identifier turbofan_back_tex =
            Library.id("textures/block/models/machines/turbofan_back.png");
    public static final Identifier turbofan_afterburner_tex =
            Library.id("textures/block/models/machines/turbofan_afterburner.png");
    public static final Identifier industrial_turbine_tex =
            Library.id("textures/block/models/machines/industrial_turbine.png");
    public static final Identifier lpw2_tex = Library.id("textures/block/models/machines/lpw2.png");

    public static final Identifier lpw2_error_tex =
            Library.id("textures/models/machines/lpw2_term_error.png");
    public static final Identifier chungus_tex =
            Library.id("textures/block/models/machines/chungus.png");
    public static final Identifier boiler_tex =
            Library.id("textures/block/models/machines/boiler.png");
    public static final Identifier steam_engine_tex =
            Library.id("textures/block/models/machines/steam_engine.png");
    public static final Identifier fensu_tex =
            Library.id("textures/block/models/machines/fensu.png");
    public static final Identifier battery_redd_tex =
            Library.id("textures/block/models/machines/fensu2.png");
    public static final Identifier combustion_engine_tex =
            Library.id("textures/block/models/machines/combustion_engine.png");
    public static final Identifier pump_steam_tex =
            Library.id("textures/block/models/machines/pump_steam.png");
    public static final Identifier solar_mirror_tex =
            Library.id("textures/block/models/machines/solar_mirror.png");
    public static final Identifier heater_firebox_tex =
            Library.id("textures/block/models/machines/firebox.png");
    public static final Identifier crucible_tex =
            Library.id("textures/block/models/machines/crucible_heat.png");
    public static final Identifier strand_caster_tex =
            Library.id("textures/block/models/machines/strand_caster.png");
    public static final Identifier precass_tex =
            Library.id("textures/block/models/machines/precass.png");
    public static final Identifier rotary_furnace_tex =
            Library.id("textures/block/models/machines/rotary_furnace.png");
    public static final Identifier arc_furnace_tex =
            Library.id("textures/block/models/machines/arc_furnace.png");
    public static final Identifier furnace_iron_tex =
            Library.id("textures/block/models/machines/furnace_iron.png");
    public static final Identifier heater_oven_tex =
            Library.id("textures/block/models/machines/heating_oven.png");
    public static final Identifier ashpit_tex =
            Library.id("textures/block/models/machines/ashpit.png");
    public static final Identifier crystallizer_tex =
            Library.id("textures/block/models/machines/crystallizer.png");
    public static final Identifier pa_beamline_tex =
            Library.id("textures/block/models/particleaccelerator/beamline.png");
    public static final Identifier coker_tex =
            Library.id("textures/block/models/machines/coker.png");
    public static final Identifier liquefactor_tex =
            Library.id("textures/block/models/machines/liquefactor.png");
    public static final Identifier solidifier_tex =
            Library.id("textures/block/models/machines/solidifier.png");
    public static final Identifier pyrooven_tex =
            Library.id("textures/block/models/machines/pyrooven.png");
    public static final Identifier pump_electric_tex =
            Library.id("textures/block/models/machines/pump_electric.png");
    public static final Identifier intake_tex =
            Library.id("textures/block/models/machines/intake.png");
    public static final Identifier drain_tex =
            Library.id("textures/block/models/machines/drain.png");
    public static final Identifier condenser_tex =
            Library.id("textures/block/models/machines/condenser.png");
    public static final Identifier uf6_tex = Library.id("textures/block/models/uf6tank.png");
    public static final Identifier puf6_tex = Library.id("textures/block/models/puf6tank.png");
    public static final Identifier microwave_tex =
            Library.id("textures/block/models/machines/microwave.png");
    public static final Identifier supercomputer_tex =
            Library.id("textures/block/models/machines/supercomputer.png");
    public static final Identifier supercomputer_scan_tex =
            Library.id("textures/block/models/machines/supercomputer_scan.png");
    public static final Identifier tape_drive_tex =
            Library.id("textures/block/models/machines/tape_drive.png");
    public static final Identifier thresher_tex =
            Library.id("textures/block/models/machines/thresher.png");
    public static final Identifier autosaw_tex =
            Library.id("textures/block/models/machines/autosaw.png");
    public static final Identifier sawmill_tex =
            Library.id("textures/block/models/machines/sawmill.png");
    public static final Identifier stirling_tex =
            Library.id("textures/block/models/machines/stirling.png");
    public static final Identifier stirling_steel_tex =
            Library.id("textures/block/models/machines/stirling_steel.png");
    public static final Identifier stirling_creative_tex =
            Library.id("textures/block/models/machines/stirling_creative.png");
    public static final Identifier ammo_press_tex =
            Library.id("textures/block/models/machines/ammo_press.png");

    public static final Identifier foundry_lava_tex =
            Library.id("textures/models/machines/lava.png");

    public static final Identifier foundry_stream_tex =
            Library.id("textures/models/machines/lava_gray.png");
    public static final Identifier zirnox_tex =
            Library.id("textures/block/models/machines/zirnox.png");
    public static final Identifier zirnox_destroyed_tex =
            Library.id("textures/block/models/machines/zirnox_destroyed.png");
    public static final Identifier zirnox_deb_element_tex =
            Library.id("textures/models/machines/zirnox_deb_element.png");
    public static final Identifier chemical_plant_fluid_tex =
            Library.id("textures/models/machines/chemical_plant_fluid.png");
    public static final Identifier battery_socket_tex =
            Library.id("textures/block/models/machines/battery_socket.png");
    public static final Identifier blast_door_base_tex =
            Library.id("textures/block/models/machines/blast_door_base.png");
    public static final Identifier blast_door_block_tex =
            Library.id("textures/block/models/machines/blast_door_block.png");
    public static final Identifier blast_door_tooth_tex =
            Library.id("textures/block/models/machines/blast_door_tooth.png");
    public static final Identifier blast_door_slider_tex =
            Library.id("textures/block/models/machines/blast_door_slider.png");
    public static final Identifier battery_sc_tex =
            Library.id("textures/models/machines/battery_sc.png");
    public static final Identifier horse_sunburst_tex =
            Library.id("textures/models/horse/sunburst.png");
    public static final Identifier floodlight_tex =
            Library.id("textures/block/models/machines/floodlight.png");
    public static final Identifier rbmk_fuel_tex =
            Library.id("textures/block/rbmk/rbmk_element_fuel.png");
    public static final Identifier rbmk_control_tex =
            Library.id("textures/block/rbmk/rbmk_control.png");
    public static final Identifier rbmk_crane_console_tex =
            Library.id("textures/block/models/machines/crane_console.png");
    public static final Identifier rbmk_crane_tex =
            Library.id("textures/models/machines/rbmk_crane.png");
    public static final Identifier rbmk_control_auto_tex =
            Library.id("textures/block/rbmk/rbmk_control_auto.png");

    public static final Identifier[] rbmk_control_color_tex = {
        Library.id("textures/block/rbmk/rbmk_control_red.png"),
        Library.id("textures/block/rbmk/rbmk_control_yellow.png"),
        Library.id("textures/block/rbmk/rbmk_control_green.png"),
        Library.id("textures/block/rbmk/rbmk_control_blue.png"),
        Library.id("textures/block/rbmk/rbmk_control_purple.png"),
    };

    public static final Identifier deb_element_tex =
            Library.id("textures/entity/rbmk_debris/element.png");
    public static final Identifier deb_fuel_tex =
            Library.id("textures/entity/rbmk_debris/fuel.png");
    public static final Identifier deb_control_tex =
            Library.id("textures/entity/rbmk_debris/control.png");
    public static final Identifier deb_blank_tex =
            Library.id("textures/entity/rbmk_debris/blank.png");
    public static final Identifier deb_lid_tex = Library.id("textures/entity/rbmk_debris/lid.png");
    public static final Identifier deb_graphite_tex =
            Library.id("textures/entity/rbmk_debris/graphite.png");
    public static final Identifier lance_tex = Library.id("textures/models/weapons/lance.png");

    public static final Identifier dud_balefire_tex =
            Library.id("textures/block/models/bombs/dud_balefire.png");
    public static final Identifier dud_conventional_tex =
            Library.id("textures/block/models/bombs/dud_conventional.png");
    public static final Identifier dud_nuke_tex =
            Library.id("textures/block/models/bombs/dud_nuke.png");
    public static final Identifier dud_salted_tex =
            Library.id("textures/block/models/bombs/dud_salted.png");

    public static final Identifier white_tex = RenderTextures.WHITE;
    public static final Identifier rbmk_gauge_tex = Library.id("textures/models/network/gauge.png");
    public static final Identifier rbmk_numitron_tex =
            Library.id("textures/models/network/numitron.png");
    public static final Identifier rbmk_numitron_lights_tex =
            Library.id("textures/models/network/numitron_lights.png");
    public static final Identifier rbmk_indicator_tex =
            Library.id("textures/models/network/indicator.png");
    public static final Identifier rbmk_lever_tex = Library.id("textures/models/network/lever.png");
    public static final Identifier rbmk_keypad_tex =
            Library.id("textures/models/network/keypad.png");
    public static final Identifier rbmk_terminal_tex =
            Library.id("textures/models/network/terminal.png");
    public static final Identifier rbmk_autoloader_tex =
            Library.id("textures/block/models/machines/rbmk_autoloader.png");

    public static final Identifier missileV2_HE_tex =
            Library.id("textures/models/missiles/missile_v2.png");
    public static final Identifier missileV2_IN_tex =
            Library.id("textures/models/missiles/missile_v2_inc.png");
    public static final Identifier missileV2_BU_tex =
            Library.id("textures/models/missiles/missile_v2_bu.png");
    public static final Identifier missileV2_CL_tex =
            Library.id("textures/models/missiles/missile_v2_cl.png");
    public static final Identifier missileV2_decoy_tex =
            Library.id("textures/models/missiles/missile_v2_decoy.png");
    public static final Identifier missileStrong_HE_tex =
            Library.id("textures/models/missiles/missile_strong.png");
    public static final Identifier missileStrong_IN_tex =
            Library.id("textures/models/missiles/missile_strong_inc.png");
    public static final Identifier missileStrong_BU_tex =
            Library.id("textures/models/missiles/missile_strong_bu.png");
    public static final Identifier missileStrong_CL_tex =
            Library.id("textures/models/missiles/missile_strong_cl.png");
    public static final Identifier missileStrong_EMP_tex =
            Library.id("textures/models/missiles/missile_strong_emp.png");
    public static final Identifier missileStealth_tex =
            Library.id("textures/models/missiles/missile_stealth.png");
    public static final Identifier missileMicro_tex =
            Library.id("textures/models/missiles/missile_micro.png");
    public static final Identifier missileMicroTaint_tex =
            Library.id("textures/models/missiles/missile_micro_taint.png");
    public static final Identifier missileMicroBHole_tex =
            Library.id("textures/models/missiles/missile_micro_bhole.png");
    public static final Identifier missileMicroSchrab_tex =
            Library.id("textures/models/missiles/missile_micro_schrab.png");
    public static final Identifier missileMicroEMP_tex =
            Library.id("textures/models/missiles/missile_micro_emp.png");
    public static final Identifier missileMicroTest_tex =
            Library.id("textures/models/missiles/missile_test.png");
    public static final Identifier missileHuge_HE_tex =
            Library.id("textures/models/missiles/missile_huge.png");
    public static final Identifier missileHuge_IN_tex =
            Library.id("textures/models/missiles/missile_huge_inc.png");
    public static final Identifier missileHuge_CL_tex =
            Library.id("textures/models/missiles/missile_huge_cl.png");
    public static final Identifier missileHuge_BU_tex =
            Library.id("textures/models/missiles/missile_huge_bu.png");
    public static final Identifier missileNuclear_tex =
            Library.id("textures/models/missiles/missile_atlas_nuclear.png");
    public static final Identifier missileMIRV_tex =
            Library.id("textures/models/missiles/missile_atlas_thermo.png");
    public static final Identifier missileVolcano_tex =
            Library.id("textures/models/missiles/missile_atlas_tectonic.png");
    public static final Identifier missileDoomsday_tex =
            Library.id("textures/models/missiles/missile_atlas_doomsday.png");
    public static final Identifier missileDoomsdayRusted_tex =
            Library.id("textures/block/models/missile/missile_atlas_doomsday_weathered.png");
    public static final HFRWavefrontObject missileABM = load("models/missile_abm.obj");
    public static final Identifier missileAA_tex =
            Library.id("textures/models/missiles/missile_abm.png");
    public static final HFRWavefrontObject missileShuttle = load("models/missile_shuttle.obj");
    public static final Identifier missileShuttle_tex =
            Library.id("textures/models/missiles/missile_shuttle.png");
    public static final Identifier turret_base_tex =
            Library.id("textures/block/models/turrets/base.png");
    public static final Identifier turret_base_friendly_tex =
            Library.id("textures/block/models/turrets/base_friendly.png");
    public static final Identifier turret_carriage_tex =
            Library.id("textures/block/models/turrets/carriage.png");
    public static final Identifier turret_carriage_ciws_tex =
            Library.id("textures/block/models/turrets/carriage_ciws.png");
    public static final Identifier turret_carriage_friendly_tex =
            Library.id("textures/block/models/turrets/carriage_friendly.png");
    public static final Identifier turret_connector_tex =
            Library.id("textures/block/models/turrets/connector.png");
    public static final Identifier turret_chekhov_tex =
            Library.id("textures/block/models/turrets/chekhov.png");
    public static final Identifier turret_chekhov_barrels_tex =
            Library.id("textures/block/models/turrets/chekhov_barrels.png");
    public static final Identifier turret_jeremy_tex =
            Library.id("textures/block/models/turrets/jeremy.png");
    public static final Identifier turret_tauon_tex =
            Library.id("textures/block/models/turrets/tauon.png");
    public static final Identifier turret_richard_tex =
            Library.id("textures/block/models/turrets/richard.png");
    public static final Identifier turret_howard_tex =
            Library.id("textures/block/models/turrets/howard.png");
    public static final Identifier turret_howard_barrels_tex =
            Library.id("textures/block/models/turrets/howard_barrels.png");
    public static final Identifier turret_maxwell_tex =
            Library.id("textures/block/models/turrets/maxwell.png");
    public static final Identifier turret_fritz_tex =
            Library.id("textures/block/models/turrets/fritz.png");
    public static final Identifier turret_arty_tex =
            Library.id("textures/block/models/turrets/arty.png");
    public static final Identifier turret_himars_tex =
            Library.id("textures/block/models/turrets/himars.png");
    public static final Identifier turret_sentry_tex =
            Library.id("textures/block/models/turrets/sentry.png");
    public static final Identifier turret_sentry_damaged_tex =
            Library.id("textures/block/models/turrets/sentry_damaged.png");
    public static final Identifier turret_base_rusted_tex =
            Library.id("textures/block/models/turrets/rusted/base.png");
    public static final Identifier turret_carriage_ciws_rusted_tex =
            Library.id("textures/block/models/turrets/rusted/carriage_ciws.png");
    public static final Identifier turret_howard_rusted_tex =
            Library.id("textures/block/models/turrets/rusted/howard.png");
    public static final Identifier turret_howard_barrels_rusted_tex =
            Library.id("textures/block/models/turrets/rusted/howard_barrels.png");
    public static final Identifier pheo_secure_door_tex =
            Library.id("textures/block/models/pheodoors/secure_door.png");
    public static final Identifier pheo_secure_door_grey_tex =
            Library.id("textures/block/models/pheodoors/secure_door_grey.png");
    public static final Identifier pheo_secure_door_black_tex =
            Library.id("textures/block/models/pheodoors/secure_door_black.png");
    public static final Identifier pheo_secure_door_yellow_tex =
            Library.id("textures/block/models/pheodoors/secure_door_yellow.png");
    public static final Identifier pheo_sliding_door_tex =
            Library.id("textures/block/models/pheodoors/sliding_door.png");
    public static final Identifier pheo_cargo_door_tex =
            Library.id("textures/block/models/pheodoors/cargo_door.png");
    public static final Identifier pheo_water_door_tex =
            Library.id("textures/block/models/pheodoors/water_door.png");
    public static final Identifier pheo_water_door_clean_tex =
            Library.id("textures/block/models/pheodoors/water_door_clean.png");
    public static final Identifier pheo_vault_door_3 =
            Library.id("textures/block/models/pheodoors/vault/vault_door_3.png");
    public static final Identifier pheo_vault_door_4 =
            Library.id("textures/block/models/pheodoors/vault/vault_door_4.png");
    public static final Identifier pheo_vault_door_s =
            Library.id("textures/block/models/pheodoors/vault/vault_door_s.png");
    public static final Identifier pheo_label_2 =
            Library.id("textures/models/pheodoors/vault/label_2.png");
    public static final Identifier pheo_label_81 =
            Library.id("textures/models/pheodoors/vault/label_81.png");
    public static final Identifier pheo_label_87 =
            Library.id("textures/models/pheodoors/vault/label_87.png");
    public static final Identifier pheo_label_99 =
            Library.id("textures/models/pheodoors/vault/label_99.png");
    public static final Identifier pheo_label_101 =
            Library.id("textures/models/pheodoors/vault/label_101.png");
    public static final Identifier pheo_label_106 =
            Library.id("textures/models/pheodoors/vault/label_106.png");
    public static final Identifier pheo_label_111 =
            Library.id("textures/models/pheodoors/vault/label_111.png");
    public static final Identifier pheo_seal_door_tex =
            Library.id("textures/block/models/pheodoors/seal_door.png");
    public static final Identifier pheo_containment_door_tex =
            Library.id("textures/block/models/pheodoors/containment_door.png");
    public static final Identifier pheo_containment_door_trefoil_tex =
            Library.id("textures/block/models/pheodoors/containment_door_trefoil.png");
    public static final Identifier pheo_containment_door_trefoil_yellow_tex =
            Library.id("textures/block/models/pheodoors/containment_door_trefoil_yellow.png");
    public static final Identifier pheo_airlock_door_tex =
            Library.id("textures/block/models/pheodoors/airlock_door.png");
    public static final Identifier pheo_airlock_door_clean_tex =
            Library.id("textures/block/models/pheodoors/airlock_door_clean.png");
    public static final Identifier pheo_airlock_door_green_tex =
            Library.id("textures/block/models/pheodoors/airlock_door_green.png");
    public static final Identifier pheo_blast_door_tex =
            Library.id("textures/block/models/pheodoors/blast_door.png");
    public static final Identifier pheo_vehicle_door_tex =
            Library.id("textures/block/models/pheodoors/vehicle_door.png");
    public static final Identifier pheo_fire_door_tex =
            Library.id("textures/block/models/pheodoors/fire_door.png");
    public static final Identifier pheo_fire_door_black_tex =
            Library.id("textures/block/models/pheodoors/fire_door_black.png");
    public static final Identifier pheo_fire_door_orange_tex =
            Library.id("textures/block/models/pheodoors/fire_door_orange.png");
    public static final Identifier pheo_fire_door_yellow_tex =
            Library.id("textures/block/models/pheodoors/fire_door_yellow.png");
    public static final Identifier pheo_fire_door_trefoil_tex =
            Library.id("textures/block/models/pheodoors/fire_door_trefoil.png");
    public static final Identifier transition_seal_tex =
            Library.id("textures/block/models/doors/transition_seal.png");
    public static final Identifier silo_hatch_tex =
            Library.id("textures/block/models/doors/silo_hatch.png");
    public static final Identifier silo_hatch_large_tex =
            Library.id("textures/block/models/doors/silo_hatch_large.png");
    public static final Identifier launch_pad_rusted_tex =
            Library.id("textures/block/models/launchpad/silo_rusted.png");

    public static final HFRWavefrontObject bobble = load("models/trinkets/bobble.obj");

    public static final HFRWavefrontObject fatman = load("models/weapons/fatman.obj");
    public static final HFRWavefrontObject double_barrel = load("models/weapons/sacred_dragon.obj");
    public static final HFRWavefrontObject n_i_4_n_i = load("models/weapons/n_i_4_n_i.obj");
    public static final HFRWavefrontObject armor_hev = load("models/armor/hev.obj");
    public static final HFRWavefrontObject armor_hat = load("models/armor/hat.obj");
    public static final HFRWavefrontObject shimmer_axe = load("models/shimmer_axe.obj");

    public static final Identifier universal_tex = Library.id("textures/models/thegadget3_.png");
    public static final Identifier bobble_socket_tex =
            Library.id("textures/block/models/trinkets/socket.png");
    public static final Identifier bobble_glow_tex =
            Library.id("textures/models/trinkets/glow.png");

    public static final Identifier fluorescent_lamp_tex =
            Library.id("textures/block/fluorescent_lamp.png");
    public static final Identifier bobble_vaultboy_tex =
            Library.id("textures/models/trinkets/vaultboy.png");
    public static final Identifier bobble_hbm_tex = Library.id("textures/models/trinkets/hbm.png");
    public static final Identifier bobble_pu238_tex =
            Library.id("textures/models/trinkets/pellet.png");
    public static final Identifier bobble_frizzle_tex =
            Library.id("textures/models/trinkets/frizzle.png");
    public static final Identifier bobble_vt_tex = Library.id("textures/models/trinkets/vt.png");
    public static final Identifier bobble_doc_tex =
            Library.id("textures/models/trinkets/doctor17ph.png");
    public static final Identifier bobble_blue_tex =
            Library.id("textures/models/trinkets/thebluehat.png");
    public static final Identifier bobble_pheo_tex =
            Library.id("textures/models/trinkets/pheo.png");
    public static final Identifier bobble_adam_tex =
            Library.id("textures/models/trinkets/adam29.png");
    public static final Identifier bobble_uffr_tex =
            Library.id("textures/models/trinkets/uffr.png");
    public static final Identifier bobble_vaer_tex =
            Library.id("textures/models/trinkets/vaer.png");
    public static final Identifier bobble_nos_tex = Library.id("textures/models/trinkets/nos.png");
    public static final Identifier bobble_drillgon_tex =
            Library.id("textures/models/trinkets/drillgon200.png");
    public static final Identifier bobble_cirno_tex =
            Library.id("textures/models/trinkets/cirno.png");
    public static final Identifier bobble_microwave_tex =
            Library.id("textures/models/trinkets/microwave.png");
    public static final Identifier bobble_peep_tex =
            Library.id("textures/models/trinkets/peep.png");
    public static final Identifier bobble_mellow_tex =
            Library.id("textures/models/trinkets/mellowrpg8.png");
    public static final Identifier bobble_mellow_glow_tex =
            Library.id("textures/models/trinkets/mellowrpg8_glow.png");
    public static final Identifier bobble_abel_tex =
            Library.id("textures/models/trinkets/abel.png");
    public static final Identifier bobble_abel_glow_tex =
            Library.id("textures/models/trinkets/abel_glow.png");

    public static final HFRWavefrontObject snowglobe = load("models/trinkets/snowglobe.obj");
    public static final Identifier snowglobe_tex =
            Library.id("textures/models/trinkets/snowglobe.png");
    public static final Identifier snowglobe_glass_tex =
            Library.id("textures/models/trinkets/snowglobe_glass.png");
    public static final Identifier snowglobe_features_tex =
            Library.id("textures/models/trinkets/snowglobe_features.png");

    public static final HFRWavefrontObject lantern = load("models/trinkets/lantern.obj").noSmooth();
    public static final Identifier lantern_tex = Library.id("textures/block/lantern.png");
    public static final Identifier lantern_rusty_tex =
            Library.id("textures/block/models/trinkets/lantern_rusty.png");

    public static final HFRWavefrontObject hundun = load("models/trinkets/hundun.obj");
    public static final HFRWavefrontObject derg = load("models/trinkets/derg.obj");
    public static final Identifier hundun_tex = Library.id("textures/models/trinkets/hundun.png");
    public static final Identifier derg_tex = Library.id("textures/models/trinkets/derg.png");
    public static final Identifier numbernine_tex =
            Library.id("textures/models/horse/numbernine.png");

    public static final Identifier fatman_mininuke_tex =
            Library.id("textures/models/weapons/fatman_mininuke.png");
    public static final Identifier cluster_submunition_tex =
            Library.id("textures/models/weapons/fatman_submunition.png");
    public static final Identifier double_barrel_tex =
            Library.id("textures/models/weapons/double_barrel.png");
    public static final Identifier double_barrel_sacred_dragon_tex =
            Library.id("textures/models/weapons/double_barrel_sacred_dragon.png");
    public static final Identifier n_i_4_n_i_tex =
            Library.id("textures/models/weapons/n_i_4_n_i.png");
    public static final Identifier n_i_4_n_i_greyscale_tex =
            Library.id("textures/models/weapons/n_i_4_n_i_greyscale.png");
    public static final Identifier hev_helmet_tex = Library.id("textures/armor/hev_helmet.png");
    public static final Identifier hat_tex = Library.id("textures/armor/hat.png");
    public static final Identifier shimmer_axe_tex = Library.id("textures/models/shimmer_axe.png");

    public static final HFRWavefrontObject carbine = load("models/weapons/carbine.obj");
    public static final HFRWavefrontObject casings = load("models/effect/casings.obj");
    public static final Identifier casings_tex = Library.id("textures/particle/casings.png");
    public static final Identifier carbine_tex = Library.id("textures/models/weapons/huntsman.png");
    public static final Identifier carbine_bayonet_tex =
            Library.id("textures/models/weapons/carbine_bayonet.png");
    public static final Identifier carbine_scope_tex =
            Library.id("textures/models/weapons/carbine_scope.png");
    public static final Identifier debug_gun_tex =
            Library.id("textures/models/weapons/debug_gun.png");
    public static final Identifier flash_plume =
            Library.id("textures/models/weapons/lilmac_plume.png");
    public static final Identifier laser_flash =
            Library.id("textures/models/weapons/laser_flash.png");
    public static final HFRWavefrontObject minigun = load("models/weapons/minigun.obj");
    public static final Identifier minigun_tex = Library.id("textures/models/weapons/minigun.png");
    public static final Identifier minigun_lacunae_tex =
            Library.id("textures/models/weapons/minigun_lacunae.png");
    public static final Identifier minigun_dual_tex =
            Library.id("textures/models/weapons/minigun_dual.png");
    public static final HFRWavefrontObject mas36 = load("models/weapons/mas36.obj");
    public static final Identifier mas36_tex = Library.id("textures/models/weapons/mas36.png");
    public static final HFRWavefrontObject bio_revolver = load("models/weapons/bio_revolver.obj");
    public static final Identifier bio_revolver_tex =
            Library.id("textures/models/weapons/bio_revolver.png");
    public static final Identifier bio_revolver_atlas_tex =
            Library.id("textures/models/weapons/bio_revolver_atlas.png");
    public static final Identifier dani_celestial_tex =
            Library.id("textures/models/weapons/dani_celestial.png");
    public static final Identifier dani_lunar_tex =
            Library.id("textures/models/weapons/dani_lunar.png");
    public static final HFRWavefrontObject henry = load("models/weapons/henry.obj");
    public static final Identifier henry_tex = Library.id("textures/models/weapons/henry.png");
    public static final Identifier henry_lincoln_tex =
            Library.id("textures/models/weapons/henry_lincoln.png");
    public static final HFRWavefrontObject lilmac = load("models/weapons/lilmac.obj");
    public static final Identifier heavy_revolver_tex =
            Library.id("textures/models/weapons/heavy_revolver.png");
    public static final Identifier heavy_revolver_protege_tex =
            Library.id("textures/models/weapons/protege.png");
    public static final Identifier lilmac_tex = Library.id("textures/models/weapons/lilmac.png");
    public static final Identifier lilmac_scope_tex =
            Library.id("textures/models/weapons/lilmac_scope.png");
    public static final HFRWavefrontObject hangman = load("models/weapons/hangman.obj");
    public static final Identifier hangman_tex = Library.id("textures/models/weapons/hangman.png");
    public static final HFRWavefrontObject greasegun = load("models/weapons/greasegun.obj");
    public static final Identifier greasegun_tex =
            Library.id("textures/models/weapons/greasegun.png");
    public static final Identifier greasegun_clean_tex =
            Library.id("textures/models/weapons/greasegun_clean.png");
    public static final HFRWavefrontObject uzi = load("models/weapons/uzi.obj");
    public static final Identifier uzi_tex = Library.id("textures/models/weapons/uzi.png");
    public static final Identifier uzi_saturnite_tex =
            Library.id("textures/models/weapons/uzi_saturnite.png");
    public static final HFRWavefrontObject maresleg = load("models/weapons/maresleg.obj");
    public static final Identifier maresleg_tex =
            Library.id("textures/models/weapons/maresleg.png");
    public static final Identifier maresleg_broken_tex =
            Library.id("textures/models/weapons/maresleg_broken.png");
    public static final HFRWavefrontObject panzerschreck = load("models/weapons/panzerschreck.obj");
    public static final Identifier panzerschreck_tex =
            Library.id("textures/models/weapons/panzerschreck.png");
    public static final HFRWavefrontObject liberator = load("models/weapons/liberator.obj");
    public static final Identifier liberator_tex =
            Library.id("textures/models/weapons/liberator.png");
    public static final HFRWavefrontObject spas_12 = load("models/weapons/spas-12.obj");
    public static final Identifier spas_12_tex = Library.id("textures/models/weapons/spas-12.png");
    public static final HashMap<String, BusAnimation> spas_12_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/spas12.json"));
    public static final HFRWavefrontObject shredder = load("models/weapons/shredder.obj");
    public static final Identifier shredder_tex =
            Library.id("textures/models/weapons/shredder.png");
    public static final Identifier shredder_orig_tex =
            Library.id("textures/models/weapons/shredder_orig.png");
    public static final HFRWavefrontObject sexy = load("models/weapons/sexy.obj");
    public static final Identifier sexy_tex =
            Library.id("textures/models/weapons/sexy_real_no_fake.png");
    public static final Identifier heretic_tex =
            Library.id("textures/models/weapons/sexy_heretic.png");
    public static final HFRWavefrontObject whiskey = load("models/weapons/whiskey.obj");
    public static final Identifier whiskey_tex = Library.id("textures/models/weapons/whiskey.png");
    public static final HFRWavefrontObject stinger = load("models/weapons/stinger.obj");
    public static final Identifier stinger_tex = Library.id("textures/models/weapons/stinger.png");
    public static final HFRWavefrontObject quadro = load("models/weapons/quadro.obj");
    public static final Identifier quadro_tex = Library.id("textures/models/weapons/quadro.png");
    public static final Identifier quadro_rocket_tex =
            Library.id("textures/models/weapons/quadro_rocket.png");
    public static final HFRWavefrontObject missile_launcher =
            load("models/weapons/missile_launcher.obj");
    public static final Identifier missile_launcher_tex =
            Library.id("textures/models/weapons/missile_launcher.png");
    public static final HFRWavefrontObject projectiles = load("models/projectiles/projectiles.obj");
    public static final Identifier rocket_mirv_tex =
            Library.id("textures/models/projectiles/rocket_mirv.png");

    public static final HFRWavefrontObject grenades = load("models/weapons/grenades.obj");
    public static final Identifier grenade_frag_tex =
            Library.id("textures/models/grenades/frag.png");
    public static final Identifier grenade_frag_body_tex =
            Library.id("textures/models/grenades/frag_body.png");
    public static final Identifier grenade_frag_label_tex =
            Library.id("textures/models/grenades/frag_label.png");
    public static final Identifier grenade_frag_fuze_tex =
            Library.id("textures/models/grenades/frag_fuze.png");
    public static final Identifier grenade_stick_tex =
            Library.id("textures/models/grenades/stick.png");
    public static final Identifier grenade_stick_body_tex =
            Library.id("textures/models/grenades/stick_body.png");
    public static final Identifier grenade_stick_label_tex =
            Library.id("textures/models/grenades/stick_label.png");
    public static final Identifier grenade_stick_fuze_tex =
            Library.id("textures/models/grenades/stick_fuze.png");
    public static final Identifier grenade_tech_tex =
            Library.id("textures/models/grenades/tech.png");
    public static final Identifier grenade_tech_body_tex =
            Library.id("textures/models/grenades/tech_body.png");
    public static final Identifier grenade_tech_fuze_tex =
            Library.id("textures/models/grenades/tech_fuze.png");
    public static final Identifier grenade_tech_lights_tex =
            Library.id("textures/models/grenades/tech_lights.png");
    public static final Identifier grenade_nuka_tex =
            Library.id("textures/models/grenades/nuka.png");
    public static final Identifier grenade_nuka_body_tex =
            Library.id("textures/models/grenades/nuka_body.png");
    public static final Identifier grenade_nuka_label_tex =
            Library.id("textures/models/grenades/nuka_label.png");
    public static final Identifier grenade_nuka_fuze_tex =
            Library.id("textures/models/grenades/nuka_fuze.png");
    public static final HFRWavefrontObject dornier = load("models/dornier.obj");
    public static final HFRWavefrontObject b29 = load("models/b29.obj");
    public static final Identifier dornier_1_tex = Library.id("textures/models/dornier_1.png");
    public static final Identifier dornier_2_tex = Library.id("textures/models/dornier_2.png");
    public static final Identifier dornier_4_tex = Library.id("textures/models/dornier_4.png");
    public static final Identifier b29_0_tex = Library.id("textures/models/b29_0.png");
    public static final Identifier b29_1_tex = Library.id("textures/models/b29_1.png");
    public static final Identifier b29_2_tex = Library.id("textures/models/b29_2.png");
    public static final Identifier b29_3_tex = Library.id("textures/models/b29_3.png");
    public static final Identifier rocket_tex =
            Library.id("textures/models/projectiles/rocket.png");
    public static final HFRWavefrontObject g3 = load("models/weapons/g3.obj");
    public static final Identifier g3_tex = Library.id("textures/models/weapons/g3.png");
    public static final Identifier g3_polymer_green_tex =
            Library.id("textures/models/weapons/g3_polymer_green.png");
    public static final Identifier g3_polymer_black_tex =
            Library.id("textures/models/weapons/g3_polymer_black.png");
    public static final Identifier g3_zebra_tex =
            Library.id("textures/models/weapons/g3_zebra.png");
    public static final Identifier g3_attachments =
            Library.id("textures/models/weapons/g3_attachments.png");
    public static final HFRWavefrontObject stg77 = load("models/weapons/stg77.obj");
    public static final Identifier stg77_tex = Library.id("textures/models/weapons/stg77.png");
    public static final HashMap<String, BusAnimation> stg77_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/stg77.json"));
    public static final HFRWavefrontObject flaregun = load("models/weapons/flaregun.obj");
    public static final Identifier flaregun_tex =
            Library.id("textures/models/weapons/flaregun.png");
    public static final HFRWavefrontObject congolake = load("models/weapons/congolake.obj");
    public static final Identifier congolake_tex =
            Library.id("textures/models/weapons/congolake.png");
    public static final HashMap<String, BusAnimation> congolake_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/congolake.json"));
    public static final HFRWavefrontObject mk108 = load("models/weapons/mk108.obj");
    public static final Identifier mk108_tex = Library.id("textures/models/weapons/mk108.png");
    public static final HFRWavefrontObject flamethrower = load("models/weapons/flamethrower.obj");
    public static final Identifier flamethrower_tex =
            Library.id("textures/models/weapons/flamethrower.png");
    public static final Identifier flamethrower_topaz_tex =
            Library.id("textures/models/weapons/flamethrower_topaz.png");
    public static final Identifier flamethrower_daybreaker_tex =
            Library.id("textures/models/weapons/flamethrower_daybreaker.png");
    public static final HashMap<String, BusAnimation> flamethrower_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/flamethrower.json"));
    public static final HFRWavefrontObject chemthrower = load("models/weapons/chemthrower.obj");
    public static final Identifier chemthrower_tex =
            Library.id("textures/models/weapons/chemthrower.png");
    public static final HFRWavefrontObject tesla_cannon = load("models/weapons/tesla_cannon.obj");
    public static final Identifier tesla_cannon_tex =
            Library.id("textures/models/weapons/tesla_cannon.png");
    public static final HFRWavefrontObject laser_pistol = load("models/weapons/laser_pistol.obj");
    public static final Identifier laser_pistol_tex =
            Library.id("textures/models/weapons/laser_pistol.png");
    public static final Identifier laser_pistol_pew_pew_tex =
            Library.id("textures/models/weapons/laser_pistol_pew_pew.png");
    public static final Identifier laser_pistol_morning_glory_tex =
            Library.id("textures/models/weapons/laser_pistol_morning_glory.png");
    public static final HFRWavefrontObject lasrifle = load("models/weapons/lasrifle.obj");
    public static final Identifier lasrifle_tex =
            Library.id("textures/models/weapons/lasrifle.png");
    public static final HFRWavefrontObject lasrifle_mods = load("models/weapons/lasrifle_mods.obj");
    public static final Identifier lasrifle_mods_tex =
            Library.id("textures/models/weapons/lasrifle_mods.png");
    public static final HFRWavefrontObject yomi = load("models/trinkets/yomi.obj");
    public static final Identifier yomi_tex = Library.id("textures/models/trinkets/yomi.png");
    public static final HFRWavefrontObject charge_thrower =
            load("models/weapons/charge_thrower.obj");
    public static final Identifier charge_thrower_tex =
            Library.id("textures/models/weapons/charge_thrower.png");
    public static final Identifier charge_thrower_hook_tex =
            Library.id("textures/models/weapons/charge_thrower_hook.png");
    public static final Identifier charge_thrower_mortar_tex =
            Library.id("textures/models/weapons/charge_thrower_mortar.png");
    public static final Identifier charge_thrower_rocket_tex =
            Library.id("textures/models/weapons/charge_thrower_rocket.png");
    public static final HFRWavefrontObject drill = load("models/weapons/drill.obj");
    public static final Identifier drill_tex = Library.id("textures/models/weapons/drill.png");
    public static final Identifier ncrpa_arm = Library.id("textures/armor/ncrpa_arm.png");
    public static final HFRWavefrontObject boxcar = load("models/boxcar.obj");
    public static final Identifier boxcar_tex = Library.id("textures/block/models/boxcar.png");
    public static final HFRWavefrontObject duchessgambit = load("models/duchessgambit.obj");
    public static final Identifier duchessgambit_tex =
            Library.id("textures/block/models/duchessgambit.png");
    public static final HFRWavefrontObject building = load("models/weapons/building.obj");
    public static final Identifier building_tex =
            Library.id("textures/models/weapons/building.png");
    public static final HFRWavefrontObject torpedo = load("models/weapons/torpedo.obj");
    public static final Identifier torpedo_tex = Library.id("textures/models/weapons/torpedo.png");
    public static final HFRWavefrontObject bomblet_theta = load("models/bomblet_theta.obj");
    public static final Identifier bomblet_zeta_tex =
            Library.id("textures/models/bomblet_zeta_texture.png");
    public static final HFRWavefrontObject folly = load("models/weapons/folly.obj");

    public static final Identifier folly_tex = Library.id("textures/models/weapons/moonlight.png");
    public static final HFRWavefrontObject aberrator = load("models/weapons/aberrator.obj");
    public static final Identifier aberrator_tex =
            Library.id("textures/models/weapons/aberrator.png");
    public static final Identifier eott_tex = Library.id("textures/models/weapons/eott.png");
    public static final Identifier fatman_tex = Library.id("textures/models/weapons/fatman.png");
    public static final Identifier fatman_balefire_tex =
            Library.id("textures/models/weapons/fatman_balefire.png");
    public static final Identifier glint_bf_tex = Library.id("textures/misc/glint_bf.png");
    public static final Identifier glint_tex = Library.id("textures/misc/glint.png");
    public static final HFRWavefrontObject c130 = load("models/weapons/c130.obj");
    public static final Identifier c130_0_tex = Library.id("textures/models/weapons/c130_0.png");
    public static final HFRWavefrontObject fstbmb = load("models/blocks/fstbmb.obj");
    public static final Identifier fstbmb_tex =
            Library.id("textures/block/models/bombs/fstbmb.png");
    public static final HFRWavefrontObject soyuz_lander = load("models/soyuz_lander.obj");
    public static final HFRWavefrontObject dropship = load("models/dropship.obj");
    public static final Identifier dropship_tex = Library.id("textures/entity/dropship.png");
    public static final Identifier soyuz_lander_tex =
            Library.id("textures/models/soyuz_capsule/soyuz_lander.png");
    public static final Identifier soyuz_lander_rust_tex =
            Library.id("textures/models/soyuz_capsule/soyuz_lander_rust.png");
    public static final Identifier soyuz_chute_tex =
            Library.id("textures/models/soyuz_capsule/soyuz_chute.png");
    public static final HFRWavefrontObject soyuz = load("models/soyuz.obj");
    public static final Identifier soyuz_memento_tex =
            Library.id("textures/item/polaroid_memento.png");

    public static final Identifier[][] soyuz_skin_tex =
            soyuzSkins("soyuz", "soyuz_luna", "soyuz_authentic");

    public static final HFRWavefrontObject miner_rocket = load("models/miner_rocket.obj");
    public static final Identifier bobmazon_tex = Library.id("textures/models/bobmazon.png");
    public static final HFRWavefrontObject lil_boy = load("models/lil_boy.obj");
    public static final Identifier bomb_boy_tex =
            Library.id("textures/block/models/bombs/lilboy.png");
    public static final Identifier custom_nuke_tex = Library.id("textures/models/custom_nuke.png");
    public static final HFRWavefrontObject emp_ring = load("models/ring.obj");
    public static final Identifier emp_ring_tex = Library.id("textures/models/emp_blast.png");
    public static final HFRWavefrontObject sat_foeq_burning = load("models/sat_foeq_burning.obj");
    public static final HFRWavefrontObject sat_foeq_fire = load("models/sat_foeq_fire.obj");
    public static final Identifier sat_foeq_burning_tex =
            Library.id("textures/models/sat_foeq_burning.png");
    public static final HFRWavefrontObject turret_himars = load("models/turrets/turret_himars.obj");
    public static final Identifier bullet_tex = Library.id("textures/models/bullet.png");
    public static final Identifier bullet_chopper_tex = Library.id("textures/models/emplacer.png");
    public static final Identifier bullet_critical_tex = Library.id("textures/models/tau.png");
    public static final Identifier chopper_bomb_tex =
            Library.id("textures/models/chopper_bomb.png");
    public static final Identifier shrapnel_tex = Library.id("textures/entity/shrapnel.png");
    public static final Identifier tomblast_tex =
            Library.id("textures/models/explosion/tomblast.png");
    public static final HFRWavefrontObject tom_main = flat("models/weapons/tom_main.obj");
    public static final Identifier tom_main_tex =
            Library.id("textures/models/weapons/tom_main.png");

    private static final float HMF_UV_INSET = 0.0005F;

    public static final HFRWavefrontObject tom_flame =
            load("models/weapons/tom_flame.obj").noSmooth().insetUv(HMF_UV_INSET);
    public static final Identifier tom_flame_tex =
            Library.id("textures/models/weapons/tom_flame.png");
    public static final Identifier black_hole_disc_tex =
            Library.id("textures/entity/bhole_disc.png");
    public static final Identifier quasar_disc_tex = Library.id("textures/entity/bhole_d.png");

    public static final HFRWavefrontObject delivery_drone = load("models/machines/drone.obj");
    public static final Identifier delivery_drone_tex =
            Library.id("textures/models/machines/drone.png");
    public static final Identifier delivery_drone_request_tex =
            Library.id("textures/models/machines/drone_request.png");
    public static final Identifier delivery_drone_express_tex =
            Library.id("textures/models/machines/drone_express.png");

    public static final HFRWavefrontObject quadcopter = load("models/mobs/quadcopter.obj");
    public static final Identifier quadcopter_tex = Library.id("textures/entity/quadcopter.png");
    public static final Identifier radbeast_tex = Library.id("textures/entity/radbeast.png");

    public static final HFRWavefrontObject teslacrab = load("models/mobs/teslacrab.obj");
    public static final HFRWavefrontObject taintcrab = load("models/mobs/taintcrab.obj");
    public static final HFRWavefrontObject blockspider = load("models/mobs/blockspider.obj");
    public static final HFRWavefrontObject plasticbag = load("models/mobs/plasticbag.obj");
    public static final HFRWavefrontObject maskman = load("models/mobs/maskman.obj");
    public static final HFRWavefrontObject ufo = load("models/mobs/ufo.obj");
    public static final HFRWavefrontObject bot_prime_head = load("models/mobs/bot_prime_head.obj");
    public static final HFRWavefrontObject bot_prime_body = load("models/mobs/bot_prime_body.obj");
    public static final Identifier teslacrab_tex = Library.id("textures/entity/teslacrab.png");
    public static final Identifier taintcrab_tex = Library.id("textures/entity/taintcrab.png");
    public static final Identifier crab_tex = Library.id("textures/entity/crab.png");
    public static final Identifier blockspider_tex = Library.id("textures/entity/blockspider.png");
    public static final Identifier plasticbag_tex = Library.id("textures/entity/plasticbag.png");
    public static final Identifier duck_tex = Library.id("textures/entity/duck.png");
    public static final Identifier pigeon_tex = Library.id("textures/entity/pigeon.png");
    public static final Identifier dummy_tex = Library.id("textures/entity/dummy.png");
    public static final Identifier boat_rubber_tex = Library.id("textures/entity/boat_rubber.png");
    public static final Identifier maskman_tex = Library.id("textures/entity/maskman.png");
    public static final Identifier iou_tex = Library.id("textures/entity/iou.png");
    public static final Identifier ufo_tex = Library.id("textures/entity/ufo.png");
    public static final Identifier mark_zero_head_tex =
            Library.id("textures/entity/mark_zero_head.png");
    public static final Identifier mark_zero_body_tex =
            Library.id("textures/entity/mark_zero_body.png");
    public static final Identifier ghost_tex = Library.id("textures/entity/ghost.png");
    public static final Identifier fbi_tex = Library.id("textures/entity/fbi.png");
    public static final Identifier creeper_nuclear_tex = Library.id("textures/entity/creeper.png");
    public static final Identifier creeper_tainted_tex =
            Library.id("textures/entity/creeper_tainted.png");
    public static final Identifier creeper_phosgene_tex =
            Library.id("textures/entity/creeper_phosgene.png");
    public static final Identifier creeper_volatile_tex =
            Library.id("textures/entity/creeper_volatile.png");
    public static final Identifier creeper_gold_tex =
            Library.id("textures/entity/creeper_gold.png");
    public static final HFRWavefrontObject conservecrate = load("models/blocks/conservecrate.obj");
    public static final Identifier supply_crate_tex = Library.id("textures/block/crate_can.png");

    public static final HFRWavefrontObject file_cabinet = load("models/blocks/file_cabinet.obj");
    public static final Identifier file_cabinet_tex =
            Library.id("textures/block/filing_cabinet.png");
    public static final Identifier file_cabinet_steel_tex =
            Library.id("textures/block/filing_cabinet_steel.png");
    public static final Identifier grenade_tex =
            Library.id("textures/models/projectiles/grenade.png");
    public static final HFRWavefrontObject amat = load("models/weapons/amat.obj");
    public static final Identifier amat_tex = Library.id("textures/models/weapons/amat.png");
    public static final Identifier amat_subtlety_tex =
            Library.id("textures/models/weapons/amat_subtlety.png");
    public static final Identifier amat_penance_tex =
            Library.id("textures/models/weapons/amat_penance.png");
    public static final HFRWavefrontObject m2 = load("models/weapons/m2_browning.obj");
    public static final Identifier m2_tex = Library.id("textures/models/weapons/m2_browning.png");
    public static final HFRWavefrontObject mike_hawk = load("models/weapons/mike_hawk.obj");
    public static final Identifier mike_hawk_tex = Library.id("textures/models/weapons/lag.png");
    public static final HashMap<String, BusAnimation> lag_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/lag.json"));
    public static final HFRWavefrontObject pepperbox = load("models/weapons/pepperbox.obj");
    public static final Identifier pepperbox_tex =
            Library.id("textures/models/weapons/pepperbox.png");
    public static final HFRWavefrontObject bolter = load("models/weapons/bolter.obj");
    public static final Identifier bolter_tex = Library.id("textures/models/weapons/bolter.png");
    public static final HFRWavefrontObject am180 = load("models/weapons/am180.obj");
    public static final Identifier am180_tex = Library.id("textures/models/weapons/am180.png");
    public static final HashMap<String, BusAnimation> am180_anim =
            AnimationLoader.load(Library.id("models/weapons/animations/am180.json"));
    public static final HFRWavefrontObject star_f = load("models/weapons/star_f.obj");
    public static final Identifier star_f_tex = Library.id("textures/models/weapons/star_f.png");
    public static final Identifier star_f_elite_tex =
            Library.id("textures/models/weapons/star_f_elite.png");
    public static final HFRWavefrontObject tau = load("models/weapons/tau.obj");
    public static final Identifier tau_tex = Library.id("textures/models/weapons/tau.png");
    public static final HFRWavefrontObject coilgun = load("models/weapons/coilgun.obj");
    public static final Identifier coilgun_tex = Library.id("textures/models/weapons/coilgun.png");
    public static final Identifier shockwave_tex = Library.id("textures/particle/shockwave.png");
    public static final Identifier particle_base_tex =
            Library.id("textures/particle/particle_base.png");
    public static final Identifier rbmk_fire_tex = Library.id("textures/particle/rbmk_fire.png");
    public static final Identifier rbmk_jet_steam_tex =
            Library.id("textures/particle/rbmk_jet_steam.png");
    public static final Identifier contrail_tex = Library.id("textures/particle/contrail.png");

    public static final HFRWavefrontObject skeleton_obj = load("models/effect/skeleton.obj");
    public static final Identifier skeleton_tex = Library.id("textures/particle/skeleton.png");
    public static final Identifier skeleton_blood_tex =
            Library.id("textures/particle/skeleton_blood.png");
    public static final Identifier skoilet_tex = Library.id("textures/particle/skoilet.png");
    public static final Identifier skoilet_blood_tex =
            Library.id("textures/particle/skoilet_blood.png");

    public static final Identifier dead_leaf_tex = Library.id("textures/particle/dead_leaf.png");
    public static final Identifier meat_tex = Library.id("textures/particle/meat.png");
    public static final Identifier slime_tex = Library.id("textures/particle/slime.png");
    public static final Identifier metal_tex = Library.id("textures/particle/metal.png");

    private static AnimatedModel transition_seal;
    private static Animation transition_seal_anim;

    private ResourceManager() {}

    public static AnimatedModel transition_seal() {
        if (transition_seal == null)
            transition_seal = ColladaLoader.load(Library.id("models/doors/seal.dae"), true);
        return transition_seal;
    }

    public static Animation transition_seal_anim() {
        if (transition_seal_anim == null)
            transition_seal_anim =
                    ColladaLoader.loadAnim(24040, Library.id("models/doors/seal.dae"));
        return transition_seal_anim;
    }

    private static HFRWavefrontObject load(String path) {
        return Meshes.load(Library.id(path));
    }

    private static HFRWavefrontObject flat(String path) {
        return Meshes.faceNormals(Library.id(path));
    }

    public static final HFRWavefrontObject rail_standard_straight =
            flat("models/blocks/rail_standard.obj");
    public static final HFRWavefrontObject rail_standard_straight_short =
            flat("models/blocks/rail_standard_short.obj");
    public static final HFRWavefrontObject rail_standard_curve =
            flat("models/blocks/rail_standard_bend.obj");
    public static final HFRWavefrontObject rail_standard_curve_wide7 =
            flat("models/blocks/rail_standard_bend_wide.obj");
    public static final HFRWavefrontObject rail_standard_curve_wide9 =
            flat("models/blocks/rail_standard_bend_wide9.obj");
    public static final HFRWavefrontObject rail_standard_ramp =
            flat("models/blocks/rail_standard_ramp.obj");
    public static final HFRWavefrontObject rail_standard_buffer =
            flat("models/blocks/rail_standard_buffer.obj");
    public static final HFRWavefrontObject rail_standard_switch =
            flat("models/blocks/rail_standard_switch.obj");
    public static final HFRWavefrontObject rail_standard_switch_flipped =
            flat("models/blocks/rail_standard_switch_flipped.obj");
    public static final HFRWavefrontObject rail_narrow_straight =
            flat("models/blocks/rail_narrow.obj");
    public static final HFRWavefrontObject rail_narrow_curve =
            flat("models/blocks/rail_narrow_bend.obj");

    public static final Identifier rail_straight_tex =
            Library.id("textures/block/rail_standard_straight.png");
    public static final Identifier rail_buffer_tex =
            Library.id("textures/block/rail_standard_buffer.png");
    public static final Identifier rail_narrow_tex =
            Library.id("textures/block/rail_narrow_neo.png");
    public static final Identifier rail_switch_sign_tex =
            Library.id("textures/block/rail_switch_sign.png");
    public static final Identifier rail_switch_sign_flipped_tex =
            Library.id("textures/block/rail_switch_sign_flipped.png");

    public static final HFRWavefrontObject train_cargo_tram = flat("models/vehicles/tram.obj");
    public static final HFRWavefrontObject train_cargo_tram_trailer =
            flat("models/vehicles/tram_trailer.obj");
    public static final Identifier train_tram_tex =
            Library.id("textures/block/models/trains/tram.png");
    public static final Identifier tram_trailer_tex =
            Library.id("textures/block/models/trains/tram_trailer.png");

    public static final HFRWavefrontObject strut = load("models/strut.obj");
    public static final Identifier strut_tex = Library.id("textures/block/models/strut.png");

    public static final HFRWavefrontObject compact_launcher = load("models/compact_launcher.obj");
    public static final HFRWavefrontObject missile_erector =
            load("models/weapons/launch_pad_erector.obj");
    public static final HFRWavefrontObject launch_table_small_pad =
            load("models/launch_table/launch_table_small_pad.obj");
    public static final HFRWavefrontObject launch_table_large_pad =
            load("models/launch_table/launch_table_large_pad.obj");
    public static final HFRWavefrontObject launch_table_small_scaffold_base =
            load("models/launch_table/launch_table_small_scaffold_base.obj");
    public static final HFRWavefrontObject launch_table_small_scaffold_connector =
            load("models/launch_table/launch_table_small_scaffold_connector.obj");
    public static final HFRWavefrontObject launch_table_small_scaffold_empty =
            load("models/launch_table/launch_table_small_scaffold_empty.obj");
    public static final HFRWavefrontObject launch_table_large_scaffold_base =
            load("models/launch_table/launch_table_large_scaffold_base.obj");
    public static final HFRWavefrontObject launch_table_large_scaffold_connector =
            load("models/launch_table/launch_table_large_scaffold_connector.obj");
    public static final HFRWavefrontObject launch_table_large_scaffold_empty =
            load("models/launch_table/launch_table_large_scaffold_empty.obj");
    public static final Identifier compact_launcher_tex =
            Library.id("textures/block/models/compact_launcher.png");
    public static final Identifier missile_erector_pad_tex =
            Library.id("textures/block/models/launchpad/pad.png");
    public static final Identifier missile_erector_abm_tex =
            Library.id("textures/block/models/launchpad/erector_abm.png");
    public static final Identifier missile_erector_micro_tex =
            Library.id("textures/block/models/launchpad/erector_micro.png");
    public static final Identifier missile_erector_v2_tex =
            Library.id("textures/block/models/launchpad/erector_v2.png");
    public static final Identifier missile_erector_strong_tex =
            Library.id("textures/block/models/launchpad/erector_strong.png");
    public static final Identifier missile_erector_huge_tex =
            Library.id("textures/block/models/launchpad/erector_huge.png");
    public static final Identifier missile_erector_atlas_tex =
            Library.id("textures/block/models/launchpad/erector_atlas.png");
    public static final Identifier launch_table_small_pad_tex =
            Library.id("textures/block/models/missile_parts/launch_table_small_pad.png");
    public static final Identifier launch_table_large_pad_tex =
            Library.id("textures/block/models/missile_parts/launch_table_large_pad.png");
    public static final Identifier launch_table_small_scaffold_base_tex =
            Library.id("textures/block/models/missile_parts/launch_table_small_scaffold_base.png");
    public static final Identifier launch_table_small_scaffold_connector_tex =
            Library.id(
                    "textures/block/models/missile_parts/launch_table_small_scaffold_connector.png");
    public static final Identifier launch_table_large_scaffold_base_tex =
            Library.id("textures/block/models/missile_parts/launch_table_large_scaffold_base.png");
    public static final Identifier launch_table_large_scaffold_connector_tex =
            Library.id(
                    "textures/block/models/missile_parts/launch_table_large_scaffold_connector.png");

    public static final HFRWavefrontObject soyuz_launcher_legs =
            load("models/launch_table/soyuz_launcher_legs.obj").noSmooth();
    public static final HFRWavefrontObject soyuz_launcher_table =
            load("models/launch_table/soyuz_launcher_table.obj").noSmooth();
    public static final HFRWavefrontObject soyuz_launcher_tower_base =
            load("models/launch_table/soyuz_launcher_tower_base.obj").noSmooth();
    public static final HFRWavefrontObject soyuz_launcher_tower =
            load("models/launch_table/soyuz_launcher_tower.obj").noSmooth();
    public static final HFRWavefrontObject soyuz_launcher_support_base =
            load("models/launch_table/soyuz_launcher_support_base.obj").noSmooth();
    public static final HFRWavefrontObject soyuz_launcher_support =
            load("models/launch_table/soyuz_launcher_support.obj").noSmooth();
    public static final HFRWavefrontObject launchpad_soyuz =
            load("models/machines/launchpad_soyuz.obj");
    public static final Identifier launchpad_soyuz_tex =
            Library.id("textures/models/machines/launchpad_soyuz.png");
    public static final Identifier soyuz_launcher_legs_tex =
            Library.id("textures/models/soyuz_launcher/launcher_leg.png");
    public static final Identifier soyuz_launcher_table_tex =
            Library.id("textures/models/soyuz_launcher/launcher_table.png");
    public static final Identifier soyuz_launcher_tower_base_tex =
            Library.id("textures/models/soyuz_launcher/launcher_tower_base.png");
    public static final Identifier soyuz_launcher_tower_tex =
            Library.id("textures/models/soyuz_launcher/launcher_tower.png");
    public static final Identifier soyuz_launcher_support_base_tex =
            Library.id("textures/models/soyuz_launcher/launcher_support_base.png");
    public static final Identifier soyuz_launcher_support_tex =
            Library.id("textures/models/soyuz_launcher/launcher_support.png");

    private static Identifier[][] soyuzSkins(String... folders) {
        String[] parts = {
            "engineblock",
            "bottomstage",
            "topstage",
            "payload",
            "payloadblocks",
            "les",
            "lesthrusters",
            "mainengines",
            "sideengines",
            "booster",
            "boosterside"
        };
        Identifier[][] skins = new Identifier[folders.length][parts.length];
        for (int skin = 0; skin < folders.length; skin++)
            for (int part = 0; part < parts.length; part++)
                skins[skin][part] =
                        Library.id("textures/models/" + folders[skin] + "/" + parts[part] + ".png");
        return skins;
    }
}
