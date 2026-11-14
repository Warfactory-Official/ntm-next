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

package com.hbm.blocks;

import com.hbm.blockentity.*;
import com.hbm.blockentity.bomb.*;
import com.hbm.blockentity.deco.ObjTesterBlockEntity;
import com.hbm.blockentity.door.BlastDoorBlockEntity;
import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.effect.EmitterBlockEntity;
import com.hbm.blockentity.effect.GeyserBlockEntity;
import com.hbm.blockentity.encounter.LogicBlockEntity;
import com.hbm.blockentity.encounter.TrappedBrickBlockEntity;
import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.blockentity.generator.RadGenBlockEntity;
import com.hbm.blockentity.generator.WasteFuel;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.albion.*;
import com.hbm.blockentity.machine.dfc.*;
import com.hbm.blockentity.machine.fusion.*;
import com.hbm.blockentity.machine.oil.*;
import com.hbm.blockentity.machine.pile.*;
import com.hbm.blockentity.machine.radio.EnervationBlockEntity;
import com.hbm.blockentity.machine.radio.RadioReceiverBlockEntity;
import com.hbm.blockentity.machine.rbmk.*;
import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blockentity.machine.storage.*;
import com.hbm.blockentity.machine.thermal.BrickFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CombinationFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.CoolingHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.ElectricHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.PoweredCondenserBlockEntity;
import com.hbm.blockentity.machine.thermal.SteamCondenserBlockEntity;
import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.StirlingGeneratorBlockEntity;
import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
import com.hbm.blockentity.network.*;
import com.hbm.blockentity.network.pneumatic.*;
import com.hbm.blockentity.ordnance.launch.*;
import com.hbm.blockentity.processing.AmmoPressBlockEntity;
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blockentity.processing.ArcWelderBlockEntity;
import com.hbm.blockentity.processing.AssemblyMachineBlockEntity;
import com.hbm.blockentity.processing.BurnerPressBlockEntity;
import com.hbm.blockentity.processing.CentrifugeBlockEntity;
import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.blockentity.processing.ConveyorStampBlockEntity;
import com.hbm.blockentity.processing.CrystalProcessorBlockEntity;
import com.hbm.blockentity.processing.ElectricPressBlockEntity;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.blockentity.processing.FluidMixerBlockEntity;
import com.hbm.blockentity.processing.ShredderBlockEntity;
import com.hbm.blockentity.processing.SolderingBenchBlockEntity;
import com.hbm.blockentity.prop.BehemothLanternBlockEntity;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blockentity.travel.TeleporterBlockEntity;
import com.hbm.blockentity.turret.TurretArtyBlockEntity;
import com.hbm.blockentity.turret.TurretChekhovBlockEntity;
import com.hbm.blockentity.turret.TurretFriendlyBlockEntity;
import com.hbm.blockentity.turret.TurretFritzBlockEntity;
import com.hbm.blockentity.turret.TurretHIMARSBlockEntity;
import com.hbm.blockentity.turret.TurretHowardBlockEntity;
import com.hbm.blockentity.turret.TurretHowardDamagedBlockEntity;
import com.hbm.blockentity.turret.TurretJeremyBlockEntity;
import com.hbm.blockentity.turret.TurretMaxwellBlockEntity;
import com.hbm.blockentity.turret.TurretRichardBlockEntity;
import com.hbm.blockentity.turret.TurretSentryBlockEntity;
import com.hbm.blockentity.turret.TurretSentryDamagedBlockEntity;
import com.hbm.blockentity.turret.TurretTauonBlockEntity;
import com.hbm.blocks.generic.DynamicSlagBlock;
import com.hbm.blocks.generic.LootBlock;
import com.hbm.blocks.generic.TrappedBrickBlock;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Reg.BeHandle<TestBlockEntity> TEST =
            (Reg.<TestBlockEntity>be(
                    "test_block_entity", TestBlockEntity::new, ModBlocks.TEST_BLOCK));
    public static final Reg.BeHandle<ObjTesterBlockEntity> OBJ_TESTER =
            (Reg.<ObjTesterBlockEntity>be(
                    "objtester", ObjTesterBlockEntity::new, ModBlocks.OBJ_TESTER));
    public static final Reg.BeHandle<RailBlockEntity> RAIL =
            (Reg.<RailBlockEntity>be(
                    "rail_switch",
                    RailBlockEntity::new,
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
                    ModBlocks.RAIL_NARROW_CURVE));
    public static final Reg.BeHandle<CentrifugeBlockEntity> CENTRIFUGE =
            (Reg.<CentrifugeBlockEntity>be(
                            "centrifuge", CentrifugeBlockEntity::new, ModBlocks.MACHINE_CENTRIFUGE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<GasCentBlockEntity> GAS_CENTRIFUGE =
            (Reg.<GasCentBlockEntity>be(
                            "gas_centrifuge", GasCentBlockEntity::new, ModBlocks.MACHINE_GASCENT))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SuperComputerBlockEntity> SUPERCOMPUTER =
            (Reg.<SuperComputerBlockEntity>be(
                            "supercomputer",
                            SuperComputerBlockEntity::new,
                            ModBlocks.MACHINE_SUPERCOMPUTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<TapeDriveBlockEntity> TAPE_DRIVE =
            (Reg.<TapeDriveBlockEntity>be(
                            "tape_drive", TapeDriveBlockEntity::new, ModBlocks.MACHINE_TAPE_DRIVE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE =
            (Reg.<ElectricFurnaceBlockEntity>be(
                            "electric_furnace",
                            ElectricFurnaceBlockEntity::new,
                            ModBlocks.MACHINE_ELECTRIC_FURNACE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<GeigerBlockEntity> GEIGER =
            (Reg.<GeigerBlockEntity>be("geiger", GeigerBlockEntity::new, ModBlocks.GEIGER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CyberCrabBlockEntity> CRABS =
            (Reg.<CyberCrabBlockEntity>be(
                            "crabs", CyberCrabBlockEntity::new, ModBlocks.METEOR_SPAWNER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RebarBlockEntity> REBAR =
            (Reg.<RebarBlockEntity>be("rebar", RebarBlockEntity::new, ModBlocks.REBAR));
    public static final Reg.BeHandle<TeslaCoilBlockEntity> TESLA_COIL =
            (Reg.<TeslaCoilBlockEntity>be("tesla_coil", TeslaCoilBlockEntity::new, ModBlocks.TESLA))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BurnerPressBlockEntity> PRESS =
            (Reg.<BurnerPressBlockEntity>be(
                            "press", BurnerPressBlockEntity::new, ModBlocks.MACHINE_PRESS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ConveyorStampBlockEntity> CONVEYOR_PRESS =
            (Reg.<ConveyorStampBlockEntity>be(
                            "conveyor_press",
                            ConveyorStampBlockEntity::new,
                            ModBlocks.MACHINE_CONVEYOR_PRESS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CraneInserterBlockEntity> CRANE_INSERTER =
            (Reg.<CraneInserterBlockEntity>be(
                            "inserter", CraneInserterBlockEntity::new, ModBlocks.CRANE_INSERTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CraneExtractorBlockEntity> CRANE_EXTRACTOR =
            (Reg.<CraneExtractorBlockEntity>be(
                            "extractor", CraneExtractorBlockEntity::new, ModBlocks.CRANE_EXTRACTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CraneGrabberBlockEntity> CRANE_GRABBER =
            (Reg.<CraneGrabberBlockEntity>be(
                            "grabber", CraneGrabberBlockEntity::new, ModBlocks.CRANE_GRABBER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CraneBoxerBlockEntity> CRANE_BOXER =
            (Reg.<CraneBoxerBlockEntity>be(
                            "boxer", CraneBoxerBlockEntity::new, ModBlocks.CRANE_BOXER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CraneUnboxerBlockEntity> CRANE_UNBOXER =
            (Reg.<CraneUnboxerBlockEntity>be(
                            "unboxer", CraneUnboxerBlockEntity::new, ModBlocks.CRANE_UNBOXER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CraneRouterBlockEntity> CRANE_ROUTER =
            (Reg.<CraneRouterBlockEntity>be(
                    "router", CraneRouterBlockEntity::new, ModBlocks.CRANE_ROUTER));
    public static final Reg.BeHandle<CraneSplitterBlockEntity> CRANE_SPLITTER =
            (Reg.<CraneSplitterBlockEntity>be(
                    "splitter", CraneSplitterBlockEntity::new, ModBlocks.CRANE_SPLITTER));
    public static final Reg.BeHandle<CranePartitionerBlockEntity> CRANE_PARTITIONER =
            (Reg.<CranePartitionerBlockEntity>be(
                            "partitioner",
                            CranePartitionerBlockEntity::new,
                            ModBlocks.CRANE_PARTITIONER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<TemplateCrafterBlockEntity> AUTOCRAFTER =
            (Reg.<TemplateCrafterBlockEntity>be(
                            "autocrafter",
                            TemplateCrafterBlockEntity::new,
                            ModBlocks.MACHINE_AUTOCRAFTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<NukeManBlockEntity> NUKEMAN =
            (Reg.<NukeManBlockEntity>be("nukeman", NukeManBlockEntity::new, ModBlocks.NUKE_MAN));
    public static final Reg.BeHandle<NukeGadgetBlockEntity> NUKE_GADGET =
            (Reg.<NukeGadgetBlockEntity>be(
                    "nukegadget", NukeGadgetBlockEntity::new, ModBlocks.NUKE_GADGET));
    public static final Reg.BeHandle<NukeBoyBlockEntity> NUKE_BOY =
            (Reg.<NukeBoyBlockEntity>be("nukeboy", NukeBoyBlockEntity::new, ModBlocks.NUKE_BOY));
    public static final Reg.BeHandle<NukeMikeBlockEntity> NUKE_MIKE =
            (Reg.<NukeMikeBlockEntity>be(
                    "nukemike", NukeMikeBlockEntity::new, ModBlocks.NUKE_MIKE));
    public static final Reg.BeHandle<NukeTsarBlockEntity> NUKE_TSAR =
            (Reg.<NukeTsarBlockEntity>be(
                    "nuketsar", NukeTsarBlockEntity::new, ModBlocks.NUKE_TSAR));
    public static final Reg.BeHandle<NukeFleijaBlockEntity> NUKE_FLEIJA =
            (Reg.<NukeFleijaBlockEntity>be(
                    "nukefleija", NukeFleijaBlockEntity::new, ModBlocks.NUKE_FLEIJA));
    public static final Reg.BeHandle<NukePrototypeBlockEntity> NUKE_PROTOTYPE =
            (Reg.<NukePrototypeBlockEntity>be(
                    "nukeproto", NukePrototypeBlockEntity::new, ModBlocks.NUKE_PROTOTYPE));
    public static final Reg.BeHandle<NukeSoliniumBlockEntity> NUKE_SOLINIUM =
            (Reg.<NukeSoliniumBlockEntity>be(
                    "nuke_solinium", NukeSoliniumBlockEntity::new, ModBlocks.NUKE_SOLINIUM));
    public static final Reg.BeHandle<NukeN2BlockEntity> NUKE_N2 =
            (Reg.<NukeN2BlockEntity>be("nuke_n2", NukeN2BlockEntity::new, ModBlocks.NUKE_N2));
    public static final Reg.BeHandle<MissileWorkbenchBlockEntity> MISSILE_ASSEMBLY =
            (Reg.<MissileWorkbenchBlockEntity>be(
                    "missile_assembly",
                    MissileWorkbenchBlockEntity::new,
                    ModBlocks.MACHINE_MISSILE_ASSEMBLY));
    public static final Reg.BeHandle<LaunchPadBlockEntity> LAUNCH1 =
            (Reg.<LaunchPadBlockEntity>be(
                            "launch1", LaunchPadBlockEntity::new, ModBlocks.LAUNCH_PAD))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LaunchSiloBlockEntity> LAUNCHPAD_RUSTED =
            (Reg.<LaunchSiloBlockEntity>be(
                            "launchpad_rusted",
                            LaunchSiloBlockEntity::new,
                            ModBlocks.LAUNCH_PAD_RUSTED))
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RadarBlockEntity> RADAR =
            (Reg.<RadarBlockEntity>be("radar", RadarBlockEntity::new, ModBlocks.MACHINE_RADAR))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RadarLargeBlockEntity> RADAR_LARGE =
            (Reg.<RadarLargeBlockEntity>be(
                            "radar_large",
                            RadarLargeBlockEntity::new,
                            ModBlocks.MACHINE_RADAR_LARGE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RadarScreenBlockEntity> RADAR_SCREEN =
            (Reg.<RadarScreenBlockEntity>be(
                            "radar_screen", RadarScreenBlockEntity::new, ModBlocks.RADAR_SCREEN))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<LaunchTableBlockEntity> LARGE_LAUNCH_TABLE =
            (Reg.<LaunchTableBlockEntity>be(
                            "large_launch_table",
                            LaunchTableBlockEntity::new,
                            ModBlocks.LAUNCH_TABLE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<MissileErectorBlockEntity> LAUNCHPAD_LARGE =
            (Reg.<MissileErectorBlockEntity>be(
                            "launchpad_large",
                            MissileErectorBlockEntity::new,
                            ModBlocks.LAUNCH_PAD_LARGE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CompactLauncherBlockEntity> SMALL_LAUNCHER =
            (Reg.<CompactLauncherBlockEntity>be(
                            "small_launcher",
                            CompactLauncherBlockEntity::new,
                            ModBlocks.COMPACT_LAUNCHER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LauncherAssemblyBlockEntity> MULTI_CORE =
            (Reg.<LauncherAssemblyBlockEntity>be(
                    "multi_core",
                    LauncherAssemblyBlockEntity::new,
                    ModBlocks.STRUCT_LAUNCHER_CORE,
                    ModBlocks.STRUCT_LAUNCHER_CORE_LARGE));
    public static final Reg.BeHandle<CargoCapsuleBlockEntity> SOYUZ_CAPSULE =
            (Reg.<CargoCapsuleBlockEntity>be(
                    "soyuz_capsule", CargoCapsuleBlockEntity::new, ModBlocks.SOYUZ_CAPSULE));
    public static final Reg.BeHandle<SoyuzAssemblyBlockEntity> SOYUZ_STRUCT =
            (Reg.<SoyuzAssemblyBlockEntity>be(
                    "soyuz_struct", SoyuzAssemblyBlockEntity::new, ModBlocks.STRUCT_SOYUZ_CORE));
    public static final Reg.BeHandle<SoyuzLauncherBlockEntity> SOYUZ_LAUNCHER =
            (Reg.<SoyuzLauncherBlockEntity>be(
                            "soyuz_launcher",
                            SoyuzLauncherBlockEntity::new,
                            ModBlocks.SOYUZ_LAUNCHER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LaunchpadSoyuzBlockEntity> LAUNCHPAD_SOYUZ =
            (Reg.<LaunchpadSoyuzBlockEntity>be(
                            "launchpad_soyuz",
                            LaunchpadSoyuzBlockEntity::new,
                            ModBlocks.LAUNCHPAD_SOYUZ))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SoyuzRenderAnchorBlockEntity> LAUNCHPAD_SOYUZ_ANCHOR =
            (Reg.<SoyuzRenderAnchorBlockEntity>be(
                            "launchpad_soyuz_anchor",
                            SoyuzRenderAnchorBlockEntity::new,
                            ModBlocks.LAUNCHPAD_SOYUZ_ANCHOR))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SatDockBlockEntity> MINER_DOCK =
            (Reg.<SatDockBlockEntity>be("miner_dock", SatDockBlockEntity::new, ModBlocks.SAT_DOCK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DoorBlockEntity> NTM_DOOR =
            (Reg.<DoorBlockEntity>be(
                            "door",
                            DoorBlockEntity::new,
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
                            ModBlocks.SILO_HATCH_LARGE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretHowardDamagedBlockEntity> TURRET_HOWARD_DAMAGED =
            (Reg.<TurretHowardDamagedBlockEntity>be(
                            "turret_howard_damaged",
                            TurretHowardDamagedBlockEntity::new,
                            ModBlocks.TURRET_HOWARD_DAMAGED))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretChekhovBlockEntity> TURRET_CHEKHOV =
            (Reg.<TurretChekhovBlockEntity>be(
                            "turret_chekhov",
                            TurretChekhovBlockEntity::new,
                            ModBlocks.TURRET_CHEKHOV))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretFriendlyBlockEntity> TURRET_FRIENDLY =
            (Reg.<TurretFriendlyBlockEntity>be(
                            "turret_friendly",
                            TurretFriendlyBlockEntity::new,
                            ModBlocks.TURRET_FRIENDLY))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretJeremyBlockEntity> TURRET_JEREMY =
            (Reg.<TurretJeremyBlockEntity>be(
                            "turret_jeremy", TurretJeremyBlockEntity::new, ModBlocks.TURRET_JEREMY))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretRichardBlockEntity> TURRET_RICHARD =
            (Reg.<TurretRichardBlockEntity>be(
                            "turret_richard",
                            TurretRichardBlockEntity::new,
                            ModBlocks.TURRET_RICHARD))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretHowardBlockEntity> TURRET_HOWARD =
            (Reg.<TurretHowardBlockEntity>be(
                            "turret_howard", TurretHowardBlockEntity::new, ModBlocks.TURRET_HOWARD))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretTauonBlockEntity> TURRET_TAUON =
            (Reg.<TurretTauonBlockEntity>be(
                            "turret_tauon", TurretTauonBlockEntity::new, ModBlocks.TURRET_TAUON))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretFritzBlockEntity> TURRET_FRITZ =
            (Reg.<TurretFritzBlockEntity>be(
                            "turret_fritz", TurretFritzBlockEntity::new, ModBlocks.TURRET_FRITZ))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretMaxwellBlockEntity> TURRET_MAXWELL =
            (Reg.<TurretMaxwellBlockEntity>be(
                            "turret_maxwell",
                            TurretMaxwellBlockEntity::new,
                            ModBlocks.TURRET_MAXWELL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretSentryBlockEntity> TURRET_SENTRY =
            (Reg.<TurretSentryBlockEntity>be(
                            "turret_sentry", TurretSentryBlockEntity::new, ModBlocks.TURRET_SENTRY))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretSentryDamagedBlockEntity> TURRET_SENTRY_DAMAGED =
            (Reg.<TurretSentryDamagedBlockEntity>be(
                            "turret_sentry_damaged",
                            TurretSentryDamagedBlockEntity::new,
                            ModBlocks.TURRET_SENTRY_DAMAGED))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretArtyBlockEntity> TURRET_ARTY =
            (Reg.<TurretArtyBlockEntity>be(
                            "turret_arty", TurretArtyBlockEntity::new, ModBlocks.TURRET_ARTY))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurretHIMARSBlockEntity> TURRET_HIMARS =
            (Reg.<TurretHIMARSBlockEntity>be(
                            "turret_himars", TurretHIMARSBlockEntity::new, ModBlocks.TURRET_HIMARS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SkeletonHolderBlockEntity> NTM_SKELETON =
            (Reg.<SkeletonHolderBlockEntity>be(
                    "skeleton", SkeletonHolderBlockEntity::new, ModBlocks.SKELETON_HOLDER));
    public static final Reg.BeHandle<PedestalBlockEntity> NTM_PEDESTAL =
            (Reg.<PedestalBlockEntity>be("pedestal", PedestalBlockEntity::new, ModBlocks.PEDESTAL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DungeonSpawnerBlockEntity> NTM_DUNGEON_SPAWNER =
            (Reg.<DungeonSpawnerBlockEntity>be(
                            "dungeon_spawner",
                            DungeonSpawnerBlockEntity::new,
                            ModBlocks.DUNGEON_SPAWNER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BobbleBlockEntity> NTM_BOBBLEHEAD =
            (Reg.<BobbleBlockEntity>be("bobblehead", BobbleBlockEntity::new, ModBlocks.BOBBLEHEAD));
    public static final Reg.BeHandle<SnowglobeBlockEntity> NTM_SNOWGLOBE =
            (Reg.<SnowglobeBlockEntity>be(
                    "snowglobe", SnowglobeBlockEntity::new, ModBlocks.SNOWGLOBE));
    public static final Reg.BeHandle<PlushieBlockEntity> NTM_PLUSHIE =
            (Reg.<PlushieBlockEntity>be("plushie", PlushieBlockEntity::new, ModBlocks.PLUSHIE))
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FloodlightBlockEntity> FLOODLIGHT =
            (Reg.<FloodlightBlockEntity>be(
                            "floodlight", FloodlightBlockEntity::new, ModBlocks.FLOODLIGHT))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FloodlightBeamBlockEntity> FLOODLIGHT_BEAM =
            (Reg.<FloodlightBeamBlockEntity>be(
                    "floodlight_beam", FloodlightBeamBlockEntity::new, ModBlocks.FLOODLIGHT_BEAM));
    public static final Reg.BeHandle<DemonLampBlockEntity> DEMON_LAMP =
            (Reg.<DemonLampBlockEntity>be(
                            "demonlamp", DemonLampBlockEntity::new, ModBlocks.LAMP_DEMON))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<AssemblyMachineBlockEntity> ASSEMBLYMACHINE =
            (Reg.<AssemblyMachineBlockEntity>be(
                            "assemblymachine",
                            AssemblyMachineBlockEntity::new,
                            ModBlocks.MACHINE_ASSEMBLY_MACHINE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<AssemblyFactoryBlockEntity> ASSEMBLYFACTORY =
            (Reg.<AssemblyFactoryBlockEntity>be(
                            "assemblyfactory",
                            AssemblyFactoryBlockEntity::new,
                            ModBlocks.MACHINE_ASSEMBLY_FACTORY))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ChemicalPlantBlockEntity> CHEMICALPLANT =
            (Reg.<ChemicalPlantBlockEntity>be(
                            "chemicalplant",
                            ChemicalPlantBlockEntity::new,
                            ModBlocks.MACHINE_CHEMICAL_PLANT))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RockMillBlockEntity> ROCK_MILL =
            (Reg.<RockMillBlockEntity>be(
                            "rock_mill", RockMillBlockEntity::new, ModBlocks.MACHINE_ROCK_MILL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CustomMachineBlockEntity> CUSTOM_MACHINE =
            (Reg.<CustomMachineBlockEntity>be(
                            "custom_machine",
                            CustomMachineBlockEntity::new,
                            ModBlocks.CUSTOM_MACHINE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadiolysisBlockEntity> RADIOLYSIS =
            (Reg.<RadiolysisBlockEntity>be(
                            "radiolysis", RadiolysisBlockEntity::new, ModBlocks.MACHINE_RADIOLYSIS))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DeuteriumExtractorBlockEntity> DEUTERIUM_EXTRACTOR =
            (Reg.<DeuteriumExtractorBlockEntity>be(
                            "deuterium_extractor",
                            DeuteriumExtractorBlockEntity::new,
                            ModBlocks.MACHINE_DEUTERIUM_EXTRACTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DeuteriumTowerBlockEntity> DEUTERIUM_TOWER =
            (Reg.<DeuteriumTowerBlockEntity>be(
                            "deuterium_tower",
                            DeuteriumTowerBlockEntity::new,
                            ModBlocks.MACHINE_DEUTERIUM_TOWER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ChemicalFactoryBlockEntity> CHEMICALFACTORY =
            (Reg.<ChemicalFactoryBlockEntity>be(
                            "chemicalfactory",
                            ChemicalFactoryBlockEntity::new,
                            ModBlocks.MACHINE_CHEMICAL_FACTORY))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ElectrolysisBlockEntity> ELECTROLYSER =
            (Reg.<ElectrolysisBlockEntity>be(
                            "electrolyser",
                            ElectrolysisBlockEntity::new,
                            ModBlocks.MACHINE_ELECTROLYSER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RefineryBlockEntity> REFINERY =
            (Reg.<RefineryBlockEntity>be(
                            "refinery", RefineryBlockEntity::new, ModBlocks.MACHINE_REFINERY))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CatalyticReformerBlockEntity> CATALYTIC_REFORMER =
            (Reg.<CatalyticReformerBlockEntity>be(
                            "catalytic_reformer",
                            CatalyticReformerBlockEntity::new,
                            ModBlocks.MACHINE_CATALYTIC_REFORMER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<HydrotreaterBlockEntity> HYDROTREATER =
            (Reg.<HydrotreaterBlockEntity>be(
                            "hydrotreater",
                            HydrotreaterBlockEntity::new,
                            ModBlocks.MACHINE_HYDROTREATER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FractionTowerBlockEntity> FRACTION_TOWER =
            (Reg.<FractionTowerBlockEntity>be(
                            "fraction_tower",
                            FractionTowerBlockEntity::new,
                            ModBlocks.MACHINE_FRACTION_TOWER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FractionSpacerBlockEntity> FRACTION_SPACER =
            (Reg.<FractionSpacerBlockEntity>be(
                    "fraction_spacer", FractionSpacerBlockEntity::new, ModBlocks.FRACTION_SPACER));
    public static final Reg.BeHandle<CrackingTowerBlockEntity> CATALYTIC_CRACKER =
            (Reg.<CrackingTowerBlockEntity>be(
                            "catalytic_cracker",
                            CrackingTowerBlockEntity::new,
                            ModBlocks.MACHINE_CRACKING_TOWER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PurexBlockEntity> PUREX =
            (Reg.<PurexBlockEntity>be("purex", PurexBlockEntity::new, ModBlocks.MACHINE_PUREX))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FluidMixerBlockEntity> MIXER =
            (Reg.<FluidMixerBlockEntity>be(
                            "mixer", FluidMixerBlockEntity::new, ModBlocks.MACHINE_MIXER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SilexBlockEntity> SILEX =
            (Reg.<SilexBlockEntity>be("silex", SilexBlockEntity::new, ModBlocks.MACHINE_SILEX))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<OreSlopperBlockEntity> ORE_SLOPPER =
            (Reg.<OreSlopperBlockEntity>be(
                            "ore_slopper",
                            OreSlopperBlockEntity::new,
                            ModBlocks.MACHINE_ORE_SLOPPER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<AnnihilatorBlockEntity> ANNIHILATOR =
            (Reg.<AnnihilatorBlockEntity>be(
                            "annihilator",
                            AnnihilatorBlockEntity::new,
                            ModBlocks.MACHINE_ANNIHILATOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BlastFurnaceBlockEntity> BLAST_FURNACE =
            (Reg.<BlastFurnaceBlockEntity>be(
                            "blast_furnace",
                            BlastFurnaceBlockEntity::new,
                            ModBlocks.MACHINE_BLAST_FURNACE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<GeothermalExchanger> HEPHAESTUS =
            (Reg.<GeothermalExchanger>be(
                            "hephaestus", GeothermalExchanger::new, ModBlocks.MACHINE_HEPHAESTUS))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CrucibleBlockEntity> CRUCIBLE =
            (Reg.<CrucibleBlockEntity>be(
                            "crucible", CrucibleBlockEntity::new, ModBlocks.MACHINE_CRUCIBLE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ContinuousCaster> STRAND_CASTER =
            (Reg.<ContinuousCaster>be(
                            "strand_caster",
                            ContinuousCaster::new,
                            ModBlocks.MACHINE_STRAND_CASTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PrecisionAssemblerBlockEntity> PRECASS =
            (Reg.<PrecisionAssemblerBlockEntity>be(
                            "precass",
                            PrecisionAssemblerBlockEntity::new,
                            ModBlocks.MACHINE_PRECASS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<MiningLaserBlockEntity> MINING_LASER =
            (Reg.<MiningLaserBlockEntity>be(
                            "mining_laser",
                            MiningLaserBlockEntity::new,
                            ModBlocks.MACHINE_MINING_LASER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RepulsionFieldBlockEntity> FORCEFIELD =
            (Reg.<RepulsionFieldBlockEntity>be(
                            "machine_field",
                            RepulsionFieldBlockEntity::new,
                            ModBlocks.MACHINE_FORCEFIELD))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ExcavatorBlockEntity> EXCAVATOR =
            (Reg.<ExcavatorBlockEntity>be(
                            "excavator", ExcavatorBlockEntity::new, ModBlocks.MACHINE_EXCAVATOR))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CyclotronBlockEntity> CYCLOTRON =
            (Reg.<CyclotronBlockEntity>be(
                            "cyclotron", CyclotronBlockEntity::new, ModBlocks.MACHINE_CYCLOTRON))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ExposureChamberBlockEntity> EXPOSURE_CHAMBER =
            (Reg.<ExposureChamberBlockEntity>be(
                            "exposure_chamber",
                            ExposureChamberBlockEntity::new,
                            ModBlocks.MACHINE_EXPOSURE_CHAMBER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RadGenBlockEntity> RADGEN =
            (Reg.<RadGenBlockEntity>be("radgen", RadGenBlockEntity::new, ModBlocks.MACHINE_RADGEN))
                    .ticksServerAt(BlockEntityTicks.SERVER);

    static {
        Reg.afterRegistration(RADGEN, _ -> WasteFuel.initialize());
    }

    public static final Reg.BeHandle<ReactorBreedingBlockEntity> MACHINE_REACTOR_BREEDING =
            (Reg.<ReactorBreedingBlockEntity>be(
                            "reactor",
                            ReactorBreedingBlockEntity::new,
                            ModBlocks.MACHINE_REACTOR_BREEDING))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ResearchReactorBlockEntity> REACTOR_RESEARCH =
            (Reg.<ResearchReactorBlockEntity>be(
                            "small_reactor",
                            ResearchReactorBlockEntity::new,
                            ModBlocks.REACTOR_RESEARCH))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ReactorControlBlockEntity> REACTOR_CONTROL =
            (Reg.<ReactorControlBlockEntity>be(
                            "reactor_remote_control",
                            ReactorControlBlockEntity::new,
                            ModBlocks.MACHINE_CONTROLLER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<TowerSmallBlockEntity> TOWER_SMALL =
            (Reg.<TowerSmallBlockEntity>be(
                            "cooling_tower_small",
                            TowerSmallBlockEntity::new,
                            ModBlocks.MACHINE_TOWER_SMALL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TowerLargeBlockEntity> TOWER_LARGE =
            (Reg.<TowerLargeBlockEntity>be(
                            "cooling_tower_large",
                            TowerLargeBlockEntity::new,
                            ModBlocks.MACHINE_TOWER_LARGE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RotaryFurnaceBlockEntity> ROTARY_FURNACE =
            (Reg.<RotaryFurnaceBlockEntity>be(
                            "rotary_furnace",
                            RotaryFurnaceBlockEntity::new,
                            ModBlocks.MACHINE_ROTARY_FURNACE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ArcFurnaceBlockEntity> ARC_FURNACE_LARGE =
            (Reg.<ArcFurnaceBlockEntity>be(
                            "arc_furnace_large",
                            ArcFurnaceBlockEntity::new,
                            ModBlocks.MACHINE_ARC_FURNACE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<HeaterFireboxBlockEntity> FIREBOX =
            (Reg.<HeaterFireboxBlockEntity>be(
                            "firebox", HeaterFireboxBlockEntity::new, ModBlocks.HEATER_FIREBOX))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ElectricHeatBlockEntity> ELECTRIC_HEATER =
            (Reg.<ElectricHeatBlockEntity>be(
                            "electric_heater",
                            ElectricHeatBlockEntity::new,
                            ModBlocks.HEATER_ELECTRIC))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FoundryMoldBlockEntity> FOUNDRY_MOLD =
            (Reg.<FoundryMoldBlockEntity>be(
                            "foundry_mold", FoundryMoldBlockEntity::new, ModBlocks.FOUNDRY_MOLD))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FoundryBasinBlockEntity> FOUNDRY_BASIN =
            (Reg.<FoundryBasinBlockEntity>be(
                            "foundry_basin", FoundryBasinBlockEntity::new, ModBlocks.FOUNDRY_BASIN))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FoundryChannelBlockEntity> FOUNDRY_CHANNEL =
            (Reg.<FoundryChannelBlockEntity>be(
                            "foundry_channel",
                            FoundryChannelBlockEntity::new,
                            ModBlocks.FOUNDRY_CHANNEL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FoundryDischarge> FOUNDRY_OUTLET =
            (Reg.<FoundryDischarge>be(
                            "foundry_outlet", FoundryDischarge::new, ModBlocks.FOUNDRY_OUTLET))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SlagDischarge> FOUNDRY_SLAGTAP =
            (Reg.<SlagDischarge>be(
                            "foundry_slagtap", SlagDischarge::new, ModBlocks.FOUNDRY_SLAGTAP))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FoundryTankBlockEntity> FOUNDRY_TANK =
            (Reg.<FoundryTankBlockEntity>be(
                            "foundry_tank", FoundryTankBlockEntity::new, ModBlocks.FOUNDRY_TANK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DynamicSlagBlock.BlockEntitySlag> FOUNDRY_SLAG =
            (Reg.<DynamicSlagBlock.BlockEntitySlag>be(
                    "foundry_slag", DynamicSlagBlock.BlockEntitySlag::new, ModBlocks.SLAG));
    public static final Reg.BeHandle<LootBlock.BlockEntityLoot> NTM_LOOT =
            (Reg.<LootBlock.BlockEntityLoot>be(
                    "loot", LootBlock.BlockEntityLoot::new, ModBlocks.LOOT));
    public static final Reg.BeHandle<DerrickBlockEntity> DERRICK =
            (Reg.<DerrickBlockEntity>be("derrick", DerrickBlockEntity::new, ModBlocks.MACHINE_WELL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PumpjackBlockEntity> MACHINE_PUMPJACK =
            (Reg.<PumpjackBlockEntity>be(
                            "machine_pumpjack",
                            PumpjackBlockEntity::new,
                            ModBlocks.MACHINE_PUMPJACK))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PneumaticCompressorBlockEntity> COMPRESSOR =
            (Reg.<PneumaticCompressorBlockEntity>be(
                            "compressor",
                            PneumaticCompressorBlockEntity::new,
                            ModBlocks.MACHINE_COMPRESSOR))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CompactPneumaticCompressor> COMPRESSOR_COMPACT =
            (Reg.<CompactPneumaticCompressor>be(
                            "compressor_compact",
                            CompactPneumaticCompressor::new,
                            ModBlocks.MACHINE_COMPRESSOR_COMPACT))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<GasFlareBlockEntity> GASFLARE =
            (Reg.<GasFlareBlockEntity>be(
                            "gasflare", GasFlareBlockEntity::new, ModBlocks.MACHINE_FLARE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ChimneyBrickBlockEntity> CHIMNEY_BRICK =
            (Reg.<ChimneyBrickBlockEntity>be(
                            "chimney_brick", ChimneyBrickBlockEntity::new, ModBlocks.CHIMNEY_BRICK))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ChimneyIndustrialBlockEntity> CHIMNEY_INDUSTRIAL =
            (Reg.<ChimneyIndustrialBlockEntity>be(
                            "chimney_industrial",
                            ChimneyIndustrialBlockEntity::new,
                            ModBlocks.CHIMNEY_INDUSTRIAL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ReactorZirnoxBlockEntity> ZIRNOX =
            (Reg.<ReactorZirnoxBlockEntity>be(
                            "zirnox", ReactorZirnoxBlockEntity::new, ModBlocks.REACTOR_ZIRNOX))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ZirnoxDestroyedBlockEntity> ZIRNOX_DESTROYED =
            (Reg.<ZirnoxDestroyedBlockEntity>be(
                            "zirnox_destroyed",
                            ZirnoxDestroyedBlockEntity::new,
                            ModBlocks.ZIRNOX_DESTROYED))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<WavelengthLaserBlockEntity> FEL =
            (Reg.<WavelengthLaserBlockEntity>be(
                            "fel", WavelengthLaserBlockEntity::new, ModBlocks.MACHINE_FEL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PASourceBlockEntity> PA_SOURCE =
            (Reg.<PASourceBlockEntity>be(
                            "pa_source", PASourceBlockEntity::new, ModBlocks.PA_SOURCE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PABeamlineBlockEntity> PA_BEAMLINE =
            (Reg.<PABeamlineBlockEntity>be(
                    "pa_beamline", PABeamlineBlockEntity::new, ModBlocks.PA_BEAMLINE));
    public static final Reg.BeHandle<RFCavityBlockEntity> PA_RFC =
            (Reg.<RFCavityBlockEntity>be("pa_rfc", RFCavityBlockEntity::new, ModBlocks.PA_RFC))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PAQuadrupoleBlockEntity> PA_QUADRUPOLE =
            (Reg.<PAQuadrupoleBlockEntity>be(
                            "pa_quadrupole", PAQuadrupoleBlockEntity::new, ModBlocks.PA_QUADRUPOLE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PADipoleBlockEntity> PA_DIPOLE =
            (Reg.<PADipoleBlockEntity>be(
                            "pa_dipole", PADipoleBlockEntity::new, ModBlocks.PA_DIPOLE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PADetectorBlockEntity> PA_DETECTOR =
            (Reg.<PADetectorBlockEntity>be(
                            "pa_detector", PADetectorBlockEntity::new, ModBlocks.PA_DETECTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<SolderingBenchBlockEntity> SOLDERING_STATION =
            (Reg.<SolderingBenchBlockEntity>be(
                            "soldering_station",
                            SolderingBenchBlockEntity::new,
                            ModBlocks.MACHINE_SOLDERING_STATION))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ArcWelderBlockEntity> ARC_WELDER =
            (Reg.<ArcWelderBlockEntity>be(
                            "arc_welder", ArcWelderBlockEntity::new, ModBlocks.MACHINE_ARC_WELDER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ElectricPressBlockEntity> ELECTRIC_PRESS =
            (Reg.<ElectricPressBlockEntity>be(
                            "electric_press",
                            ElectricPressBlockEntity::new,
                            ModBlocks.MACHINE_EPRESS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FluidTankBlockEntity> FLUID_TANK =
            (Reg.<FluidTankBlockEntity>be(
                            "fluid_tank", FluidTankBlockEntity::new, ModBlocks.MACHINE_FLUID_TANK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FluidBarrelBlockEntity> FLUID_BARREL =
            (Reg.<FluidBarrelBlockEntity>be(
                            "fluid_barrel",
                            FluidBarrelBlockEntity::new,
                            ModBlocks.BARREL_PLASTIC,
                            ModBlocks.BARREL_STEEL,
                            ModBlocks.BARREL_TCALLOY,
                            ModBlocks.BARREL_ANTIMATTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumaticTubeBlockEntity> PNEUMATIC_TUBE =
            (Reg.<PneumaticTubeBlockEntity>be(
                            "pneumatic_tube",
                            PneumaticTubeBlockEntity::new,
                            ModBlocks.PNEUMATIC_TUBE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumoTubePaintableBlockEntity> PNEUMATIC_TUBE_PAINTABLE =
            (Reg.<PneumoTubePaintableBlockEntity>be(
                            "pneumatic_tube_paintable",
                            PneumoTubePaintableBlockEntity::new,
                            ModBlocks.PNEUMATIC_TUBE_PAINTABLE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumoStorageAccessBlockEntity> PNEUMATIC_STORAGE_ACCESS =
            (Reg.<PneumoStorageAccessBlockEntity>be(
                    "pneumatic_storage_access",
                    PneumoStorageAccessBlockEntity::new,
                    ModBlocks.PNEUMATIC_STORAGE_ACCESS));
    public static final Reg.BeHandle<PneumaticStorageClutterBlockEntity> PNEUMATIC_STORAGE_CLUTTER =
            (Reg.<PneumaticStorageClutterBlockEntity>be(
                            "pneumatic_storage_clutter",
                            PneumaticStorageClutterBlockEntity::new,
                            ModBlocks.PNEUMATIC_STORAGE_CLUTTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumoStorageMonoBlockEntity> PNEUMATIC_STORAGE_MONO =
            (Reg.<PneumoStorageMonoBlockEntity>be(
                            "pneumatic_storage_mono",
                            PneumoStorageMonoBlockEntity::new,
                            ModBlocks.PNEUMATIC_STORAGE_MONO))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumaticImporterBlockEntity> PNEUMATIC_STORAGE_IMPORTER =
            (Reg.<PneumaticImporterBlockEntity>be(
                            "pneumatic_storage_importer",
                            PneumaticImporterBlockEntity::new,
                            ModBlocks.PNEUMATIC_STORAGE_IMPORTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PneumoStorageExporterBlockEntity> PNEUMATIC_STORAGE_EXPORTER =
            (Reg.<PneumoStorageExporterBlockEntity>be(
                            "pneumatic_storage_exporter",
                            PneumoStorageExporterBlockEntity::new,
                            ModBlocks.PNEUMATIC_STORAGE_EXPORTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PipeGaugeBlockEntity> PIPE_GAUGE =
            (Reg.<PipeGaugeBlockEntity>be(
                            "pipe_gauge", PipeGaugeBlockEntity::new, ModBlocks.FLUID_DUCT_GAUGE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FluidCounterValveBlockEntity> PIPE_COUNTER_VALVE =
            (Reg.<FluidCounterValveBlockEntity>be(
                            "pipe_counter_valve",
                            FluidCounterValveBlockEntity::new,
                            ModBlocks.FLUID_COUNTER_VALVE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CableDiodeBlockEntity> CABLE_DIODE =
            (Reg.<CableDiodeBlockEntity>be(
                    "cable_diode", CableDiodeBlockEntity::new, ModBlocks.CABLE_DIODE));
    public static final Reg.BeHandle<PylonMediumBlockEntity> PYLON_MEDIUM =
            (Reg.<PylonMediumBlockEntity>be(
                    "pylon_medium",
                    PylonMediumBlockEntity::new,
                    ModBlocks.RED_PYLON_MEDIUM_WOOD,
                    ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                    ModBlocks.RED_PYLON_MEDIUM_STEEL,
                    ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER));
    public static final Reg.BeHandle<PylonLargeBlockEntity> PYLON_LARGE =
            (Reg.<PylonLargeBlockEntity>be(
                    "pylon_large", PylonLargeBlockEntity::new, ModBlocks.RED_PYLON_LARGE));
    public static final Reg.BeHandle<SubstationBlockEntity> SUBSTATION =
            (Reg.<SubstationBlockEntity>be(
                    "substation", SubstationBlockEntity::new, ModBlocks.SUBSTATION));
    public static final Reg.BeHandle<PylonBlockEntity> PYLON_REDWIRE =
            (Reg.<PylonBlockEntity>be(
                    "pylon_redwire",
                    PylonBlockEntity::new,
                    ModBlocks.RED_PYLON,
                    ModBlocks.RED_PYLON_STEEL));
    public static final Reg.BeHandle<ConnectorBlockEntity> CONNECTOR_REDWIRE =
            (Reg.<ConnectorBlockEntity>be(
                    "connector_redwire", ConnectorBlockEntity::new, ModBlocks.RED_CONNECTOR));
    public static final Reg.BeHandle<ConnectorSuperBlockEntity> CONNECTOR_REDWIRE_SUPER =
            (Reg.<ConnectorSuperBlockEntity>be(
                    "connector_redwire_super",
                    ConnectorSuperBlockEntity::new,
                    ModBlocks.RED_CONNECTOR_SUPER));
    public static final Reg.BeHandle<CableGaugeBlockEntity> CABLE_GAUGE =
            (Reg.<CableGaugeBlockEntity>be(
                            "cable_gauge", CableGaugeBlockEntity::new, ModBlocks.RED_CABLE_GAUGE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FluidPumpBlockEntity> PIPE_PUMP =
            (Reg.<FluidPumpBlockEntity>be(
                            "pipe_pump", FluidPumpBlockEntity::new, ModBlocks.FLUID_PUMP))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<EnervationBlockEntity> RADIOBOX =
            (Reg.<EnervationBlockEntity>be(
                            "radio_broadcaster", EnervationBlockEntity::new, ModBlocks.RADIOBOX))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioReceiverBlockEntity> RADIO_RECEIVER =
            (Reg.<RadioReceiverBlockEntity>be(
                            "radio_receiver", RadioReceiverBlockEntity::new, ModBlocks.RADIO_REC))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioTorchSenderBlockEntity> RTTY_SENDER =
            (Reg.<RadioTorchSenderBlockEntity>be(
                            "rtty_sender",
                            RadioTorchSenderBlockEntity::new,
                            ModBlocks.RADIO_TORCH_SENDER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioTorchReceiverBlockEntity> RTTY_REC =
            (Reg.<RadioTorchReceiverBlockEntity>be(
                            "rtty_rec",
                            RadioTorchReceiverBlockEntity::new,
                            ModBlocks.RADIO_TORCH_RECEIVER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioCounterBlockEntity> RTTY_COUNTER =
            (Reg.<RadioCounterBlockEntity>be(
                            "rtty_counter",
                            RadioCounterBlockEntity::new,
                            ModBlocks.RADIO_TORCH_COUNTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioLogicBlockEntity> RTTY_LOGIC =
            (Reg.<RadioLogicBlockEntity>be(
                            "rtty_logic", RadioLogicBlockEntity::new, ModBlocks.RADIO_TORCH_LOGIC))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioReaderBlockEntity> RTTY_READER =
            (Reg.<RadioReaderBlockEntity>be(
                            "rtty_reader",
                            RadioReaderBlockEntity::new,
                            ModBlocks.RADIO_TORCH_READER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioTorchControllerBlockEntity> RTTY_CONTROLLER =
            (Reg.<RadioTorchControllerBlockEntity>be(
                            "rtty_controller",
                            RadioTorchControllerBlockEntity::new,
                            ModBlocks.RADIO_TORCH_CONTROLLER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneCrateBlockEntity> DRONE_CRATE =
            (Reg.<DroneCrateBlockEntity>be(
                            "drone_crate", DroneCrateBlockEntity::new, ModBlocks.DRONE_CRATE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneDockBlockEntity> DRONE_DOCK =
            (Reg.<DroneDockBlockEntity>be(
                            "drone_dock", DroneDockBlockEntity::new, ModBlocks.DRONE_DOCK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneProviderBlockEntity> DRONE_PROVIDER =
            (Reg.<DroneProviderBlockEntity>be(
                            "drone_provider",
                            DroneProviderBlockEntity::new,
                            ModBlocks.DRONE_CRATE_PROVIDER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneRequesterBlockEntity> DRONE_REQUESTER =
            (Reg.<DroneRequesterBlockEntity>be(
                            "drone_requester",
                            DroneRequesterBlockEntity::new,
                            ModBlocks.DRONE_CRATE_REQUESTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneWaypointRequestBlockEntity> DRONE_WAYPOINT_REQUEST =
            (Reg.<DroneWaypointRequestBlockEntity>be(
                            "drone_waypoint_request",
                            DroneWaypointRequestBlockEntity::new,
                            ModBlocks.DRONE_WAYPOINT_REQUEST))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DroneWaypointBlockEntity> DRONE_WAYPOINT =
            (Reg.<DroneWaypointBlockEntity>be(
                            "drone_waypoint",
                            DroneWaypointBlockEntity::new,
                            ModBlocks.DRONE_WAYPOINT))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RadioTelexBlockEntity> RTTY_TELEX =
            (Reg.<RadioTelexBlockEntity>be(
                            "rtty_telex", RadioTelexBlockEntity::new, ModBlocks.RADIO_TELEX))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RadioAutocalBlockEntity> RTTY_AUTOCAL =
            (Reg.<RadioAutocalBlockEntity>be(
                            "rtty_autocal", RadioAutocalBlockEntity::new, ModBlocks.RADIO_AUTOCAL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PowerDetectorBlockEntity> HE_DETECTOR =
            (Reg.<PowerDetectorBlockEntity>be(
                            "he_detector",
                            PowerDetectorBlockEntity::new,
                            ModBlocks.MACHINE_DETECTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PipePaintableBlockEntity> PIPE_PAINTABLE =
            (Reg.<PipePaintableBlockEntity>be(
                    "pipe_paintable",
                    PipePaintableBlockEntity::new,
                    ModBlocks.FLUID_DUCT_PAINTABLE,
                    ModBlocks.FLUID_DUCT_PAINTABLE_EXHAUST));
    public static final Reg.BeHandle<CablePaintableBlockEntity> CABLE_PAINTABLE =
            (Reg.<CablePaintableBlockEntity>be(
                    "cable_paintable",
                    CablePaintableBlockEntity::new,
                    ModBlocks.RED_CABLE_PAINTABLE));
    public static final Reg.BeHandle<PipeAnchorBlockEntity> PIOE_ANCHOR =
            (Reg.<PipeAnchorBlockEntity>be(
                            "pioe_anchor", PipeAnchorBlockEntity::new, ModBlocks.PIPE_ANCHOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<WoodGeneratorBlockEntity> WOOD_BURNER =
            (Reg.<WoodGeneratorBlockEntity>be(
                            "wood_burner",
                            WoodGeneratorBlockEntity::new,
                            ModBlocks.MACHINE_WOOD_BURNER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<DieselGeneratorBlockEntity> DIESEL_GENERATOR =
            (Reg.<DieselGeneratorBlockEntity>be(
                            "diesel_generator",
                            DieselGeneratorBlockEntity::new,
                            ModBlocks.MACHINE_DIESEL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<IsotopeGeneratorBlockEntity> MACHINE_RTG =
            (Reg.<IsotopeGeneratorBlockEntity>be(
                            "machine_rtg", IsotopeGeneratorBlockEntity::new, ModBlocks.MACHINE_RTG))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CompactTurbineBlockEntity> TURBINE =
            (Reg.<CompactTurbineBlockEntity>be(
                            "turbine", CompactTurbineBlockEntity::new, ModBlocks.MACHINE_TURBINE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<LargeTurbineBlockEntity> INDUSTRIAL_TURBINE =
            (Reg.<LargeTurbineBlockEntity>be(
                            "industrial_turbine",
                            LargeTurbineBlockEntity::new,
                            ModBlocks.MACHINE_LARGE_TURBINE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<IndustrialTurbineBlockEntity> IND_TURBINE =
            (Reg.<IndustrialTurbineBlockEntity>be(
                            "ind_turbine",
                            IndustrialTurbineBlockEntity::new,
                            ModBlocks.MACHINE_INDUSTRIAL_TURBINE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LeviathanTurbineBlockEntity> CHUNGUS =
            (Reg.<LeviathanTurbineBlockEntity>be(
                            "chungus", LeviathanTurbineBlockEntity::new, ModBlocks.MACHINE_CHUNGUS))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<HeatBoilerBlockEntity> HEAT_BOILER =
            (Reg.<HeatBoilerBlockEntity>be(
                            "heat_boiler", HeatBoilerBlockEntity::new, ModBlocks.MACHINE_BOILER))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<HeatBoilerIndustrialBlockEntity> HEAT_BOILER_INDUSTRIAL =
            (Reg.<HeatBoilerIndustrialBlockEntity>be(
                            "heat_boiler_industrial",
                            HeatBoilerIndustrialBlockEntity::new,
                            ModBlocks.MACHINE_INDUSTRIAL_BOILER))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SteamEngineBlockEntity> STEAM_ENGINE =
            (Reg.<SteamEngineBlockEntity>be(
                            "steam_engine",
                            SteamEngineBlockEntity::new,
                            ModBlocks.MACHINE_STEAM_ENGINE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CombustionEngineBlockEntity> COMBUSTION_ENGINE =
            (Reg.<CombustionEngineBlockEntity>be(
                            "combustion_engine",
                            CombustionEngineBlockEntity::new,
                            ModBlocks.MACHINE_COMBUSTION_ENGINE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurbineGasBlockEntity> MACHINE_GASTURBINE =
            (Reg.<TurbineGasBlockEntity>be(
                            "machine_gasturbine",
                            TurbineGasBlockEntity::new,
                            ModBlocks.MACHINE_TURBINEGAS))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TurbofanBlockEntity> MACHINE_TURBOFAN =
            (Reg.<TurbofanBlockEntity>be(
                            "machine_turbofan",
                            TurbofanBlockEntity::new,
                            ModBlocks.MACHINE_TURBOFAN))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LPW2BlockEntity> LPW2 =
            (Reg.<LPW2BlockEntity>be("machine_lpw2", LPW2BlockEntity::new, ModBlocks.MACHINE_LPW2));
    public static final Reg.BeHandle<SteamPumpBlockEntity> STEAM_PUMP =
            (Reg.<SteamPumpBlockEntity>be(
                            "steam_pump", SteamPumpBlockEntity::new, ModBlocks.PUMP_STEAM))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SolarBoilerBlockEntity> SOLARBOILER =
            (Reg.<SolarBoilerBlockEntity>be(
                            "solarboiler",
                            SolarBoilerBlockEntity::new,
                            ModBlocks.MACHINE_SOLAR_BOILER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SolarMirrorBlockEntity> SOLARMIRROR =
            (Reg.<SolarMirrorBlockEntity>be(
                            "solarmirror", SolarMirrorBlockEntity::new, ModBlocks.SOLAR_MIRROR))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SpentFuelCoolingBlockEntity> WASTE_DRUM =
            (Reg.<SpentFuelCoolingBlockEntity>be(
                            "waste_drum", SpentFuelCoolingBlockEntity::new, ModBlocks.WASTE_DRUM))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<SocketBlockEntity> BATTERY_SOCKET =
            (Reg.<SocketBlockEntity>be(
                            "battery_socket",
                            SocketBlockEntity::new,
                            ModBlocks.MACHINE_BATTERY_SOCKET))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<BatteryBlockEntity> BATTERY =
            (Reg.<BatteryBlockEntity>be(
                            "battery", BatteryBlockEntity::new, ModBlocks.MACHINE_BATTERY))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FensuBlockEntity> FENSU =
            (Reg.<FensuBlockEntity>be("fensu", FensuBlockEntity::new, ModBlocks.MACHINE_FENSU))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ReddBlockEntity> BATTERY_REDD =
            (Reg.<ReddBlockEntity>be(
                            "battery_redd", ReddBlockEntity::new, ModBlocks.MACHINE_BATTERY_REDD))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CapacitorBlockEntity> CAPACITOR =
            (Reg.<CapacitorBlockEntity>be(
                            "capacitor", CapacitorBlockEntity::new, ModBlocks.CAPACITOR_COPPER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ChargerBlockEntity> NTM_CHARGER =
            (Reg.<ChargerBlockEntity>be("charger", ChargerBlockEntity::new, ModBlocks.CHARGER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RefuelerBlockEntity> REFUELER =
            (Reg.<RefuelerBlockEntity>be("refueler", RefuelerBlockEntity::new, ModBlocks.REFUELER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CrateBlockEntity> CRATE_IRON =
            crate(CrateType.IRON, ModBlocks.CRATE_IRON).andThen(CrateType.IRON::setTypeSupplier);
    public static final Reg.BeHandle<CrateBlockEntity> CRATE_STEEL =
            crate(CrateType.STEEL, ModBlocks.CRATE_STEEL).andThen(CrateType.STEEL::setTypeSupplier);
    public static final Reg.BeHandle<CrateBlockEntity> CRATE_DESH =
            crate(CrateType.DESH, ModBlocks.CRATE_DESH).andThen(CrateType.DESH::setTypeSupplier);
    public static final Reg.BeHandle<CrateTungstenBlockEntity> CRATE_HOT =
            (Reg.<CrateTungstenBlockEntity>be(
                            "crate_hot", CrateTungstenBlockEntity::new, ModBlocks.CRATE_TUNGSTEN))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT)
                    .andThen(CrateType.TUNGSTEN::setTypeSupplier);
    public static final Reg.BeHandle<CrateBlockEntity> SAFE =
            crate(CrateType.SAFE, ModBlocks.SAFE).andThen(CrateType.SAFE::setTypeSupplier);
    public static final Reg.BeHandle<MassStorageBlockEntity> MASS_STORAGE =
            (Reg.<MassStorageBlockEntity>be(
                            "mass_storage",
                            MassStorageBlockEntity::new,
                            ModBlocks.MASS_STORAGE_WOOD,
                            ModBlocks.MASS_STORAGE_IRON,
                            ModBlocks.MASS_STORAGE_DESH,
                            ModBlocks.MASS_STORAGE_TCALLOY))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CrateSupplyBlockEntity> SUPPLY_CRATE =
            (Reg.<CrateSupplyBlockEntity>be(
                    "supply_crate", CrateSupplyBlockEntity::new, ModBlocks.CRATE_SUPPLY));
    public static final Reg.BeHandle<FileCabinetBlockEntity> FILE_CABINET =
            (Reg.<FileCabinetBlockEntity>be(
                            "file_cabinet",
                            FileCabinetBlockEntity::new,
                            ModBlocks.FILING_CABINET_GREEN,
                            ModBlocks.FILING_CABINET_STEEL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKRodBlockEntity> RBMK_ROD =
            (Reg.<RBMKRodBlockEntity>be(
                            "rbmk_rod",
                            RBMKRodBlockEntity::new,
                            ModBlocks.RBMK_ROD,
                            ModBlocks.RBMK_ROD_MODERATED))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKRodReaSimBlockEntity> RBMK_ROD_REASIM =
            (Reg.<RBMKRodReaSimBlockEntity>be(
                            "rbmk_rod_reasim",
                            RBMKRodReaSimBlockEntity::new,
                            ModBlocks.RBMK_ROD_REASIM,
                            ModBlocks.RBMK_ROD_REASIM_MODERATED))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKManualControlBlockEntity> RBMK_CONTROL =
            (Reg.<RBMKManualControlBlockEntity>be(
                            "rbmk_control",
                            RBMKManualControlBlockEntity::new,
                            ModBlocks.RBMK_CONTROL,
                            ModBlocks.RBMK_CONTROL_MOD,
                            ModBlocks.RBMK_CONTROL_REASIM))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKControlAutoBlockEntity> RBMK_CONTROL_AUTO =
            (Reg.<RBMKControlAutoBlockEntity>be(
                            "rbmk_control_auto",
                            RBMKControlAutoBlockEntity::new,
                            ModBlocks.RBMK_CONTROL_AUTO,
                            ModBlocks.RBMK_CONTROL_REASIM_AUTO))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKBoilerBlockEntity> RBMK_BOILER =
            (Reg.<RBMKBoilerBlockEntity>be(
                            "rbmk_boiler", RBMKBoilerBlockEntity::new, ModBlocks.RBMK_BOILER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKPassiveBlockEntity> RBMK_MODERATOR =
            (Reg.<RBMKPassiveBlockEntity>be(
                            "rbmk_moderator",
                            (pos, state) ->
                                    new RBMKPassiveBlockEntity(
                                            RBMKPassiveBlockEntity.MODERATOR, pos, state),
                            ModBlocks.RBMK_MODERATOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKPassiveBlockEntity> RBMK_REFLECTOR =
            (Reg.<RBMKPassiveBlockEntity>be(
                            "rbmk_reflector",
                            (pos, state) ->
                                    new RBMKPassiveBlockEntity(
                                            RBMKPassiveBlockEntity.REFLECTOR, pos, state),
                            ModBlocks.RBMK_REFLECTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKPassiveBlockEntity> RBMK_ABSORBER =
            (Reg.<RBMKPassiveBlockEntity>be(
                            "rbmk_absorber",
                            (pos, state) ->
                                    new RBMKPassiveBlockEntity(
                                            RBMKPassiveBlockEntity.ABSORBER, pos, state),
                            ModBlocks.RBMK_ABSORBER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKBlankBlockEntity> RBMK_BLANK =
            (Reg.<RBMKBlankBlockEntity>be(
                            "rbmk_blank", RBMKBlankBlockEntity::new, ModBlocks.RBMK_BLANK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKCoolerBlockEntity> RBMK_COOLER =
            (Reg.<RBMKCoolerBlockEntity>be(
                            "rbmk_cooler", RBMKCoolerBlockEntity::new, ModBlocks.RBMK_COOLER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKStorageBlockEntity> RBMK_STORAGE =
            (Reg.<RBMKStorageBlockEntity>be(
                            "rbmk_storage", RBMKStorageBlockEntity::new, ModBlocks.RBMK_STORAGE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKHeaterBlockEntity> RBMK_HEATER =
            (Reg.<RBMKHeaterBlockEntity>be(
                            "rbmk_heater", RBMKHeaterBlockEntity::new, ModBlocks.RBMK_HEATER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKIrradiationColumnBlockEntity> RBMK_OUTGASSER =
            (Reg.<RBMKIrradiationColumnBlockEntity>be(
                            "rbmk_outgasser",
                            RBMKIrradiationColumnBlockEntity::new,
                            ModBlocks.RBMK_OUTGASSER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKInletBlockEntity> RBMK_INLET =
            (Reg.<RBMKInletBlockEntity>be(
                            "rbmk_inlet", RBMKInletBlockEntity::new, ModBlocks.RBMK_INLET))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKOutletBlockEntity> RBMK_OUTLET =
            (Reg.<RBMKOutletBlockEntity>be(
                            "rbmk_outlet", RBMKOutletBlockEntity::new, ModBlocks.RBMK_OUTLET))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKConsoleBlockEntity> RBMK_CONSOLE =
            (Reg.<RBMKConsoleBlockEntity>be(
                            "rbmk_console", RBMKConsoleBlockEntity::new, ModBlocks.RBMK_CONSOLE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKCraneConsoleBlockEntity> RBMK_CRANE_CONSOLE =
            (Reg.<RBMKCraneConsoleBlockEntity>be(
                            "rbmk_crane_console",
                            RBMKCraneConsoleBlockEntity::new,
                            ModBlocks.RBMK_CRANE_CONSOLE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKGaugeBlockEntity> RBMK_GAUGE =
            (Reg.<RBMKGaugeBlockEntity>be(
                            "rbmk_gauge", RBMKGaugeBlockEntity::new, ModBlocks.RBMK_GAUGE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKDisplayBlockEntity> RBMK_DISPLAY =
            (Reg.<RBMKDisplayBlockEntity>be(
                            "rbmk_display", RBMKDisplayBlockEntity::new, ModBlocks.RBMK_DISPLAY))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKNumitronBlockEntity> RBMK_NUMITRON =
            (Reg.<RBMKNumitronBlockEntity>be(
                            "rbmk_numitron", RBMKNumitronBlockEntity::new, ModBlocks.RBMK_NUMITRON))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKIndicatorBlockEntity> RBMK_INDICATOR =
            (Reg.<RBMKIndicatorBlockEntity>be(
                            "rbmk_indicator",
                            RBMKIndicatorBlockEntity::new,
                            ModBlocks.RBMK_INDICATOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKGraphBlockEntity> RBMK_GRAPH =
            (Reg.<RBMKGraphBlockEntity>be(
                            "rbmk_graph", RBMKGraphBlockEntity::new, ModBlocks.RBMK_GRAPH))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKLeverBlockEntity> RBMK_LEVER =
            (Reg.<RBMKLeverBlockEntity>be(
                            "rbmk_lever", RBMKLeverBlockEntity::new, ModBlocks.RBMK_LEVER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<RBMKKeyPadBlockEntity> RBMK_KEYPAD =
            (Reg.<RBMKKeyPadBlockEntity>be(
                            "rbmk_keypad", RBMKKeyPadBlockEntity::new, ModBlocks.RBMK_KEY_PAD))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKTerminalBlockEntity> RBMK_TERMINAL =
            (Reg.<RBMKTerminalBlockEntity>be(
                            "rbmk_terminal", RBMKTerminalBlockEntity::new, ModBlocks.RBMK_TERMINAL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<RBMKAutoloaderBlockEntity> RBMK_AUTOLOADER =
            (Reg.<RBMKAutoloaderBlockEntity>be(
                            "rbmk_autoloader",
                            RBMKAutoloaderBlockEntity::new,
                            ModBlocks.RBMK_AUTOLOADER))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PileFuelBlockEntity> PILE_FUEL =
            (Reg.<PileFuelBlockEntity>be(
                            "pile_fuel", PileFuelBlockEntity::new, ModBlocks.BLOCK_GRAPHITE_FUEL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PileBreedingFuelBlockEntity> PILE_BREEDINGFUEL =
            (Reg.<PileBreedingFuelBlockEntity>be(
                            "pile_breedingfuel",
                            PileBreedingFuelBlockEntity::new,
                            ModBlocks.BLOCK_GRAPHITE_LITHIUM))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PileSourceBlockEntity> PILE_SOURCE =
            (Reg.<PileSourceBlockEntity>be(
                            "pile_source",
                            PileSourceBlockEntity::new,
                            ModBlocks.BLOCK_GRAPHITE_SOURCE,
                            ModBlocks.BLOCK_GRAPHITE_PLUTONIUM))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PileNeutronDetectorBlockEntity> PILE_NEUTRONDETECTOR =
            (Reg.<PileNeutronDetectorBlockEntity>be(
                            "pile_neutrondetector",
                            PileNeutronDetectorBlockEntity::new,
                            ModBlocks.BLOCK_GRAPHITE_DETECTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PileCoreBlockEntity> PILE_CORE =
            (Reg.<PileCoreBlockEntity>be(
                            "pile_core", PileCoreBlockEntity::new, ModBlocks.PILE_BLOCK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PileLoaderBlockEntity> PILE_LOADER =
            (Reg.<PileLoaderBlockEntity>be(
                            "pile_loader", PileLoaderBlockEntity::new, ModBlocks.PILE_LOADER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PileVentBlockEntity> PILE_VENT =
            (Reg.<PileVentBlockEntity>be(
                            "pile_vent", PileVentBlockEntity::new, ModBlocks.PILE_VENT))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PileControlBlockEntity> PILE_CONTROL =
            (Reg.<PileControlBlockEntity>be(
                            "pile_control", PileControlBlockEntity::new, ModBlocks.PILE_CONTROL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FanBlockEntity> FAN =
            (Reg.<FanBlockEntity>be("fan", FanBlockEntity::new, ModBlocks.FAN))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PistonInserterBlockEntity> PISTON_INSERTER =
            (Reg.<PistonInserterBlockEntity>be(
                            "piston_inserter",
                            PistonInserterBlockEntity::new,
                            ModBlocks.PISTON_INSERTER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<PWRControllerBlockEntity> PWR_CONTROLLER =
            (Reg.<PWRControllerBlockEntity>be(
                            "pwr_controller",
                            PWRControllerBlockEntity::new,
                            ModBlocks.PWR_CONTROLLER))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ICFControllerBlockEntity> ICF_CONTROLLER =
            (Reg.<ICFControllerBlockEntity>be(
                            "icf_controller",
                            ICFControllerBlockEntity::new,
                            ModBlocks.ICF_CONTROLLER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ICFBlockEntity> ICF =
            (Reg.<ICFBlockEntity>be("icf", ICFBlockEntity::new, ModBlocks.MACHINE_ICF))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ICFAssemblyBlockEntity> ICF_STRUCT =
            (Reg.<ICFAssemblyBlockEntity>be(
                    "icf_struct", ICFAssemblyBlockEntity::new, ModBlocks.STRUCT_ICF_CORE));
    public static final Reg.BeHandle<FusionPelletPressBlockEntity> ICF_PRESS =
            (Reg.<FusionPelletPressBlockEntity>be(
                            "icf_press",
                            FusionPelletPressBlockEntity::new,
                            ModBlocks.MACHINE_ICF_PRESS))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BedrockOreBlockEntity> BEDROCK_ORE =
            (Reg.<BedrockOreBlockEntity>be(
                    "bedrock_ore", BedrockOreBlockEntity::new, ModBlocks.ORE_BEDROCK));
    public static final Reg.BeHandle<CustomNukeBlockEntity> NUKE_CUSTOM =
            (Reg.<CustomNukeBlockEntity>be(
                    "nuke_custom", CustomNukeBlockEntity::new, ModBlocks.NUKE_CUSTOM));
    public static final Reg.BeHandle<BalefireNukeBlockEntity> NUKE_FSTBMB =
            (Reg.<BalefireNukeBlockEntity>be(
                            "nuke_fstbmb", BalefireNukeBlockEntity::new, ModBlocks.NUKE_FSTBMB))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BombMultiBlockEntity> BOMB_MULTI =
            (Reg.<BombMultiBlockEntity>be(
                    "bombmulti", BombMultiBlockEntity::new, ModBlocks.BOMB_MULTI));
    public static final Reg.BeHandle<FireworksBlockEntity> FIREWORK_BOX =
            (Reg.<FireworksBlockEntity>be(
                            "firework_box", FireworksBlockEntity::new, ModBlocks.FIREWORKS))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FissureBlockEntity> FISSURE =
            (Reg.<FissureBlockEntity>be("fissure", FissureBlockEntity::new, ModBlocks.ORE_VOLCANO))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<GeyserBlockEntity> GEYSIR =
            (Reg.<GeyserBlockEntity>be(
                            "geysir",
                            GeyserBlockEntity::new,
                            ModBlocks.GEYSIR_NETHER,
                            ModBlocks.GEYSIR_CHLORINE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<VentBlockEntity> VENT =
            (Reg.<VentBlockEntity>be(
                            "vent",
                            VentBlockEntity::new,
                            ModBlocks.VENT_CHLORINE,
                            ModBlocks.VENT_CLOUD,
                            ModBlocks.VENT_PINK_CLOUD))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ChlorineSealBlockEntity> CHLORINE_SEAL =
            (Reg.<ChlorineSealBlockEntity>be(
                            "chlorine_seal",
                            ChlorineSealBlockEntity::new,
                            ModBlocks.VENT_CHLORINE_SEAL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<EmitterBlockEntity> EMITTER =
            (Reg.<EmitterBlockEntity>be("emitter", EmitterBlockEntity::new, ModBlocks.DECO_EMITTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PartEmitterBlockEntity> PART_EMITTER =
            (Reg.<PartEmitterBlockEntity>be(
                            "partemitter", PartEmitterBlockEntity::new, ModBlocks.PART_EMITTER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<BlastDoorBlockEntity> BLAST_DOOR =
            (Reg.<BlastDoorBlockEntity>be(
                            "blast_door", BlastDoorBlockEntity::new, ModBlocks.BLAST_DOOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BroadcasterBlockEntity> BROADCASTER =
            (Reg.<BroadcasterBlockEntity>be(
                            "pink_cloud_broadcaster",
                            BroadcasterBlockEntity::new,
                            ModBlocks.BROADCASTER_PC))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TrappedBrickBlockEntity> TRAPPED_BRICK =
            (Reg.<TrappedBrickBlockEntity>be(
                            "trapped_brick",
                            () ->
                                    new BlockEntityType<>(
                                            TrappedBrickBlockEntity::new,
                                            ModBlocks.JUNGLE_TRAPS.stream()
                                                    .map(RegistryHandle::get)
                                                    .filter(
                                                            b ->
                                                                    b.trap.type
                                                                            == TrappedBrickBlock
                                                                                    .TrapType
                                                                                    .DETECTOR)
                                                    .collect(Collectors.toSet()))))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CrashedBombBlockEntity> CRASHED_BALEFIRE =
            (Reg.<CrashedBombBlockEntity>be(
                            "crashed_balefire",
                            () ->
                                    new BlockEntityType<>(
                                            CrashedBombBlockEntity::new,
                                            ModBlocks.CRASHED_BOMBS.stream()
                                                    .map(RegistryHandle::get)
                                                    .collect(Collectors.toSet()))))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<LandmineBlockEntity> LANDMINE =
            (Reg.<LandmineBlockEntity>be(
                            "landmine",
                            LandmineBlockEntity::new,
                            ModBlocks.MINE_AP,
                            ModBlocks.MINE_SHRAP,
                            ModBlocks.MINE_HE,
                            ModBlocks.MINE_FAT,
                            ModBlocks.MINE_NAVAL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ChargeBlockEntity> EXPLOSIVE_CHARGE =
            (Reg.<ChargeBlockEntity>be(
                    "explosive_charge",
                    ChargeBlockEntity::new,
                    ModBlocks.CHARGE_C4,
                    ModBlocks.CHARGE_DYNAMITE,
                    ModBlocks.CHARGE_MINER,
                    ModBlocks.CHARGE_SEMTEX));
    public static final Reg.BeHandle<VolcanoCoreBlockEntity> VOLCANO_CORE =
            (Reg.<VolcanoCoreBlockEntity>be(
                            "volcano_core",
                            VolcanoCoreBlockEntity::new,
                            ModBlocks.VOLCANO_CORE,
                            ModBlocks.VOLCANO_RAD_CORE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<LogicBlockEntity> NTM_LOGIC_BLOCK =
            (Reg.<LogicBlockEntity>be(
                            "logic_block",
                            LogicBlockEntity::new,
                            ModBlocks.LOGIC_BLOCK,
                            ModBlocks.LOGIC_BLOCK_INVIS))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<IronFurnaceBlockEntity> FURNACE_IRON =
            (Reg.<IronFurnaceBlockEntity>be(
                            "furnace_iron", IronFurnaceBlockEntity::new, ModBlocks.FURNACE_IRON))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<SteelFurnaceBlockEntity> FURNACE_STEEL =
            (Reg.<SteelFurnaceBlockEntity>be(
                            "furnace_steel", SteelFurnaceBlockEntity::new, ModBlocks.FURNACE_STEEL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CombinationFurnaceBlockEntity> COMBINATION_OVEN =
            (Reg.<CombinationFurnaceBlockEntity>be(
                            "combination_oven",
                            CombinationFurnaceBlockEntity::new,
                            ModBlocks.FURNACE_COMBINATION))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<BrickFurnaceBlockEntity> FURNACE_BRICK =
            (Reg.<BrickFurnaceBlockEntity>be(
                            "furnace_brick",
                            BrickFurnaceBlockEntity::new,
                            ModBlocks.MACHINE_FURNACE_BRICK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<HeaterOvenBlockEntity> HEATING_OVEN =
            (Reg.<HeaterOvenBlockEntity>be(
                            "heating_oven", HeaterOvenBlockEntity::new, ModBlocks.HEATER_OVEN))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<AshpitBlockEntity> ASHPIT =
            (Reg.<AshpitBlockEntity>be("ashpit", AshpitBlockEntity::new, ModBlocks.MACHINE_ASHPIT))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<HeaterOilburnerBlockEntity> OILBURNER =
            (Reg.<HeaterOilburnerBlockEntity>be(
                            "oilburner",
                            HeaterOilburnerBlockEntity::new,
                            ModBlocks.HEATER_OILBURNER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CoolingHeatBlockEntity> HEATER_HEATEX =
            (Reg.<CoolingHeatBlockEntity>be(
                            "heater_heatex", CoolingHeatBlockEntity::new, ModBlocks.HEATER_HEATEX))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<CrystalProcessorBlockEntity> ACIDOMATIC =
            (Reg.<CrystalProcessorBlockEntity>be(
                            "acidomatic",
                            CrystalProcessorBlockEntity::new,
                            ModBlocks.MACHINE_CRYSTALLIZER))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CokerBlockEntity> COKER =
            (Reg.<CokerBlockEntity>be("coker", CokerBlockEntity::new, ModBlocks.MACHINE_COKER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<LiquefactorBlockEntity> LIQUEFACTOR =
            (Reg.<LiquefactorBlockEntity>be(
                            "liquefactor",
                            LiquefactorBlockEntity::new,
                            ModBlocks.MACHINE_LIQUEFACTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<SolidifierBlockEntity> SOLIDIFIER =
            (Reg.<SolidifierBlockEntity>be(
                            "solidifier", SolidifierBlockEntity::new, ModBlocks.MACHINE_SOLIDIFIER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PyroOvenBlockEntity> PYROOVEN =
            (Reg.<PyroOvenBlockEntity>be(
                            "pyrooven", PyroOvenBlockEntity::new, ModBlocks.MACHINE_PYROOVEN))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<VacuumRefineryBlockEntity> VACUUUM_DISTILL =
            (Reg.<VacuumRefineryBlockEntity>be(
                            "vacuuum_distill",
                            VacuumRefineryBlockEntity::new,
                            ModBlocks.MACHINE_VACUUM_DISTILL))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FrackingTowerBlockEntity> FRACKING_TOWER =
            (Reg.<FrackingTowerBlockEntity>be(
                            "fracking_tower",
                            FrackingTowerBlockEntity::new,
                            ModBlocks.MACHINE_FRACKING_TOWER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<ElectricPumpBlockEntity> ELECTRIC_PUMP =
            (Reg.<ElectricPumpBlockEntity>be(
                            "electric_pump", ElectricPumpBlockEntity::new, ModBlocks.PUMP_ELECTRIC))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<IntakeBlockEntity> INTAKE =
            (Reg.<IntakeBlockEntity>be("intake", IntakeBlockEntity::new, ModBlocks.MACHINE_INTAKE))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<DrainBlockEntity> FLUID_DRAIN =
            (Reg.<DrainBlockEntity>be(
                            "fluid_drain", DrainBlockEntity::new, ModBlocks.MACHINE_DRAIN))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<ItemCompressionBlockEntity> FUNNEL =
            (Reg.<ItemCompressionBlockEntity>be(
                            "funnel", ItemCompressionBlockEntity::new, ModBlocks.MACHINE_FUNNEL))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<SteamCondenserBlockEntity> CONDENSER =
            (Reg.<SteamCondenserBlockEntity>be(
                            "condenser",
                            SteamCondenserBlockEntity::new,
                            ModBlocks.MACHINE_CONDENSER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<PoweredCondenserBlockEntity> CONDENSER_POWERED =
            (Reg.<PoweredCondenserBlockEntity>be(
                            "condenser_powered",
                            PoweredCondenserBlockEntity::new,
                            ModBlocks.MACHINE_CONDENSER_POWERED))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<BigAssTankBlockEntity> BIGASSTANK =
            (Reg.<BigAssTankBlockEntity>be(
                            "bigasstank", BigAssTankBlockEntity::new, ModBlocks.MACHINE_BIGASSTANK))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<OrbusBlockEntity> ORBUS =
            (Reg.<OrbusBlockEntity>be("orbus", OrbusBlockEntity::new, ModBlocks.MACHINE_ORBUS))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<UF6TankBlockEntity> UF6_TANK =
            (Reg.<UF6TankBlockEntity>be(
                    "uf6_tank",
                    () ->
                            new BlockEntityType<>(
                                    (pos, state) ->
                                            new UF6TankBlockEntity(
                                                    ModBlockEntities.UF6_TANK.get(), pos, state),
                                    Set.of(ModBlocks.MACHINE_UF6_TANK.get()))));
    public static final Reg.BeHandle<UF6TankBlockEntity> PUF6_TANK =
            (Reg.<UF6TankBlockEntity>be(
                    "puf6_tank",
                    () ->
                            new BlockEntityType<>(
                                    (pos, state) ->
                                            new UF6TankBlockEntity(
                                                    ModBlockEntities.PUF6_TANK.get(), pos, state),
                                    Set.of(ModBlocks.MACHINE_PUF6_TANK.get()))));
    public static final Reg.BeHandle<MicrowaveBlockEntity> MICROWAVE =
            (Reg.<MicrowaveBlockEntity>be(
                            "microwave", MicrowaveBlockEntity::new, ModBlocks.MACHINE_MICROWAVE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<SatLinkerBlockEntity> SATLINKER =
            (Reg.<SatLinkerBlockEntity>be(
                    "satlinker", SatLinkerBlockEntity::new, ModBlocks.MACHINE_SATLINKER));
    public static final Reg.BeHandle<SatelliteLinkerBlockEntity> SATLINK =
            (Reg.<SatelliteLinkerBlockEntity>be(
                            "satlink", SatelliteLinkerBlockEntity::new, ModBlocks.MACHINE_SATLINK))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<KeyForgeBlockEntity> KEY_FORGE =
            (Reg.<KeyForgeBlockEntity>be(
                    "key_forge", KeyForgeBlockEntity::new, ModBlocks.MACHINE_KEYFORGE));
    public static final Reg.BeHandle<ThresherBlockEntity> THRESHER =
            (Reg.<ThresherBlockEntity>be(
                            "thresher", ThresherBlockEntity::new, ModBlocks.MACHINE_THRESHER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<AutosawBlockEntity> AUTOSAW =
            (Reg.<AutosawBlockEntity>be(
                            "autosaw", AutosawBlockEntity::new, ModBlocks.MACHINE_AUTOSAW))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<TimberSawBlockEntity> SAWMILL =
            (Reg.<TimberSawBlockEntity>be(
                            "sawmill", TimberSawBlockEntity::new, ModBlocks.MACHINE_SAWMILL))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<StirlingGeneratorBlockEntity> STIRLING =
            (Reg.<StirlingGeneratorBlockEntity>be(
                            "stirling",
                            StirlingGeneratorBlockEntity::new,
                            ModBlocks.MACHINE_STIRLING,
                            ModBlocks.MACHINE_STIRLING_STEEL,
                            ModBlocks.MACHINE_STIRLING_CREATIVE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<AmmoPressBlockEntity> AMMO_PRESS =
            (Reg.<AmmoPressBlockEntity>be(
                            "ammo_press", AmmoPressBlockEntity::new, ModBlocks.MACHINE_AMMO_PRESS))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FusionTorusBlockEntity> FUSION_TORUS =
            (Reg.<FusionTorusBlockEntity>be(
                            "fusion_torus", FusionTorusBlockEntity::new, ModBlocks.FUSION_TORUS))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<DfcChamberBlockEntity> CORE_CORE =
            (Reg.<DfcChamberBlockEntity>be("v0", DfcChamberBlockEntity::new, ModBlocks.DFC_CORE))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DfcEmitterBlockEntity> CORE_EMITTER =
            (Reg.<DfcEmitterBlockEntity>be(
                            "v0_emitter", DfcEmitterBlockEntity::new, ModBlocks.DFC_EMITTER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DfcReceiverBlockEntity> CORE_RECEIVER =
            (Reg.<DfcReceiverBlockEntity>be(
                            "v0_receiver", DfcReceiverBlockEntity::new, ModBlocks.DFC_RECEIVER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DfcInjectorBlockEntity> CORE_INJECTOR =
            (Reg.<DfcInjectorBlockEntity>be(
                            "v0_injector", DfcInjectorBlockEntity::new, ModBlocks.DFC_INJECTOR))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<DfcStabilizerBlockEntity> CORE_STABILIZER =
            (Reg.<DfcStabilizerBlockEntity>be(
                            "v0_stabilizer",
                            DfcStabilizerBlockEntity::new,
                            ModBlocks.DFC_STABILIZER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FusionKlystronBlockEntity> FUSION_KLYSTRON =
            (Reg.<FusionKlystronBlockEntity>be(
                            "fusion_klystron",
                            FusionKlystronBlockEntity::new,
                            ModBlocks.FUSION_KLYSTRON))
                    .ticksServerAt(BlockEntityTicks.SERVER_WITH_SOUND)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CreativeFusionKlystronBlockEntity> FUSION_KLYSTRON_CREATIVE =
            (Reg.<CreativeFusionKlystronBlockEntity>be(
                            "fusion_klystron_creative",
                            CreativeFusionKlystronBlockEntity::new,
                            ModBlocks.FUSION_KLYSTRON_CREATIVE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FusionNeutronBlockEntity> FUSION_BREEDER =
            (Reg.<FusionNeutronBlockEntity>be(
                            "fusion_breeder",
                            FusionNeutronBlockEntity::new,
                            ModBlocks.FUSION_BREEDER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FusionCollectorBlockEntity> FUSION_COLLECTOR =
            (Reg.<FusionCollectorBlockEntity>be(
                    "fusion_collector",
                    FusionCollectorBlockEntity::new,
                    ModBlocks.FUSION_COLLECTOR));
    public static final Reg.BeHandle<FusionBoilerBlockEntity> FUSION_BOILER =
            (Reg.<FusionBoilerBlockEntity>be(
                            "fusion_boiler", FusionBoilerBlockEntity::new, ModBlocks.FUSION_BOILER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<FusionMHDTBlockEntity> FUSION_MHDT =
            (Reg.<FusionMHDTBlockEntity>be(
                            "fusion_mhdt", FusionMHDTBlockEntity::new, ModBlocks.FUSION_MHDT))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FusionCouplerBlockEntity> FUSION_COUPLER =
            (Reg.<FusionCouplerBlockEntity>be(
                    "fusion_coupler", FusionCouplerBlockEntity::new, ModBlocks.FUSION_COUPLER));
    public static final Reg.BeHandle<FusionPlasmaForgeBlockEntity> FUSION_PLASMA_FORGE =
            (Reg.<FusionPlasmaForgeBlockEntity>be(
                            "fusion_plasma_forge",
                            FusionPlasmaForgeBlockEntity::new,
                            ModBlocks.FUSION_PLASMA_FORGE))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<FusionTorusAssemblyBlockEntity> FUSION_TORUS_STRUCT =
            (Reg.<FusionTorusAssemblyBlockEntity>be(
                    "fusion_torus_struct",
                    FusionTorusAssemblyBlockEntity::new,
                    ModBlocks.STRUCT_TORUS_CORE));
    public static final Reg.BeHandle<WatzBlockEntity> WATZ =
            (Reg.<WatzBlockEntity>be("watz", WatzBlockEntity::new, ModBlocks.WATZ))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<WatzAssemblyBlockEntity> WATZ_STRUCT =
            (Reg.<WatzAssemblyBlockEntity>be(
                    "watz_struct", WatzAssemblyBlockEntity::new, ModBlocks.STRUCT_WATZ_CORE));
    public static final Reg.BeHandle<StorageDrumBlockEntity> WASTE_STORAGE_DRUM =
            (Reg.<StorageDrumBlockEntity>be(
                            "waste_storage_drum",
                            StorageDrumBlockEntity::new,
                            ModBlocks.STORAGE_DRUM))
                    .ticksServerAt(BlockEntityTicks.SERVER);

    static {
        Reg.afterRegistration(
                WASTE_STORAGE_DRUM,
                _ -> StorageDrumBlockEntity.resolveInputs());
    }

    public static final Reg.BeHandle<SirenBlockEntity> SIREN =
            (Reg.<SirenBlockEntity>be("siren", SirenBlockEntity::new, ModBlocks.MACHINE_SIREN))
                    .ticksServerAt(SoundEmitter.TICKER);
    public static final Reg.BeHandle<ShredderBlockEntity> MACHINE_SHREDDER =
            (Reg.<ShredderBlockEntity>be(
                            "machine_shredder",
                            ShredderBlockEntity::new,
                            ModBlocks.MACHINE_SHREDDER))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<TeleporterBlockEntity> TELEBLOCK =
            (Reg.<TeleporterBlockEntity>be(
                            "teleblock", TeleporterBlockEntity::new, ModBlocks.MACHINE_TELEPORTER))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<CargoElevatorBlockEntity> CARGO_ELEVATOR =
            (Reg.<CargoElevatorBlockEntity>be(
                            "cargo_elevator",
                            CargoElevatorBlockEntity::create,
                            ModBlocks.CARGO_ELEVATOR))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<DeconBlockEntity> DECON =
            (Reg.<DeconBlockEntity>be("decon", DeconBlockEntity::new, ModBlocks.DECON))
                    .ticksServerAt(BlockEntityTicks.SERVER)
                    .ticksClientAt(BlockEntityTicks.CLIENT);
    public static final Reg.BeHandle<GlyphidSpawnerBlockEntity> GLYPHID_SPAWNER =
            (Reg.<GlyphidSpawnerBlockEntity>be(
                            "glyphid_spawner",
                            GlyphidSpawnerBlockEntity::new,
                            ModBlocks.GLYPHID_SPAWNER,
                            ModBlocks.GLYPHID_SPAWNER_INFESTED,
                            ModBlocks.GLYPHID_SPAWNER_RAD))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<StatueElbFBlockEntity> STATUE_ELB_F =
            (Reg.<StatueElbFBlockEntity>be(
                            "deco_f", StatueElbFBlockEntity::new, ModBlocks.STATUE_ELB_F))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<BehemothLanternBlockEntity> LANTERN_BEHEMOTH =
            (Reg.<BehemothLanternBlockEntity>be(
                            "lantern_behemoth",
                            BehemothLanternBlockEntity::new,
                            ModBlocks.LANTERN_BEHEMOTH))
                    .ticksServerAt(BlockEntityTicks.SERVER);
    public static final Reg.BeHandle<LanternBlockEntity> LANTERN =
            (Reg.<LanternBlockEntity>be(
                            "lantern_ordinary", LanternBlockEntity::new, ModBlocks.LANTERN))
                    .ticksServerAt(BlockEntityTicks.SERVER);

    private ModBlockEntities() {}

    private static Reg.BeHandle<CrateBlockEntity> crate(
            CrateType type, RegistryHandle<? extends Block> block) {
        return (Reg.<CrateBlockEntity>be(
                type.id, (pos, state) -> new CrateBlockEntity(type, pos, state), block));
    }
}
