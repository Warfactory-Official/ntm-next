// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.sound;

import com.hbm.items.ModJukeboxSongs;
import com.hbm.items.machine.ItemCassette;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<SoundEvent>[] GEIGER = new RegistryHandle[6];

    public static RegistryHandle<SoundEvent> CENTRIFUGE_LOOP;

    public static RegistryHandle<SoundEvent> CRATE_BREAK;

    public static RegistryHandle<SoundEvent> CRATE_OPEN;
    public static RegistryHandle<SoundEvent> CRATE_CLOSE;

    public static RegistryHandle<SoundEvent> STORAGE_OPEN;
    public static RegistryHandle<SoundEvent> STORAGE_CLOSE;

    public static RegistryHandle<SoundEvent> BLOCK_DEBRIS;
    public static RegistryHandle<SoundEvent> BLOCK_SCREM;

    public static RegistryHandle<SoundEvent> FEL_LOOP;
    public static RegistryHandle<SoundEvent> FUSION_REACTOR_LOOP;

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<SoundEvent>[] BROADCAST_TRACK = new RegistryHandle[3];

    public static RegistryHandle<SoundEvent> HEPHAESTUS_LOOP;
    public static RegistryHandle<SoundEvent> FENSU_HUM_LOOP;
    public static RegistryHandle<SoundEvent> HORN_NEAR_SINGLE;
    public static RegistryHandle<SoundEvent> HORN_NEAR_DUAL;
    public static RegistryHandle<SoundEvent> HORN_FAR_SINGLE;
    public static RegistryHandle<SoundEvent> HORN_FAR_DUAL;

    public static RegistryHandle<SoundEvent> PLAYER_COUGH;
    public static RegistryHandle<SoundEvent> PLAYER_VOMIT;

    public static RegistryHandle<SoundEvent> EXPLOSION_TINY;

    public static RegistryHandle<SoundEvent> GAVEL;
    public static RegistryHandle<SoundEvent> CRUCIBLE_DEPLOY;
    public static RegistryHandle<SoundEvent> CRUCIBLE_SWING;
    public static RegistryHandle<SoundEvent> QMAW_SINGER;

    public static RegistryHandle<SoundEvent> STOP;
    public static RegistryHandle<SoundEvent> UFO_BLAST;
    public static RegistryHandle<SoundEvent> PRESS_OPERATE;

    public static RegistryHandle<SoundEvent> LEVER_LARGE;

    public static RegistryHandle<SoundEvent> LEVER_START;
    public static RegistryHandle<SoundEvent> LEVER_STOP;
    public static RegistryHandle<SoundEvent> SPARK;
    public static RegistryHandle<SoundEvent> ASSEMBLER_LOOP;
    public static RegistryHandle<SoundEvent> CHEMICAL_PLANT_LOOP;
    public static RegistryHandle<SoundEvent> REFINERY_LOOP;
    public static RegistryHandle<SoundEvent> REACTOR_LOOP;
    public static RegistryHandle<SoundEvent> ASSEMBLER_START;
    public static RegistryHandle<SoundEvent> ASSEMBLER_STRIKE;
    public static RegistryHandle<SoundEvent> ASSEMBLER_STOP;
    public static RegistryHandle<SoundEvent> ASSEMBLER_CUT;
    public static RegistryHandle<SoundEvent> TECH_BOOP;

    public static RegistryHandle<SoundEvent> POTATOS_RANDOM;
    public static RegistryHandle<SoundEvent> TECH_BLEEP;

    public static RegistryHandle<SoundEvent> FSTBMB_START;
    public static RegistryHandle<SoundEvent> FSTBMB_PING;

    public static RegistryHandle<SoundEvent> PIPE_PLACED;
    public static RegistryHandle<SoundEvent> PLATEMETAL_PLACE;
    public static RegistryHandle<SoundEvent> PLATEMETAL_STEP;
    public static RegistryHandle<SoundEvent> FLESH;
    public static RegistryHandle<SoundEvent> SONAR_PING;
    public static RegistryHandle<SoundEvent> LOCK_HANG;
    public static RegistryHandle<SoundEvent> LOCK_OPEN;
    public static RegistryHandle<SoundEvent> PIN_UNLOCK;
    public static RegistryHandle<SoundEvent> PIN_BREAK;

    public static RegistryHandle<SoundEvent> GASMASK_SCREW;
    public static RegistryHandle<SoundEvent> MISSILE_TAKE_OFF;

    public static RegistryHandle<SoundEvent> MISSILE_ASSEMBLY2;
    public static RegistryHandle<SoundEvent> NUCLEAR_EXPLOSION;
    public static RegistryHandle<SoundEvent> RBMK_EXPLOSION;
    public static RegistryHandle<SoundEvent> RBMK_AZ5_COVER;
    public static RegistryHandle<SoundEvent> RBMK_SHUTDOWN;
    public static RegistryHandle<SoundEvent> DFLASH;
    public static RegistryHandle<SoundEvent> FLAMETHROWER_SHOOT;
    public static RegistryHandle<SoundEvent> COMPRESSOR_PISTON;
    public static RegistryHandle<SoundEvent> BOLTGUN;
    public static RegistryHandle<SoundEvent> METAL_IMPACT;
    public static RegistryHandle<SoundEvent> TUBE_FWOOMP;

    public static RegistryHandle<SoundEvent> REACTOR_START;
    public static RegistryHandle<SoundEvent> REACTOR_STOP;

    public static RegistryHandle<SoundEvent> UPGRADE_PLUG;

    public static RegistryHandle<SoundEvent> SLICER;

    public static RegistryHandle<SoundEvent> DOOR_OPEN;
    public static RegistryHandle<SoundEvent> DOOR_WGH_BIG_START;
    public static RegistryHandle<SoundEvent> DOOR_WGH_BIG_STOP;

    public static RegistryHandle<SoundEvent> DOOR_TRANSITION_SEAL_OPEN;
    public static RegistryHandle<SoundEvent> DOOR_ALARM6;
    public static RegistryHandle<SoundEvent> DOOR_GARAGE_MOVE;
    public static RegistryHandle<SoundEvent> DOOR_GARAGE_STOP;
    public static RegistryHandle<SoundEvent> DOOR_LEVER;
    public static RegistryHandle<SoundEvent> DOOR_QE_SLIDING_OPENED;
    public static RegistryHandle<SoundEvent> DOOR_QE_SLIDING_OPENING;
    public static RegistryHandle<SoundEvent> DOOR_QE_SLIDING_SHUT;
    public static RegistryHandle<SoundEvent> DOOR_SLIDING_DOOR_OPENED;
    public static RegistryHandle<SoundEvent> DOOR_SLIDING_DOOR_OPENING;
    public static RegistryHandle<SoundEvent> DOOR_SLIDING_DOOR_SHUT;
    public static RegistryHandle<SoundEvent> DOOR_SLIDING_SEAL_OPEN;
    public static RegistryHandle<SoundEvent> DOOR_SLIDING_SEAL_STOP;
    public static RegistryHandle<SoundEvent> DOOR_WGH_START_STREAM;
    public static RegistryHandle<SoundEvent> DOOR_WGH_STOP_STREAM;
    public static RegistryHandle<SoundEvent> DOOR_WGH_START;
    public static RegistryHandle<SoundEvent> DOOR_WGH_STOP;
    public static RegistryHandle<SoundEvent> BLOCK_VAULT_SCRAPE_NEW;
    public static RegistryHandle<SoundEvent> BLOCK_VAULT_THUD_NEW;

    public static RegistryHandle<SoundEvent> BLOCK_OPEN_C;
    public static RegistryHandle<SoundEvent> BLOCK_CLOSE_C;
    public static RegistryHandle<SoundEvent> TURRET_HOWARD_FIRE;

    public static RegistryHandle<SoundEvent> TURRET_JEREMY_FIRE;
    public static RegistryHandle<SoundEvent> TURRET_JEREMY_RELOAD;
    public static RegistryHandle<SoundEvent> TURRET_RICHARD_RELOAD;
    public static RegistryHandle<SoundEvent> TURRET_SENTRY_FIRE;
    public static RegistryHandle<SoundEvent> TURRET_SENTRY_LOCKON;
    public static RegistryHandle<SoundEvent> TURRET_MAXWELL_LOOP;

    public static RegistryHandle<SoundEvent> BOBBLE;
    public static RegistryHandle<SoundEvent> ELECTRIC_HUM_LOOP;
    public static RegistryHandle<SoundEvent> ENGINE_LOOP;
    public static RegistryHandle<SoundEvent> TURBOFAN_LOOP;

    public static RegistryHandle<SoundEvent> BLOCK_DAMAGE;
    public static RegistryHandle<SoundEvent> IGENERATOR_OPERATE;
    public static RegistryHandle<SoundEvent> LARGE_TURBINE_LOOP;
    public static RegistryHandle<SoundEvent> CHUNGUS_TURBINE_LOOP;
    public static RegistryHandle<SoundEvent> CHUNGUS_LEVER;
    public static RegistryHandle<SoundEvent> BOILER_LOOP;
    public static RegistryHandle<SoundEvent> BOILER_GROAN;
    public static RegistryHandle<SoundEvent> STEAM_ENGINE_OPERATE;
    public static RegistryHandle<SoundEvent> TURBINE_GAS_RUNNING;
    public static RegistryHandle<SoundEvent> TURBINE_GAS_STARTUP;
    public static RegistryHandle<SoundEvent> TURBINE_GAS_SHUTDOWN;
    public static RegistryHandle<SoundEvent> PYRO_OVEN_LOOP;
    public static RegistryHandle<SoundEvent> MOTOR_LOOP;
    public static RegistryHandle<SoundEvent> WARN_OVERSPEED;

    public static RegistryHandle<SoundEvent> ITEM_UNPACK;

    public static RegistryHandle<SoundEvent> ITEM_SYRINGE;
    public static RegistryHandle<SoundEvent> ITEM_RADAWAY;

    public static RegistryHandle<SoundEvent> ITEM_SPRAY;
    public static RegistryHandle<SoundEvent> ITEM_REPAIR;

    public static RegistryHandle<SoundEvent> ITEM_BATTERY;

    public static RegistryHandle<SoundEvent> STEP_METAL;
    public static RegistryHandle<SoundEvent> STEP_IRON_JUMP;
    public static RegistryHandle<SoundEvent> STEP_IRON_LAND;

    public static RegistryHandle<SoundEvent> STEP_POWERED;

    public static RegistryHandle<SoundEvent> JETPACK_THRUST;

    public static RegistryHandle<SoundEvent> WEAPON_RICOCHET;

    public static RegistryHandle<SoundEvent> WEAPON_SWITCH_MODE_1;
    public static RegistryHandle<SoundEvent> WEAPON_SWITCH_MODE_2;

    public static RegistryHandle<SoundEvent> GUN_POWDER_FIRE;

    public static RegistryHandle<SoundEvent> DISINTEGRATION;
    public static RegistryHandle<SoundEvent> CHAINSAW;
    public static RegistryHandle<SoundEvent> BONK;

    public static RegistryHandle<SoundEvent> BANG;

    public static RegistryHandle<SoundEvent> LASER_BANG;

    public static RegistryHandle<SoundEvent> VICE;
    public static RegistryHandle<SoundEvent> SLICE;

    public static RegistryHandle<SoundEvent> GUN_DRY_FIRE;

    public static RegistryHandle<SoundEvent> GUN_PISTOL_COCK;

    public static RegistryHandle<SoundEvent> GUN_MAG_REMOVE;
    public static RegistryHandle<SoundEvent> GUN_MAG_INSERT;

    public static RegistryHandle<SoundEvent> GUN_REVOLVER_CLOSE;

    public static RegistryHandle<SoundEvent> GUN_MINIGUN_FIRE;
    public static RegistryHandle<SoundEvent> GUN_LASER_GATLING_FIRE;
    public static RegistryHandle<SoundEvent> GUN_HEAVY_RIFLE_FIRE;

    public static RegistryHandle<SoundEvent> GUN_RIFLE_FIRE;
    public static RegistryHandle<SoundEvent> GUN_HEAVY_REVOLVER_FIRE;
    public static RegistryHandle<SoundEvent> GUN_LEVER_COCK;
    public static RegistryHandle<SoundEvent> GUN_SMACK;

    public static RegistryHandle<SoundEvent> GUN_COIL_FIRE;
    public static RegistryHandle<SoundEvent> GUN_COIL_RELOAD;

    public static RegistryHandle<SoundEvent> GUN_ROCKET_FIRE;
    public static RegistryHandle<SoundEvent> GUN_CANISTER_INSERT;

    public static RegistryHandle<SoundEvent> GUN_SHOTGUN_FIRE;
    public static RegistryHandle<SoundEvent> GUN_SHOTGUN_LOAD;

    public static RegistryHandle<SoundEvent> GUN_SPAS_FIRE;
    public static RegistryHandle<SoundEvent> GUN_LIBERATOR_FIRE;
    public static RegistryHandle<SoundEvent> GUN_SHREDDER_FIRE;
    public static RegistryHandle<SoundEvent> GUN_SHREDDER_CYCLE;
    public static RegistryHandle<SoundEvent> GUN_LOCKON;

    public static RegistryHandle<SoundEvent> WEAPON_TAU_SHOOT;
    public static RegistryHandle<SoundEvent> GUN_TAU_FIRE;
    public static RegistryHandle<SoundEvent> GUN_TAU_STOPFIRE;
    public static RegistryHandle<SoundEvent> GUN_TAU_LOOP;

    public static RegistryHandle<SoundEvent> GUN_STARF_FIRE;
    public static RegistryHandle<SoundEvent> GUN_ROCKET_INSERT;
    public static RegistryHandle<SoundEvent> GUN_SCREW;
    public static RegistryHandle<SoundEvent> GUN_STAB;

    public static RegistryHandle<SoundEvent> GUN_ASSAULT_FIRE;
    public static RegistryHandle<SoundEvent> GUN_RIFLE_SILENCER;
    public static RegistryHandle<SoundEvent> GUN_AMAT_FIRE;
    public static RegistryHandle<SoundEvent> GUN_AMAT_SILENCER;
    public static RegistryHandle<SoundEvent> TURRET_50BMG;
    public static RegistryHandle<SoundEvent> TURRET_CIWS_RELOAD;

    public static RegistryHandle<SoundEvent> GUN_UNDERBARREL_FIRE;
    public static RegistryHandle<SoundEvent> GUN_CONGO_FIRE;
    public static RegistryHandle<SoundEvent> GUN_MK108_FIRE;

    public static RegistryHandle<SoundEvent> GUN_GRENADE_RELOAD;
    public static RegistryHandle<SoundEvent> GUN_GRENADE_OPEN;
    public static RegistryHandle<SoundEvent> GUN_GRENADE_CLOSE;

    public static RegistryHandle<SoundEvent> GUN_FATMAN_FIRE;
    public static RegistryHandle<SoundEvent> GUN_FATMAN_RELOAD;
    public static RegistryHandle<SoundEvent> GUN_MINI_NUKE_EXPLOSION;

    public static RegistryHandle<SoundEvent> GUN_VALVE;
    public static RegistryHandle<SoundEvent> GUN_FLAMER_LOOP;

    public static RegistryHandle<SoundEvent> GUN_TESLA_FIRE;

    public static RegistryHandle<SoundEvent> TESLA_ZAP;

    public static RegistryHandle<SoundEvent> SAW_SHOOT;

    public static RegistryHandle<SoundEvent> ENTITY_CYBERCRAB;

    public static RegistryHandle<SoundEvent> ENTITY_DUCC;
    public static RegistryHandle<SoundEvent> ENTITY_MEGAQUACC;
    public static RegistryHandle<SoundEvent> GUN_LASER_PISTOL_FIRE;
    public static RegistryHandle<SoundEvent> GUN_LASER_RIFLE_FIRE;
    public static RegistryHandle<SoundEvent> BLOCK_PLUSHY;

    public static RegistryHandle<SoundEvent> BLOCK_HUNDUNS_MAGNIFICENT_HOWL;

    public static RegistryHandle<SoundEvent> GUN_EXTINGUISHER_FIRE;
    public static RegistryHandle<SoundEvent> GUN_CHARGE_FIRE;

    public static RegistryHandle<SoundEvent> GUN_ABERRATOR_FIRE;
    public static RegistryHandle<SoundEvent> GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS;
    public static RegistryHandle<SoundEvent>
            GUN_VYLET_PONY_CUTIEMARKS_AND_THE_THINGS_THAT_BIND_US_INTRO_JINGLE;

    public static RegistryHandle<SoundEvent>
            GUN_GO_GO_GADGET_FUCK_EVERYTHING_IN_THIS_GENERAL_DIRECTION;
    public static RegistryHandle<SoundEvent> GUN_SOLDIER_TF2_BOAT_EXE_WAV_MP3;
    public static RegistryHandle<SoundEvent> TRAIN_IMPACT;
    public static RegistryHandle<SoundEvent> ALARM_GAMBIT;

    public static RegistryHandle<SoundEvent> GUN_SPARK_SHOOT;

    public static RegistryHandle<SoundEvent> GUN_B92_RELOAD;
    public static RegistryHandle<SoundEvent> ALARM_CHIME;

    public static RegistryHandle<SoundEvent> ALARM_SOYUZED;

    public static RegistryHandle<SoundEvent> ALARM_HATCH;

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<SoundEvent>[] RECORD_TRACK =
            new RegistryHandle[ModJukeboxSongs.Song.VALUES.length];

    public static RegistryHandle<SoundEvent> SOYUZ_READY;

    public static RegistryHandle<SoundEvent> SOYUZ_TAKEOFF;

    public static RegistryHandle<SoundEvent> ROBIN_EXPLOSION;

    public static RegistryHandle<SoundEvent> MORTAR_WHISTLE;

    public static RegistryHandle<SoundEvent> NULL_MINE;

    public static RegistryHandle<SoundEvent> CHOPPER_CHARGE;

    public static RegistryHandle<SoundEvent> CHOPPER_DROP;

    public static RegistryHandle<SoundEvent> CHOPPER_DAMAGE;

    public static RegistryHandle<SoundEvent> CHOPPER_FLYING_LOOP;
    public static RegistryHandle<SoundEvent> CHOPPER_CRASHING_LOOP;

    public static RegistryHandle<SoundEvent> UFO_BEAM;

    public static RegistryHandle<SoundEvent> BALLS_LASER;

    public static RegistryHandle<SoundEvent> RICHARD_FIRE;

    public static RegistryHandle<SoundEvent> WEAPON_ROCKET_FLAME;
    public static RegistryHandle<SoundEvent> OLD_EXPLOSION;
    public static RegistryHandle<SoundEvent> WEAPON_EXPLOSION_MEDIUM;
    public static RegistryHandle<SoundEvent> WEAPON_EXPLOSION_LARGE_NEAR;
    public static RegistryHandle<SoundEvent> WEAPON_EXPLOSION_LARGE_FAR;
    public static RegistryHandle<SoundEvent> WEAPON_EXPLOSION_SMALL_NEAR;
    public static RegistryHandle<SoundEvent> WEAPON_EXPLOSION_SMALL_FAR;
    public static RegistryHandle<SoundEvent> METEORITE_FALLING_LOOP;
    public static RegistryHandle<SoundEvent> ENTITY_BOMBER_LOOP;
    public static RegistryHandle<SoundEvent> ENTITY_BOMBER_SMALL_LOOP;
    public static RegistryHandle<SoundEvent> BOMBER_WHISTLE;

    public static RegistryHandle<SoundEvent> BOMB_DET;
    public static RegistryHandle<SoundEvent> GRENADE_BOUNCE;

    public static RegistryHandle<SoundEvent> G_BOUNCE;
    public static RegistryHandle<SoundEvent> GRENADE_TECH;
    public static RegistryHandle<SoundEvent> GRENADE_NUKA;
    public static RegistryHandle<SoundEvent> ENTITY_PLANE_CRASH;
    public static RegistryHandle<SoundEvent> ENTITY_PLANE_SHOT_DOWN;

    public static RegistryHandle<SoundEvent> GUN_SHOTGUN_COCK;
    public static RegistryHandle<SoundEvent> GUN_SHOTGUN_OPEN;
    public static RegistryHandle<SoundEvent> GUN_SHOTGUN_CLOSE;
    public static RegistryHandle<SoundEvent> GUN_IMPACT;
    public static RegistryHandle<SoundEvent> GUN_WHACK;
    public static RegistryHandle<SoundEvent> JETPACK_TANK;
    public static RegistryHandle<SoundEvent> PLAYER_GULP;
    public static RegistryHandle<SoundEvent> PLAYER_GROAN;

    public static RegistryHandle<SoundEvent> GUN_GREASEGUN_FIRE;
    public static RegistryHandle<SoundEvent> GUN_UZI_FIRE;

    public static RegistryHandle<SoundEvent> GUN_PISTOL_FIRE;
    public static RegistryHandle<SoundEvent> GUN_REVOLVER_COCK;
    public static RegistryHandle<SoundEvent> GUN_MAG_SMALL_REMOVE;
    public static RegistryHandle<SoundEvent> GUN_MAG_SMALL_INSERT;

    public static RegistryHandle<SoundEvent> GUN_REVOLVER_SPIN;
    public static RegistryHandle<SoundEvent> GUN_LATCH_OPEN;
    public static RegistryHandle<SoundEvent> GUN_BOLT_OPEN;
    public static RegistryHandle<SoundEvent> GUN_BOLT_CLOSE;
    public static RegistryHandle<SoundEvent> GUN_RIFLE_COCK;

    public static RegistryHandle<SoundEvent> WEAPON_CASING_SHELL;
    public static RegistryHandle<SoundEvent> WEAPON_CASING_SMALL;
    public static RegistryHandle<SoundEvent> WEAPON_CASING_MEDIUM;
    public static RegistryHandle<SoundEvent> WEAPON_CASING_LARGE;

    @SuppressWarnings("unchecked")
    public static final RegistryHandle<SoundEvent>[] SIREN_TRACK =
            new RegistryHandle[ItemCassette.TrackType.VALUES.length];

    private ModSounds() {}

    public static void register(IRegistrar r) {
        CENTRIFUGE_LOOP = r.registerSound("centrifuge_loop");
        CRATE_BREAK = r.registerSound("crate_break");
        CRATE_OPEN = r.registerSound("crate_open");
        CRATE_CLOSE = r.registerSound("crate_close");
        STORAGE_OPEN = r.registerSound("storage_open");
        STORAGE_CLOSE = r.registerSound("storage_close");
        BLOCK_DEBRIS = r.registerSound("block.debris");
        BLOCK_SCREM = r.registerSound("block.screm");
        FEL_LOOP = r.registerSound("block.fel");
        FUSION_REACTOR_LOOP = r.registerSound("block.fusion_reactor_running");
        for (int i = 0; i < BROADCAST_TRACK.length; i++)
            BROADCAST_TRACK[i] = r.registerSound("block.broadcast" + (i + 1));
        HEPHAESTUS_LOOP = r.registerSound("block.hephaestus_running");
        FENSU_HUM_LOOP = r.registerSound("block.fensu_hum");
        HORN_NEAR_SINGLE = r.registerSound("block.horn_near_single");
        HORN_NEAR_DUAL = r.registerSound("block.horn_near_dual");
        HORN_FAR_SINGLE = r.registerSound("block.horn_far_single");
        HORN_FAR_DUAL = r.registerSound("block.horn_far_dual");
        PLAYER_COUGH = r.registerSound("player.cough");
        PLAYER_VOMIT = r.registerSound("player.vomit");
        EXPLOSION_TINY = r.registerSound("weapon.explosion_tiny");
        GAVEL = r.registerSound("weapon.whack");
        STOP = r.registerSound("weapon.stop");
        UFO_BLAST = r.registerSound("ufo_blast");
        PRESS_OPERATE = r.registerSound("press_operate");
        LEVER_LARGE = r.registerSound("block.lever_large");
        LEVER_START = r.registerSound("block.lever_start");
        LEVER_STOP = r.registerSound("block.lever_stop");
        SPARK = r.registerSound("block.spark");
        ASSEMBLER_LOOP = r.registerSound("assembler_loop");
        CHEMICAL_PLANT_LOOP = r.registerSound("chemical_plant_loop");
        REFINERY_LOOP = r.registerSound("refinery_loop");
        REACTOR_LOOP = r.registerSound("reactor_loop");
        ASSEMBLER_START = r.registerSound("assembler_start");
        ASSEMBLER_STRIKE = r.registerSound("assembler_strike");
        ASSEMBLER_STOP = r.registerSound("assembler_stop");
        ASSEMBLER_CUT = r.registerSound("assembler_cut");
        for (int i = 0; i < GEIGER.length; i++) GEIGER[i] = r.registerSound("geiger_" + (i + 1));
        TECH_BOOP = r.registerSound("tech_boop");
        POTATOS_RANDOM = r.registerSound("potatos.random");
        TECH_BLEEP = r.registerSound("tech_bleep");
        FSTBMB_START = r.registerSound("weapon.fstbmb_start");
        FSTBMB_PING = r.registerSound("weapon.fstbmb_ping");
        PIPE_PLACED = r.registerSound("block.pipe_placed");
        PLATEMETAL_PLACE = r.registerSound("block.platemetal_place");
        PLATEMETAL_STEP = r.registerSound("step.platemetal");
        FLESH = r.registerSound("block.flesh");
        SONAR_PING = r.registerSound("block.sonar_ping");
        LOCK_HANG = r.registerSound("block.lock_hang");
        LOCK_OPEN = r.registerSound("block.lock_open");
        PIN_UNLOCK = r.registerSound("item.pin_unlock");
        PIN_BREAK = r.registerSound("item.pin_break");
        GASMASK_SCREW = r.registerSound("item.gasmask_screw");
        ITEM_BATTERY = r.registerSound("item.battery");
        MISSILE_TAKE_OFF = r.registerSound("missile_take_off");
        MISSILE_ASSEMBLY2 = r.registerSound("block.missile_assembly2");
        NUCLEAR_EXPLOSION = r.registerSound("nuclear_explosion");
        RBMK_EXPLOSION = r.registerSound("rbmk_explosion");
        RBMK_AZ5_COVER = r.registerSound("rbmk_az5_cover");
        RBMK_SHUTDOWN = r.registerSound("rbmk_shutdown");
        DFLASH = r.registerSound("dflash");
        FLAMETHROWER_SHOOT = r.registerSound("flamethrower_shoot");

        COMPRESSOR_PISTON = r.registerSound("compressor_piston");

        BOLTGUN = r.registerSound("boltgun");
        METAL_IMPACT = r.registerSound("metal_impact");
        TUBE_FWOOMP = r.registerSound("weapon.reload.tube_fwoomp");
        REACTOR_START = r.registerSound("reactor_start");
        REACTOR_STOP = r.registerSound("reactor_stop");
        UPGRADE_PLUG = r.registerSound("upgrade_plug");
        SLICER = r.registerSound("slicer");
        DOOR_OPEN = r.registerSound("door_open");
        DOOR_WGH_BIG_START = r.registerSound("door.wgh_big_start");
        DOOR_WGH_BIG_STOP = r.registerSound("door.wgh_big_stop");
        DOOR_TRANSITION_SEAL_OPEN = r.registerSound("door.transition_seal_open");
        DOOR_ALARM6 = r.registerSound("door.alarm6");
        DOOR_GARAGE_MOVE = r.registerSound("door.garage_move");
        DOOR_GARAGE_STOP = r.registerSound("door.garage_stop");
        DOOR_LEVER = r.registerSound("door.lever");
        DOOR_QE_SLIDING_OPENED = r.registerSound("door.qe_sliding_opened");
        DOOR_QE_SLIDING_OPENING = r.registerSound("door.qe_sliding_opening");
        DOOR_QE_SLIDING_SHUT = r.registerSound("door.qe_sliding_shut");
        DOOR_SLIDING_DOOR_OPENED = r.registerSound("door.sliding_door_opened");
        DOOR_SLIDING_DOOR_OPENING = r.registerSound("door.sliding_door_opening");
        DOOR_SLIDING_DOOR_SHUT = r.registerSound("door.sliding_door_shut");
        DOOR_SLIDING_SEAL_OPEN = r.registerSound("door.sliding_seal_open");
        DOOR_SLIDING_SEAL_STOP = r.registerSound("door.sliding_seal_stop");
        DOOR_WGH_START_STREAM = r.registerSound("door.wgh_start_stream");
        DOOR_WGH_STOP_STREAM = r.registerSound("door.wgh_stop_stream");
        DOOR_WGH_START = r.registerSound("door.wgh_start");
        DOOR_WGH_STOP = r.registerSound("door.wgh_stop");
        BLOCK_VAULT_SCRAPE_NEW = r.registerSound("block.vault_scrape_new");
        BLOCK_VAULT_THUD_NEW = r.registerSound("block.vault_thud_new");
        BLOCK_OPEN_C = r.registerSound("block.open_c");
        BLOCK_CLOSE_C = r.registerSound("block.close_c");
        TURRET_HOWARD_FIRE = r.registerSound("turret.howard_fire");
        TURRET_JEREMY_FIRE = r.registerSound("turret.jeremy_fire");
        TURRET_JEREMY_RELOAD = r.registerSound("turret.jeremy_reload");
        TURRET_RICHARD_RELOAD = r.registerSound("turret.richard_reload");
        TURRET_SENTRY_FIRE = r.registerSound("turret.sentry_fire");
        TURRET_SENTRY_LOCKON = r.registerSound("turret.sentry_lockon");
        TURRET_MAXWELL_LOOP = r.registerSound("turret.maxwell_loop");
        WEAPON_ROCKET_FLAME = r.registerSound("weapon.rocket_flame");
        BOBBLE = r.registerSound("bobble");
        ELECTRIC_HUM_LOOP = r.registerSound("electric_hum_loop");
        ENGINE_LOOP = r.registerSound("engine_loop");
        TURBOFAN_LOOP = r.registerSound("turbofan_loop");
        BLOCK_DAMAGE = r.registerSound("block.damage");
        IGENERATOR_OPERATE = r.registerSound("igenerator_operate");
        LARGE_TURBINE_LOOP = r.registerSound("large_turbine_loop");
        CHUNGUS_TURBINE_LOOP = r.registerSound("chungus_turbine_loop");
        CHUNGUS_LEVER = r.registerSound("chungus_lever");
        BOILER_LOOP = r.registerSound("boiler_loop");
        BOILER_GROAN = r.registerSound("boiler_groan");
        STEAM_ENGINE_OPERATE = r.registerSound("steam_engine_operate");
        TURBINE_GAS_RUNNING = r.registerSound("turbine_gas_running");
        TURBINE_GAS_STARTUP = r.registerSound("turbine_gas_startup");
        TURBINE_GAS_SHUTDOWN = r.registerSound("turbine_gas_shutdown");
        PYRO_OVEN_LOOP = r.registerSound("pyro_operate");
        MOTOR_LOOP = r.registerSound("motor_loop");
        WARN_OVERSPEED = r.registerSound("block.warnoverspeed");
        ITEM_UNPACK = r.registerSound("item.unpack");
        ITEM_SYRINGE = r.registerSound("item.syringe");
        ITEM_RADAWAY = r.registerSound("item.radaway");
        ITEM_SPRAY = r.registerSound("item.spray");
        ITEM_REPAIR = r.registerSound("item.repair");
        STEP_METAL = r.registerSound("step.metal");
        STEP_IRON_JUMP = r.registerSound("step.iron_jump");
        STEP_IRON_LAND = r.registerSound("step.iron_land");
        STEP_POWERED = r.registerSound("step.powered");
        JETPACK_THRUST = r.registerSound("weapon.immolator_shoot");

        WEAPON_RICOCHET = r.registerSound("weapon.ricochet");
        WEAPON_SWITCH_MODE_1 = r.registerSound("weapon.switchmode1");
        WEAPON_SWITCH_MODE_2 = r.registerSound("weapon.switchmode2");
        GUN_POWDER_FIRE = r.registerSound("weapon.fire.black_powder");
        DISINTEGRATION = r.registerSound("weapon.fire.disintegration");
        CHAINSAW = r.registerSound("weapon.chainsaw");
        CRUCIBLE_DEPLOY = r.registerSound("weapon.cdeploy");
        CRUCIBLE_SWING = r.registerSound("weapon.cswing");
        BONK = r.registerSound("weapon.bonk");
        BANG = r.registerSound("weapon.bang");
        LASER_BANG = r.registerSound("weapon.laser_bang");
        VICE = r.registerSound("item.vice");
        SLICE = r.registerSound("weapon.slice");
        GUN_DRY_FIRE = r.registerSound("weapon.dry_fire");
        GUN_PISTOL_COCK = r.registerSound("weapon.pistol_cock");
        GUN_MAG_REMOVE = r.registerSound("weapon.mag_remove");
        GUN_MAG_INSERT = r.registerSound("weapon.mag_insert");
        GUN_REVOLVER_CLOSE = r.registerSound("weapon.revolver_close");
        GUN_RIFLE_FIRE = r.registerSound("weapon.fire.rifle");
        GUN_HEAVY_REVOLVER_FIRE = r.registerSound("weapon.44_shoot");
        GUN_LEVER_COCK = r.registerSound("weapon.lever_cock");
        GUN_SMACK = r.registerSound("weapon.fire.smack");
        GUN_COIL_FIRE = r.registerSound("weapon.coilgun_shoot");
        GUN_COIL_RELOAD = r.registerSound("weapon.coilgun_reload");
        GUN_ROCKET_FIRE = r.registerSound("weapon.rpg_shoot");
        GUN_CANISTER_INSERT = r.registerSound("weapon.insert_canister");
        GUN_SHOTGUN_FIRE = r.registerSound("weapon.fire.shotgun");
        GUN_SHOTGUN_LOAD = r.registerSound("weapon.shotgun_reload");
        GUN_SPAS_FIRE = r.registerSound("weapon.shotgun_shoot");
        GUN_LIBERATOR_FIRE = r.registerSound("weapon.fire.shotgun_alt");
        GUN_SHREDDER_FIRE = r.registerSound("weapon.fire.shotgun_auto");
        GUN_SHREDDER_CYCLE = r.registerSound("weapon.fire.shredder_cycle");
        GUN_LOCKON = r.registerSound("weapon.fire.lockon");
        WEAPON_TAU_SHOOT = r.registerSound("weapon.tau_shoot");
        GUN_TAU_FIRE = r.registerSound("weapon.fire.tau");
        GUN_TAU_STOPFIRE = r.registerSound("weapon.fire.tau_release");
        GUN_TAU_LOOP = r.registerSound("weapon.fire.tau_loop");
        GUN_STARF_FIRE = r.registerSound("weapon.fire.pistol_light");
        GUN_ROCKET_INSERT = r.registerSound("weapon.reload.insert_rocket");
        GUN_SCREW = r.registerSound("weapon.reload.screw");
        GUN_STAB = r.registerSound("weapon.fire.stab");
        GUN_ASSAULT_FIRE = r.registerSound("weapon.fire.assault");
        GUN_RIFLE_SILENCER = r.registerSound("weapon.fire.silenced");
        GUN_AMAT_FIRE = r.registerSound("weapon.fire.amat");
        GUN_AMAT_SILENCER = r.registerSound("weapon.silencer_shoot");
        TURRET_50BMG = r.registerSound("turret.chekhov_fire");
        TURRET_CIWS_RELOAD = r.registerSound("turret.howard_reload");
        GUN_UNDERBARREL_FIRE = r.registerSound("weapon.fire.underbarrel");
        GUN_CONGO_FIRE = r.registerSound("weapon.fire.gl");
        GUN_MK108_FIRE = r.registerSound("weapon.fire.mk108");
        GUN_GRENADE_RELOAD = r.registerSound("weapon.gl_reload");
        GUN_GRENADE_OPEN = r.registerSound("weapon.gl_open");
        GUN_GRENADE_CLOSE = r.registerSound("weapon.gl_close");
        GUN_FATMAN_FIRE = r.registerSound("weapon.fire.fatman");
        GUN_FATMAN_RELOAD = r.registerSound("weapon.reload.fatman_full");
        GUN_MINI_NUKE_EXPLOSION = r.registerSound("weapon.muke_explosion");
        GUN_VALVE = r.registerSound("weapon.reload.pressure_valve");
        GUN_FLAMER_LOOP = r.registerSound("weapon.fire.flame_loop");
        GUN_TESLA_FIRE = r.registerSound("weapon.fire.tesla");
        TESLA_ZAP = r.registerSound("weapon.tesla");
        SAW_SHOOT = r.registerSound("weapon.saw_shoot");
        ENTITY_CYBERCRAB = r.registerSound("entity.cybercrab");
        ENTITY_DUCC = r.registerSound("entity.ducc");
        ENTITY_MEGAQUACC = r.registerSound("entity.megaquacc");
        GUN_LASER_PISTOL_FIRE = r.registerSound("weapon.fire.laser_pistol");
        GUN_LASER_RIFLE_FIRE = r.registerSound("weapon.fire.laser");
        BLOCK_PLUSHY = r.registerSound("block.squeaky_toy");
        BLOCK_HUNDUNS_MAGNIFICENT_HOWL = r.registerSound("block.hunduns_magnificent_howl");
        GUN_EXTINGUISHER_FIRE = r.registerSound("weapon.extinguisher");
        GUN_CHARGE_FIRE = r.registerSound("weapon.fire.gl_charge");
        GUN_ABERRATOR_FIRE = r.registerSound("weapon.fire.aberrator");
        GUN_PLEASE_REMOVE_MY_EARDRUMS_THANKS =
                r.registerSound("weapon.fire.loudest_noise_on_earth");
        GUN_VYLET_PONY_CUTIEMARKS_AND_THE_THINGS_THAT_BIND_US_INTRO_JINGLE =
                r.registerSound("weapon.fire.vstar");
        GUN_GO_GO_GADGET_FUCK_EVERYTHING_IN_THIS_GENERAL_DIRECTION =
                r.registerSound("alarm.train_horn");
        GUN_SOLDIER_TF2_BOAT_EXE_WAV_MP3 = r.registerSound("weapon.boat");
        TRAIN_IMPACT = r.registerSound("weapon.train_impact");
        ALARM_GAMBIT = r.registerSound("alarm.gambit");
        GUN_SPARK_SHOOT = r.registerSound("weapon.spark_shoot");
        GUN_B92_RELOAD = r.registerSound("weapon.b92_reload");
        ALARM_CHIME = r.registerSound("alarm.chime");
        ALARM_SOYUZED = r.registerSound("alarm.soyuzed");
        SOYUZ_READY = r.registerSound("block.soyuz_ready");
        SOYUZ_TAKEOFF = r.registerSound("entity.soyuz_takeoff");
        ROBIN_EXPLOSION = r.registerSound("weapon.robin_explosion");
        MORTAR_WHISTLE = r.registerSound("turret.mortar_whistle");
        NULL_MINE = r.registerSound("misc.null_mine");
        CHOPPER_CHARGE = r.registerSound("entity.chopper_charge");
        CHOPPER_DROP = r.registerSound("entity.chopper_drop");
        CHOPPER_DAMAGE = r.registerSound("entity.chopper_damage");
        CHOPPER_FLYING_LOOP = r.registerSound("entity.chopper_flying_loop");
        CHOPPER_CRASHING_LOOP = r.registerSound("entity.chopper_crashing_loop");
        UFO_BEAM = r.registerSound("entity.ufo_beam");
        BALLS_LASER = r.registerSound("weapon.balls_laser");
        RICHARD_FIRE = r.registerSound("turret.richard_fire");
        OLD_EXPLOSION = r.registerSound("entity.old_explosion");
        WEAPON_EXPLOSION_MEDIUM = r.registerSound("weapon.explosion_medium");
        WEAPON_EXPLOSION_LARGE_NEAR = r.registerSound("weapon.explosion_large_near");
        WEAPON_EXPLOSION_LARGE_FAR = r.registerSound("weapon.explosion_large_far");
        WEAPON_EXPLOSION_SMALL_NEAR = r.registerSound("weapon.explosion_small_near");
        WEAPON_EXPLOSION_SMALL_FAR = r.registerSound("weapon.explosion_small_far");
        METEORITE_FALLING_LOOP = r.registerSound("entity.meteorite_falling_loop");
        ENTITY_BOMBER_LOOP = r.registerSound("entity.bomber_loop");
        ENTITY_BOMBER_SMALL_LOOP = r.registerSound("entity.bomber_small_loop");
        BOMBER_WHISTLE = r.registerSound("entity.bomb_whistle");
        BOMB_DET = r.registerSound("entity.bomb_det");
        GRENADE_BOUNCE = r.registerSound("weapon.grenade_bounce");
        G_BOUNCE = r.registerSound("weapon.g_bounce");
        GRENADE_TECH = r.registerSound("weapon.reload.grenade_tech");
        GRENADE_NUKA = r.registerSound("weapon.reload.grenade_nuka");
        ENTITY_PLANE_CRASH = r.registerSound("entity.plane_crash");
        ENTITY_PLANE_SHOT_DOWN = r.registerSound("entity.plane_shot_down");
        GUN_SHOTGUN_COCK = r.registerSound("weapon.reload.shotgun_cock");
        GUN_SHOTGUN_OPEN = r.registerSound("weapon.reload.shotgun_cock_open");
        GUN_SHOTGUN_CLOSE = r.registerSound("weapon.reload.shotgun_cock_close");
        GUN_IMPACT = r.registerSound("weapon.reload.impact");
        GUN_WHACK = r.registerSound("weapon.foley.gun_whack");
        JETPACK_TANK = r.registerSound("item.jetpack_tank");
        PLAYER_GULP = r.registerSound("player.gulp");
        PLAYER_GROAN = r.registerSound("player.groan");
        GUN_GREASEGUN_FIRE = r.registerSound("weapon.fire.grease_gun");
        GUN_UZI_FIRE = r.registerSound("weapon.fire.uzi");
        GUN_PISTOL_FIRE = r.registerSound("weapon.fire.pistol");
        GUN_REVOLVER_COCK = r.registerSound("weapon.revolver_cock");
        GUN_MAG_SMALL_REMOVE = r.registerSound("weapon.mag_small_remove");
        GUN_MAG_SMALL_INSERT = r.registerSound("weapon.mag_small_insert");
        GUN_MINIGUN_FIRE = r.registerSound("weapon.cal_shoot");
        GUN_LASER_GATLING_FIRE = r.registerSound("weapon.fire.laser_gatling");
        GUN_HEAVY_RIFLE_FIRE = r.registerSound("weapon.fire.rifle_heavy");
        GUN_REVOLVER_SPIN = r.registerSound("weapon.revolver_spin");
        GUN_LATCH_OPEN = r.registerSound("weapon.open_latch");
        GUN_BOLT_OPEN = r.registerSound("weapon.bolt_open");
        GUN_BOLT_CLOSE = r.registerSound("weapon.bolt_close");
        GUN_RIFLE_COCK = r.registerSound("weapon.rifle_cock");
        WEAPON_CASING_SHELL = r.registerSound("weapon.casing.shell");
        WEAPON_CASING_SMALL = r.registerSound("weapon.casing.small");
        WEAPON_CASING_MEDIUM = r.registerSound("weapon.casing.medium");
        WEAPON_CASING_LARGE = r.registerSound("weapon.casing.large");
        for (ItemCassette.TrackType track : ItemCassette.TrackType.VALUES) {
            String name = track.getSoundName();
            if (name != null) SIREN_TRACK[track.ordinal()] = r.registerSound(name);
        }
        ALARM_HATCH = SIREN_TRACK[ItemCassette.TrackType.HATCH.ordinal()];
        for (ModJukeboxSongs.Song song : ModJukeboxSongs.Song.VALUES) {
            RECORD_TRACK[song.ordinal()] = r.registerSound(song.soundName());
        }
        QMAW_SINGER = r.registerSound("alarm.singer");
    }
}
