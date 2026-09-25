// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.blocks.generic.BlockDynamicSlag;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.*;
import com.hbm.tileentity.bomb.*;
import com.hbm.tileentity.deco.BlockEntityObjTester;
import com.hbm.tileentity.machine.*;
import com.hbm.tileentity.machine.albion.*;
import com.hbm.tileentity.machine.fusion.*;
import com.hbm.tileentity.machine.oil.*;
import com.hbm.tileentity.machine.pile.*;
import com.hbm.tileentity.machine.rbmk.*;
import com.hbm.tileentity.machine.storage.*;
import com.hbm.tileentity.network.*;
import com.hbm.tileentity.network.pneumatic.*;
import com.hbm.tileentity.turret.BlockEntityTurretArty;
import com.hbm.tileentity.turret.BlockEntityTurretChekhov;
import com.hbm.tileentity.turret.BlockEntityTurretFriendly;
import com.hbm.tileentity.turret.BlockEntityTurretFritz;
import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import com.hbm.tileentity.turret.BlockEntityTurretHoward;
import com.hbm.tileentity.turret.BlockEntityTurretHowardDamaged;
import com.hbm.tileentity.turret.BlockEntityTurretJeremy;
import com.hbm.tileentity.turret.BlockEntityTurretMaxwell;
import com.hbm.tileentity.turret.BlockEntityTurretRichard;
import com.hbm.tileentity.turret.BlockEntityTurretSentry;
import com.hbm.tileentity.turret.BlockEntityTurretSentryDamaged;
import com.hbm.tileentity.turret.BlockEntityTurretTauon;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final RegistryHandle<BlockEntityType<TestBlockEntity>> TEST =
            be("test_block_entity", TestBlockEntity::new, ModBlocks.TEST_BLOCK);
    public static final RegistryHandle<BlockEntityType<BlockEntityObjTester>> OBJ_TESTER =
            be("objtester", BlockEntityObjTester::new, ModBlocks.OBJ_TESTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRail>> RAIL =
            be(
                    "rail_switch",
                    BlockEntityRail::new,
                    ModBlocks.RAIL_LARGE_STRAIGHT,
                    ModBlocks.RAIL_LARGE_STRAIGHT_SHORT,
                    ModBlocks.RAIL_LARGE_CURVE,
                    ModBlocks.RAIL_LARGE_CURVE_7,
                    ModBlocks.RAIL_LARGE_CURVE_9,
                    ModBlocks.RAIL_LARGE_RAMP,
                    ModBlocks.RAIL_LARGE_BUFFER,
                    ModBlocks.RAIL_LARGE_SWITCH,
                    ModBlocks.RAIL_LARGE_SWITCH_FLIPPED,
                    ModBlocks.RAIL_NARROW_STRAIGHT,
                    ModBlocks.RAIL_NARROW_CURVE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCentrifuge>> CENTRIFUGE =
            clientMachine(
                    "centrifuge", BlockEntityMachineCentrifuge::new, ModBlocks.MACHINE_CENTRIFUGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineGasCent>> GAS_CENTRIFUGE =
            clientMachine(
                    "gas_centrifuge", BlockEntityMachineGasCent::new, ModBlocks.MACHINE_GASCENT);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSuperComputer>>
            SUPERCOMPUTER =
                    machine(
                            "supercomputer",
                            BlockEntityMachineSuperComputer::new,
                            ModBlocks.MACHINE_SUPERCOMPUTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineTapeDrive>> TAPE_DRIVE =
            machine("tape_drive", BlockEntityMachineTapeDrive::new, ModBlocks.MACHINE_TAPE_DRIVE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineElectricFurnace>>
            ELECTRIC_FURNACE =
                    machine(
                            "electric_furnace",
                            BlockEntityMachineElectricFurnace::new,
                            ModBlocks.MACHINE_ELECTRIC_FURNACE);
    public static final RegistryHandle<BlockEntityType<BlockEntityGeiger>> GEIGER =
            be("geiger", BlockEntityGeiger::new, ModBlocks.GEIGER)
                    .ticksServerAt(BlockEntityGeiger::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityCyberCrab>> CRABS =
            be("crabs", BlockEntityCyberCrab::new, ModBlocks.METEOR_SPAWNER)
                    .ticksServerAt(BlockEntityCyberCrab::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityRebar>> REBAR =
            be("rebar", BlockEntityRebar::new, ModBlocks.REBAR);
    public static final RegistryHandle<BlockEntityType<BlockEntityTesla>> TESLA_COIL =
            machine("tesla_coil", BlockEntityTesla::new, ModBlocks.TESLA);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePress>> PRESS =
            clientMachine("press", BlockEntityMachinePress::new, ModBlocks.MACHINE_PRESS);
    public static final RegistryHandle<BlockEntityType<BlockEntityConveyorPress>> CONVEYOR_PRESS =
            clientMachine(
                    "conveyor_press",
                    BlockEntityConveyorPress::new,
                    ModBlocks.MACHINE_CONVEYOR_PRESS);

    public static final RegistryHandle<BlockEntityType<BlockEntityCraneInserter>> CRANE_INSERTER =
            machine("inserter", BlockEntityCraneInserter::new, ModBlocks.CRANE_INSERTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneExtractor>> CRANE_EXTRACTOR =
            machine("extractor", BlockEntityCraneExtractor::new, ModBlocks.CRANE_EXTRACTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneGrabber>> CRANE_GRABBER =
            machine("grabber", BlockEntityCraneGrabber::new, ModBlocks.CRANE_GRABBER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneBoxer>> CRANE_BOXER =
            machine("boxer", BlockEntityCraneBoxer::new, ModBlocks.CRANE_BOXER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneUnboxer>> CRANE_UNBOXER =
            machine("unboxer", BlockEntityCraneUnboxer::new, ModBlocks.CRANE_UNBOXER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneRouter>> CRANE_ROUTER =
            be("router", BlockEntityCraneRouter::new, ModBlocks.CRANE_ROUTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneSplitter>> CRANE_SPLITTER =
            be("splitter", BlockEntityCraneSplitter::new, ModBlocks.CRANE_SPLITTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCranePartitioner>>
            CRANE_PARTITIONER =
                    machine(
                            "partitioner",
                            BlockEntityCranePartitioner::new,
                            ModBlocks.CRANE_PARTITIONER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAutocrafter>> AUTOCRAFTER =
            machine(
                    "autocrafter",
                    BlockEntityMachineAutocrafter::new,
                    ModBlocks.MACHINE_AUTOCRAFTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeMan>> NUKEMAN =
            be("nukeman", BlockEntityNukeMan::new, ModBlocks.NUKE_MAN);

    public static final RegistryHandle<BlockEntityType<BlockEntityNukeGadget>> NUKE_GADGET =
            be("nukegadget", BlockEntityNukeGadget::new, ModBlocks.NUKE_GADGET);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeBoy>> NUKE_BOY =
            be("nukeboy", BlockEntityNukeBoy::new, ModBlocks.NUKE_BOY);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeMike>> NUKE_MIKE =
            be("nukemike", BlockEntityNukeMike::new, ModBlocks.NUKE_MIKE);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeTsar>> NUKE_TSAR =
            be("nuketsar", BlockEntityNukeTsar::new, ModBlocks.NUKE_TSAR);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeFleija>> NUKE_FLEIJA =
            be("nukefleija", BlockEntityNukeFleija::new, ModBlocks.NUKE_FLEIJA);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukePrototype>> NUKE_PROTOTYPE =
            be("nukeproto", BlockEntityNukePrototype::new, ModBlocks.NUKE_PROTOTYPE);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeSolinium>> NUKE_SOLINIUM =
            be("nuke_solinium", BlockEntityNukeSolinium::new, ModBlocks.NUKE_SOLINIUM);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeN2>> NUKE_N2 =
            be("nuke_n2", BlockEntityNukeN2::new, ModBlocks.NUKE_N2);

    public static final RegistryHandle<BlockEntityType<BlockEntityMachineMissileAssembly>>
            MISSILE_ASSEMBLY =
                    be(
                            "missile_assembly",
                            BlockEntityMachineMissileAssembly::new,
                            ModBlocks.MACHINE_MISSILE_ASSEMBLY);
    public static final RegistryHandle<BlockEntityType<BlockEntityLaunchPad>> LAUNCH1 =
            clientMachine("launch1", BlockEntityLaunchPad::new, ModBlocks.LAUNCH_PAD);
    public static final RegistryHandle<BlockEntityType<BlockEntityLaunchPadRusted>>
            LAUNCHPAD_RUSTED =
                    be(
                                    "launchpad_rusted",
                                    BlockEntityLaunchPadRusted::new,
                                    ModBlocks.LAUNCH_PAD_RUSTED)
                            .ticksClient(BlockEntityLaunchPadRusted::tickClient);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRadar>> RADAR =
            clientMachine("radar", BlockEntityMachineRadar::new, ModBlocks.MACHINE_RADAR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRadarLarge>> RADAR_LARGE =
            clientMachine(
                    "radar_large",
                    BlockEntityMachineRadarLarge::new,
                    ModBlocks.MACHINE_RADAR_LARGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRadarScreen>>
            RADAR_SCREEN =
                    be("radar_screen", BlockEntityMachineRadarScreen::new, ModBlocks.RADAR_SCREEN)
                            .ticksServer(BlockEntityMachineRadarScreen::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityLaunchTable>> LARGE_LAUNCH_TABLE =
            clientMachine(
                    "large_launch_table", BlockEntityLaunchTable::new, ModBlocks.LAUNCH_TABLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityLaunchPadLarge>> LAUNCHPAD_LARGE =
            clientMachine(
                    "launchpad_large", BlockEntityLaunchPadLarge::new, ModBlocks.LAUNCH_PAD_LARGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityCompactLauncher>> SMALL_LAUNCHER =
            clientMachine(
                    "small_launcher", BlockEntityCompactLauncher::new, ModBlocks.COMPACT_LAUNCHER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMultiblock>> MULTI_CORE =
            be(
                            "multi_core",
                            BlockEntityMultiblock::new,
                            ModBlocks.STRUCT_LAUNCHER_CORE,
                            ModBlocks.STRUCT_LAUNCHER_CORE_LARGE)
                    .ticksServer(BlockEntityMultiblock::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntitySoyuzCapsule>> SOYUZ_CAPSULE =
            be("soyuz_capsule", BlockEntitySoyuzCapsule::new, ModBlocks.SOYUZ_CAPSULE);
    public static final RegistryHandle<BlockEntityType<BlockEntitySoyuzStruct>> SOYUZ_STRUCT =
            be("soyuz_struct", BlockEntitySoyuzStruct::new, ModBlocks.STRUCT_SOYUZ_CORE)
                    .ticksServer(BlockEntitySoyuzStruct::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntitySoyuzLauncher>> SOYUZ_LAUNCHER =
            clientMachine(
                    "soyuz_launcher", BlockEntitySoyuzLauncher::new, ModBlocks.SOYUZ_LAUNCHER);
    public static final RegistryHandle<BlockEntityType<BlockEntityLaunchpadSoyuz>> LAUNCHPAD_SOYUZ =
            clientMachine(
                    "launchpad_soyuz", BlockEntityLaunchpadSoyuz::new, ModBlocks.LAUNCHPAD_SOYUZ);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSatDock>> MINER_DOCK =
            machine("miner_dock", BlockEntityMachineSatDock::new, ModBlocks.SAT_DOCK);
    public static final RegistryHandle<BlockEntityType<BlockEntityDoorGeneric>> NTM_DOOR =
            be(
                            "door",
                            BlockEntityDoorGeneric::new,
                            ModBlocks.TRANSITION_SEAL,
                            ModBlocks.VAULT_DOOR,
                            ModBlocks.SLIDING_SEAL_DOOR,
                            ModBlocks.QE_CONTAINMENT,
                            ModBlocks.ROUND_AIRLOCK_DOOR,
                            ModBlocks.SLIDING_BLAST_DOOR,
                            ModBlocks.LARGE_VEHICLE_DOOR,
                            ModBlocks.SECURE_ACCESS_DOOR,
                            ModBlocks.QE_SLIDING_DOOR,
                            ModBlocks.CARGO_DOOR,
                            ModBlocks.WATER_DOOR,
                            ModBlocks.FIRE_DOOR,
                            ModBlocks.SILO_HATCH,
                            ModBlocks.SILO_HATCH_LARGE)
                    .ticks(BlockEntityDoorGeneric::tickClient, BlockEntityDoorGeneric::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretHowardDamaged>>
            TURRET_HOWARD_DAMAGED =
                    be(
                                    "turret_howard_damaged",
                                    BlockEntityTurretHowardDamaged::new,
                                    ModBlocks.TURRET_HOWARD_DAMAGED)
                            .ticks(
                                    BlockEntityTurretHowardDamaged::tickClient,
                                    BlockEntityTurretHowardDamaged::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretChekhov>> TURRET_CHEKHOV =
            be("turret_chekhov", BlockEntityTurretChekhov::new, ModBlocks.TURRET_CHEKHOV)
                    .ticks(
                            BlockEntityTurretChekhov::tickClient,
                            BlockEntityTurretChekhov::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretFriendly>> TURRET_FRIENDLY =
            be("turret_friendly", BlockEntityTurretFriendly::new, ModBlocks.TURRET_FRIENDLY)
                    .ticks(
                            BlockEntityTurretFriendly::tickClient,
                            BlockEntityTurretFriendly::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretJeremy>> TURRET_JEREMY =
            be("turret_jeremy", BlockEntityTurretJeremy::new, ModBlocks.TURRET_JEREMY)
                    .ticks(
                            BlockEntityTurretJeremy::tickClient,
                            BlockEntityTurretJeremy::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretRichard>> TURRET_RICHARD =
            be("turret_richard", BlockEntityTurretRichard::new, ModBlocks.TURRET_RICHARD)
                    .ticks(
                            BlockEntityTurretRichard::tickClient,
                            BlockEntityTurretRichard::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretHoward>> TURRET_HOWARD =
            be("turret_howard", BlockEntityTurretHoward::new, ModBlocks.TURRET_HOWARD)
                    .ticks(
                            BlockEntityTurretHoward::tickClient,
                            BlockEntityTurretHoward::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretTauon>> TURRET_TAUON =
            be("turret_tauon", BlockEntityTurretTauon::new, ModBlocks.TURRET_TAUON)
                    .ticks(BlockEntityTurretTauon::tickClient, BlockEntityTurretTauon::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretFritz>> TURRET_FRITZ =
            be("turret_fritz", BlockEntityTurretFritz::new, ModBlocks.TURRET_FRITZ)
                    .ticks(BlockEntityTurretFritz::tickClient, BlockEntityTurretFritz::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretMaxwell>> TURRET_MAXWELL =
            be("turret_maxwell", BlockEntityTurretMaxwell::new, ModBlocks.TURRET_MAXWELL)
                    .ticks(
                            BlockEntityTurretMaxwell::tickClient,
                            BlockEntityTurretMaxwell::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretSentry>> TURRET_SENTRY =
            be("turret_sentry", BlockEntityTurretSentry::new, ModBlocks.TURRET_SENTRY)
                    .ticks(
                            BlockEntityTurretSentry::tickClient,
                            BlockEntityTurretSentry::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretSentryDamaged>>
            TURRET_SENTRY_DAMAGED =
                    be(
                                    "turret_sentry_damaged",
                                    BlockEntityTurretSentryDamaged::new,
                                    ModBlocks.TURRET_SENTRY_DAMAGED)
                            .ticks(
                                    BlockEntityTurretSentryDamaged::tickClient,
                                    BlockEntityTurretSentryDamaged::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretArty>> TURRET_ARTY =
            be("turret_arty", BlockEntityTurretArty::new, ModBlocks.TURRET_ARTY)
                    .ticks(BlockEntityTurretArty::tickClient, BlockEntityTurretArty::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTurretHIMARS>> TURRET_HIMARS =
            be("turret_himars", BlockEntityTurretHIMARS::new, ModBlocks.TURRET_HIMARS)
                    .ticks(
                            BlockEntityTurretHIMARS::tickClient,
                            BlockEntityTurretHIMARS::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntitySkeletonHolder>> NTM_SKELETON =
            be("skeleton", BlockEntitySkeletonHolder::new, ModBlocks.SKELETON_HOLDER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPedestal>> NTM_PEDESTAL =
            be("pedestal", BlockEntityPedestal::new, ModBlocks.PEDESTAL)
                    .ticksServerAt(BlockEntityPedestal::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityDungeonSpawner>>
            NTM_DUNGEON_SPAWNER =
                    be("dungeon_spawner", BlockEntityDungeonSpawner::new, ModBlocks.DUNGEON_SPAWNER)
                            .ticksServerAt(BlockEntityDungeonSpawner::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityBobble>> NTM_BOBBLEHEAD =
            be("bobblehead", BlockEntityBobble::new, ModBlocks.BOBBLEHEAD);
    public static final RegistryHandle<BlockEntityType<BlockEntitySnowglobe>> NTM_SNOWGLOBE =
            be("snowglobe", BlockEntitySnowglobe::new, ModBlocks.SNOWGLOBE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPlushie>> NTM_PLUSHIE =
            be("plushie", BlockEntityPlushie::new, ModBlocks.PLUSHIE);
    public static final RegistryHandle<BlockEntityType<BlockEntityFloodlight>> FLOODLIGHT =
            be("floodlight", BlockEntityFloodlight::new, ModBlocks.FLOODLIGHT)
                    .ticksServer(BlockEntityFloodlight::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFloodlightBeam>> FLOODLIGHT_BEAM =
            be("floodlight_beam", BlockEntityFloodlightBeam::new, ModBlocks.FLOODLIGHT_BEAM);
    public static final RegistryHandle<BlockEntityType<BlockEntityDemonLamp>> DEMON_LAMP =
            be("demonlamp", BlockEntityDemonLamp::new, ModBlocks.LAMP_DEMON)
                    .ticksServer(BlockEntityDemonLamp::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAssemblyMachine>>
            ASSEMBLYMACHINE =
                    clientMachine(
                            "assemblymachine",
                            BlockEntityMachineAssemblyMachine::new,
                            ModBlocks.MACHINE_ASSEMBLY_MACHINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAssemblyFactory>>
            ASSEMBLYFACTORY =
                    clientMachine(
                            "assemblyfactory",
                            BlockEntityMachineAssemblyFactory::new,
                            ModBlocks.MACHINE_ASSEMBLY_FACTORY);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineChemicalPlant>>
            CHEMICALPLANT =
                    clientMachine(
                            "chemicalplant",
                            BlockEntityMachineChemicalPlant::new,
                            ModBlocks.MACHINE_CHEMICAL_PLANT);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRockMill>> ROCK_MILL =
            clientMachine(
                    "rock_mill", BlockEntityMachineRockMill::new, ModBlocks.MACHINE_ROCK_MILL);
    public static final RegistryHandle<BlockEntityType<BlockEntityCustomMachine>> CUSTOM_MACHINE =
            machine("custom_machine", BlockEntityCustomMachine::new, ModBlocks.CUSTOM_MACHINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRadiolysis>> RADIOLYSIS =
            machine("radiolysis", BlockEntityMachineRadiolysis::new, ModBlocks.MACHINE_RADIOLYSIS);
    public static final RegistryHandle<BlockEntityType<BlockEntityDeuteriumExtractor>>
            DEUTERIUM_EXTRACTOR =
                    machine(
                            "deuterium_extractor",
                            BlockEntityDeuteriumExtractor::new,
                            ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityDeuteriumTower>> DEUTERIUM_TOWER =
            machine(
                    "deuterium_tower",
                    BlockEntityDeuteriumTower::new,
                    ModBlocks.MACHINE_DEUTERIUM_TOWER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineChemicalFactory>>
            CHEMICALFACTORY =
                    clientMachine(
                            "chemicalfactory",
                            BlockEntityMachineChemicalFactory::new,
                            ModBlocks.MACHINE_CHEMICAL_FACTORY);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineElectrolyser>>
            ELECTROLYSER =
                    clientMachine(
                            "electrolyser",
                            BlockEntityMachineElectrolyser::new,
                            ModBlocks.MACHINE_ELECTROLYSER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRefinery>> REFINERY =
            clientMachine("refinery", BlockEntityMachineRefinery::new, ModBlocks.MACHINE_REFINERY);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCatalyticReformer>>
            CATALYTIC_REFORMER =
                    machine(
                            "catalytic_reformer",
                            BlockEntityMachineCatalyticReformer::new,
                            ModBlocks.MACHINE_CATALYTIC_REFORMER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineHydrotreater>>
            HYDROTREATER =
                    machine(
                            "hydrotreater",
                            BlockEntityMachineHydrotreater::new,
                            ModBlocks.MACHINE_HYDROTREATER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineFractionTower>>
            FRACTION_TOWER =
                    machine(
                            "fraction_tower",
                            BlockEntityMachineFractionTower::new,
                            ModBlocks.MACHINE_FRACTION_TOWER);
    public static final RegistryHandle<BlockEntityType<BlockEntityFractionSpacer>> FRACTION_SPACER =
            be("fraction_spacer", BlockEntityFractionSpacer::new, ModBlocks.FRACTION_SPACER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCrackingTower>>
            CATALYTIC_CRACKER =
                    machine(
                            "catalytic_cracker",
                            BlockEntityMachineCrackingTower::new,
                            ModBlocks.MACHINE_CRACKING_TOWER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePUREX>> PUREX =
            clientMachine("purex", BlockEntityMachinePUREX::new, ModBlocks.MACHINE_PUREX);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineMixer>> MIXER =
            clientMachine("mixer", BlockEntityMachineMixer::new, ModBlocks.MACHINE_MIXER);
    public static final RegistryHandle<BlockEntityType<BlockEntitySILEX>> SILEX =
            machine("silex", BlockEntitySILEX::new, ModBlocks.MACHINE_SILEX);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineOreSlopper>> ORE_SLOPPER =
            clientMachine(
                    "ore_slopper",
                    BlockEntityMachineOreSlopper::new,
                    ModBlocks.MACHINE_ORE_SLOPPER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAnnihilator>> ANNIHILATOR =
            machine(
                    "annihilator",
                    BlockEntityMachineAnnihilator::new,
                    ModBlocks.MACHINE_ANNIHILATOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineBlastFurnace>>
            BLAST_FURNACE =
                    clientMachine(
                            "blast_furnace",
                            BlockEntityMachineBlastFurnace::new,
                            ModBlocks.MACHINE_BLAST_FURNACE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineHephaestus>> HEPHAESTUS =
            clientMachine(
                    "hephaestus", BlockEntityMachineHephaestus::new, ModBlocks.MACHINE_HEPHAESTUS);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrucible>> CRUCIBLE =
            clientMachine("crucible", BlockEntityCrucible::new, ModBlocks.MACHINE_CRUCIBLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineStrandCaster>>
            STRAND_CASTER =
                    foundry(
                            "strand_caster",
                            BlockEntityMachineStrandCaster::new,
                            ModBlocks.MACHINE_STRAND_CASTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePrecAss>> PRECASS =
            clientMachine("precass", BlockEntityMachinePrecAss::new, ModBlocks.MACHINE_PRECASS);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineMiningLaser>>
            MINING_LASER =
                    machine(
                            "mining_laser",
                            BlockEntityMachineMiningLaser::new,
                            ModBlocks.MACHINE_MINING_LASER);

    public static final RegistryHandle<BlockEntityType<BlockEntityForceField>> FORCEFIELD =
            clientMachine(
                    "machine_field", BlockEntityForceField::new, ModBlocks.MACHINE_FORCEFIELD);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineExcavator>> EXCAVATOR =
            clientMachine(
                    "excavator", BlockEntityMachineExcavator::new, ModBlocks.MACHINE_EXCAVATOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCyclotron>> CYCLOTRON =
            machine("cyclotron", BlockEntityMachineCyclotron::new, ModBlocks.MACHINE_CYCLOTRON);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineExposureChamber>>
            EXPOSURE_CHAMBER =
                    clientMachine(
                            "exposure_chamber",
                            BlockEntityMachineExposureChamber::new,
                            ModBlocks.MACHINE_EXPOSURE_CHAMBER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRadGen>> RADGEN =
            machine("radgen", BlockEntityMachineRadGen::new, ModBlocks.MACHINE_RADGEN);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineReactorBreeding>>
            MACHINE_REACTOR_BREEDING =
                    machine(
                            "reactor",
                            BlockEntityMachineReactorBreeding::new,
                            ModBlocks.MACHINE_REACTOR_BREEDING);
    public static final RegistryHandle<BlockEntityType<BlockEntityReactorResearch>>
            REACTOR_RESEARCH =
                    clientMachine(
                            "small_reactor",
                            BlockEntityReactorResearch::new,
                            ModBlocks.REACTOR_RESEARCH);
    public static final RegistryHandle<BlockEntityType<BlockEntityReactorControl>> REACTOR_CONTROL =
            machine(
                    "reactor_remote_control",
                    BlockEntityReactorControl::new,
                    ModBlocks.MACHINE_CONTROLLER);
    public static final RegistryHandle<BlockEntityType<BlockEntityTowerSmall>> TOWER_SMALL =
            clientMachine(
                    "cooling_tower_small",
                    BlockEntityTowerSmall::new,
                    ModBlocks.MACHINE_TOWER_SMALL);
    public static final RegistryHandle<BlockEntityType<BlockEntityTowerLarge>> TOWER_LARGE =
            clientMachine(
                    "cooling_tower_large",
                    BlockEntityTowerLarge::new,
                    ModBlocks.MACHINE_TOWER_LARGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRotaryFurnace>>
            ROTARY_FURNACE =
                    clientMachine(
                            "rotary_furnace",
                            BlockEntityMachineRotaryFurnace::new,
                            ModBlocks.MACHINE_ROTARY_FURNACE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineArcFurnace>>
            ARC_FURNACE_LARGE =
                    clientMachine(
                            "arc_furnace_large",
                            BlockEntityMachineArcFurnace::new,
                            ModBlocks.MACHINE_ARC_FURNACE);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeaterFirebox>> FIREBOX =
            clientMachine("firebox", BlockEntityHeaterFirebox::new, ModBlocks.HEATER_FIREBOX);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeaterElectric>> ELECTRIC_HEATER =
            clientMachine(
                    "electric_heater", BlockEntityHeaterElectric::new, ModBlocks.HEATER_ELECTRIC);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundryMold>> FOUNDRY_MOLD =
            foundry("foundry_mold", BlockEntityFoundryMold::new, ModBlocks.FOUNDRY_MOLD);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundryBasin>> FOUNDRY_BASIN =
            foundry("foundry_basin", BlockEntityFoundryBasin::new, ModBlocks.FOUNDRY_BASIN);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundryChannel>> FOUNDRY_CHANNEL =
            foundry("foundry_channel", BlockEntityFoundryChannel::new, ModBlocks.FOUNDRY_CHANNEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundryOutlet>> FOUNDRY_OUTLET =
            clientFoundry(
                    "foundry_outlet", BlockEntityFoundryOutlet::new, ModBlocks.FOUNDRY_OUTLET);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundrySlagtap>> FOUNDRY_SLAGTAP =
            clientFoundry(
                    "foundry_slagtap", BlockEntityFoundrySlagtap::new, ModBlocks.FOUNDRY_SLAGTAP);
    public static final RegistryHandle<BlockEntityType<BlockEntityFoundryTank>> FOUNDRY_TANK =
            foundry("foundry_tank", BlockEntityFoundryTank::new, ModBlocks.FOUNDRY_TANK);
    public static final RegistryHandle<BlockEntityType<BlockDynamicSlag.BlockEntitySlag>>
            FOUNDRY_SLAG =
                    be("foundry_slag", BlockDynamicSlag.BlockEntitySlag::new, ModBlocks.SLAG);
    public static final RegistryHandle<BlockEntityType<BlockLoot.TileEntityLoot>> NTM_LOOT =
            be("loot", BlockLoot.TileEntityLoot::new, ModBlocks.LOOT);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineOilWell>> DERRICK =
            machine("derrick", BlockEntityMachineOilWell::new, ModBlocks.MACHINE_WELL);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePumpjack>>
            MACHINE_PUMPJACK =
                    clientMachine(
                            "machine_pumpjack",
                            BlockEntityMachinePumpjack::new,
                            ModBlocks.MACHINE_PUMPJACK);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCompressor>> COMPRESSOR =
            clientMachine(
                    "compressor", BlockEntityMachineCompressor::new, ModBlocks.MACHINE_COMPRESSOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCompressorCompact>>
            COMPRESSOR_COMPACT =
                    clientMachine(
                            "compressor_compact",
                            BlockEntityMachineCompressorCompact::new,
                            ModBlocks.MACHINE_COMPRESSOR_COMPACT);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineGasFlare>> GASFLARE =
            clientMachine("gasflare", BlockEntityMachineGasFlare::new, ModBlocks.MACHINE_FLARE);
    public static final RegistryHandle<BlockEntityType<BlockEntityChimneyBrick>> CHIMNEY_BRICK =
            be("chimney_brick", BlockEntityChimneyBrick::new, ModBlocks.CHIMNEY_BRICK)
                    .ticks(BlockEntityChimneyBase::tickClient, BlockEntityChimneyBase::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityChimneyIndustrial>>
            CHIMNEY_INDUSTRIAL =
                    be(
                                    "chimney_industrial",
                                    BlockEntityChimneyIndustrial::new,
                                    ModBlocks.CHIMNEY_INDUSTRIAL)
                            .ticks(
                                    BlockEntityChimneyBase::tickClient,
                                    BlockEntityChimneyBase::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityReactorZirnox>> ZIRNOX =
            machine("zirnox", BlockEntityReactorZirnox::new, ModBlocks.REACTOR_ZIRNOX);
    public static final RegistryHandle<BlockEntityType<BlockEntityZirnoxDestroyed>>
            ZIRNOX_DESTROYED =
                    be(
                                    "zirnox_destroyed",
                                    BlockEntityZirnoxDestroyed::new,
                                    ModBlocks.ZIRNOX_DESTROYED)
                            .ticksServerAt(BlockEntityZirnoxDestroyed::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityFEL>> FEL =
            clientMachine("fel", BlockEntityFEL::new, ModBlocks.MACHINE_FEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityPASource>> PA_SOURCE =
            machine("pa_source", BlockEntityPASource::new, ModBlocks.PA_SOURCE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPABeamline>> PA_BEAMLINE =
            be("pa_beamline", BlockEntityPABeamline::new, ModBlocks.PA_BEAMLINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPARFC>> PA_RFC =
            machine("pa_rfc", BlockEntityPARFC::new, ModBlocks.PA_RFC);
    public static final RegistryHandle<BlockEntityType<BlockEntityPAQuadrupole>> PA_QUADRUPOLE =
            machine("pa_quadrupole", BlockEntityPAQuadrupole::new, ModBlocks.PA_QUADRUPOLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPADipole>> PA_DIPOLE =
            machine("pa_dipole", BlockEntityPADipole::new, ModBlocks.PA_DIPOLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPADetector>> PA_DETECTOR =
            machine("pa_detector", BlockEntityPADetector::new, ModBlocks.PA_DETECTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSolderingStation>>
            SOLDERING_STATION =
                    machine(
                            "soldering_station",
                            BlockEntityMachineSolderingStation::new,
                            ModBlocks.MACHINE_SOLDERING_STATION);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineArcWelder>> ARC_WELDER =
            machine("arc_welder", BlockEntityMachineArcWelder::new, ModBlocks.MACHINE_ARC_WELDER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineEPress>> ELECTRIC_PRESS =
            clientMachine(
                    "electric_press", BlockEntityMachineEPress::new, ModBlocks.MACHINE_EPRESS);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineFluidTank>> FLUID_TANK =
            machine("fluid_tank", BlockEntityMachineFluidTank::new, ModBlocks.MACHINE_FLUID_TANK);
    public static final RegistryHandle<BlockEntityType<BlockEntityBarrel>> FLUID_BARREL =
            machine(
                    "fluid_barrel",
                    BlockEntityBarrel::new,
                    ModBlocks.BARREL_PLASTIC,
                    ModBlocks.BARREL_STEEL,
                    ModBlocks.BARREL_TCALLOY,
                    ModBlocks.BARREL_ANTIMATTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoTube>> PNEUMATIC_TUBE =
            machine("pneumatic_tube", BlockEntityPneumoTube::new, ModBlocks.PNEUMATIC_TUBE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoTubePaintable>>
            PNEUMATIC_TUBE_PAINTABLE =
                    machine(
                            "pneumatic_tube_paintable",
                            BlockEntityPneumoTubePaintable::new,
                            ModBlocks.PNEUMATIC_TUBE_PAINTABLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoStorageAccess>>
            PNEUMATIC_STORAGE_ACCESS =
                    be(
                            "pneumatic_storage_access",
                            BlockEntityPneumoStorageAccess::new,
                            ModBlocks.PNEUMATIC_STORAGE_ACCESS);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoStorageClutter>>
            PNEUMATIC_STORAGE_CLUTTER =
                    machine(
                            "pneumatic_storage_clutter",
                            BlockEntityPneumoStorageClutter::new,
                            ModBlocks.PNEUMATIC_STORAGE_CLUTTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoStorageMono>>
            PNEUMATIC_STORAGE_MONO =
                    machine(
                            "pneumatic_storage_mono",
                            BlockEntityPneumoStorageMono::new,
                            ModBlocks.PNEUMATIC_STORAGE_MONO);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoStorageImporter>>
            PNEUMATIC_STORAGE_IMPORTER =
                    machine(
                            "pneumatic_storage_importer",
                            BlockEntityPneumoStorageImporter::new,
                            ModBlocks.PNEUMATIC_STORAGE_IMPORTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPneumoStorageExporter>>
            PNEUMATIC_STORAGE_EXPORTER =
                    machine(
                            "pneumatic_storage_exporter",
                            BlockEntityPneumoStorageExporter::new,
                            ModBlocks.PNEUMATIC_STORAGE_EXPORTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPipeGauge>> PIPE_GAUGE =
            be("pipe_gauge", BlockEntityPipeGauge::new, ModBlocks.FLUID_DUCT_GAUGE)
                    .ticksServer(BlockEntityPipeGauge::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFluidCounterValve>>
            PIPE_COUNTER_VALVE =
                    be(
                                    "pipe_counter_valve",
                                    BlockEntityFluidCounterValve::new,
                                    ModBlocks.FLUID_COUNTER_VALVE)
                            .ticksServer(BlockEntityFluidCounterValve::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityCableDiode>> CABLE_DIODE =
            be("cable_diode", BlockEntityCableDiode::new, ModBlocks.CABLE_DIODE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPylonMedium>> PYLON_MEDIUM =
            be(
                    "pylon_medium",
                    BlockEntityPylonMedium::new,
                    ModBlocks.RED_PYLON_MEDIUM_WOOD,
                    ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                    ModBlocks.RED_PYLON_MEDIUM_STEEL,
                    ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPylonLarge>> PYLON_LARGE =
            be("pylon_large", BlockEntityPylonLarge::new, ModBlocks.RED_PYLON_LARGE);
    public static final RegistryHandle<BlockEntityType<BlockEntitySubstation>> SUBSTATION =
            be("substation", BlockEntitySubstation::new, ModBlocks.SUBSTATION);
    public static final RegistryHandle<BlockEntityType<BlockEntityPylon>> PYLON_REDWIRE =
            be(
                    "pylon_redwire",
                    BlockEntityPylon::new,
                    ModBlocks.RED_PYLON,
                    ModBlocks.RED_PYLON_STEEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityConnector>> CONNECTOR_REDWIRE =
            be("connector_redwire", BlockEntityConnector::new, ModBlocks.RED_CONNECTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityConnectorSuper>>
            CONNECTOR_REDWIRE_SUPER =
                    be(
                            "connector_redwire_super",
                            BlockEntityConnectorSuper::new,
                            ModBlocks.RED_CONNECTOR_SUPER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCableGauge>> CABLE_GAUGE =
            be("cable_gauge", BlockEntityCableGauge::new, ModBlocks.RED_CABLE_GAUGE)
                    .ticksServer(BlockEntityCableGauge::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFluidPump>> PIPE_PUMP =
            be("pipe_pump", BlockEntityFluidPump::new, ModBlocks.FLUID_PUMP)
                    .ticksServer(BlockEntityFluidPump::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadiobox>> RADIOBOX =
            machine("radio_broadcaster", BlockEntityRadiobox::new, ModBlocks.RADIOBOX);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioRec>> RADIO_RECEIVER =
            be("radio_receiver", BlockEntityRadioRec::new, ModBlocks.RADIO_REC)
                    .ticksServer(BlockEntityRadioRec::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchSender>> RTTY_SENDER =
            be("rtty_sender", BlockEntityRadioTorchSender::new, ModBlocks.RADIO_TORCH_SENDER)
                    .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchReceiver>> RTTY_REC =
            be("rtty_rec", BlockEntityRadioTorchReceiver::new, ModBlocks.RADIO_TORCH_RECEIVER)
                    .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchCounter>> RTTY_COUNTER =
            be("rtty_counter", BlockEntityRadioTorchCounter::new, ModBlocks.RADIO_TORCH_COUNTER)
                    .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchLogic>> RTTY_LOGIC =
            be("rtty_logic", BlockEntityRadioTorchLogic::new, ModBlocks.RADIO_TORCH_LOGIC)
                    .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchReader>> RTTY_READER =
            be("rtty_reader", BlockEntityRadioTorchReader::new, ModBlocks.RADIO_TORCH_READER)
                    .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTorchController>>
            RTTY_CONTROLLER =
                    be(
                                    "rtty_controller",
                                    BlockEntityRadioTorchController::new,
                                    ModBlocks.RADIO_TORCH_CONTROLLER)
                            .ticksServer(BlockEntityRadioTorch::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneCrate>> DRONE_CRATE =
            machine("drone_crate", BlockEntityDroneCrate::new, ModBlocks.DRONE_CRATE);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneDock>> DRONE_DOCK =
            machine("drone_dock", BlockEntityDroneDock::new, ModBlocks.DRONE_DOCK);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneProvider>> DRONE_PROVIDER =
            machine(
                    "drone_provider",
                    BlockEntityDroneProvider::new,
                    ModBlocks.DRONE_CRATE_PROVIDER);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneRequester>> DRONE_REQUESTER =
            machine(
                    "drone_requester",
                    BlockEntityDroneRequester::new,
                    ModBlocks.DRONE_CRATE_REQUESTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneWaypointRequest>>
            DRONE_WAYPOINT_REQUEST =
                    machine(
                            "drone_waypoint_request",
                            BlockEntityDroneWaypointRequest::new,
                            ModBlocks.DRONE_WAYPOINT_REQUEST);
    public static final RegistryHandle<BlockEntityType<BlockEntityDroneWaypoint>> DRONE_WAYPOINT =
            be("drone_waypoint", BlockEntityDroneWaypoint::new, ModBlocks.DRONE_WAYPOINT)
                    .ticks(
                            BlockEntityDroneWaypoint::tickClient,
                            BlockEntityDroneWaypoint::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioTelex>> RTTY_TELEX =
            be("rtty_telex", BlockEntityRadioTelex::new, ModBlocks.RADIO_TELEX)
                    .ticksServer(BlockEntityRadioTelex::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRadioAUTOCAL>> RTTY_AUTOCAL =
            be("rtty_autocal", BlockEntityRadioAUTOCAL::new, ModBlocks.RADIO_AUTOCAL)
                    .ticksServer(BlockEntityRadioAUTOCAL::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPowerDetector>> HE_DETECTOR =
            be("he_detector", BlockEntityPowerDetector::new, ModBlocks.MACHINE_DETECTOR)
                    .ticksServer(BlockEntityPowerDetector::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPipePaintable>> PIPE_PAINTABLE =
            be(
                    "pipe_paintable",
                    BlockEntityPipePaintable::new,
                    ModBlocks.FLUID_DUCT_PAINTABLE,
                    ModBlocks.FLUID_DUCT_PAINTABLE_EXHAUST);
    public static final RegistryHandle<BlockEntityType<BlockEntityCablePaintable>> CABLE_PAINTABLE =
            be("cable_paintable", BlockEntityCablePaintable::new, ModBlocks.RED_CABLE_PAINTABLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityPipeAnchor>> PIOE_ANCHOR =
            be("pioe_anchor", BlockEntityPipeAnchor::new, ModBlocks.PIPE_ANCHOR)
                    .ticksServer(BlockEntityPipeAnchor::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineWoodBurner>> WOOD_BURNER =
            clientMachine(
                    "wood_burner",
                    BlockEntityMachineWoodBurner::new,
                    ModBlocks.MACHINE_WOOD_BURNER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineDiesel>> DIESEL_GENERATOR =
            clientMachine(
                    "diesel_generator", BlockEntityMachineDiesel::new, ModBlocks.MACHINE_DIESEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineRTG>> MACHINE_RTG =
            machine("machine_rtg", BlockEntityMachineRTG::new, ModBlocks.MACHINE_RTG);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineTurbine>> TURBINE =
            machine("turbine", BlockEntityMachineTurbine::new, ModBlocks.MACHINE_TURBINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineLargeTurbine>>
            INDUSTRIAL_TURBINE =
                    clientMachine(
                            "industrial_turbine",
                            BlockEntityMachineLargeTurbine::new,
                            ModBlocks.MACHINE_LARGE_TURBINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineIndustrialTurbine>>
            IND_TURBINE =
                    clientMachine(
                            "ind_turbine",
                            BlockEntityMachineIndustrialTurbine::new,
                            ModBlocks.MACHINE_INDUSTRIAL_TURBINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityChungus>> CHUNGUS =
            clientMachine("chungus", BlockEntityChungus::new, ModBlocks.MACHINE_CHUNGUS);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeatBoiler>> HEAT_BOILER =
            clientMachine("heat_boiler", BlockEntityHeatBoiler::new, ModBlocks.MACHINE_BOILER);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeatBoilerIndustrial>>
            HEAT_BOILER_INDUSTRIAL =
                    clientMachine(
                            "heat_boiler_industrial",
                            BlockEntityHeatBoilerIndustrial::new,
                            ModBlocks.MACHINE_INDUSTRIAL_BOILER);
    public static final RegistryHandle<BlockEntityType<BlockEntitySteamEngine>> STEAM_ENGINE =
            clientMachine(
                    "steam_engine", BlockEntitySteamEngine::new, ModBlocks.MACHINE_STEAM_ENGINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCombustionEngine>>
            COMBUSTION_ENGINE =
                    clientMachine(
                            "combustion_engine",
                            BlockEntityMachineCombustionEngine::new,
                            ModBlocks.MACHINE_COMBUSTION_ENGINE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineTurbineGas>>
            MACHINE_GASTURBINE =
                    clientMachine(
                            "machine_gasturbine",
                            BlockEntityMachineTurbineGas::new,
                            ModBlocks.MACHINE_TURBINEGAS);

    public static final RegistryHandle<BlockEntityType<BlockEntityMachineTurbofan>>
            MACHINE_TURBOFAN =
                    clientMachine(
                            "machine_turbofan",
                            BlockEntityMachineTurbofan::new,
                            ModBlocks.MACHINE_TURBOFAN);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineLPW2>> LPW2 =
            be("machine_lpw2", BlockEntityMachineLPW2::new, ModBlocks.MACHINE_LPW2);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePumpSteam>> STEAM_PUMP =
            pump("steam_pump", BlockEntityMachinePumpSteam::new, ModBlocks.PUMP_STEAM);
    public static final RegistryHandle<BlockEntityType<BlockEntitySolarBoiler>> SOLARBOILER =
            clientMachine(
                    "solarboiler", BlockEntitySolarBoiler::new, ModBlocks.MACHINE_SOLAR_BOILER);
    public static final RegistryHandle<BlockEntityType<BlockEntitySolarMirror>> SOLARMIRROR =
            clientMachine("solarmirror", BlockEntitySolarMirror::new, ModBlocks.SOLAR_MIRROR);
    public static final RegistryHandle<BlockEntityType<BlockEntityWasteDrum>> WASTE_DRUM =
            machine("waste_drum", BlockEntityWasteDrum::new, ModBlocks.WASTE_DRUM);
    public static final RegistryHandle<BlockEntityType<BlockEntityBatterySocket>> BATTERY_SOCKET =
            clientMachine(
                    "battery_socket",
                    BlockEntityBatterySocket::new,
                    ModBlocks.MACHINE_BATTERY_SOCKET);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineBattery>> BATTERY =
            machine("battery", BlockEntityMachineBattery::new, ModBlocks.MACHINE_BATTERY);

    public static final RegistryHandle<BlockEntityType<BlockEntityMachineFENSU>> FENSU =
            clientMachine("fensu", BlockEntityMachineFENSU::new, ModBlocks.MACHINE_FENSU);
    public static final RegistryHandle<BlockEntityType<BlockEntityBatteryREDD>> BATTERY_REDD =
            be("battery_redd", BlockEntityBatteryREDD::new, ModBlocks.MACHINE_BATTERY_REDD)
                    .ticksServer(BlockEntityBatteryREDD::tickServer)
                    .ticksClient(BlockEntityBatteryREDD::tickClient);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCapacitor>> CAPACITOR =
            be("capacitor", BlockEntityMachineCapacitor::new, ModBlocks.CAPACITOR_COPPER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCharger>> NTM_CHARGER =
            be("charger", BlockEntityCharger::new, ModBlocks.CHARGER)
                    .ticksServerAt(BlockEntityCharger::tick)
                    .ticksClientAt(BlockEntityCharger::tickClient);
    public static final RegistryHandle<BlockEntityType<BlockEntityRefueler>> REFUELER =
            clientMachine("refueler", BlockEntityRefueler::new, ModBlocks.REFUELER);

    public static final RegistryHandle<BlockEntityType<BlockEntityCrate>> CRATE_IRON =
            crate(CrateType.IRON, ModBlocks.CRATE_IRON).andThen(CrateType.IRON::setTypeSupplier);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrate>> CRATE_STEEL =
            crate(CrateType.STEEL, ModBlocks.CRATE_STEEL).andThen(CrateType.STEEL::setTypeSupplier);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrate>> CRATE_DESH =
            crate(CrateType.DESH, ModBlocks.CRATE_DESH).andThen(CrateType.DESH::setTypeSupplier);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrateTungsten>> CRATE_HOT =
            be("crate_hot", BlockEntityCrateTungsten::new, ModBlocks.CRATE_TUNGSTEN)
                    .ticks(
                            BlockEntityCrateTungsten::tickClient,
                            BlockEntityCrateTungsten::tickServer)
                    .andThen(CrateType.TUNGSTEN::setTypeSupplier);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrate>> SAFE =
            crate(CrateType.SAFE, ModBlocks.SAFE).andThen(CrateType.SAFE::setTypeSupplier);
    public static final RegistryHandle<BlockEntityType<BlockEntityMassStorage>> MASS_STORAGE =
            machine(
                    "mass_storage",
                    BlockEntityMassStorage::new,
                    ModBlocks.MASS_STORAGE_WOOD,
                    ModBlocks.MASS_STORAGE_IRON,
                    ModBlocks.MASS_STORAGE_DESH,
                    ModBlocks.MASS_STORAGE_TCALLOY);
    public static final RegistryHandle<BlockEntityType<BlockEntityCrateSupply>> SUPPLY_CRATE =
            be("supply_crate", BlockEntityCrateSupply::new, ModBlocks.CRATE_SUPPLY);
    public static final RegistryHandle<BlockEntityType<BlockEntityFileCabinet>> FILE_CABINET =
            clientMachine(
                    "file_cabinet",
                    BlockEntityFileCabinet::new,
                    ModBlocks.FILING_CABINET_GREEN,
                    ModBlocks.FILING_CABINET_STEEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKRod>> RBMK_ROD =
            machine(
                    "rbmk_rod",
                    BlockEntityRBMKRod::new,
                    ModBlocks.RBMK_ROD,
                    ModBlocks.RBMK_ROD_MODERATED);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKRodReaSim>> RBMK_ROD_REASIM =
            machine(
                    "rbmk_rod_reasim",
                    BlockEntityRBMKRodReaSim::new,
                    ModBlocks.RBMK_ROD_REASIM,
                    ModBlocks.RBMK_ROD_REASIM_MODERATED);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKControlManual>> RBMK_CONTROL =
            clientMachine(
                    "rbmk_control",
                    BlockEntityRBMKControlManual::new,
                    ModBlocks.RBMK_CONTROL,
                    ModBlocks.RBMK_CONTROL_MOD,
                    ModBlocks.RBMK_CONTROL_REASIM);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKControlAuto>>
            RBMK_CONTROL_AUTO =
                    clientMachine(
                            "rbmk_control_auto",
                            BlockEntityRBMKControlAuto::new,
                            ModBlocks.RBMK_CONTROL_AUTO,
                            ModBlocks.RBMK_CONTROL_REASIM_AUTO);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKBoiler>> RBMK_BOILER =
            machine("rbmk_boiler", BlockEntityRBMKBoiler::new, ModBlocks.RBMK_BOILER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKModerator>> RBMK_MODERATOR =
            machine("rbmk_moderator", BlockEntityRBMKModerator::new, ModBlocks.RBMK_MODERATOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKReflector>> RBMK_REFLECTOR =
            machine("rbmk_reflector", BlockEntityRBMKReflector::new, ModBlocks.RBMK_REFLECTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKAbsorber>> RBMK_ABSORBER =
            machine("rbmk_absorber", BlockEntityRBMKAbsorber::new, ModBlocks.RBMK_ABSORBER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKBlank>> RBMK_BLANK =
            machine("rbmk_blank", BlockEntityRBMKBlank::new, ModBlocks.RBMK_BLANK);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKCooler>> RBMK_COOLER =
            machine("rbmk_cooler", BlockEntityRBMKCooler::new, ModBlocks.RBMK_COOLER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKStorage>> RBMK_STORAGE =
            machine("rbmk_storage", BlockEntityRBMKStorage::new, ModBlocks.RBMK_STORAGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKHeater>> RBMK_HEATER =
            machine("rbmk_heater", BlockEntityRBMKHeater::new, ModBlocks.RBMK_HEATER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKOutgasser>> RBMK_OUTGASSER =
            machine("rbmk_outgasser", BlockEntityRBMKOutgasser::new, ModBlocks.RBMK_OUTGASSER);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKInlet>> RBMK_INLET =
            machine("rbmk_inlet", BlockEntityRBMKInlet::new, ModBlocks.RBMK_INLET);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKOutlet>> RBMK_OUTLET =
            machine("rbmk_outlet", BlockEntityRBMKOutlet::new, ModBlocks.RBMK_OUTLET);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKConsole>> RBMK_CONSOLE =
            machine("rbmk_console", BlockEntityRBMKConsole::new, ModBlocks.RBMK_CONSOLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityCraneConsole>>
            RBMK_CRANE_CONSOLE =
                    clientMachine(
                            "rbmk_crane_console",
                            BlockEntityCraneConsole::new,
                            ModBlocks.RBMK_CRANE_CONSOLE);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKGauge>> RBMK_GAUGE =
            be("rbmk_gauge", BlockEntityRBMKGauge::new, ModBlocks.RBMK_GAUGE)
                    .ticksClient(BlockEntityRBMKGauge::tickClient)
                    .ticksServerAt(BlockEntityRBMKGauge::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKDisplay>> RBMK_DISPLAY =
            be("rbmk_display", BlockEntityRBMKDisplay::new, ModBlocks.RBMK_DISPLAY)
                    .ticksServerAt(BlockEntityRBMKDisplay::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKNumitron>> RBMK_NUMITRON =
            be("rbmk_numitron", BlockEntityRBMKNumitron::new, ModBlocks.RBMK_NUMITRON)
                    .ticksServerAt(BlockEntityRBMKNumitron::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKIndicator>> RBMK_INDICATOR =
            be("rbmk_indicator", BlockEntityRBMKIndicator::new, ModBlocks.RBMK_INDICATOR)
                    .ticksServerAt(BlockEntityRBMKIndicator::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKGraph>> RBMK_GRAPH =
            be("rbmk_graph", BlockEntityRBMKGraph::new, ModBlocks.RBMK_GRAPH)
                    .ticksServerAt(BlockEntityRBMKGraph::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKLever>> RBMK_LEVER =
            be("rbmk_lever", BlockEntityRBMKLever::new, ModBlocks.RBMK_LEVER)
                    .ticksClient(BlockEntityRBMKLever::tickClient)
                    .ticksServerAt(BlockEntityRBMKLever::tickServer);

    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKKeyPad>> RBMK_KEYPAD =
            be("rbmk_keypad", BlockEntityRBMKKeyPad::new, ModBlocks.RBMK_KEY_PAD)
                    .ticksServerAt(BlockEntityRBMKKeyPad::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKTerminal>> RBMK_TERMINAL =
            be("rbmk_terminal", BlockEntityRBMKTerminal::new, ModBlocks.RBMK_TERMINAL)
                    .ticksServerAt(BlockEntityRBMKTerminal::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityRBMKAutoloader>> RBMK_AUTOLOADER =
            clientMachine(
                    "rbmk_autoloader", BlockEntityRBMKAutoloader::new, ModBlocks.RBMK_AUTOLOADER);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileFuel>> PILE_FUEL =
            be("pile_fuel", BlockEntityPileFuel::new, ModBlocks.BLOCK_GRAPHITE_FUEL)
                    .ticksServer(BlockEntityPileBase::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileBreedingFuel>>
            PILE_BREEDINGFUEL =
                    be(
                                    "pile_breedingfuel",
                                    BlockEntityPileBreedingFuel::new,
                                    ModBlocks.BLOCK_GRAPHITE_LITHIUM)
                            .ticksServer(BlockEntityPileBase::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileSource>> PILE_SOURCE =
            be(
                            "pile_source",
                            BlockEntityPileSource::new,
                            ModBlocks.BLOCK_GRAPHITE_SOURCE,
                            ModBlocks.BLOCK_GRAPHITE_PLUTONIUM)
                    .ticksServer(BlockEntityPileBase::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileNeutronDetector>>
            PILE_NEUTRONDETECTOR =
                    be(
                                    "pile_neutrondetector",
                                    BlockEntityPileNeutronDetector::new,
                                    ModBlocks.BLOCK_GRAPHITE_DETECTOR)
                            .ticksServer(BlockEntityPileNeutronDetector::tickServer);

    public static final RegistryHandle<BlockEntityType<BlockEntityPileCore>> PILE_CORE =
            be("pile_core", BlockEntityPileCore::new, ModBlocks.PILE_BLOCK)
                    .ticksServer(BlockEntityPileCore::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileLoader>> PILE_LOADER =
            be("pile_loader", BlockEntityPileLoader::new, ModBlocks.PILE_LOADER)
                    .ticks(BlockEntityPileLoader::tickClient, BlockEntityPileLoader::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileVent>> PILE_VENT =
            be("pile_vent", BlockEntityPileVent::new, ModBlocks.PILE_VENT)
                    .ticks(BlockEntityPileVent::tickClient, BlockEntityPileVent::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPileControl>> PILE_CONTROL =
            be("pile_control", BlockEntityPileControl::new, ModBlocks.PILE_CONTROL)
                    .ticks(BlockEntityPileControl::tickClient, BlockEntityPileControl::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFan>> FAN =
            be("fan", BlockEntityFan::new, ModBlocks.FAN).ticks(BlockEntityFan::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityPistonInserter>> PISTON_INSERTER =
            clientMachine(
                    "piston_inserter", BlockEntityPistonInserter::new, ModBlocks.PISTON_INSERTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePWRController>>
            PWR_CONTROLLER =
                    clientMachine(
                            "pwr_controller",
                            BlockEntityMachinePWRController::new,
                            ModBlocks.PWR_CONTROLLER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineICFController>>
            ICF_CONTROLLER =
                    be(
                                    "icf_controller",
                                    BlockEntityMachineICFController::new,
                                    ModBlocks.ICF_CONTROLLER)
                            .ticks(
                                    BlockEntityMachineICFController::tickClient,
                                    BlockEntityMachineICFController::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityICF>> ICF =
            machine("icf", BlockEntityICF::new, ModBlocks.MACHINE_ICF);
    public static final RegistryHandle<BlockEntityType<BlockEntityICFStruct>> ICF_STRUCT =
            be("icf_struct", BlockEntityICFStruct::new, ModBlocks.STRUCT_ICF_CORE)
                    .ticksServer(BlockEntityICFStruct::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityICFPress>> ICF_PRESS =
            machine("icf_press", BlockEntityICFPress::new, ModBlocks.MACHINE_ICF_PRESS);
    public static final RegistryHandle<BlockEntityType<BlockEntityBedrockOre>> BEDROCK_ORE =
            be("bedrock_ore", BlockEntityBedrockOre::new, ModBlocks.ORE_BEDROCK);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeCustom>> NUKE_CUSTOM =
            be("nuke_custom", BlockEntityNukeCustom::new, ModBlocks.NUKE_CUSTOM);
    public static final RegistryHandle<BlockEntityType<BlockEntityNukeBalefire>> NUKE_FSTBMB =
            machine("nuke_fstbmb", BlockEntityNukeBalefire::new, ModBlocks.NUKE_FSTBMB);
    public static final RegistryHandle<BlockEntityType<BlockEntityBombMulti>> BOMB_MULTI =
            be("bombmulti", BlockEntityBombMulti::new, ModBlocks.BOMB_MULTI);
    public static final RegistryHandle<BlockEntityType<BlockEntityFireworks>> FIREWORK_BOX =
            be("firework_box", BlockEntityFireworks::new, ModBlocks.FIREWORKS)
                    .ticksServerAt(BlockEntityFireworks::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityFissure>> FISSURE =
            be("fissure", BlockEntityFissure::new, ModBlocks.ORE_VOLCANO)
                    .ticksServerAt(BlockEntityFissure::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityGeysir>> GEYSIR =
            be("geysir", BlockEntityGeysir::new, ModBlocks.GEYSIR_NETHER, ModBlocks.GEYSIR_CHLORINE)
                    .ticksServerAt(BlockEntityGeysir::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityVent>> VENT =
            be(
                            "vent",
                            BlockEntityVent::new,
                            ModBlocks.VENT_CHLORINE,
                            ModBlocks.VENT_CLOUD,
                            ModBlocks.VENT_PINK_CLOUD)
                    .ticksServerAt(BlockEntityVent::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityChlorineSeal>> CHLORINE_SEAL =
            be("chlorine_seal", BlockEntityChlorineSeal::new, ModBlocks.VENT_CHLORINE_SEAL)
                    .ticksServerAt(BlockEntityChlorineSeal::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityEmitter>> EMITTER =
            be("emitter", BlockEntityEmitter::new, ModBlocks.DECO_EMITTER)
                    .ticksServerAt(BlockEntityEmitter::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityPartEmitter>> PART_EMITTER =
            be("partemitter", BlockEntityPartEmitter::new, ModBlocks.PART_EMITTER)
                    .ticksServerAt(BlockEntityPartEmitter::tickServer)
                    .ticksClientAt(BlockEntityPartEmitter::tickClient);
    public static final RegistryHandle<BlockEntityType<BlockEntityBlastDoor>> BLAST_DOOR =
            machine("blast_door", BlockEntityBlastDoor::new, ModBlocks.BLAST_DOOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityBroadcaster>> BROADCASTER =
            be("pink_cloud_broadcaster", BlockEntityBroadcaster::new, ModBlocks.BROADCASTER_PC)
                    .ticksClientAt(BlockEntityBroadcaster::tickClient)
                    .ticksServerAt(BlockEntityBroadcaster::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityTrappedBrick>> TRAPPED_BRICK =
            be(
                            "trapped_brick",
                            () ->
                                    new BlockEntityType<>(
                                            BlockEntityTrappedBrick::new,
                                            ModBlocks.JUNGLE_TRAPS.stream()
                                                    .map(RegistryHandle::get)
                                                    .filter(
                                                            b ->
                                                                    b.trap.type
                                                                            == TrappedBrick.TrapType
                                                                                    .DETECTOR)
                                                    .collect(Collectors.toSet())))
                    .ticksServerAt(BlockEntityTrappedBrick::tick);
    public static final RegistryHandle<BlockEntityType<TileEntityCrashedBomb>> CRASHED_BALEFIRE =
            be(
                            "crashed_balefire",
                            () ->
                                    new BlockEntityType<>(
                                            TileEntityCrashedBomb::new,
                                            ModBlocks.CRASHED_BOMBS.stream()
                                                    .map(RegistryHandle::get)
                                                    .collect(Collectors.toSet())))
                    .ticksServerAt(TileEntityCrashedBomb::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityLandmine>> LANDMINE =
            be(
                            "landmine",
                            BlockEntityLandmine::new,
                            ModBlocks.MINE_AP,
                            ModBlocks.MINE_SHRAP,
                            ModBlocks.MINE_HE,
                            ModBlocks.MINE_FAT,
                            ModBlocks.MINE_NAVAL)
                    .ticksServerAt(BlockEntityLandmine::tick);

    public static final RegistryHandle<BlockEntityType<BlockEntityCharge>> EXPLOSIVE_CHARGE =
            be(
                    "explosive_charge",
                    BlockEntityCharge::new,
                    ModBlocks.CHARGE_C4,
                    ModBlocks.CHARGE_DYNAMITE,
                    ModBlocks.CHARGE_MINER,
                    ModBlocks.CHARGE_SEMTEX);
    public static final RegistryHandle<BlockEntityType<BlockEntityVolcanoCore>> VOLCANO_CORE =
            be(
                            "volcano_core",
                            BlockEntityVolcanoCore::new,
                            ModBlocks.VOLCANO_CORE,
                            ModBlocks.VOLCANO_RAD_CORE)
                    .ticksServerAt(BlockEntityVolcanoCore::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityLogicBlock>> NTM_LOGIC_BLOCK =
            be(
                            "logic_block",
                            BlockEntityLogicBlock::new,
                            ModBlocks.LOGIC_BLOCK,
                            ModBlocks.LOGIC_BLOCK_INVIS)
                    .ticksServerAt(BlockEntityLogicBlock::tick);
    public static final RegistryHandle<BlockEntityType<BlockEntityFurnaceIron>> FURNACE_IRON =
            clientMachine("furnace_iron", BlockEntityFurnaceIron::new, ModBlocks.FURNACE_IRON);
    public static final RegistryHandle<BlockEntityType<BlockEntityFurnaceSteel>> FURNACE_STEEL =
            clientMachine("furnace_steel", BlockEntityFurnaceSteel::new, ModBlocks.FURNACE_STEEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityFurnaceCombination>>
            COMBINATION_OVEN =
                    clientMachine(
                            "combination_oven",
                            BlockEntityFurnaceCombination::new,
                            ModBlocks.FURNACE_COMBINATION);
    public static final RegistryHandle<BlockEntityType<BlockEntityFurnaceBrick>> FURNACE_BRICK =
            machine("furnace_brick", BlockEntityFurnaceBrick::new, ModBlocks.MACHINE_FURNACE_BRICK);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeaterOven>> HEATING_OVEN =
            clientMachine("heating_oven", BlockEntityHeaterOven::new, ModBlocks.HEATER_OVEN);
    public static final RegistryHandle<BlockEntityType<BlockEntityAshpit>> ASHPIT =
            clientMachine("ashpit", BlockEntityAshpit::new, ModBlocks.MACHINE_ASHPIT);
    public static final RegistryHandle<BlockEntityType<BlockEntityHeaterOilburner>> OILBURNER =
            machine("oilburner", BlockEntityHeaterOilburner::new, ModBlocks.HEATER_OILBURNER);

    public static final RegistryHandle<BlockEntityType<BlockEntityHeaterHeatex>> HEATER_HEATEX =
            machine("heater_heatex", BlockEntityHeaterHeatex::new, ModBlocks.HEATER_HEATEX);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCrystallizer>> ACIDOMATIC =
            clientMachine(
                    "acidomatic",
                    BlockEntityMachineCrystallizer::new,
                    ModBlocks.MACHINE_CRYSTALLIZER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineCoker>> COKER =
            clientMachine("coker", BlockEntityMachineCoker::new, ModBlocks.MACHINE_COKER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineLiquefactor>> LIQUEFACTOR =
            machine(
                    "liquefactor",
                    BlockEntityMachineLiquefactor::new,
                    ModBlocks.MACHINE_LIQUEFACTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSolidifier>> SOLIDIFIER =
            machine("solidifier", BlockEntityMachineSolidifier::new, ModBlocks.MACHINE_SOLIDIFIER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePyroOven>> PYROOVEN =
            clientMachine("pyrooven", BlockEntityMachinePyroOven::new, ModBlocks.MACHINE_PYROOVEN);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineVacuumDistill>>
            VACUUUM_DISTILL =
                    clientMachine(
                            "vacuuum_distill",
                            BlockEntityMachineVacuumDistill::new,
                            ModBlocks.MACHINE_VACUUM_DISTILL);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineFrackingTower>>
            FRACKING_TOWER =
                    machine(
                            "fracking_tower",
                            BlockEntityMachineFrackingTower::new,
                            ModBlocks.MACHINE_FRACKING_TOWER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachinePumpElectric>>
            ELECTRIC_PUMP =
                    pump(
                            "electric_pump",
                            BlockEntityMachinePumpElectric::new,
                            ModBlocks.PUMP_ELECTRIC);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineIntake>> INTAKE =
            be("intake", BlockEntityMachineIntake::new, ModBlocks.MACHINE_INTAKE)
                    .ticks(
                            BlockEntityMachineIntake::tickClient,
                            BlockEntityMachineIntake::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineDrain>> FLUID_DRAIN =
            be("fluid_drain", BlockEntityMachineDrain::new, ModBlocks.MACHINE_DRAIN)
                    .ticks(
                            BlockEntityMachineDrain::tickClient,
                            BlockEntityMachineDrain::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineFunnel>> FUNNEL =
            machine("funnel", BlockEntityMachineFunnel::new, ModBlocks.MACHINE_FUNNEL);
    public static final RegistryHandle<BlockEntityType<BlockEntityCondenser>> CONDENSER =
            machine("condenser", BlockEntityCondenser::new, ModBlocks.MACHINE_CONDENSER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCondenserPowered>>
            CONDENSER_POWERED =
                    clientMachine(
                            "condenser_powered",
                            BlockEntityCondenserPowered::new,
                            ModBlocks.MACHINE_CONDENSER_POWERED);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineBigAssTank>> BIGASSTANK =
            machine("bigasstank", BlockEntityMachineBigAssTank::new, ModBlocks.MACHINE_BIGASSTANK);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineOrbus>> ORBUS =
            machine("orbus", BlockEntityMachineOrbus::new, ModBlocks.MACHINE_ORBUS);

    public static final RegistryHandle<BlockEntityType<BlockEntityUF6Tank>> UF6_TANK =
            be(
                    "uf6_tank",
                    () ->
                            new BlockEntityType<>(
                                    (pos, state) ->
                                            new BlockEntityUF6Tank(
                                                    ModBlockEntities.UF6_TANK.get(), pos, state),
                                    Set.of(ModBlocks.MACHINE_UF6_TANK.get())));
    public static final RegistryHandle<BlockEntityType<BlockEntityUF6Tank>> PUF6_TANK =
            be(
                    "puf6_tank",
                    () ->
                            new BlockEntityType<>(
                                    (pos, state) ->
                                            new BlockEntityUF6Tank(
                                                    ModBlockEntities.PUF6_TANK.get(), pos, state),
                                    Set.of(ModBlocks.MACHINE_PUF6_TANK.get())));
    public static final RegistryHandle<BlockEntityType<BlockEntityMicrowave>> MICROWAVE =
            machine("microwave", BlockEntityMicrowave::new, ModBlocks.MACHINE_MICROWAVE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSatLinker>> SATLINKER =
            be("satlinker", BlockEntityMachineSatLinker::new, ModBlocks.MACHINE_SATLINKER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSatLink>> SATLINK =
            be("satlink", BlockEntityMachineSatLink::new, ModBlocks.MACHINE_SATLINK)
                    .ticksClientAt(BlockEntityMachineSatLink::tickClient)
                    .ticksServerAt(BlockEntityMachineSatLink::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineKeyForge>> KEY_FORGE =
            be("key_forge", BlockEntityMachineKeyForge::new, ModBlocks.MACHINE_KEYFORGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityThresher>> THRESHER =
            be("thresher", BlockEntityThresher::new, ModBlocks.MACHINE_THRESHER)
                    .ticksClient(BlockEntityThresher::tickClient)
                    .ticksServerAt(BlockEntityThresher::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAutosaw>> AUTOSAW =
            be("autosaw", BlockEntityMachineAutosaw::new, ModBlocks.MACHINE_AUTOSAW)
                    .ticksClient(BlockEntityMachineAutosaw::tickClient)
                    .ticksServerAt(BlockEntityMachineAutosaw::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntitySawmill>> SAWMILL =
            clientMachine("sawmill", BlockEntitySawmill::new, ModBlocks.MACHINE_SAWMILL);
    public static final RegistryHandle<BlockEntityType<BlockEntityStirling>> STIRLING =
            clientMachine(
                    "stirling",
                    BlockEntityStirling::new,
                    ModBlocks.MACHINE_STIRLING,
                    ModBlocks.MACHINE_STIRLING_STEEL,
                    ModBlocks.MACHINE_STIRLING_CREATIVE);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineAmmoPress>> AMMO_PRESS =
            clientMachine(
                    "ammo_press", BlockEntityMachineAmmoPress::new, ModBlocks.MACHINE_AMMO_PRESS);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionTorus>> FUSION_TORUS =
            clientMachine("fusion_torus", BlockEntityFusionTorus::new, ModBlocks.FUSION_TORUS);
    public static final RegistryHandle<BlockEntityType<BlockEntityCore>> CORE_CORE =
            machine("v0", BlockEntityCore::new, ModBlocks.DFC_CORE);
    public static final RegistryHandle<BlockEntityType<BlockEntityCoreEmitter>> CORE_EMITTER =
            machine("v0_emitter", BlockEntityCoreEmitter::new, ModBlocks.DFC_EMITTER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCoreReceiver>> CORE_RECEIVER =
            machine("v0_receiver", BlockEntityCoreReceiver::new, ModBlocks.DFC_RECEIVER);
    public static final RegistryHandle<BlockEntityType<BlockEntityCoreInjector>> CORE_INJECTOR =
            machine("v0_injector", BlockEntityCoreInjector::new, ModBlocks.DFC_INJECTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityCoreStabilizer>> CORE_STABILIZER =
            machine("v0_stabilizer", BlockEntityCoreStabilizer::new, ModBlocks.DFC_STABILIZER);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionKlystron>> FUSION_KLYSTRON =
            clientMachine(
                    "fusion_klystron", BlockEntityFusionKlystron::new, ModBlocks.FUSION_KLYSTRON);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionKlystronCreative>>
            FUSION_KLYSTRON_CREATIVE =
                    be(
                                    "fusion_klystron_creative",
                                    BlockEntityFusionKlystronCreative::new,
                                    ModBlocks.FUSION_KLYSTRON_CREATIVE)
                            .ticks(
                                    BlockEntityFusionKlystronCreative::tickClient,
                                    BlockEntityFusionKlystronCreative::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionBreeder>> FUSION_BREEDER =
            machine("fusion_breeder", BlockEntityFusionBreeder::new, ModBlocks.FUSION_BREEDER);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionCollector>>
            FUSION_COLLECTOR =
                    be(
                            "fusion_collector",
                            BlockEntityFusionCollector::new,
                            ModBlocks.FUSION_COLLECTOR);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionBoiler>> FUSION_BOILER =
            be("fusion_boiler", BlockEntityFusionBoiler::new, ModBlocks.FUSION_BOILER)
                    .ticksServer(BlockEntityFusionBoiler::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionMHDT>> FUSION_MHDT =
            be("fusion_mhdt", BlockEntityFusionMHDT::new, ModBlocks.FUSION_MHDT)
                    .ticks(BlockEntityFusionMHDT::tickClient, BlockEntityFusionMHDT::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionCoupler>> FUSION_COUPLER =
            be("fusion_coupler", BlockEntityFusionCoupler::new, ModBlocks.FUSION_COUPLER);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionPlasmaForge>>
            FUSION_PLASMA_FORGE =
                    clientMachine(
                            "fusion_plasma_forge",
                            BlockEntityFusionPlasmaForge::new,
                            ModBlocks.FUSION_PLASMA_FORGE);
    public static final RegistryHandle<BlockEntityType<BlockEntityFusionTorusStruct>>
            FUSION_TORUS_STRUCT =
                    be(
                                    "fusion_torus_struct",
                                    BlockEntityFusionTorusStruct::new,
                                    ModBlocks.STRUCT_TORUS_CORE)
                            .ticksServer(BlockEntityFusionTorusStruct::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityWatz>> WATZ =
            machine("watz", BlockEntityWatz::new, ModBlocks.WATZ);
    public static final RegistryHandle<BlockEntityType<BlockEntityWatzStruct>> WATZ_STRUCT =
            be("watz_struct", BlockEntityWatzStruct::new, ModBlocks.STRUCT_WATZ_CORE)
                    .ticksServer(BlockEntityWatzStruct::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityStorageDrum>> WASTE_STORAGE_DRUM =
            machine("waste_storage_drum", BlockEntityStorageDrum::new, ModBlocks.STORAGE_DRUM);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineSiren>> SIREN =
            be("siren", BlockEntityMachineSiren::new, ModBlocks.MACHINE_SIREN);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineShredder>>
            MACHINE_SHREDDER =
                    machine(
                            "machine_shredder",
                            BlockEntityMachineShredder::new,
                            ModBlocks.MACHINE_SHREDDER);
    public static final RegistryHandle<BlockEntityType<BlockEntityMachineTeleporter>> TELEBLOCK =
            be("teleblock", BlockEntityMachineTeleporter::new, ModBlocks.MACHINE_TELEPORTER)
                    .ticks(
                            BlockEntityMachineTeleporter::tickClient,
                            BlockEntityMachineTeleporter::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityCargoElevator>> CARGO_ELEVATOR =
            be("cargo_elevator", BlockEntityCargoElevator::new, ModBlocks.CARGO_ELEVATOR)
                    .ticks(
                            BlockEntityCargoElevator::tickClient,
                            BlockEntityCargoElevator::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityDecon>> DECON =
            be("decon", BlockEntityDecon::new, ModBlocks.DECON)
                    .ticks(BlockEntityDecon::tickClient, BlockEntityDecon::tickServer);
    public static final RegistryHandle<BlockEntityType<TileEntityGlyphidSpawner>> GLYPHID_SPAWNER =
            be(
                            "glyphid_spawner",
                            TileEntityGlyphidSpawner::new,
                            ModBlocks.GLYPHID_SPAWNER,
                            ModBlocks.GLYPHID_SPAWNER_INFESTED,
                            ModBlocks.GLYPHID_SPAWNER_RAD)
                    .ticksServer(TileEntityGlyphidSpawner::tickServer);
    public static final RegistryHandle<BlockEntityType<BlockEntityStatueElbF>> STATUE_ELB_F =
            be("deco_f", BlockEntityStatueElbF::new, ModBlocks.STATUE_ELB_F)
                    .ticksServerAt((level, pos, state, blockEntity) -> blockEntity.tickServer());
    public static final RegistryHandle<BlockEntityType<BlockEntityLanternBehemoth>>
            LANTERN_BEHEMOTH =
                    be(
                                    "lantern_behemoth",
                                    BlockEntityLanternBehemoth::new,
                                    ModBlocks.LANTERN_BEHEMOTH)
                            .ticksServerAt(
                                    (level, pos, state, blockEntity) -> blockEntity.tickServer());
    public static final RegistryHandle<BlockEntityType<TileEntityLantern>> LANTERN =
            be("lantern_ordinary", TileEntityLantern::new, ModBlocks.LANTERN)
                    .ticksServer(TileEntityLantern::tickServer);

    private ModBlockEntities() {}

    @SafeVarargs
    private static <T extends BlockEntity> Reg.BeHandle<T> be(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return be(
                name,
                () ->
                        new BlockEntityType<>(
                                factory,
                                Arrays.stream(blocks)
                                        .map(RegistryHandle::get)
                                        .collect(Collectors.toUnmodifiableSet())));
    }

    private static <T extends BlockEntity> Reg.BeHandle<T> be(
            String name, Supplier<BlockEntityType<T>> typeFactory) {
        return Reg.be(name, typeFactory);
    }

    @SafeVarargs
    private static <T extends BlockEntityMachineBase> Reg.BeHandle<T> machine(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return be(name, factory, blocks).ticksServer(BlockEntityMachineBase::tickServer);
    }

    @SafeVarargs
    private static <T extends BlockEntityMachineBase> Reg.BeHandle<T> clientMachine(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return machine(name, factory, blocks).ticksClient(BlockEntityMachineBase::tickClient);
    }

    @SafeVarargs
    private static <T extends BlockEntityFoundryBase> Reg.BeHandle<T> foundry(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return be(name, factory, blocks).ticksServer(BlockEntityFoundryBase::tickServer);
    }

    @SafeVarargs
    private static <T extends BlockEntityFoundryBase> Reg.BeHandle<T> clientFoundry(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return foundry(name, factory, blocks).ticksClient(BlockEntityFoundryBase::tickClient);
    }

    @SafeVarargs
    private static <T extends BlockEntityMachinePumpBase> Reg.BeHandle<T> pump(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            RegistryHandle<? extends Block>... blocks) {
        return be(name, factory, blocks)
                .ticks(
                        BlockEntityMachinePumpBase::tickClient,
                        BlockEntityMachinePumpBase::tickServer);
    }

    private static Reg.BeHandle<BlockEntityCrate> crate(
            CrateType type, RegistryHandle<? extends Block> block) {
        return be(type.id, (pos, state) -> new BlockEntityCrate(type, pos, state), block);
    }
}
