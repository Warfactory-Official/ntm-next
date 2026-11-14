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

package com.hbm.bulk;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blockentity.SkeletonHolderBlockEntity;
import com.hbm.blockentity.SmeltingFurnaceBlockEntity;
import com.hbm.blockentity.bomb.BalefireNukeBlockEntity;
import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.blockentity.bomb.CustomNukeBlockEntity;
import com.hbm.blockentity.bomb.LaunchPadBlockEntity;
import com.hbm.blockentity.bomb.NukeAssemblyBlockEntity;
import com.hbm.blockentity.bomb.NukeBoyBlockEntity;
import com.hbm.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm.blockentity.bomb.NukeGadgetBlockEntity;
import com.hbm.blockentity.bomb.NukeManBlockEntity;
import com.hbm.blockentity.bomb.NukeMikeBlockEntity;
import com.hbm.blockentity.bomb.NukeN2BlockEntity;
import com.hbm.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm.blockentity.bomb.NukeTsarBlockEntity;
import com.hbm.blockentity.door.BlastDoorBlockEntity;
import com.hbm.blockentity.door.DoorBlockEntity;
import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.blockentity.generator.RadGenBlockEntity;
import com.hbm.blockentity.machine.AnnihilatorBlockEntity;
import com.hbm.blockentity.machine.AshpitBlockEntity;
import com.hbm.blockentity.machine.AssemblyFactoryBlockEntity;
import com.hbm.blockentity.machine.BlastFurnaceBlockEntity;
import com.hbm.blockentity.machine.CatalyticReformerBlockEntity;
import com.hbm.blockentity.machine.CombustionEngineBlockEntity;
import com.hbm.blockentity.machine.CompactPneumaticCompressor;
import com.hbm.blockentity.machine.ContinuousCaster;
import com.hbm.blockentity.machine.CrucibleBlockEntity;
import com.hbm.blockentity.machine.CustomMachineBlockEntity;
import com.hbm.blockentity.machine.CyclotronBlockEntity;
import com.hbm.blockentity.machine.DeuteriumExtractorBlockEntity;
import com.hbm.blockentity.machine.DeuteriumTowerBlockEntity;
import com.hbm.blockentity.machine.DieselGeneratorBlockEntity;
import com.hbm.blockentity.machine.ElectricFurnaceBlockEntity;
import com.hbm.blockentity.machine.ExcavatorBlockEntity;
import com.hbm.blockentity.machine.ExposureChamberBlockEntity;
import com.hbm.blockentity.machine.FireboxBaseBlockEntity;
import com.hbm.blockentity.machine.FoundryBaseBlockEntity;
import com.hbm.blockentity.machine.FoundryBasinBlockEntity;
import com.hbm.blockentity.machine.FoundryCastingBaseBlockEntity;
import com.hbm.blockentity.machine.FoundryChannelBlockEntity;
import com.hbm.blockentity.machine.FoundryDischarge;
import com.hbm.blockentity.machine.FoundryMoldBlockEntity;
import com.hbm.blockentity.machine.FoundryTankBlockEntity;
import com.hbm.blockentity.machine.FusionPelletPressBlockEntity;
import com.hbm.blockentity.machine.GasCentBlockEntity;
import com.hbm.blockentity.machine.GeothermalExchanger;
import com.hbm.blockentity.machine.HeatBoilerBlockEntity;
import com.hbm.blockentity.machine.HeatBoilerIndustrialBlockEntity;
import com.hbm.blockentity.machine.HeaterFireboxBlockEntity;
import com.hbm.blockentity.machine.HeaterOilburnerBlockEntity;
import com.hbm.blockentity.machine.HeaterOvenBlockEntity;
import com.hbm.blockentity.machine.HydrotreaterBlockEntity;
import com.hbm.blockentity.machine.ICFBlockEntity;
import com.hbm.blockentity.machine.ICFControllerBlockEntity;
import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.ItemCompressionBlockEntity;
import com.hbm.blockentity.machine.KeyForgeBlockEntity;
import com.hbm.blockentity.machine.LPW2BlockEntity;
import com.hbm.blockentity.machine.LargeTurbineBlockEntity;
import com.hbm.blockentity.machine.LaunchpadSoyuzBlockEntity;
import com.hbm.blockentity.machine.MicrowaveBlockEntity;
import com.hbm.blockentity.machine.MiningLaserBlockEntity;
import com.hbm.blockentity.machine.MissileWorkbenchBlockEntity;
import com.hbm.blockentity.machine.OreSlopperBlockEntity;
import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.blockentity.machine.PistonInserterBlockEntity;
import com.hbm.blockentity.machine.PneumaticCompressorBlockEntity;
import com.hbm.blockentity.machine.PrecisionAssemblerBlockEntity;
import com.hbm.blockentity.machine.PurexBlockEntity;
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.blockentity.machine.RadarLargeBlockEntity;
import com.hbm.blockentity.machine.RadiolysisBlockEntity;
import com.hbm.blockentity.machine.ReactorBreedingBlockEntity;
import com.hbm.blockentity.machine.ReactorZirnoxBlockEntity;
import com.hbm.blockentity.machine.RefuelerBlockEntity;
import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.blockentity.machine.RockMillBlockEntity;
import com.hbm.blockentity.machine.RotaryFurnaceBlockEntity;
import com.hbm.blockentity.machine.SatDockBlockEntity;
import com.hbm.blockentity.machine.SatLinkerBlockEntity;
import com.hbm.blockentity.machine.SatelliteLinkerBlockEntity;
import com.hbm.blockentity.machine.SilexBlockEntity;
import com.hbm.blockentity.machine.SirenBlockEntity;
import com.hbm.blockentity.machine.SlagDischarge;
import com.hbm.blockentity.machine.SolarBoilerBlockEntity;
import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
import com.hbm.blockentity.machine.SoyuzLauncherBlockEntity;
import com.hbm.blockentity.machine.SpentFuelCoolingBlockEntity;
import com.hbm.blockentity.machine.SteamEngineBlockEntity;
import com.hbm.blockentity.machine.StorageDrumBlockEntity;
import com.hbm.blockentity.machine.SuperComputerBlockEntity;
import com.hbm.blockentity.machine.TapeDriveBlockEntity;
import com.hbm.blockentity.machine.TemplateCrafterBlockEntity;
import com.hbm.blockentity.machine.TeslaCoilBlockEntity;
import com.hbm.blockentity.machine.TowerLargeBlockEntity;
import com.hbm.blockentity.machine.TowerSmallBlockEntity;
import com.hbm.blockentity.machine.TurbineBaseBlockEntity;
import com.hbm.blockentity.machine.TurbineGasBlockEntity;
import com.hbm.blockentity.machine.TurbofanBlockEntity;
import com.hbm.blockentity.machine.WatzBlockEntity;
import com.hbm.blockentity.machine.WavelengthLaserBlockEntity;
import com.hbm.blockentity.machine.WoodGeneratorBlockEntity;
import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.blockentity.machine.albion.PADetectorBlockEntity;
import com.hbm.blockentity.machine.albion.PADipoleBlockEntity;
import com.hbm.blockentity.machine.albion.PAQuadrupoleBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity;
import com.hbm.blockentity.machine.albion.RFCavityBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcChamberBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcEmitterBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcInjectorBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcReceiverBlockEntity;
import com.hbm.blockentity.machine.dfc.DfcStabilizerBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.blockentity.machine.oil.CokerBlockEntity;
import com.hbm.blockentity.machine.oil.CrackingTowerBlockEntity;
import com.hbm.blockentity.machine.oil.DerrickBlockEntity;
import com.hbm.blockentity.machine.oil.FrackingTowerBlockEntity;
import com.hbm.blockentity.machine.oil.FractionTowerBlockEntity;
import com.hbm.blockentity.machine.oil.GasFlareBlockEntity;
import com.hbm.blockentity.machine.oil.LiquefactorBlockEntity;
import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.blockentity.machine.oil.PhaseConverterBlockEntity;
import com.hbm.blockentity.machine.oil.PumpjackBlockEntity;
import com.hbm.blockentity.machine.oil.PyroOvenBlockEntity;
import com.hbm.blockentity.machine.oil.RefineryBlockEntity;
import com.hbm.blockentity.machine.oil.SolidifierBlockEntity;
import com.hbm.blockentity.machine.oil.VacuumRefineryBlockEntity;
import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blockentity.machine.pile.PileDeviceBaseBlockEntity;
import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blockentity.machine.pile.PileVentBlockEntity;
import com.hbm.blockentity.machine.radio.EnervationBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBlankBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKCoolerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKInletBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKOutletBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKPassiveBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodReaSimBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKStorageBlockEntity;
import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.blockentity.machine.storage.BigAssTankBlockEntity;
import com.hbm.blockentity.machine.storage.CargoCapsuleBlockEntity;
import com.hbm.blockentity.machine.storage.CrateBlockEntity;
import com.hbm.blockentity.machine.storage.CrateSupplyBlockEntity;
import com.hbm.blockentity.machine.storage.CrateTungstenBlockEntity;
import com.hbm.blockentity.machine.storage.EnergyStoreBlockEntity;
import com.hbm.blockentity.machine.storage.FensuBlockEntity;
import com.hbm.blockentity.machine.storage.FileCabinetBlockEntity;
import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.blockentity.machine.storage.FluidTankBlockEntity;
import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.blockentity.machine.storage.OrbusBlockEntity;
import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import com.hbm.blockentity.machine.storage.SocketBlockEntity;
import com.hbm.blockentity.machine.thermal.BrickFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CombinationFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.CoolingHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.ElectricHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.FluidBoilerBlockEntity;
import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.PoweredCondenserBlockEntity;
import com.hbm.blockentity.machine.thermal.SteamCondenserBlockEntity;
import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.StirlingGeneratorBlockEntity;
import com.hbm.blockentity.machine.thermal.TimberSawBlockEntity;
import com.hbm.blockentity.network.CraneBaseBlockEntity;
import com.hbm.blockentity.network.CraneBoxerBlockEntity;
import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import com.hbm.blockentity.network.CraneInserterBlockEntity;
import com.hbm.blockentity.network.CranePartitionerBlockEntity;
import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.blockentity.network.CraneSplitterBlockEntity;
import com.hbm.blockentity.network.CraneUnboxerBlockEntity;
import com.hbm.blockentity.network.DroneCrateBlockEntity;
import com.hbm.blockentity.network.DroneDockBlockEntity;
import com.hbm.blockentity.network.DroneProviderBlockEntity;
import com.hbm.blockentity.network.DroneRequesterBlockEntity;
import com.hbm.blockentity.network.DroneWaypointRequestBlockEntity;
import com.hbm.blockentity.network.RequestNetworkBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticImporterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticMachineBaseBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticStorageBankBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticStorageClutterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageAccessBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoTubePaintableBlockEntity;
import com.hbm.blockentity.ordnance.launch.CompactLauncherBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchPlatformBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchSiloBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.blockentity.ordnance.launch.MissileAssemblyLauncherBlockEntity;
import com.hbm.blockentity.ordnance.launch.MissileErectorBlockEntity;
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
import com.hbm.blockentity.processing.FabricationBlockEntity;
import com.hbm.blockentity.processing.FluidMixerBlockEntity;
import com.hbm.blockentity.processing.ShredderBlockEntity;
import com.hbm.blockentity.processing.SolderingBenchBlockEntity;
import com.hbm.blockentity.processing.StampingMachineBlockEntity;
import com.hbm.blockentity.prop.PedestalBlockEntity;
import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blockentity.turret.TurretArtyBlockEntity;
import com.hbm.blockentity.turret.TurretBaseArtilleryBlockEntity;
import com.hbm.blockentity.turret.TurretBaseNTBlockEntity;
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
import com.hbm.blocks.PhysicalPortBlock;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.bomb.APLandmineBlock;
import com.hbm.blocks.bomb.BombMultiBlock;
import com.hbm.blocks.bomb.CompactLauncherBlock;
import com.hbm.blocks.bomb.CrashedBombBlock;
import com.hbm.blocks.bomb.FatLandmineBlock;
import com.hbm.blocks.bomb.FireworksBlock;
import com.hbm.blocks.bomb.HELandmineBlock;
import com.hbm.blocks.bomb.LandmineBlock;
import com.hbm.blocks.bomb.LargeLaunchPadBlock;
import com.hbm.blocks.bomb.LaunchPadBlock;
import com.hbm.blocks.bomb.LaunchTableBlock;
import com.hbm.blocks.bomb.NavalLandmineBlock;
import com.hbm.blocks.bomb.NukeBalefireBlock;
import com.hbm.blocks.bomb.RustedLaunchPadBlock;
import com.hbm.blocks.bomb.ShrapnelLandmineBlock;
import com.hbm.blocks.bomb.VolcanoBlock;
import com.hbm.blocks.generic.ChlorineSealBlock;
import com.hbm.blocks.generic.DungeonSpawnerBlock;
import com.hbm.blocks.generic.EmitterBlock;
import com.hbm.blocks.generic.FileCabinetBlock;
import com.hbm.blocks.generic.FissureBlock;
import com.hbm.blocks.generic.GenericDoorBlock;
import com.hbm.blocks.generic.GeysirBlock;
import com.hbm.blocks.generic.GlyphidSpawnerBlock;
import com.hbm.blocks.generic.InvisibleLogicBlock;
import com.hbm.blocks.generic.LanternBehemothBlock;
import com.hbm.blocks.generic.LanternBlock;
import com.hbm.blocks.generic.LogicBlock;
import com.hbm.blocks.generic.PartEmitterBlock;
import com.hbm.blocks.generic.PedestalBlock;
import com.hbm.blocks.generic.PlushieBlock;
import com.hbm.blocks.generic.RebarBlock;
import com.hbm.blocks.generic.StatueElbFBlock;
import com.hbm.blocks.generic.TrappedBrickBlock.Detector;
import com.hbm.blocks.generic.VentBlock;
import com.hbm.blocks.machine.AmmoPressBlock;
import com.hbm.blocks.machine.AnnihilatorBlock;
import com.hbm.blocks.machine.ArcFurnaceBlock;
import com.hbm.blocks.machine.ArcWelderBlock;
import com.hbm.blocks.machine.AshpitBlock;
import com.hbm.blocks.machine.AssemblyFactoryBlock;
import com.hbm.blocks.machine.AssemblyMachineBlock;
import com.hbm.blocks.machine.AutocrafterBlock;
import com.hbm.blocks.machine.AutosawBlock;
import com.hbm.blocks.machine.BatterySocketBlock;
import com.hbm.blocks.machine.BlastDoorBlock;
import com.hbm.blocks.machine.BlastFurnaceBlock;
import com.hbm.blocks.machine.CMPortBlock;
import com.hbm.blocks.machine.CargoElevatorBlock;
import com.hbm.blocks.machine.CatalyticReformerBlock;
import com.hbm.blocks.machine.CentrifugeBlock;
import com.hbm.blocks.machine.ChargerBlock;
import com.hbm.blocks.machine.ChemicalFactoryBlock;
import com.hbm.blocks.machine.ChemicalPlantBlock;
import com.hbm.blocks.machine.ChimneyBaseBlock;
import com.hbm.blocks.machine.ChimneyBrickBlock;
import com.hbm.blocks.machine.ChimneyIndustrialBlock;
import com.hbm.blocks.machine.ChungusBlock;
import com.hbm.blocks.machine.CokerBlock;
import com.hbm.blocks.machine.CombustionEngineBlock;
import com.hbm.blocks.machine.CompactCompressorBlock;
import com.hbm.blocks.machine.CompressorBlock;
import com.hbm.blocks.machine.CondenserBlock;
import com.hbm.blocks.machine.ConveyorPressBlock;
import com.hbm.blocks.machine.CoreComponentBlock;
import com.hbm.blocks.machine.CoreCoreBlock;
import com.hbm.blocks.machine.CrackingTowerBlock;
import com.hbm.blocks.machine.CrucibleBlock;
import com.hbm.blocks.machine.CrystallizerBlock;
import com.hbm.blocks.machine.CustomMachineBlock;
import com.hbm.blocks.machine.CybercrabBlock;
import com.hbm.blocks.machine.CyclotronBlock;
import com.hbm.blocks.machine.DeconBlock;
import com.hbm.blocks.machine.DemonLampBlock;
import com.hbm.blocks.machine.DestroyedZirnoxBlock;
import com.hbm.blocks.machine.DeuteriumExtractorBlock;
import com.hbm.blocks.machine.DeuteriumTowerBlock;
import com.hbm.blocks.machine.DieselBlock;
import com.hbm.blocks.machine.DrainBlock;
import com.hbm.blocks.machine.EPressBlock;
import com.hbm.blocks.machine.ElectricFurnaceBlock;
import com.hbm.blocks.machine.ElectrolyserBlock;
import com.hbm.blocks.machine.ExcavatorBlock;
import com.hbm.blocks.machine.ExposureChamberBlock;
import com.hbm.blocks.machine.FELBlock;
import com.hbm.blocks.machine.FanBlock;
import com.hbm.blocks.machine.FloodlightBlock;
import com.hbm.blocks.machine.ForceFieldBlock;
import com.hbm.blocks.machine.FoundryBasinBlock;
import com.hbm.blocks.machine.FoundryCastingBlock;
import com.hbm.blocks.machine.FoundryChannelBlock;
import com.hbm.blocks.machine.FoundryMoldBlock;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.blocks.machine.FoundrySlagtapBlock;
import com.hbm.blocks.machine.FoundryTankBlock;
import com.hbm.blocks.machine.FrackingTowerBlock;
import com.hbm.blocks.machine.FractionSpacerBlock;
import com.hbm.blocks.machine.FractionTowerBlock;
import com.hbm.blocks.machine.FunnelBlock;
import com.hbm.blocks.machine.FurnaceBrickBlock;
import com.hbm.blocks.machine.FurnaceCombinationBlock;
import com.hbm.blocks.machine.FurnaceIronBlock;
import com.hbm.blocks.machine.FurnaceSteelBlock;
import com.hbm.blocks.machine.GasCentBlock;
import com.hbm.blocks.machine.GasFlareBlock;
import com.hbm.blocks.machine.GasTurbineBlock;
import com.hbm.blocks.machine.GeigerCounterBlock;
import com.hbm.blocks.machine.HeatBoilerBlock;
import com.hbm.blocks.machine.HeaterElectricBlock;
import com.hbm.blocks.machine.HeaterFireboxBlock;
import com.hbm.blocks.machine.HeaterHeatexBlock;
import com.hbm.blocks.machine.HeaterOilburnerBlock;
import com.hbm.blocks.machine.HeaterOvenBlock;
import com.hbm.blocks.machine.HephaestusBlock;
import com.hbm.blocks.machine.HorizontalMachineBlock;
import com.hbm.blocks.machine.HydrotreaterBlock;
import com.hbm.blocks.machine.ICFBlock;
import com.hbm.blocks.machine.ICFControllerBlock;
import com.hbm.blocks.machine.ICFMachineBlock;
import com.hbm.blocks.machine.ICFPressBlock;
import com.hbm.blocks.machine.ICFStructBlock;
import com.hbm.blocks.machine.IndustrialHeatBoilerBlock;
import com.hbm.blocks.machine.IndustrialTurbineBlock;
import com.hbm.blocks.machine.IntakeBlock;
import com.hbm.blocks.machine.KeyForgeBlock;
import com.hbm.blocks.machine.LPW2Block;
import com.hbm.blocks.machine.LargeTowerBlock;
import com.hbm.blocks.machine.LargeTurbineBlock;
import com.hbm.blocks.machine.LaunchpadSoyuzAnchorCellBlock;
import com.hbm.blocks.machine.LaunchpadSoyuzBlock;
import com.hbm.blocks.machine.LiquefactorBlock;
import com.hbm.blocks.machine.MachineBlock;
import com.hbm.blocks.machine.MicrowaveBlock;
import com.hbm.blocks.machine.MiningLaserBlock;
import com.hbm.blocks.machine.MissileAssemblyBlock;
import com.hbm.blocks.machine.MixerBlock;
import com.hbm.blocks.machine.OilWellBlock;
import com.hbm.blocks.machine.OreSlopperBlock;
import com.hbm.blocks.machine.PUREXBlock;
import com.hbm.blocks.machine.PWRBlock;
import com.hbm.blocks.machine.PWRControllerBlock;
import com.hbm.blocks.machine.PinkCloudBroadcasterBlock;
import com.hbm.blocks.machine.PistonInserterBlock;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.blocks.machine.PoweredCondenserBlock;
import com.hbm.blocks.machine.PrecAssBlock;
import com.hbm.blocks.machine.PressBlock;
import com.hbm.blocks.machine.PumpBlock;
import com.hbm.blocks.machine.PumpjackBlock;
import com.hbm.blocks.machine.PyroOvenBlock;
import com.hbm.blocks.machine.RTGBlock;
import com.hbm.blocks.machine.RadGenBlock;
import com.hbm.blocks.machine.RadarBlock;
import com.hbm.blocks.machine.RadarLargeBlock;
import com.hbm.blocks.machine.RadarMastCellBlock;
import com.hbm.blocks.machine.RadarScreenBlock;
import com.hbm.blocks.machine.RadioRecBlock;
import com.hbm.blocks.machine.RadioboxBlock;
import com.hbm.blocks.machine.RadiolysisBlock;
import com.hbm.blocks.machine.ReactorBreedingBlock;
import com.hbm.blocks.machine.ReactorControlBlock;
import com.hbm.blocks.machine.ReactorResearchBlock;
import com.hbm.blocks.machine.ReactorZirnoxBlock;
import com.hbm.blocks.machine.RefineryBlock;
import com.hbm.blocks.machine.RefuelerBlock;
import com.hbm.blocks.machine.RockMillBlock;
import com.hbm.blocks.machine.RotaryFurnaceBlock;
import com.hbm.blocks.machine.SILEXBlock;
import com.hbm.blocks.machine.SatDockBlock;
import com.hbm.blocks.machine.SatLinkBlock;
import com.hbm.blocks.machine.SatLinkerBlock;
import com.hbm.blocks.machine.SawmillBlock;
import com.hbm.blocks.machine.SealBlock;
import com.hbm.blocks.machine.SealHatchBlock;
import com.hbm.blocks.machine.ShredderBlock;
import com.hbm.blocks.machine.SirenBlock;
import com.hbm.blocks.machine.SmallTowerBlock;
import com.hbm.blocks.machine.SolarBoilerBlock;
import com.hbm.blocks.machine.SolarMirrorBlock;
import com.hbm.blocks.machine.SolderingStationBlock;
import com.hbm.blocks.machine.SolidifierBlock;
import com.hbm.blocks.machine.SoyuzCapsuleBlock;
import com.hbm.blocks.machine.SoyuzLauncherBlock;
import com.hbm.blocks.machine.SoyuzStructBlock;
import com.hbm.blocks.machine.SteamEngineBlock;
import com.hbm.blocks.machine.StirlingBlock;
import com.hbm.blocks.machine.StorageDrumBlock;
import com.hbm.blocks.machine.StrandCasterBlock;
import com.hbm.blocks.machine.StructBlock;
import com.hbm.blocks.machine.SuperComputerBlock;
import com.hbm.blocks.machine.TapeDriveBlock;
import com.hbm.blocks.machine.TeleporterBlock;
import com.hbm.blocks.machine.TeslaBlock;
import com.hbm.blocks.machine.ThresherBlock;
import com.hbm.blocks.machine.TurbineBlock;
import com.hbm.blocks.machine.TurbofanBlock;
import com.hbm.blocks.machine.VacuumDistillationBlock;
import com.hbm.blocks.machine.VendingMachineBlock;
import com.hbm.blocks.machine.WasteDrumBlock;
import com.hbm.blocks.machine.WatzBlock;
import com.hbm.blocks.machine.WatzPumpBlock;
import com.hbm.blocks.machine.WatzStructBlock;
import com.hbm.blocks.machine.WoodBurnerBlock;
import com.hbm.blocks.machine.albion.PABeamlineBlock;
import com.hbm.blocks.machine.albion.PACooledBlock;
import com.hbm.blocks.machine.albion.PADetectorBlock;
import com.hbm.blocks.machine.albion.PADipoleBlock;
import com.hbm.blocks.machine.albion.PAQuadrupoleBlock;
import com.hbm.blocks.machine.albion.PARFCBlock;
import com.hbm.blocks.machine.albion.PASourceBlock;
import com.hbm.blocks.machine.fusion.CreativeFusionKlystronBlock;
import com.hbm.blocks.machine.fusion.FusionBoilerBlock;
import com.hbm.blocks.machine.fusion.FusionBreederBlock;
import com.hbm.blocks.machine.fusion.FusionCollectorBlock;
import com.hbm.blocks.machine.fusion.FusionCouplerBlock;
import com.hbm.blocks.machine.fusion.FusionKlystronBlock;
import com.hbm.blocks.machine.fusion.FusionMHDTBlock;
import com.hbm.blocks.machine.fusion.FusionMachineBlock;
import com.hbm.blocks.machine.fusion.FusionPlasmaForgeBlock;
import com.hbm.blocks.machine.fusion.FusionTorusBlock;
import com.hbm.blocks.machine.fusion.FusionTorusStructBlock;
import com.hbm.blocks.machine.pile.DrilledGraphiteEntityBlock;
import com.hbm.blocks.machine.pile.GraphiteBreedingFuelBlock;
import com.hbm.blocks.machine.pile.GraphiteFuelBlock;
import com.hbm.blocks.machine.pile.GraphiteNeutronDetectorBlock;
import com.hbm.blocks.machine.pile.GraphiteSourceBlock;
import com.hbm.blocks.machine.pile.PileBlock;
import com.hbm.blocks.machine.pile.PileDeviceBlock;
import com.hbm.blocks.machine.pile.PileVentBlock;
import com.hbm.blocks.machine.rbmk.ConfigurableMiniPanelBlock;
import com.hbm.blocks.machine.rbmk.RBMKAbsorberBlock;
import com.hbm.blocks.machine.rbmk.RBMKAutoloaderBlock;
import com.hbm.blocks.machine.rbmk.RBMKBlankBlock;
import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.machine.rbmk.RBMKBoilerBlock;
import com.hbm.blocks.machine.rbmk.RBMKConsoleBlock;
import com.hbm.blocks.machine.rbmk.RBMKControlAutoBlock;
import com.hbm.blocks.machine.rbmk.RBMKControlBlock;
import com.hbm.blocks.machine.rbmk.RBMKCoolerBlock;
import com.hbm.blocks.machine.rbmk.RBMKCraneConsoleBlock;
import com.hbm.blocks.machine.rbmk.RBMKDisplayBlock;
import com.hbm.blocks.machine.rbmk.RBMKGaugeBlock;
import com.hbm.blocks.machine.rbmk.RBMKGraphBlock;
import com.hbm.blocks.machine.rbmk.RBMKHeaterBlock;
import com.hbm.blocks.machine.rbmk.RBMKIndicatorBlock;
import com.hbm.blocks.machine.rbmk.RBMKInletBlock;
import com.hbm.blocks.machine.rbmk.RBMKKeyPadBlock;
import com.hbm.blocks.machine.rbmk.RBMKLeverBlock;
import com.hbm.blocks.machine.rbmk.RBMKLoaderBlock;
import com.hbm.blocks.machine.rbmk.RBMKModeratorBlock;
import com.hbm.blocks.machine.rbmk.RBMKNumitronBlock;
import com.hbm.blocks.machine.rbmk.RBMKOutgasserBlock;
import com.hbm.blocks.machine.rbmk.RBMKOutletBlock;
import com.hbm.blocks.machine.rbmk.RBMKReflectorBlock;
import com.hbm.blocks.machine.rbmk.RBMKRodBlock;
import com.hbm.blocks.machine.rbmk.RBMKRodReaSimBlock;
import com.hbm.blocks.machine.rbmk.RBMKStorageBlock;
import com.hbm.blocks.machine.rbmk.RBMKTerminalBlock;
import com.hbm.blocks.machine.storage.BatteryBlock;
import com.hbm.blocks.machine.storage.BigAssTankBlock;
import com.hbm.blocks.machine.storage.CapacitorBlock;
import com.hbm.blocks.machine.storage.CrateBlock;
import com.hbm.blocks.machine.storage.FENSUBlock;
import com.hbm.blocks.machine.storage.FluidBarrelBlock.Functional;
import com.hbm.blocks.machine.storage.FluidTankBlock;
import com.hbm.blocks.machine.storage.MassStorageBlock;
import com.hbm.blocks.machine.storage.OrbusBlock;
import com.hbm.blocks.machine.storage.SafeBlock;
import com.hbm.blocks.machine.storage.TungstenCrateBlock;
import com.hbm.blocks.multiblock.AssembledMembers.Member;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockGeometryCellBlock;
import com.hbm.blocks.network.BasePylonBlock;
import com.hbm.blocks.network.CableBlock;
import com.hbm.blocks.network.CableBoxBlock;
import com.hbm.blocks.network.CableConductorBlock;
import com.hbm.blocks.network.CableDetectorBlock;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.blocks.network.CableGaugeBlock;
import com.hbm.blocks.network.CablePaintableBlock;
import com.hbm.blocks.network.CableSwitchBlock;
import com.hbm.blocks.network.CoatedWireBlock;
import com.hbm.blocks.network.ConnectorBlock;
import com.hbm.blocks.network.CraneBlock;
import com.hbm.blocks.network.CraneBoxerBlock;
import com.hbm.blocks.network.CraneExtractorBlock;
import com.hbm.blocks.network.CraneGrabberBlock;
import com.hbm.blocks.network.CraneInserterBlock;
import com.hbm.blocks.network.CranePartitionerBlock;
import com.hbm.blocks.network.CraneRouterBlock;
import com.hbm.blocks.network.CraneSplitterBlock;
import com.hbm.blocks.network.CraneUnboxerBlock;
import com.hbm.blocks.network.DroneCrateBlock;
import com.hbm.blocks.network.DroneDockBlock;
import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.blocks.network.FluidCounterValveBlock;
import com.hbm.blocks.network.FluidDuctBlock;
import com.hbm.blocks.network.FluidDuctBoxBlock;
import com.hbm.blocks.network.FluidDuctBoxExhaustBlock;
import com.hbm.blocks.network.FluidDuctGaugeBlock;
import com.hbm.blocks.network.FluidDuctPaintableBlock;
import com.hbm.blocks.network.FluidDuctPaintableExhaustBlock;
import com.hbm.blocks.network.FluidPipeAnchorBlock;
import com.hbm.blocks.network.FluidPipeBlock;
import com.hbm.blocks.network.FluidPumpBlock;
import com.hbm.blocks.network.FluidSwitchBlock;
import com.hbm.blocks.network.FluidValveBlock;
import com.hbm.blocks.network.PylonBlock;
import com.hbm.blocks.network.PylonLargeBlock;
import com.hbm.blocks.network.PylonMediumBlock;
import com.hbm.blocks.network.REDDBatteryBlock;
import com.hbm.blocks.network.RadioAUTOCALBlock;
import com.hbm.blocks.network.RadioTelexBlock;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.blocks.network.SubstationBlock;
import com.hbm.blocks.network.pneumatic.PneumaticStorageBlock;
import com.hbm.blocks.network.pneumatic.PneumoStorageAccessBlock;
import com.hbm.blocks.network.pneumatic.PneumoStorageClutterBlock;
import com.hbm.blocks.network.pneumatic.PneumoStorageExporterBlock;
import com.hbm.blocks.network.pneumatic.PneumoStorageImporterBlock;
import com.hbm.blocks.network.pneumatic.PneumoStorageMonoBlock;
import com.hbm.blocks.network.pneumatic.PneumoTubeBlock;
import com.hbm.blocks.network.pneumatic.PneumoTubePaintableBlock;
import com.hbm.blocks.rail.NtmRailBlock;
import com.hbm.blocks.rail.RailNarrowCurveBlock;
import com.hbm.blocks.rail.RailStandardCurveWide7Block;
import com.hbm.blocks.rail.RailStandardCurveWide9Block;
import com.hbm.blocks.rail.RailSwitchBlock;
import com.hbm.blocks.rail.StandardCurveRailBlock;
import com.hbm.blocks.rail.StraightRailBlock;
import com.hbm.blocks.turret.ArtilleryTurretBlock;
import com.hbm.blocks.turret.TurretArtyBlock;
import com.hbm.blocks.turret.TurretBlock;
import com.hbm.blocks.turret.TurretChekhovBlock;
import com.hbm.blocks.turret.TurretFriendlyBlock;
import com.hbm.blocks.turret.TurretFritzBlock;
import com.hbm.blocks.turret.TurretHIMARSBlock;
import com.hbm.blocks.turret.TurretHowardBlock;
import com.hbm.blocks.turret.TurretHowardDamagedBlock;
import com.hbm.blocks.turret.TurretJeremyBlock;
import com.hbm.blocks.turret.TurretMaxwellBlock;
import com.hbm.blocks.turret.TurretRichardBlock;
import com.hbm.blocks.turret.TurretSentryBlock;
import com.hbm.blocks.turret.TurretSentryDamagedBlock;
import com.hbm.blocks.turret.TurretTauonBlock;
import com.hbm.capability.CapabilityBlock;
import com.hbm.capability.PhysicalConnector;
import com.hbm.world.bulk.BulkTransform;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BulkBindings implements com.hbm.world.bulk.BulkBindings {
    public Class<?>[] types() {
        return new Class<?>[] {
            MachineBlockEntity.class,
            PollutingMachineBlockEntity.class,
            SkeletonHolderBlockEntity.class,
            SmeltingFurnaceBlockEntity.class,
            BalefireNukeBlockEntity.class,
            BombMultiBlockEntity.class,
            CustomNukeBlockEntity.class,
            LaunchPadBlockEntity.class,
            NukeAssemblyBlockEntity.class,
            NukeBoyBlockEntity.class,
            NukeFleijaBlockEntity.class,
            NukeGadgetBlockEntity.class,
            NukeManBlockEntity.class,
            NukeMikeBlockEntity.class,
            NukeN2BlockEntity.class,
            NukePrototypeBlockEntity.class,
            NukeSoliniumBlockEntity.class,
            NukeTsarBlockEntity.class,
            BlastDoorBlockEntity.class,
            DoorBlockEntity.class,
            IsotopeGeneratorBlockEntity.class,
            RadGenBlockEntity.class,
            AnnihilatorBlockEntity.class,
            AshpitBlockEntity.class,
            AssemblyFactoryBlockEntity.class,
            BlastFurnaceBlockEntity.class,
            CatalyticReformerBlockEntity.class,
            CombustionEngineBlockEntity.class,
            CompactPneumaticCompressor.class,
            ContinuousCaster.class,
            CrucibleBlockEntity.class,
            CustomMachineBlockEntity.class,
            CyclotronBlockEntity.class,
            DeuteriumExtractorBlockEntity.class,
            DeuteriumTowerBlockEntity.class,
            DieselGeneratorBlockEntity.class,
            ElectricFurnaceBlockEntity.class,
            ExcavatorBlockEntity.class,
            ExposureChamberBlockEntity.class,
            FireboxBaseBlockEntity.class,
            FoundryBaseBlockEntity.class,
            FoundryBasinBlockEntity.class,
            FoundryCastingBaseBlockEntity.class,
            FoundryChannelBlockEntity.class,
            FoundryDischarge.class,
            FoundryMoldBlockEntity.class,
            FoundryTankBlockEntity.class,
            FusionPelletPressBlockEntity.class,
            GasCentBlockEntity.class,
            GeothermalExchanger.class,
            HeatBoilerBlockEntity.class,
            HeatBoilerIndustrialBlockEntity.class,
            HeaterFireboxBlockEntity.class,
            HeaterOilburnerBlockEntity.class,
            HeaterOvenBlockEntity.class,
            HydrotreaterBlockEntity.class,
            ICFBlockEntity.class,
            ICFControllerBlockEntity.class,
            IndustrialTurbineBlockEntity.class,
            ItemCompressionBlockEntity.class,
            KeyForgeBlockEntity.class,
            LPW2BlockEntity.class,
            LargeTurbineBlockEntity.class,
            LaunchpadSoyuzBlockEntity.class,
            MicrowaveBlockEntity.class,
            MiningLaserBlockEntity.class,
            MissileWorkbenchBlockEntity.class,
            OreSlopperBlockEntity.class,
            PWRControllerBlockEntity.class,
            PistonInserterBlockEntity.class,
            PneumaticCompressorBlockEntity.class,
            PrecisionAssemblerBlockEntity.class,
            PurexBlockEntity.class,
            RadarBlockEntity.class,
            RadarLargeBlockEntity.class,
            RadiolysisBlockEntity.class,
            ReactorBreedingBlockEntity.class,
            ReactorZirnoxBlockEntity.class,
            RefuelerBlockEntity.class,
            RepulsionFieldBlockEntity.class,
            RockMillBlockEntity.class,
            RotaryFurnaceBlockEntity.class,
            SatDockBlockEntity.class,
            SatLinkerBlockEntity.class,
            SatelliteLinkerBlockEntity.class,
            SilexBlockEntity.class,
            SirenBlockEntity.class,
            SlagDischarge.class,
            SolarBoilerBlockEntity.class,
            SolarMirrorBlockEntity.class,
            SoyuzLauncherBlockEntity.class,
            SpentFuelCoolingBlockEntity.class,
            SteamEngineBlockEntity.class,
            StorageDrumBlockEntity.class,
            SuperComputerBlockEntity.class,
            TapeDriveBlockEntity.class,
            TemplateCrafterBlockEntity.class,
            TeslaCoilBlockEntity.class,
            TowerLargeBlockEntity.class,
            TowerSmallBlockEntity.class,
            TurbineBaseBlockEntity.class,
            TurbineGasBlockEntity.class,
            TurbofanBlockEntity.class,
            WatzBlockEntity.class,
            WavelengthLaserBlockEntity.class,
            WoodGeneratorBlockEntity.class,
            CooledAcceleratorBlockEntity.class,
            PADetectorBlockEntity.class,
            PADipoleBlockEntity.class,
            PAQuadrupoleBlockEntity.class,
            PASourceBlockEntity.class,
            RFCavityBlockEntity.class,
            DfcChamberBlockEntity.class,
            DfcEmitterBlockEntity.class,
            DfcInjectorBlockEntity.class,
            DfcReceiverBlockEntity.class,
            DfcStabilizerBlockEntity.class,
            FusionKlystronBlockEntity.class,
            FusionNeutronBlockEntity.class,
            FusionPlasmaForgeBlockEntity.class,
            FusionTorusBlockEntity.class,
            CokerBlockEntity.class,
            CrackingTowerBlockEntity.class,
            DerrickBlockEntity.class,
            FrackingTowerBlockEntity.class,
            FractionTowerBlockEntity.class,
            GasFlareBlockEntity.class,
            LiquefactorBlockEntity.class,
            OilDrillBaseBlockEntity.class,
            PhaseConverterBlockEntity.class,
            PumpjackBlockEntity.class,
            PyroOvenBlockEntity.class,
            RefineryBlockEntity.class,
            SolidifierBlockEntity.class,
            VacuumRefineryBlockEntity.class,
            PileControlBlockEntity.class,
            PileCoreBlockEntity.class,
            PileDeviceBaseBlockEntity.class,
            PileLoaderBlockEntity.class,
            PileVentBlockEntity.class,
            EnervationBlockEntity.class,
            RBMKAutoloaderBlockEntity.class,
            RBMKBaseBlockEntity.class,
            RBMKBlankBlockEntity.class,
            RBMKBoilerBlockEntity.class,
            RBMKConsoleBlockEntity.class,
            RBMKControlAutoBlockEntity.class,
            RBMKControlBlockEntity.class,
            RBMKCoolerBlockEntity.class,
            RBMKCraneConsoleBlockEntity.class,
            RBMKHeaterBlockEntity.class,
            RBMKInletBlockEntity.class,
            RBMKIrradiationColumnBlockEntity.class,
            RBMKManualControlBlockEntity.class,
            RBMKOutletBlockEntity.class,
            RBMKPassiveBlockEntity.class,
            RBMKRodBlockEntity.class,
            RBMKRodReaSimBlockEntity.class,
            RBMKStorageBlockEntity.class,
            ReactorControlBlockEntity.class,
            ResearchReactorBlockEntity.class,
            BatteryBlockEntity.class,
            BigAssTankBlockEntity.class,
            CargoCapsuleBlockEntity.class,
            CrateBlockEntity.class,
            CrateSupplyBlockEntity.class,
            CrateTungstenBlockEntity.class,
            EnergyStoreBlockEntity.class,
            FensuBlockEntity.class,
            FileCabinetBlockEntity.class,
            FluidBarrelBlockEntity.class,
            FluidTankBlockEntity.class,
            MassStorageBlockEntity.class,
            OrbusBlockEntity.class,
            ReddBlockEntity.class,
            SocketBlockEntity.class,
            BrickFurnaceBlockEntity.class,
            CombinationFurnaceBlockEntity.class,
            CompactTurbineBlockEntity.class,
            CoolingHeatBlockEntity.class,
            ElectricHeatBlockEntity.class,
            FluidBoilerBlockEntity.class,
            IronFurnaceBlockEntity.class,
            LeviathanTurbineBlockEntity.class,
            PoweredCondenserBlockEntity.class,
            SteamCondenserBlockEntity.class,
            SteelFurnaceBlockEntity.class,
            StirlingGeneratorBlockEntity.class,
            TimberSawBlockEntity.class,
            CraneBaseBlockEntity.class,
            CraneBoxerBlockEntity.class,
            CraneExtractorBlockEntity.class,
            CraneGrabberBlockEntity.class,
            CraneInserterBlockEntity.class,
            CranePartitionerBlockEntity.class,
            CraneRouterBlockEntity.class,
            CraneSplitterBlockEntity.class,
            CraneUnboxerBlockEntity.class,
            DroneCrateBlockEntity.class,
            DroneDockBlockEntity.class,
            DroneProviderBlockEntity.class,
            DroneRequesterBlockEntity.class,
            DroneWaypointRequestBlockEntity.class,
            RequestNetworkBlockEntity.class,
            PneumaticImporterBlockEntity.class,
            PneumaticMachineBaseBlockEntity.class,
            PneumaticStorageBankBlockEntity.class,
            PneumaticStorageClutterBlockEntity.class,
            PneumaticTubeBlockEntity.class,
            PneumoStorageAccessBlockEntity.class,
            PneumoStorageExporterBlockEntity.class,
            PneumoStorageMonoBlockEntity.class,
            PneumoTubePaintableBlockEntity.class,
            CompactLauncherBlockEntity.class,
            LaunchPlatformBlockEntity.class,
            LaunchSiloBlockEntity.class,
            LaunchTableBlockEntity.class,
            MissileAssemblyLauncherBlockEntity.class,
            MissileErectorBlockEntity.class,
            AmmoPressBlockEntity.class,
            ArcFurnaceBlockEntity.class,
            ArcWelderBlockEntity.class,
            AssemblyMachineBlockEntity.class,
            BurnerPressBlockEntity.class,
            CentrifugeBlockEntity.class,
            ChemicalFactoryBlockEntity.class,
            ChemicalPlantBlockEntity.class,
            ConveyorStampBlockEntity.class,
            CrystalProcessorBlockEntity.class,
            ElectricPressBlockEntity.class,
            ElectrolysisBlockEntity.class,
            FabricationBlockEntity.class,
            FluidMixerBlockEntity.class,
            ShredderBlockEntity.class,
            SolderingBenchBlockEntity.class,
            StampingMachineBlockEntity.class,
            PedestalBlockEntity.class,
            LockableBlockEntity.class,
            TurretArtyBlockEntity.class,
            TurretBaseArtilleryBlockEntity.class,
            TurretBaseNTBlockEntity.class,
            TurretChekhovBlockEntity.class,
            TurretFriendlyBlockEntity.class,
            TurretFritzBlockEntity.class,
            TurretHIMARSBlockEntity.class,
            TurretHowardBlockEntity.class,
            TurretHowardDamagedBlockEntity.class,
            TurretJeremyBlockEntity.class,
            TurretMaxwellBlockEntity.class,
            TurretRichardBlockEntity.class,
            TurretSentryBlockEntity.class,
            TurretSentryDamagedBlockEntity.class,
            TurretTauonBlockEntity.class,
            PhysicalPortBlock.class,
            TickingEntityBlock.class,
            APLandmineBlock.class,
            BombMultiBlock.class,
            CompactLauncherBlock.class,
            CrashedBombBlock.class,
            FatLandmineBlock.class,
            FireworksBlock.class,
            HELandmineBlock.class,
            LandmineBlock.class,
            LargeLaunchPadBlock.class,
            LaunchPadBlock.class,
            LaunchTableBlock.class,
            NavalLandmineBlock.class,
            NukeBalefireBlock.class,
            RustedLaunchPadBlock.class,
            ShrapnelLandmineBlock.class,
            VolcanoBlock.class,
            ChlorineSealBlock.class,
            DungeonSpawnerBlock.class,
            EmitterBlock.class,
            FileCabinetBlock.class,
            FissureBlock.class,
            GenericDoorBlock.class,
            GeysirBlock.class,
            GlyphidSpawnerBlock.class,
            InvisibleLogicBlock.class,
            LanternBehemothBlock.class,
            LanternBlock.class,
            LogicBlock.class,
            PartEmitterBlock.class,
            PedestalBlock.class,
            PlushieBlock.class,
            RebarBlock.class,
            StatueElbFBlock.class,
            Detector.class,
            VentBlock.class,
            AmmoPressBlock.class,
            AnnihilatorBlock.class,
            ArcFurnaceBlock.class,
            ArcWelderBlock.class,
            AshpitBlock.class,
            AssemblyFactoryBlock.class,
            AssemblyMachineBlock.class,
            AutocrafterBlock.class,
            AutosawBlock.class,
            BatterySocketBlock.class,
            BlastDoorBlock.class,
            BlastFurnaceBlock.class,
            CMPortBlock.class,
            CargoElevatorBlock.class,
            CatalyticReformerBlock.class,
            CentrifugeBlock.class,
            ChargerBlock.class,
            ChemicalFactoryBlock.class,
            ChemicalPlantBlock.class,
            ChimneyBaseBlock.class,
            ChimneyBrickBlock.class,
            ChimneyIndustrialBlock.class,
            ChungusBlock.class,
            CokerBlock.class,
            CombustionEngineBlock.class,
            CompactCompressorBlock.class,
            CompressorBlock.class,
            CondenserBlock.class,
            ConveyorPressBlock.class,
            CoreComponentBlock.class,
            CoreCoreBlock.class,
            CrackingTowerBlock.class,
            CrucibleBlock.class,
            CrystallizerBlock.class,
            CustomMachineBlock.class,
            CybercrabBlock.class,
            CyclotronBlock.class,
            DeconBlock.class,
            DemonLampBlock.class,
            DestroyedZirnoxBlock.class,
            DeuteriumExtractorBlock.class,
            DeuteriumTowerBlock.class,
            DieselBlock.class,
            DrainBlock.class,
            EPressBlock.class,
            ElectricFurnaceBlock.class,
            ElectrolyserBlock.class,
            ExcavatorBlock.class,
            ExposureChamberBlock.class,
            FELBlock.class,
            FanBlock.class,
            FloodlightBlock.class,
            ForceFieldBlock.class,
            FoundryBasinBlock.class,
            FoundryCastingBlock.class,
            FoundryChannelBlock.class,
            FoundryMoldBlock.class,
            FoundryOutletBlock.class,
            FoundrySlagtapBlock.class,
            FoundryTankBlock.class,
            FrackingTowerBlock.class,
            FractionSpacerBlock.class,
            FractionTowerBlock.class,
            FunnelBlock.class,
            FurnaceBrickBlock.class,
            FurnaceCombinationBlock.class,
            FurnaceIronBlock.class,
            FurnaceSteelBlock.class,
            GasCentBlock.class,
            GasFlareBlock.class,
            GasTurbineBlock.class,
            GeigerCounterBlock.class,
            HeatBoilerBlock.class,
            HeaterElectricBlock.class,
            HeaterFireboxBlock.class,
            HeaterHeatexBlock.class,
            HeaterOilburnerBlock.class,
            HeaterOvenBlock.class,
            HephaestusBlock.class,
            HorizontalMachineBlock.class,
            HydrotreaterBlock.class,
            ICFBlock.class,
            ICFControllerBlock.class,
            ICFMachineBlock.class,
            ICFPressBlock.class,
            ICFStructBlock.class,
            IndustrialHeatBoilerBlock.class,
            IndustrialTurbineBlock.class,
            IntakeBlock.class,
            KeyForgeBlock.class,
            LPW2Block.class,
            LargeTowerBlock.class,
            LargeTurbineBlock.class,
            LaunchpadSoyuzAnchorCellBlock.class,
            LaunchpadSoyuzBlock.class,
            LiquefactorBlock.class,
            MachineBlock.class,
            MicrowaveBlock.class,
            MiningLaserBlock.class,
            MissileAssemblyBlock.class,
            MixerBlock.class,
            OilWellBlock.class,
            OreSlopperBlock.class,
            PUREXBlock.class,
            PWRBlock.class,
            PWRControllerBlock.class,
            PinkCloudBroadcasterBlock.class,
            PistonInserterBlock.class,
            PowerDetectorBlock.class,
            PoweredCondenserBlock.class,
            PrecAssBlock.class,
            PressBlock.class,
            PumpBlock.class,
            PumpjackBlock.class,
            PyroOvenBlock.class,
            RTGBlock.class,
            RadGenBlock.class,
            RadarBlock.class,
            RadarLargeBlock.class,
            RadarMastCellBlock.class,
            RadarScreenBlock.class,
            RadioRecBlock.class,
            RadioboxBlock.class,
            RadiolysisBlock.class,
            ReactorBreedingBlock.class,
            ReactorControlBlock.class,
            ReactorResearchBlock.class,
            ReactorZirnoxBlock.class,
            RefineryBlock.class,
            RefuelerBlock.class,
            RockMillBlock.class,
            RotaryFurnaceBlock.class,
            SILEXBlock.class,
            SatDockBlock.class,
            SatLinkBlock.class,
            SatLinkerBlock.class,
            SawmillBlock.class,
            SealBlock.class,
            SealHatchBlock.class,
            ShredderBlock.class,
            SirenBlock.class,
            SmallTowerBlock.class,
            SolarBoilerBlock.class,
            SolarMirrorBlock.class,
            SolderingStationBlock.class,
            SolidifierBlock.class,
            SoyuzCapsuleBlock.class,
            SoyuzLauncherBlock.class,
            SoyuzStructBlock.class,
            SteamEngineBlock.class,
            StirlingBlock.class,
            StorageDrumBlock.class,
            StrandCasterBlock.class,
            StructBlock.class,
            SuperComputerBlock.class,
            TapeDriveBlock.class,
            TeleporterBlock.class,
            TeslaBlock.class,
            ThresherBlock.class,
            TurbineBlock.class,
            TurbofanBlock.class,
            VacuumDistillationBlock.class,
            VendingMachineBlock.class,
            WasteDrumBlock.class,
            WatzBlock.class,
            WatzPumpBlock.class,
            WatzStructBlock.class,
            WoodBurnerBlock.class,
            PABeamlineBlock.class,
            PACooledBlock.class,
            PADetectorBlock.class,
            PADipoleBlock.class,
            PAQuadrupoleBlock.class,
            PARFCBlock.class,
            PASourceBlock.class,
            CreativeFusionKlystronBlock.class,
            FusionBoilerBlock.class,
            FusionBreederBlock.class,
            FusionCollectorBlock.class,
            FusionCouplerBlock.class,
            FusionKlystronBlock.class,
            FusionMHDTBlock.class,
            FusionMachineBlock.class,
            FusionPlasmaForgeBlock.class,
            FusionTorusBlock.class,
            FusionTorusStructBlock.class,
            DrilledGraphiteEntityBlock.class,
            GraphiteBreedingFuelBlock.class,
            GraphiteFuelBlock.class,
            GraphiteNeutronDetectorBlock.class,
            GraphiteSourceBlock.class,
            PileBlock.class,
            PileDeviceBlock.class,
            PileVentBlock.class,
            ConfigurableMiniPanelBlock.class,
            RBMKAbsorberBlock.class,
            RBMKAutoloaderBlock.class,
            RBMKBlankBlock.class,
            RBMKBlock.class,
            RBMKBoilerBlock.class,
            RBMKConsoleBlock.class,
            RBMKControlAutoBlock.class,
            RBMKControlBlock.class,
            RBMKCoolerBlock.class,
            RBMKCraneConsoleBlock.class,
            RBMKDisplayBlock.class,
            RBMKGaugeBlock.class,
            RBMKGraphBlock.class,
            RBMKHeaterBlock.class,
            RBMKIndicatorBlock.class,
            RBMKInletBlock.class,
            RBMKKeyPadBlock.class,
            RBMKLeverBlock.class,
            RBMKLoaderBlock.class,
            RBMKModeratorBlock.class,
            RBMKNumitronBlock.class,
            RBMKOutgasserBlock.class,
            RBMKOutletBlock.class,
            RBMKReflectorBlock.class,
            RBMKRodBlock.class,
            RBMKRodReaSimBlock.class,
            RBMKStorageBlock.class,
            RBMKTerminalBlock.class,
            BatteryBlock.class,
            BigAssTankBlock.class,
            CapacitorBlock.class,
            CrateBlock.class,
            FENSUBlock.class,
            Functional.class,
            FluidTankBlock.class,
            MassStorageBlock.class,
            OrbusBlock.class,
            SafeBlock.class,
            TungstenCrateBlock.class,
            Member.class,
            MultiblockCellBlock.class,
            MultiblockCoreBlock.class,
            MultiblockGeometryCellBlock.class,
            BasePylonBlock.class,
            CableBlock.class,
            CableBoxBlock.class,
            CableConductorBlock.class,
            CableDetectorBlock.class,
            CableDiodeBlock.class,
            CableGaugeBlock.class,
            CablePaintableBlock.class,
            CableSwitchBlock.class,
            CoatedWireBlock.class,
            ConnectorBlock.class,
            CraneBlock.class,
            CraneBoxerBlock.class,
            CraneExtractorBlock.class,
            CraneGrabberBlock.class,
            CraneInserterBlock.class,
            CranePartitionerBlock.class,
            CraneRouterBlock.class,
            CraneSplitterBlock.class,
            CraneUnboxerBlock.class,
            DroneCrateBlock.class,
            DroneDockBlock.class,
            DroneWaypointBlock.class,
            FluidCounterValveBlock.class,
            FluidDuctBlock.class,
            FluidDuctBoxBlock.class,
            FluidDuctBoxExhaustBlock.class,
            FluidDuctGaugeBlock.class,
            FluidDuctPaintableBlock.class,
            FluidDuctPaintableExhaustBlock.class,
            FluidPipeAnchorBlock.class,
            FluidPipeBlock.class,
            FluidPumpBlock.class,
            FluidSwitchBlock.class,
            FluidValveBlock.class,
            PylonBlock.class,
            PylonLargeBlock.class,
            PylonMediumBlock.class,
            REDDBatteryBlock.class,
            RadioAUTOCALBlock.class,
            RadioTelexBlock.class,
            RadioTorchBlock.class,
            SubstationBlock.class,
            PneumaticStorageBlock.class,
            PneumoStorageAccessBlock.class,
            PneumoStorageClutterBlock.class,
            PneumoStorageExporterBlock.class,
            PneumoStorageImporterBlock.class,
            PneumoStorageMonoBlock.class,
            PneumoTubeBlock.class,
            PneumoTubePaintableBlock.class,
            NtmRailBlock.class,
            RailNarrowCurveBlock.class,
            RailStandardCurveWide7Block.class,
            RailStandardCurveWide9Block.class,
            RailSwitchBlock.class,
            StandardCurveRailBlock.class,
            StraightRailBlock.class,
            ArtilleryTurretBlock.class,
            TurretArtyBlock.class,
            TurretBlock.class,
            TurretChekhovBlock.class,
            TurretFriendlyBlock.class,
            TurretFritzBlock.class,
            TurretHIMARSBlock.class,
            TurretHowardBlock.class,
            TurretHowardDamagedBlock.class,
            TurretJeremyBlock.class,
            TurretMaxwellBlock.class,
            TurretRichardBlock.class,
            TurretSentryBlock.class,
            TurretSentryDamagedBlock.class,
            TurretTauonBlock.class,
            CapabilityBlock.class,
            PhysicalConnector.class,
        };
    }

    public boolean relevant(int kind, BlockState state) {
        switch (kind) {
            case 253,
                    528,
                    529,
                    530,
                    531,
                    532,
                    535,
                    536,
                    537,
                    538,
                    552,
                    553,
                    554,
                    556,
                    557,
                    559,
                    561,
                    562,
                    602 -> {
                boolean result = false;
                result |= ((PhysicalConnector) state.getBlock()).bulkPhysicalRelevant(state);
                return result;
            }
            case 254,
                    255,
                    256,
                    258,
                    259,
                    260,
                    261,
                    262,
                    266,
                    267,
                    269,
                    270,
                    271,
                    272,
                    273,
                    274,
                    277,
                    278,
                    279,
                    282,
                    283,
                    284,
                    285,
                    287,
                    288,
                    289,
                    325,
                    327,
                    328,
                    340,
                    344,
                    345,
                    347,
                    348,
                    349,
                    361,
                    369,
                    375,
                    379,
                    386,
                    396,
                    411,
                    415,
                    433,
                    438,
                    443,
                    445,
                    453,
                    456,
                    475,
                    476,
                    477,
                    478,
                    479,
                    480,
                    484,
                    495,
                    496,
                    497,
                    499,
                    501,
                    502,
                    505,
                    512,
                    520,
                    539,
                    545,
                    550,
                    569,
                    571,
                    572 -> {
                boolean result = false;
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                return result;
            }
            case 257,
                    263,
                    264,
                    265,
                    290,
                    291,
                    292,
                    293,
                    294,
                    295,
                    296,
                    299,
                    301,
                    304,
                    305,
                    307,
                    308,
                    309,
                    310,
                    311,
                    312,
                    313,
                    314,
                    315,
                    316,
                    318,
                    321,
                    322,
                    323,
                    326,
                    331,
                    333,
                    334,
                    336,
                    337,
                    338,
                    339,
                    350,
                    352,
                    355,
                    356,
                    357,
                    358,
                    359,
                    360,
                    362,
                    363,
                    364,
                    365,
                    366,
                    367,
                    368,
                    370,
                    373,
                    376,
                    377,
                    378,
                    381,
                    382,
                    384,
                    385,
                    388,
                    390,
                    391,
                    392,
                    393,
                    399,
                    400,
                    402,
                    403,
                    404,
                    406,
                    408,
                    413,
                    414,
                    416,
                    417,
                    418,
                    420,
                    421,
                    422,
                    423,
                    426,
                    431,
                    432,
                    434,
                    435,
                    437,
                    439,
                    440,
                    442,
                    444,
                    450,
                    451,
                    454,
                    457,
                    460,
                    461,
                    462,
                    463,
                    464,
                    465,
                    466,
                    467,
                    468,
                    469,
                    470,
                    471,
                    473,
                    474,
                    486,
                    489,
                    491,
                    492,
                    493,
                    498,
                    506,
                    509,
                    510,
                    511,
                    514,
                    517,
                    519,
                    521,
                    566,
                    586,
                    587,
                    588,
                    589,
                    590,
                    591,
                    592,
                    593,
                    594,
                    595,
                    596,
                    597,
                    600 -> {
                boolean result = false;
                result |= ((MultiblockCoreBlock) state.getBlock()).bulkCoreRelevant(state);
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                result |= ((CapabilityBlock) state.getBlock()).bulkCapabilityRelevant(state);
                return result;
            }
            case 268,
                    276,
                    280,
                    281,
                    300,
                    303,
                    329,
                    401,
                    410,
                    424,
                    459,
                    472,
                    485,
                    487,
                    488,
                    490,
                    494,
                    504,
                    508,
                    546,
                    567,
                    568 -> {
                boolean result = false;
                result |= ((MultiblockCoreBlock) state.getBlock()).bulkCoreRelevant(state);
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                return result;
            }
            case 275,
                    297,
                    298,
                    306,
                    317,
                    319,
                    320,
                    324,
                    330,
                    332,
                    335,
                    341,
                    342,
                    343,
                    346,
                    353,
                    354,
                    372,
                    374,
                    387,
                    389,
                    395,
                    397,
                    398,
                    405,
                    407,
                    412,
                    419,
                    429,
                    430,
                    436,
                    441,
                    446,
                    447,
                    448,
                    449,
                    482,
                    483,
                    500,
                    507,
                    513,
                    515,
                    516,
                    518,
                    522,
                    523,
                    533,
                    540,
                    541,
                    542,
                    543,
                    544,
                    547,
                    548,
                    549,
                    560,
                    573,
                    574,
                    575,
                    576,
                    577,
                    578,
                    598,
                    599 -> {
                boolean result = false;
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                result |= ((CapabilityBlock) state.getBlock()).bulkCapabilityRelevant(state);
                return result;
            }
            case 286, 425, 503, 601 -> {
                boolean result = false;
                result |= ((CapabilityBlock) state.getBlock()).bulkCapabilityRelevant(state);
                return result;
            }
            case 302 -> {
                boolean result = false;
                result |= ((CMPortBlock) state.getBlock()).bulkPortRelevant(state);
                return result;
            }
            case 351, 380, 452, 455, 458, 526, 564, 570, 579, 580, 581, 582, 583, 584, 585 -> {
                boolean result = false;
                result |= ((MultiblockCoreBlock) state.getBlock()).bulkCoreRelevant(state);
                return result;
            }
            case 371, 394 -> {
                boolean result = false;
                result |= ((CapabilityBlock) state.getBlock()).bulkCapabilityRelevant(state);
                result |= ((Member) state.getBlock()).bulkMemberRelevant(state);
                return result;
            }
            case 383 -> {
                boolean result = false;
                result |= ((MultiblockCellBlock) state.getBlock()).bulkCellRelevant(state);
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                return result;
            }
            case 409, 525, 527 -> {
                boolean result = false;
                result |= ((MultiblockCellBlock) state.getBlock()).bulkCellRelevant(state);
                return result;
            }
            case 427 -> {
                boolean result = false;
                result |= ((SealBlock) state.getBlock()).bulkSealRelevant(state);
                return result;
            }
            case 428, 524 -> {
                boolean result = false;
                result |= ((Member) state.getBlock()).bulkMemberRelevant(state);
                return result;
            }
            case 481 -> {
                boolean result = false;
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                result |= ((Member) state.getBlock()).bulkMemberRelevant(state);
                return result;
            }
            case 534, 551, 555, 558 -> {
                boolean result = false;
                result |= ((PhysicalConnector) state.getBlock()).bulkPhysicalRelevant(state);
                result |= ((TickingEntityBlock) state.getBlock()).bulkTickingRelevant(state);
                return result;
            }
            case 563, 565 -> {
                boolean result = false;
                result |= ((MultiblockCoreBlock) state.getBlock()).bulkCoreRelevant(state);
                result |= ((PhysicalConnector) state.getBlock()).bulkPhysicalRelevant(state);
                return result;
            }
            default -> {
                return com.hbm.world.bulk.BulkBindings.super.relevant(kind, state);
            }
        }
    }

    public boolean cell(int kind, BlockState state) {
        switch (kind) {
            case 257,
                    263,
                    264,
                    265,
                    268,
                    276,
                    280,
                    281,
                    290,
                    291,
                    292,
                    293,
                    294,
                    295,
                    296,
                    299,
                    300,
                    301,
                    303,
                    304,
                    305,
                    307,
                    308,
                    309,
                    310,
                    311,
                    312,
                    313,
                    314,
                    315,
                    316,
                    318,
                    321,
                    322,
                    323,
                    326,
                    329,
                    331,
                    333,
                    334,
                    336,
                    337,
                    338,
                    339,
                    350,
                    351,
                    352,
                    355,
                    356,
                    357,
                    358,
                    359,
                    360,
                    362,
                    363,
                    364,
                    365,
                    366,
                    367,
                    368,
                    370,
                    373,
                    376,
                    377,
                    378,
                    380,
                    381,
                    382,
                    384,
                    385,
                    388,
                    390,
                    391,
                    392,
                    393,
                    399,
                    400,
                    401,
                    402,
                    403,
                    404,
                    406,
                    408,
                    410,
                    413,
                    414,
                    416,
                    417,
                    418,
                    420,
                    421,
                    422,
                    423,
                    424,
                    426,
                    431,
                    432,
                    434,
                    435,
                    437,
                    439,
                    440,
                    442,
                    444,
                    450,
                    451,
                    452,
                    454,
                    455,
                    457,
                    458,
                    459,
                    460,
                    461,
                    462,
                    463,
                    464,
                    465,
                    466,
                    467,
                    468,
                    469,
                    470,
                    471,
                    472,
                    473,
                    474,
                    485,
                    486,
                    487,
                    488,
                    489,
                    490,
                    491,
                    492,
                    493,
                    494,
                    498,
                    504,
                    506,
                    508,
                    509,
                    510,
                    511,
                    514,
                    517,
                    519,
                    521,
                    526,
                    546,
                    563,
                    564,
                    565,
                    566,
                    567,
                    568,
                    570,
                    579,
                    580,
                    581,
                    582,
                    583,
                    584,
                    585,
                    586,
                    587,
                    588,
                    589,
                    590,
                    591,
                    592,
                    593,
                    594,
                    595,
                    596,
                    597,
                    600 -> {
                boolean result = false;
                result |= ((MultiblockCoreBlock) state.getBlock()).bulkSnapshotCell(state);
                return result;
            }
            case 302 -> {
                boolean result = false;
                result |= ((CMPortBlock) state.getBlock()).bulkSnapshotCell(state);
                return result;
            }
            case 371, 394, 428, 481, 524 -> {
                boolean result = false;
                result |= ((Member) state.getBlock()).bulkMemberCell(state);
                return result;
            }
            case 383, 409, 525, 527 -> {
                boolean result = false;
                result |= ((MultiblockCellBlock) state.getBlock()).bulkSnapshotCell(state);
                return result;
            }
            default -> {
                return com.hbm.world.bulk.BulkBindings.super.cell(kind, state);
            }
        }
    }

    public boolean capture(
            int kind,
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            CompoundTag data,
            Consumer<BlockPos> cells) {
        switch (kind) {
            case 257,
                    263,
                    264,
                    265,
                    268,
                    276,
                    280,
                    281,
                    290,
                    291,
                    292,
                    293,
                    294,
                    295,
                    296,
                    299,
                    300,
                    301,
                    303,
                    304,
                    305,
                    307,
                    308,
                    309,
                    310,
                    311,
                    312,
                    313,
                    314,
                    315,
                    316,
                    318,
                    321,
                    322,
                    323,
                    326,
                    329,
                    331,
                    333,
                    334,
                    336,
                    337,
                    338,
                    339,
                    350,
                    351,
                    352,
                    355,
                    356,
                    357,
                    358,
                    359,
                    360,
                    362,
                    363,
                    364,
                    365,
                    366,
                    367,
                    368,
                    370,
                    373,
                    376,
                    377,
                    378,
                    380,
                    381,
                    382,
                    384,
                    385,
                    388,
                    390,
                    391,
                    392,
                    393,
                    399,
                    400,
                    401,
                    402,
                    403,
                    404,
                    406,
                    408,
                    410,
                    413,
                    414,
                    416,
                    417,
                    418,
                    420,
                    421,
                    422,
                    423,
                    424,
                    426,
                    431,
                    432,
                    434,
                    435,
                    437,
                    439,
                    440,
                    442,
                    444,
                    450,
                    451,
                    452,
                    454,
                    455,
                    457,
                    458,
                    459,
                    460,
                    461,
                    462,
                    463,
                    464,
                    465,
                    466,
                    467,
                    468,
                    469,
                    470,
                    471,
                    472,
                    473,
                    474,
                    485,
                    486,
                    487,
                    488,
                    489,
                    490,
                    491,
                    492,
                    493,
                    494,
                    498,
                    504,
                    506,
                    508,
                    509,
                    510,
                    511,
                    514,
                    517,
                    519,
                    521,
                    526,
                    546,
                    563,
                    564,
                    565,
                    566,
                    567,
                    568,
                    570,
                    579,
                    580,
                    581,
                    582,
                    583,
                    584,
                    585,
                    586,
                    587,
                    588,
                    589,
                    590,
                    591,
                    592,
                    593,
                    594,
                    595,
                    596,
                    597,
                    600 -> {
                boolean result = false;
                result |=
                        ((MultiblockCoreBlock) state.getBlock())
                                .captureBulkCells(level, pos, state, data, cells);
                return result;
            }
            case 427 -> {
                boolean result = false;
                result |=
                        ((SealBlock) state.getBlock())
                                .captureBulkLids(level, pos, state, data, cells);
                return result;
            }
            case 553, 554, 559 -> {
                boolean result = false;
                result |=
                        ((FluidPipeBlock) state.getBlock())
                                .captureBulkFluid(level, pos, state, data, cells);
                return result;
            }
            default -> {
                return com.hbm.world.bulk.BulkBindings.super.capture(
                        kind, level, pos, state, data, cells);
            }
        }
    }

    public void relocate(int kind, CompoundTag tag, BulkTransform transform) {
        switch (kind) {
            case 372 -> {
                ICFControllerBlock.relocateBulkData(tag, transform);
                return;
            }
            case 395 -> {
                PWRControllerBlock.relocateBulkData(tag, transform);
                return;
            }
            case 481 -> {
                PileBlock.relocateBulkData(tag, transform);
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.relocate(kind, tag, transform);
            }
        }
    }

    public void transform(int kind, CompoundTag tag, BulkTransform transform) {
        switch (kind) {
            case 481 -> {
                PileBlock.transformBulkData(tag, transform);
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.transform(kind, tag, transform);
            }
        }
    }

    public CompoundTag publicData(int kind, CompoundTag tag) {
        switch (kind) {
            case 372 -> {
                return ICFControllerBlock.publicBulkData(tag);
            }
            case 395 -> {
                return PWRControllerBlock.publicBulkData(tag);
            }
            case 481 -> {
                return PileBlock.publicBulkData(tag);
            }
            default -> {
                return com.hbm.world.bulk.BulkBindings.super.publicData(kind, tag);
            }
        }
    }

    public void blockPublished(
            int kind, ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        switch (kind) {
            case 257,
                    263,
                    264,
                    265,
                    268,
                    276,
                    280,
                    281,
                    290,
                    291,
                    292,
                    293,
                    294,
                    295,
                    296,
                    299,
                    300,
                    301,
                    303,
                    304,
                    305,
                    307,
                    308,
                    309,
                    310,
                    311,
                    312,
                    313,
                    314,
                    315,
                    316,
                    318,
                    321,
                    322,
                    323,
                    326,
                    329,
                    331,
                    333,
                    334,
                    336,
                    337,
                    338,
                    339,
                    350,
                    351,
                    352,
                    355,
                    356,
                    357,
                    358,
                    359,
                    360,
                    362,
                    363,
                    364,
                    365,
                    366,
                    367,
                    368,
                    370,
                    373,
                    376,
                    377,
                    378,
                    380,
                    381,
                    382,
                    384,
                    385,
                    388,
                    390,
                    391,
                    392,
                    393,
                    399,
                    400,
                    401,
                    402,
                    403,
                    404,
                    406,
                    408,
                    410,
                    413,
                    414,
                    416,
                    417,
                    418,
                    420,
                    421,
                    422,
                    423,
                    424,
                    426,
                    431,
                    432,
                    434,
                    435,
                    437,
                    439,
                    440,
                    442,
                    444,
                    450,
                    451,
                    452,
                    454,
                    455,
                    457,
                    458,
                    459,
                    460,
                    461,
                    462,
                    463,
                    464,
                    465,
                    466,
                    467,
                    468,
                    469,
                    470,
                    471,
                    472,
                    473,
                    474,
                    485,
                    486,
                    487,
                    488,
                    489,
                    490,
                    491,
                    492,
                    493,
                    494,
                    498,
                    504,
                    506,
                    508,
                    509,
                    510,
                    511,
                    514,
                    517,
                    519,
                    521,
                    526,
                    546,
                    563,
                    564,
                    565,
                    566,
                    567,
                    568,
                    570,
                    579,
                    580,
                    581,
                    582,
                    583,
                    584,
                    585,
                    586,
                    587,
                    588,
                    589,
                    590,
                    591,
                    592,
                    593,
                    594,
                    595,
                    596,
                    597,
                    600 -> {
                ((MultiblockCoreBlock) state.getBlock()).publishBulkCells(level, pos, state, data);
                return;
            }
            case 553, 554, 559 -> {
                ((FluidPipeBlock) state.getBlock()).publishBulkFluid(level, pos, state, data);
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.blockPublished(kind, level, pos, state, data);
            }
        }
    }

    public boolean entityCells(
            int kind, BlockEntity be, ServerLevel level, Consumer<BlockPos> cells) {
        switch (kind) {
            case 31 -> {
                boolean result = false;
                result |= ((CustomMachineBlockEntity) be).capturePortCells(level, cells);
                return result;
            }
            case 57 -> {
                boolean result = false;
                result |= ((ICFControllerBlockEntity) be).captureBulkCells(level, cells);
                return result;
            }
            case 68 -> {
                boolean result = false;
                result |= ((PWRControllerBlockEntity) be).captureBulkCells(level, cells);
                return result;
            }
            case 136 -> {
                boolean result = false;
                result |= ((PileCoreBlockEntity) be).captureBulkCells(level, cells);
                return result;
            }
            default -> {
                return com.hbm.world.bulk.BulkBindings.super.entityCells(kind, be, level, cells);
            }
        }
    }

    public void clearInventory(int kind, BlockEntity be) {
        switch (kind) {
            case 0,
                    1,
                    3,
                    4,
                    5,
                    6,
                    7,
                    8,
                    9,
                    10,
                    11,
                    12,
                    13,
                    14,
                    15,
                    16,
                    17,
                    18,
                    19,
                    20,
                    21,
                    22,
                    23,
                    24,
                    25,
                    26,
                    27,
                    28,
                    30,
                    31,
                    32,
                    33,
                    34,
                    35,
                    36,
                    37,
                    38,
                    39,
                    47,
                    48,
                    49,
                    50,
                    51,
                    52,
                    53,
                    54,
                    55,
                    56,
                    58,
                    59,
                    60,
                    61,
                    62,
                    63,
                    64,
                    65,
                    66,
                    67,
                    68,
                    69,
                    70,
                    71,
                    72,
                    73,
                    74,
                    75,
                    76,
                    77,
                    78,
                    79,
                    80,
                    81,
                    82,
                    83,
                    84,
                    85,
                    86,
                    88,
                    89,
                    90,
                    91,
                    92,
                    93,
                    94,
                    95,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    102,
                    103,
                    104,
                    105,
                    106,
                    107,
                    108,
                    109,
                    110,
                    111,
                    112,
                    113,
                    114,
                    115,
                    116,
                    117,
                    118,
                    119,
                    120,
                    121,
                    122,
                    123,
                    124,
                    125,
                    126,
                    127,
                    128,
                    129,
                    130,
                    131,
                    132,
                    133,
                    134,
                    135,
                    137,
                    138,
                    139,
                    140,
                    141,
                    142,
                    143,
                    144,
                    145,
                    146,
                    147,
                    148,
                    149,
                    150,
                    151,
                    152,
                    153,
                    154,
                    155,
                    156,
                    157,
                    158,
                    159,
                    160,
                    161,
                    162,
                    163,
                    164,
                    165,
                    166,
                    167,
                    168,
                    169,
                    170,
                    171,
                    172,
                    173,
                    174,
                    175,
                    176,
                    177,
                    178,
                    179,
                    180,
                    181,
                    182,
                    183,
                    184,
                    185,
                    186,
                    187,
                    188,
                    189,
                    190,
                    191,
                    192,
                    193,
                    194,
                    195,
                    196,
                    197,
                    198,
                    199,
                    200,
                    201,
                    202,
                    203,
                    204,
                    205,
                    206,
                    207,
                    208,
                    209,
                    210,
                    211,
                    212,
                    213,
                    214,
                    215,
                    216,
                    217,
                    218,
                    219,
                    220,
                    221,
                    222,
                    223,
                    224,
                    225,
                    226,
                    227,
                    228,
                    229,
                    230,
                    231,
                    232,
                    233,
                    234,
                    235,
                    237,
                    238,
                    239,
                    240,
                    241,
                    242,
                    243,
                    244,
                    245,
                    246,
                    247,
                    248,
                    249,
                    250,
                    251,
                    252 -> {
                ((MachineBlockEntity) be).clearBulkInventory();
                return;
            }
            case 2 -> {
                ((SkeletonHolderBlockEntity) be).clearBulkInventory();
                return;
            }
            case 29, 40, 41, 42, 43, 44, 45, 46, 87 -> {
                ((FoundryBaseBlockEntity) be).clearBulkInventory();
                return;
            }
            case 136 -> {
                ((PileCoreBlockEntity) be).clearBulkInventory();
                return;
            }
            case 236 -> {
                ((PedestalBlockEntity) be).clearDisplay();
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.clearInventory(kind, be);
            }
        }
    }

    public void forgetDrops(int kind, BlockEntity be) {
        switch (kind) {
            case 0,
                    1,
                    3,
                    4,
                    5,
                    6,
                    7,
                    8,
                    9,
                    10,
                    11,
                    12,
                    13,
                    14,
                    15,
                    16,
                    17,
                    18,
                    19,
                    20,
                    21,
                    22,
                    23,
                    24,
                    25,
                    26,
                    27,
                    28,
                    30,
                    31,
                    32,
                    33,
                    34,
                    35,
                    36,
                    37,
                    38,
                    39,
                    47,
                    48,
                    49,
                    50,
                    51,
                    52,
                    53,
                    54,
                    55,
                    56,
                    58,
                    59,
                    60,
                    61,
                    62,
                    63,
                    64,
                    65,
                    66,
                    67,
                    68,
                    69,
                    70,
                    71,
                    72,
                    73,
                    74,
                    75,
                    76,
                    77,
                    78,
                    79,
                    80,
                    81,
                    82,
                    83,
                    84,
                    85,
                    86,
                    88,
                    89,
                    90,
                    91,
                    92,
                    93,
                    94,
                    95,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    102,
                    103,
                    104,
                    105,
                    106,
                    107,
                    108,
                    109,
                    110,
                    111,
                    112,
                    113,
                    114,
                    115,
                    116,
                    117,
                    118,
                    119,
                    120,
                    121,
                    122,
                    123,
                    124,
                    125,
                    126,
                    127,
                    128,
                    129,
                    130,
                    131,
                    132,
                    133,
                    134,
                    135,
                    137,
                    138,
                    139,
                    140,
                    141,
                    142,
                    143,
                    144,
                    145,
                    146,
                    147,
                    148,
                    149,
                    150,
                    151,
                    152,
                    153,
                    154,
                    155,
                    156,
                    157,
                    158,
                    159,
                    160,
                    161,
                    162,
                    163,
                    164,
                    165,
                    166,
                    167,
                    168,
                    169,
                    170,
                    171,
                    172,
                    173,
                    174,
                    175,
                    176,
                    177,
                    178,
                    179,
                    180,
                    181,
                    182,
                    183,
                    184,
                    185,
                    186,
                    187,
                    188,
                    189,
                    190,
                    191,
                    192,
                    193,
                    194,
                    195,
                    196,
                    197,
                    198,
                    199,
                    200,
                    201,
                    202,
                    203,
                    204,
                    205,
                    206,
                    207,
                    208,
                    209,
                    210,
                    211,
                    212,
                    213,
                    214,
                    215,
                    216,
                    217,
                    218,
                    219,
                    220,
                    221,
                    222,
                    223,
                    224,
                    225,
                    226,
                    227,
                    228,
                    229,
                    230,
                    231,
                    232,
                    233,
                    234,
                    235,
                    237,
                    238,
                    239,
                    240,
                    241,
                    242,
                    243,
                    244,
                    245,
                    246,
                    247,
                    248,
                    249,
                    250,
                    251,
                    252 -> {
                ((MachineBlockEntity) be).forgetBulkDrops();
                return;
            }
            case 29, 40, 41, 42, 43, 44, 45, 46, 87 -> {
                ((FoundryBaseBlockEntity) be).forgetBulkDrops();
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.forgetDrops(kind, be);
            }
        }
    }

    public void entityPublished(int kind, BlockEntity be) {
        switch (kind) {
            case 31 -> {
                ((CustomMachineBlockEntity) be).publishPortCells();
                return;
            }
            default -> {
                com.hbm.world.bulk.BulkBindings.super.entityPublished(kind, be);
            }
        }
    }
}
