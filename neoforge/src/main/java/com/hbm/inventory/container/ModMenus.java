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

package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.BalefireNukeBlockEntity;
import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.blockentity.bomb.CustomNukeBlockEntity;
import com.hbm.blockentity.bomb.LaunchPadBlockEntity;
import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.blockentity.generator.RadGenBlockEntity;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.albion.*;
import com.hbm.blockentity.machine.albion.*;
import com.hbm.blockentity.machine.dfc.*;
import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.blockentity.machine.oil.*;
import com.hbm.blockentity.machine.oil.*;
import com.hbm.blockentity.machine.radio.RadioReceiverBlockEntity;
import com.hbm.blockentity.machine.rbmk.*;
import com.hbm.blockentity.machine.rbmk.*;
import com.hbm.blockentity.machine.rbmk.RBMKGaugeBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIndicatorBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blockentity.machine.storage.*;
import com.hbm.blockentity.machine.thermal.BrickFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CombinationFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.CoolingHeatBlockEntity;
import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.blockentity.network.CableDiodeBlockEntity;
import com.hbm.blockentity.network.CraneBoxerBlockEntity;
import com.hbm.blockentity.network.CraneExtractorBlockEntity;
import com.hbm.blockentity.network.CraneGrabberBlockEntity;
import com.hbm.blockentity.network.CraneInserterBlockEntity;
import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.blockentity.network.CraneUnboxerBlockEntity;
import com.hbm.blockentity.network.DroneCrateBlockEntity;
import com.hbm.blockentity.network.DroneDockBlockEntity;
import com.hbm.blockentity.network.DroneProviderBlockEntity;
import com.hbm.blockentity.network.DroneRequesterBlockEntity;
import com.hbm.blockentity.network.FluidPumpBlockEntity;
import com.hbm.blockentity.network.RadioAutocalBlockEntity;
import com.hbm.blockentity.network.RadioCounterBlockEntity;
import com.hbm.blockentity.network.RadioTelexBlockEntity;
import com.hbm.blockentity.network.RadioTorchBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticImporterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticStorageClutterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumaticTubeBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageAccessBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
import com.hbm.blockentity.ordnance.launch.CompactLauncherBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchSiloBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.blockentity.ordnance.launch.MissileErectorBlockEntity;
import com.hbm.blockentity.processing.AmmoPressBlockEntity;
import com.hbm.blockentity.processing.ArcFurnaceBlockEntity;
import com.hbm.blockentity.processing.ArcWelderBlockEntity;
import com.hbm.blockentity.processing.AssemblyMachineBlockEntity;
import com.hbm.blockentity.processing.BurnerPressBlockEntity;
import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.blockentity.processing.CrystalProcessorBlockEntity;
import com.hbm.blockentity.processing.ElectricPressBlockEntity;
import com.hbm.blockentity.processing.ElectrolysisBlockEntity;
import com.hbm.blockentity.processing.FluidMixerBlockEntity;
import com.hbm.blockentity.processing.SolderingBenchBlockEntity;
import com.hbm.blockentity.turret.TurretBaseNTBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.BlockMenuType;
import com.hbm.inventory.container.StorageDrumMenu;
import com.hbm.inventory.container.WatzMenu;
import com.hbm.items.tool.AmmoBagItem;
import com.hbm.items.tool.CasingBagItem;
import com.hbm.items.tool.LeadBoxItem;
import com.hbm.items.tool.PlasticBagItem;
import com.hbm.items.tool.ToolBoxItem;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {

    private static final RegistryHandle<MenuType<CrateMenu>>[] CRATE = newCrateArray();
    private static final Map<Integer, RegistryHandle<MenuType<AnvilMenu>>> ANVIL = new HashMap<>();
    public static RegistryHandle<MenuType<TrainCargoTramMenu>> TRAIN_CARGO_TRAM;
    public static RegistryHandle<MenuType<TrainCargoTramTrailerMenu>> TRAIN_CARGO_TRAM_TRAILER;
    public static RegistryHandle<MenuType<CartDestroyerMenu>> CART_DESTROYER;
    public static RegistryHandle<MenuType<ItemBoxMenu>> CONTAINMENT_BOX;
    public static RegistryHandle<MenuType<ItemBoxMenu>> TOOLBOX;
    public static RegistryHandle<MenuType<ItemBoxMenu>> AMMO_BAG;
    public static RegistryHandle<MenuType<ItemBoxMenu>> CASING_BAG;
    public static RegistryHandle<MenuType<ItemBoxMenu>> PLASTIC_BAG;
    public static RegistryHandle<MenuType<RebarPlacerMenu>> REBAR_PLACER;
    public static RegistryHandle<MenuType<HeldItemMenu>> CATALOG;
    public static RegistryHandle<MenuType<HeldItemMenu>> FLUID_IDENTIFIER;
    public static RegistryHandle<MenuType<HeldItemMenu>> DESIGNATOR_MANUAL;
    public static RegistryHandle<MenuType<HeldItemMenu>> RTTY_PAGER;
    public static RegistryHandle<MenuType<HeldItemMenu>> SATELLITE_INTERFACE;
    public static RegistryHandle<MenuType<HeldItemMenu>> TOOL_PRESETS;
    public static RegistryHandle<MenuType<CentrifugeMenu>> MACHINE_CENTRIFUGE;
    public static RegistryHandle<MenuType<GasCentMenu>> MACHINE_GASCENT;
    public static RegistryHandle<MenuType<ElectricFurnaceMenu>> MACHINE_ELECTRIC_FURNACE;
    public static RegistryHandle<MenuType<PressMenu>> MACHINE_PRESS;
    public static RegistryHandle<MenuType<AutocrafterMenu>> MACHINE_AUTOCRAFTER;
    public static RegistryHandle<MenuType<PneumoTubeMenu>> PNEUMATIC_TUBE;
    public static RegistryHandle<MenuType<CraneInserterMenu>> CRANE_INSERTER;
    public static RegistryHandle<MenuType<CraneExtractorMenu>> CRANE_EXTRACTOR;
    public static RegistryHandle<MenuType<CraneGrabberMenu>> CRANE_GRABBER;
    public static RegistryHandle<MenuType<CraneBoxerMenu>> CRANE_BOXER;
    public static RegistryHandle<MenuType<CraneUnboxerMenu>> CRANE_UNBOXER;
    public static RegistryHandle<MenuType<CraneRouterMenu>> CRANE_ROUTER;
    public static RegistryHandle<MenuType<PneumoStorageAccessMenu>> PNEUMATIC_STORAGE_ACCESS;
    public static RegistryHandle<MenuType<PneumoStorageClutterMenu>> PNEUMATIC_STORAGE_CLUTTER;
    public static RegistryHandle<MenuType<PneumoStorageMonoMenu>> PNEUMATIC_STORAGE_MONO;
    public static RegistryHandle<MenuType<PneumoStorageImporterMenu>> PNEUMATIC_STORAGE_IMPORTER;
    public static RegistryHandle<MenuType<PneumoStorageExporterMenu>> PNEUMATIC_STORAGE_EXPORTER;
    public static RegistryHandle<MenuType<AssemblyMachineMenu>> MACHINE_ASSEMBLY_MACHINE;
    public static RegistryHandle<MenuType<AssemblyFactoryMenu>> MACHINE_ASSEMBLY_FACTORY;
    public static RegistryHandle<MenuType<ChemicalPlantMenu>> MACHINE_CHEMICAL_PLANT;
    public static RegistryHandle<MenuType<RockMillMenu>> MACHINE_ROCK_MILL;
    public static RegistryHandle<MenuType<CustomMenu>> MACHINE_CUSTOM;
    public static RegistryHandle<MenuType<RadiolysisMenu>> MACHINE_RADIOLYSIS;
    public static RegistryHandle<MenuType<FusionTorusMenu>> FUSION_TORUS;
    public static RegistryHandle<MenuType<CoreMenu>> CORE_CORE;
    public static RegistryHandle<MenuType<CoreEmitterMenu>> CORE_EMITTER;
    public static RegistryHandle<MenuType<CoreReceiverMenu>> CORE_RECEIVER;
    public static RegistryHandle<MenuType<CoreInjectorMenu>> CORE_INJECTOR;
    public static RegistryHandle<MenuType<CoreStabilizerMenu>> CORE_STABILIZER;
    public static RegistryHandle<MenuType<FusionKlystronMenu>> FUSION_KLYSTRON;
    public static RegistryHandle<MenuType<FusionBreederMenu>> FUSION_BREEDER;
    public static RegistryHandle<MenuType<PlasmaForgeMenu>> MACHINE_PLASMA_FORGE;
    public static RegistryHandle<MenuType<ChemicalFactoryMenu>> MACHINE_CHEMICAL_FACTORY;
    public static BlockMenuType<ElectrolysisBlockEntity, ElectrolyserFluidMenu>
            MACHINE_ELECTROLYSER_FLUID;
    public static BlockMenuType<ElectrolysisBlockEntity, ElectrolyserMetalMenu>
            MACHINE_ELECTROLYSER_METAL;
    public static RegistryHandle<MenuType<RefineryMenu>> MACHINE_REFINERY;
    public static RegistryHandle<MenuType<CatalyticReformerMenu>> MACHINE_CATALYTIC_REFORMER;
    public static RegistryHandle<MenuType<HydrotreaterMenu>> MACHINE_HYDROTREATER;
    public static RegistryHandle<MenuType<PUREXMenu>> MACHINE_PUREX;
    public static RegistryHandle<MenuType<MixerMenu>> MACHINE_MIXER;
    public static RegistryHandle<MenuType<SILEXMenu>> MACHINE_SILEX;
    public static RegistryHandle<MenuType<ICFMenu>> MACHINE_ICF;
    public static RegistryHandle<MenuType<ICFPressMenu>> MACHINE_ICF_PRESS;
    public static RegistryHandle<MenuType<OreSlopperMenu>> MACHINE_ORE_SLOPPER;
    public static RegistryHandle<MenuType<AnnihilatorMenu>> MACHINE_ANNIHILATOR;
    public static RegistryHandle<MenuType<ArcFurnaceMenu>> MACHINE_ARC_FURNACE;
    public static RegistryHandle<MenuType<BlastFurnaceMenu>> MACHINE_BLAST_FURNACE;
    public static RegistryHandle<MenuType<CrucibleMenu>> MACHINE_CRUCIBLE;
    public static RegistryHandle<MenuType<StrandCasterMenu>> MACHINE_STRAND_CASTER;
    public static RegistryHandle<MenuType<PrecAssMenu>> MACHINE_PRECASS;
    public static RegistryHandle<MenuType<MiningLaserMenu>> MACHINE_MINING_LASER;
    public static RegistryHandle<MenuType<ForceFieldMenu>> FORCEFIELD;
    public static RegistryHandle<MenuType<ExcavatorMenu>> MACHINE_EXCAVATOR;
    public static RegistryHandle<MenuType<CyclotronMenu>> MACHINE_CYCLOTRON;
    public static RegistryHandle<MenuType<ExposureChamberMenu>> MACHINE_EXPOSURE_CHAMBER;
    public static RegistryHandle<MenuType<RadGenMenu>> MACHINE_RADGEN;
    public static BlockMenuType<RadarBlockEntity, RadarMenu> MACHINE_RADAR;
    public static BlockMenuType<RadarBlockEntity, RadarSlotsMenu> MACHINE_RADAR_SLOTS;
    public static RegistryHandle<MenuType<ReactorBreedingMenu>> MACHINE_REACTOR_BREEDING;
    public static RegistryHandle<MenuType<ReactorResearchMenu>> REACTOR_RESEARCH;
    public static RegistryHandle<MenuType<ReactorControlMenu>> REACTOR_CONTROL;
    public static RegistryHandle<MenuType<RotaryFurnaceMenu>> MACHINE_ROTARY_FURNACE;
    public static RegistryHandle<MenuType<FireboxMenu>> HEATER_FIREBOX;
    public static RegistryHandle<MenuType<OilWellMenu>> MACHINE_WELL;
    public static RegistryHandle<MenuType<CompressorMenu>> MACHINE_COMPRESSOR;
    public static RegistryHandle<MenuType<GasFlareMenu>> MACHINE_GAS_FLARE;
    public static RegistryHandle<MenuType<ReactorZirnoxMenu>> REACTOR_ZIRNOX;
    public static RegistryHandle<MenuType<WatzMenu>> WATZ;
    public static RegistryHandle<MenuType<FELMenu>> MACHINE_FEL;
    public static RegistryHandle<MenuType<PASourceMenu>> PA_SOURCE;
    public static RegistryHandle<MenuType<PARFCMenu>> PA_RFC;
    public static RegistryHandle<MenuType<PAQuadrupoleMenu>> PA_QUADRUPOLE;
    public static RegistryHandle<MenuType<PADipoleMenu>> PA_DIPOLE;
    public static RegistryHandle<MenuType<PADetectorMenu>> PA_DETECTOR;
    public static RegistryHandle<MenuType<SolderingStationMenu>> MACHINE_SOLDERING_STATION;
    public static RegistryHandle<MenuType<ArcWelderMenu>> MACHINE_ARC_WELDER;
    public static RegistryHandle<MenuType<EPressMenu>> MACHINE_EPRESS;
    public static RegistryHandle<MenuType<ShredderMenu>> MACHINE_SHREDDER;
    public static RegistryHandle<MenuType<FluidTankMenu>> MACHINE_FLUID_TANK;
    public static RegistryHandle<MenuType<TurretBaseMenu>> TURRET_BASE;
    public static RegistryHandle<MenuType<BarrelMenu>> BARREL;
    public static RegistryHandle<MenuType<MassStorageMenu>> MASS_STORAGE;
    public static RegistryHandle<MenuType<FileCabinetMenu>> FILE_CABINET;
    public static RegistryHandle<MenuType<WoodBurnerMenu>> MACHINE_WOOD_BURNER;
    public static RegistryHandle<MenuType<DieselMenu>> MACHINE_DIESEL;
    public static RegistryHandle<MenuType<RTGMenu>> MACHINE_RTG;
    public static RegistryHandle<MenuType<TurbineMenu>> MACHINE_TURBINE;
    public static RegistryHandle<MenuType<LargeTurbineMenu>> MACHINE_LARGE_TURBINE;
    public static RegistryHandle<MenuType<CombustionEngineMenu>> MACHINE_COMBUSTION_ENGINE;
    public static RegistryHandle<MenuType<TurbineGasMenu>> MACHINE_TURBINEGAS;
    public static RegistryHandle<MenuType<TurbofanMenu>> MACHINE_TURBOFAN;
    public static RegistryHandle<MenuType<WasteDrumMenu>> WASTE_DRUM;
    public static RegistryHandle<MenuType<StorageDrumMenu>> STORAGE_DRUM;
    public static RegistryHandle<MenuType<BatterySocketMenu>> BATTERY_SOCKET;
    public static RegistryHandle<MenuType<BatteryMenu>> MACHINE_BATTERY;
    public static RegistryHandle<MenuType<BatteryREDDMenu>> BATTERY_REDD;
    public static RegistryHandle<MenuType<NukeManMenu>> NUKE_MAN;
    public static RegistryHandle<MenuType<NukeGadgetMenu>> NUKE_GADGET;
    public static RegistryHandle<MenuType<NukeBoyMenu>> NUKE_BOY;
    public static RegistryHandle<MenuType<NukeMikeMenu>> NUKE_MIKE;
    public static RegistryHandle<MenuType<NukeTsarMenu>> NUKE_TSAR;
    public static RegistryHandle<MenuType<NukeFleijaMenu>> NUKE_FLEIJA;
    public static RegistryHandle<MenuType<NukePrototypeMenu>> NUKE_PROTOTYPE;
    public static RegistryHandle<MenuType<NukeSoliniumMenu>> NUKE_SOLINIUM;
    public static RegistryHandle<MenuType<NukeN2Menu>> NUKE_N2;
    public static RegistryHandle<MenuType<MissileAssemblyMenu>> MACHINE_MISSILE_ASSEMBLY;
    public static RegistryHandle<MenuType<LaunchPadMenu>> LAUNCH_PAD;
    public static RegistryHandle<MenuType<LaunchPadRustedMenu>> LAUNCH_PAD_RUSTED;
    public static RegistryHandle<MenuType<LaunchPadLargeMenu>> LAUNCH_PAD_LARGE;
    public static RegistryHandle<MenuType<LaunchTableMenu>> LAUNCH_TABLE;
    public static RegistryHandle<MenuType<CompactLauncherMenu>> COMPACT_LAUNCHER;
    public static RegistryHandle<MenuType<NukeCustomMenu>> NUKE_CUSTOM;
    public static RegistryHandle<MenuType<NukeFstbmbMenu>> NUKE_FSTBMB;
    public static RegistryHandle<MenuType<BombMultiMenu>> BOMB_MULTI;
    public static RegistryHandle<MenuType<SoyuzCapsuleMenu>> SOYUZ_CAPSULE;
    public static RegistryHandle<MenuType<SoyuzLauncherMenu>> SOYUZ_LAUNCHER;
    public static RegistryHandle<MenuType<LaunchpadSoyuzMenu>> LAUNCHPAD_SOYUZ;
    public static RegistryHandle<MenuType<SatDockMenu>> SAT_DOCK;
    public static RegistryHandle<MenuType<RadioTorchCounterMenu>> RADIO_TORCH_COUNTER;
    public static RegistryHandle<MenuType<ScreenMenu<CableDiodeBlockEntity>>> CABLE_DIODE;
    public static RegistryHandle<MenuType<ScreenMenu<FluidPumpBlockEntity>>> FLUID_PUMP;
    public static RegistryHandle<MenuType<ScreenMenu<RadioReceiverBlockEntity>>> RADIO_REC;
    public static RegistryHandle<MenuType<ScreenMenu<RadioTelexBlockEntity>>> RADIO_TELEX;
    public static RegistryHandle<MenuType<ScreenMenu<RadioAutocalBlockEntity>>> RADIO_AUTOCAL;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKGaugeBlockEntity>>> RBMK_GAUGE;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKGraphBlockEntity>>> RBMK_GRAPH;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKIndicatorBlockEntity>>> RBMK_INDICATOR;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKKeyPadBlockEntity>>> RBMK_KEYPAD;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKLeverBlockEntity>>> RBMK_LEVER;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKNumitronBlockEntity>>> RBMK_NUMITRON;
    public static RegistryHandle<MenuType<ScreenMenu<RBMKTerminalBlockEntity>>> RBMK_TERMINAL;
    public static RegistryHandle<MenuType<ScreenMenu<RadioTorchBlockEntity>>> RADIO_TORCH;
    public static RegistryHandle<MenuType<DroneCrateMenu>> DRONE_CRATE;
    public static RegistryHandle<MenuType<DroneDockMenu>> DRONE_DOCK;
    public static RegistryHandle<MenuType<DroneProviderMenu>> DRONE_PROVIDER;
    public static RegistryHandle<MenuType<DroneRequesterMenu>> DRONE_REQUESTER;
    public static RegistryHandle<MenuType<WeaponTableMenu>> WEAPON_TABLE;
    public static RegistryHandle<MenuType<BookMenu>> BOOK_OF;
    public static RegistryHandle<MenuType<LemegetonMenu>> LEMEGETON;
    public static RegistryHandle<MenuType<ArmorTableMenu>> ARMOR_TABLE;
    public static RegistryHandle<MenuType<RBMKRodMenu>> RBMK_ROD;
    public static RegistryHandle<MenuType<RBMKControlMenu>> RBMK_CONTROL;
    public static RegistryHandle<MenuType<RBMKControlAutoMenu>> RBMK_CONTROL_AUTO;
    public static RegistryHandle<MenuType<RBMKBoilerMenu>> RBMK_BOILER;
    public static RegistryHandle<MenuType<RBMKStorageMenu>> RBMK_STORAGE;
    public static RegistryHandle<MenuType<RBMKHeaterMenu>> RBMK_HEATER;
    public static RegistryHandle<MenuType<RBMKOutgasserMenu>> RBMK_OUTGASSER;
    public static RegistryHandle<MenuType<RBMKConsoleMenu>> RBMK_CONSOLE;
    public static RegistryHandle<MenuType<RBMKAutoloaderMenu>> RBMK_AUTOLOADER;
    public static RegistryHandle<MenuType<PWRMenu>> PWR_CONTROLLER;
    public static RegistryHandle<MenuType<FurnaceIronMenu>> FURNACE_IRON;
    public static RegistryHandle<MenuType<FurnaceSteelMenu>> FURNACE_STEEL;
    public static RegistryHandle<MenuType<FurnaceCombinationMenu>> FURNACE_COMBINATION;
    public static RegistryHandle<MenuType<FurnaceBrickMenu>> FURNACE_BRICK;
    public static RegistryHandle<MenuType<HeaterOvenMenu>> HEATER_OVEN;
    public static RegistryHandle<MenuType<AshpitMenu>> MACHINE_ASHPIT;
    public static RegistryHandle<MenuType<SirenMenu>> MACHINE_SIREN;
    public static RegistryHandle<MenuType<OilburnerMenu>> HEATER_OILBURNER;
    public static RegistryHandle<MenuType<HeatexMenu>> HEATER_HEATEX;
    public static RegistryHandle<MenuType<CrystallizerMenu>> MACHINE_CRYSTALLIZER;
    public static RegistryHandle<MenuType<CokerMenu>> MACHINE_COKER;
    public static RegistryHandle<MenuType<LiquefactorMenu>> MACHINE_LIQUEFACTOR;
    public static RegistryHandle<MenuType<SolidifierMenu>> MACHINE_SOLIDIFIER;
    public static RegistryHandle<MenuType<PyroOvenMenu>> MACHINE_PYROOVEN;
    public static RegistryHandle<MenuType<VacuumDistillMenu>> MACHINE_VACUUM_DISTILL;
    public static RegistryHandle<MenuType<FunnelMenu>> MACHINE_FUNNEL;
    public static RegistryHandle<MenuType<MicrowaveMenu>> MACHINE_MICROWAVE;
    public static RegistryHandle<MenuType<SatLinkerMenu>> MACHINE_SATLINKER;
    public static RegistryHandle<MenuType<SuperComputerMenu>> MACHINE_SUPERCOMPUTER;
    public static RegistryHandle<MenuType<TapeDriveMenu>> MACHINE_TAPE_DRIVE;
    public static RegistryHandle<MenuType<KeyForgeMenu>> MACHINE_KEYFORGE;
    public static RegistryHandle<MenuType<AmmoPressMenu>> MACHINE_AMMO_PRESS;

    private ModMenus() {}

    public static void register() {
        MACHINE_CENTRIFUGE = Registrar.registerMenu("machine_centrifuge", CentrifugeMenu::new);
        MACHINE_GASCENT =
                Registrar.registerBlockEntityMenu("machine_gascent", GasCentBlockEntity.class);
        MACHINE_ELECTRIC_FURNACE =
                Registrar.registerMenu("machine_electric_furnace", ElectricFurnaceMenu::new);
        MACHINE_PRESS =
                Registrar.registerBlockEntityMenu("machine_press", BurnerPressBlockEntity.class);
        MACHINE_AUTOCRAFTER =
                Registrar.registerBlockEntityMenu(
                        "machine_autocrafter", TemplateCrafterBlockEntity.class);
        PNEUMATIC_TUBE =
                Registrar.registerBlockEntityMenu("pneumatic_tube", PneumaticTubeBlockEntity.class);
        CRANE_INSERTER =
                Registrar.registerBlockEntityMenu("crane_inserter", CraneInserterBlockEntity.class);
        CRANE_EXTRACTOR =
                Registrar.registerBlockEntityMenu(
                        "crane_extractor", CraneExtractorBlockEntity.class);
        CRANE_GRABBER =
                Registrar.registerBlockEntityMenu("crane_grabber", CraneGrabberBlockEntity.class);
        CRANE_BOXER = Registrar.registerBlockEntityMenu("crane_boxer", CraneBoxerBlockEntity.class);
        CRANE_UNBOXER =
                Registrar.registerBlockEntityMenu("crane_unboxer", CraneUnboxerBlockEntity.class);
        CRANE_ROUTER =
                Registrar.registerBlockEntityMenu("crane_router", CraneRouterBlockEntity.class);
        PNEUMATIC_STORAGE_ACCESS =
                Registrar.registerBlockEntityMenu(
                        "pneumatic_storage_access", PneumoStorageAccessBlockEntity.class);
        PNEUMATIC_STORAGE_CLUTTER =
                Registrar.registerBlockEntityMenu(
                        "pneumatic_storage_clutter", PneumaticStorageClutterBlockEntity.class);
        PNEUMATIC_STORAGE_MONO =
                Registrar.registerBlockEntityMenu(
                        "pneumatic_storage_mono", PneumoStorageMonoBlockEntity.class);
        PNEUMATIC_STORAGE_IMPORTER =
                Registrar.registerBlockEntityMenu(
                        "pneumatic_storage_importer", PneumaticImporterBlockEntity.class);
        PNEUMATIC_STORAGE_EXPORTER =
                Registrar.registerBlockEntityMenu(
                        "pneumatic_storage_exporter", PneumoStorageExporterBlockEntity.class);
        MACHINE_ASSEMBLY_MACHINE =
                Registrar.registerBlockEntityMenu(
                        "machine_assembly_machine", AssemblyMachineBlockEntity.class);
        MACHINE_ASSEMBLY_FACTORY =
                Registrar.registerBlockEntityMenu(
                        "machine_assembly_factory", AssemblyFactoryBlockEntity.class);
        MACHINE_CUSTOM =
                Registrar.registerBlockEntityMenu("machine_custom", CustomMachineBlockEntity.class);
        MACHINE_CHEMICAL_PLANT =
                Registrar.registerBlockEntityMenu(
                        "machine_chemical_plant", ChemicalPlantBlockEntity.class);
        MACHINE_ROCK_MILL =
                Registrar.registerBlockEntityMenu("machine_rockmill", RockMillBlockEntity.class);
        MACHINE_RADIOLYSIS =
                Registrar.registerBlockEntityMenu(
                        "machine_radiolysis", RadiolysisBlockEntity.class);
        FUSION_TORUS =
                Registrar.registerBlockEntityMenu("fusion_torus", FusionTorusBlockEntity.class);
        CORE_CORE = Registrar.registerBlockEntityMenu("dfc_core", DfcChamberBlockEntity.class);
        CORE_EMITTER =
                Registrar.registerBlockEntityMenu("dfc_emitter", DfcEmitterBlockEntity.class);
        CORE_RECEIVER =
                Registrar.registerBlockEntityMenu("dfc_receiver", DfcReceiverBlockEntity.class);
        CORE_INJECTOR =
                Registrar.registerBlockEntityMenu("dfc_injector", DfcInjectorBlockEntity.class);
        CORE_STABILIZER =
                Registrar.registerBlockEntityMenu("dfc_stabilizer", DfcStabilizerBlockEntity.class);
        FUSION_KLYSTRON =
                Registrar.registerBlockEntityMenu(
                        "fusion_klystron", FusionKlystronBlockEntity.class);
        FUSION_BREEDER =
                Registrar.registerBlockEntityMenu("fusion_breeder", FusionNeutronBlockEntity.class);
        MACHINE_PLASMA_FORGE =
                Registrar.registerBlockEntityMenu(
                        "machine_plasma_forge", FusionPlasmaForgeBlockEntity.class);
        MACHINE_CHEMICAL_FACTORY =
                Registrar.registerBlockEntityMenu(
                        "machine_chemical_factory", ChemicalFactoryBlockEntity.class);
        MACHINE_ELECTROLYSER_FLUID =
                Registrar.registerBlockEntityMenu(
                        "machine_electrolyser_fluid", ElectrolysisBlockEntity.class);
        MACHINE_ELECTROLYSER_METAL =
                Registrar.registerBlockEntityMenu(
                        "machine_electrolyser_metal",
                        ElectrolysisBlockEntity.class,
                        ElectrolyserMetalMenu::new);
        MACHINE_REFINERY =
                Registrar.registerBlockEntityMenu("machine_refinery", RefineryBlockEntity.class);
        MACHINE_CATALYTIC_REFORMER =
                Registrar.registerBlockEntityMenu(
                        "machine_catalytic_reformer", CatalyticReformerBlockEntity.class);
        MACHINE_HYDROTREATER =
                Registrar.registerBlockEntityMenu(
                        "machine_hydrotreater", HydrotreaterBlockEntity.class);
        MACHINE_PUREX = Registrar.registerBlockEntityMenu("machine_purex", PurexBlockEntity.class);
        MACHINE_MIXER =
                Registrar.registerBlockEntityMenu("machine_mixer", FluidMixerBlockEntity.class);
        MACHINE_SILEX = Registrar.registerBlockEntityMenu("machine_silex", SilexBlockEntity.class);
        MACHINE_ICF = Registrar.registerBlockEntityMenu("icf", ICFBlockEntity.class);
        MACHINE_ICF_PRESS =
                Registrar.registerBlockEntityMenu(
                        "machine_icf_press", FusionPelletPressBlockEntity.class);
        MACHINE_ORE_SLOPPER =
                Registrar.registerBlockEntityMenu(
                        "machine_ore_slopper", OreSlopperBlockEntity.class);
        MACHINE_ANNIHILATOR =
                Registrar.registerBlockEntityMenu(
                        "machine_annihilator", AnnihilatorBlockEntity.class);
        MACHINE_BLAST_FURNACE =
                Registrar.registerBlockEntityMenu(
                        "machine_blast_furnace", BlastFurnaceBlockEntity.class);
        MACHINE_CRUCIBLE =
                Registrar.registerBlockEntityMenu("machine_crucible", CrucibleBlockEntity.class);
        MACHINE_STRAND_CASTER =
                Registrar.registerBlockEntityMenu("machine_strand_caster", ContinuousCaster.class);
        MACHINE_PRECASS =
                Registrar.registerBlockEntityMenu(
                        "machine_precass", PrecisionAssemblerBlockEntity.class);
        MACHINE_MINING_LASER =
                Registrar.registerBlockEntityMenu(
                        "machine_mining_laser", MiningLaserBlockEntity.class);
        FORCEFIELD =
                Registrar.registerBlockEntityMenu(
                        "machine_forcefield", RepulsionFieldBlockEntity.class);
        MACHINE_EXCAVATOR =
                Registrar.registerBlockEntityMenu("machine_excavator", ExcavatorBlockEntity.class);
        MACHINE_CYCLOTRON =
                Registrar.registerBlockEntityMenu("machine_cyclotron", CyclotronBlockEntity.class);
        MACHINE_EXPOSURE_CHAMBER =
                Registrar.registerBlockEntityMenu(
                        "machine_exposure_chamber", ExposureChamberBlockEntity.class);
        MACHINE_RADGEN =
                Registrar.registerBlockEntityMenu("machine_radgen", RadGenBlockEntity.class);
        MACHINE_RADAR = Registrar.registerBlockEntityMenu("machine_radar", RadarBlockEntity.class);
        MACHINE_RADAR_SLOTS =
                Registrar.registerBlockEntityMenu(
                        "machine_radar_slots",
                        RadarBlockEntity.class,
                        (containerId, inventory, radar) -> radar.slotsMenu(containerId, inventory));
        MACHINE_REACTOR_BREEDING =
                Registrar.registerBlockEntityMenu(
                        "machine_reactor", ReactorBreedingBlockEntity.class);
        REACTOR_RESEARCH =
                Registrar.registerBlockEntityMenu(
                        "machine_reactor_small", ResearchReactorBlockEntity.class);
        REACTOR_CONTROL =
                Registrar.registerBlockEntityMenu(
                        "machine_controller", ReactorControlBlockEntity.class);
        MACHINE_ARC_FURNACE =
                Registrar.registerBlockEntityMenu(
                        "machine_arc_furnace", ArcFurnaceBlockEntity.class);
        MACHINE_ROTARY_FURNACE =
                Registrar.registerBlockEntityMenu(
                        "machine_rotary_furnace", RotaryFurnaceBlockEntity.class);
        HEATER_FIREBOX =
                Registrar.registerBlockEntityMenu("heater_firebox", HeaterFireboxBlockEntity.class);
        MACHINE_WELL =
                Registrar.registerBlockEntityMenu("machine_well", OilDrillBaseBlockEntity.class);
        MACHINE_COMPRESSOR =
                Registrar.registerBlockEntityMenu(
                        "machine_compressor", PneumaticCompressorBlockEntity.class);
        MACHINE_GAS_FLARE =
                Registrar.registerBlockEntityMenu("machine_flare", GasFlareBlockEntity.class);
        REACTOR_ZIRNOX =
                Registrar.registerBlockEntityMenu("reactor_zirnox", ReactorZirnoxBlockEntity.class);
        WATZ = Registrar.registerBlockEntityMenu("watz", WatzBlockEntity.class);
        MACHINE_FEL =
                Registrar.registerBlockEntityMenu("machine_fel", WavelengthLaserBlockEntity.class);
        PA_SOURCE = Registrar.registerBlockEntityMenu("pa_source", PASourceBlockEntity.class);
        PA_RFC = Registrar.registerBlockEntityMenu("pa_rfc", RFCavityBlockEntity.class);
        PA_QUADRUPOLE =
                Registrar.registerBlockEntityMenu("pa_quadrupole", PAQuadrupoleBlockEntity.class);
        PA_DIPOLE = Registrar.registerBlockEntityMenu("pa_dipole", PADipoleBlockEntity.class);
        PA_DETECTOR = Registrar.registerBlockEntityMenu("pa_detector", PADetectorBlockEntity.class);
        MACHINE_SOLDERING_STATION =
                Registrar.registerBlockEntityMenu(
                        "machine_soldering_station", SolderingBenchBlockEntity.class);
        MACHINE_ARC_WELDER =
                Registrar.registerBlockEntityMenu("machine_arc_welder", ArcWelderBlockEntity.class);
        MACHINE_EPRESS =
                Registrar.registerBlockEntityMenu("machine_epress", ElectricPressBlockEntity.class);
        MACHINE_SHREDDER = Registrar.registerMenu("machine_shredder", ShredderMenu::new);
        MACHINE_FLUID_TANK =
                Registrar.registerBlockEntityMenu("machine_fluidtank", FluidTankBlockEntity.class);
        TURRET_BASE =
                Registrar.registerBlockEntityMenu("turret_base", TurretBaseNTBlockEntity.class);
        BARREL = Registrar.registerBlockEntityMenu("barrel", FluidBarrelBlockEntity.class);
        MASS_STORAGE =
                Registrar.registerBlockEntityMenu("mass_storage", MassStorageBlockEntity.class);
        FILE_CABINET =
                Registrar.registerBlockEntityMenu("filing_cabinet", FileCabinetBlockEntity.class);
        MACHINE_WOOD_BURNER =
                Registrar.registerBlockEntityMenu(
                        "machine_wood_burner", WoodGeneratorBlockEntity.class);
        MACHINE_DIESEL =
                Registrar.registerBlockEntityMenu(
                        "machine_diesel", DieselGeneratorBlockEntity.class);
        MACHINE_RTG =
                Registrar.registerBlockEntityMenu("machine_rtg", IsotopeGeneratorBlockEntity.class);
        MACHINE_TURBINE =
                Registrar.registerBlockEntityMenu(
                        "machine_turbine", CompactTurbineBlockEntity.class);
        MACHINE_LARGE_TURBINE =
                Registrar.registerBlockEntityMenu(
                        "machine_large_turbine", LargeTurbineBlockEntity.class);
        MACHINE_COMBUSTION_ENGINE =
                Registrar.registerBlockEntityMenu(
                        "machine_combustion_engine", CombustionEngineBlockEntity.class);
        MACHINE_TURBINEGAS =
                Registrar.registerBlockEntityMenu(
                        "machine_turbinegas", TurbineGasBlockEntity.class);
        MACHINE_TURBOFAN =
                Registrar.registerBlockEntityMenu("machine_turbofan", TurbofanBlockEntity.class);
        WASTE_DRUM = Registrar.registerMenu("machine_waste_drum", WasteDrumMenu::new);
        STORAGE_DRUM =
                Registrar.registerBlockEntityMenu(
                        "machine_storage_drum", StorageDrumBlockEntity.class);
        BATTERY_SOCKET =
                Registrar.registerBlockEntityMenu(
                        "machine_battery_socket", SocketBlockEntity.class);
        MACHINE_BATTERY =
                Registrar.registerBlockEntityMenu("machine_battery", BatteryBlockEntity.class);
        BATTERY_REDD = Registrar.registerBlockEntityMenu("battery_redd", ReddBlockEntity.class);
        NUKE_MAN = Registrar.registerMenu("nuke_man", NukeManMenu::new);
        NUKE_GADGET = Registrar.registerMenu("nuke_gadget", NukeGadgetMenu::new);
        NUKE_BOY = Registrar.registerMenu("nuke_boy", NukeBoyMenu::new);
        NUKE_MIKE = Registrar.registerMenu("nuke_mike", NukeMikeMenu::new);
        NUKE_TSAR = Registrar.registerMenu("nuke_tsar", NukeTsarMenu::new);
        NUKE_FLEIJA = Registrar.registerMenu("nuke_fleija", NukeFleijaMenu::new);
        NUKE_PROTOTYPE = Registrar.registerMenu("nuke_prototype", NukePrototypeMenu::new);
        NUKE_SOLINIUM = Registrar.registerMenu("nuke_solinium", NukeSoliniumMenu::new);
        NUKE_N2 = Registrar.registerMenu("nuke_n2", NukeN2Menu::new);
        MACHINE_MISSILE_ASSEMBLY =
                Registrar.registerBlockEntityMenu(
                        "machine_missile_assembly", MissileWorkbenchBlockEntity.class);
        LAUNCH_PAD = Registrar.registerBlockEntityMenu("launch_pad", LaunchPadBlockEntity.class);
        LAUNCH_PAD_RUSTED =
                Registrar.registerBlockEntityMenu("launch_pad_rusted", LaunchSiloBlockEntity.class);
        LAUNCH_PAD_LARGE =
                Registrar.registerBlockEntityMenu(
                        "launch_pad_large", MissileErectorBlockEntity.class);
        LAUNCH_TABLE =
                Registrar.registerBlockEntityMenu("launch_table", LaunchTableBlockEntity.class);
        COMPACT_LAUNCHER =
                Registrar.registerBlockEntityMenu(
                        "compact_launcher", CompactLauncherBlockEntity.class);
        NUKE_CUSTOM = Registrar.registerBlockEntityMenu("nuke_custom", CustomNukeBlockEntity.class);
        NUKE_FSTBMB =
                Registrar.registerBlockEntityMenu("nuke_fstbmb", BalefireNukeBlockEntity.class);
        BOMB_MULTI = Registrar.registerBlockEntityMenu("bomb_multi", BombMultiBlockEntity.class);
        SOYUZ_CAPSULE =
                Registrar.registerBlockEntityMenu("soyuz_capsule", CargoCapsuleBlockEntity.class);
        SOYUZ_LAUNCHER =
                Registrar.registerBlockEntityMenu("soyuz_launcher", SoyuzLauncherBlockEntity.class);
        LAUNCHPAD_SOYUZ =
                Registrar.registerBlockEntityMenu(
                        "launchpad_soyuz", LaunchpadSoyuzBlockEntity.class);
        SAT_DOCK = Registrar.registerBlockEntityMenu("sat_dock", SatDockBlockEntity.class);
        RADIO_TORCH_COUNTER =
                Registrar.registerBlockEntityMenu(
                        "radio_torch_counter", RadioCounterBlockEntity.class);
        CABLE_DIODE = Registrar.registerBlockEntityMenu("cable_diode", CableDiodeBlockEntity.class);
        FLUID_PUMP = Registrar.registerBlockEntityMenu("fluid_pump", FluidPumpBlockEntity.class);
        RADIO_REC = Registrar.registerBlockEntityMenu("radio_rec", RadioReceiverBlockEntity.class);
        RADIO_TELEX = Registrar.registerBlockEntityMenu("radio_telex", RadioTelexBlockEntity.class);
        RADIO_AUTOCAL =
                Registrar.registerBlockEntityMenu("radio_autocal", RadioAutocalBlockEntity.class);
        RBMK_GAUGE = Registrar.registerBlockEntityMenu("rbmk_gauge", RBMKGaugeBlockEntity.class);
        RBMK_GRAPH = Registrar.registerBlockEntityMenu("rbmk_graph", RBMKGraphBlockEntity.class);
        RBMK_INDICATOR =
                Registrar.registerBlockEntityMenu("rbmk_indicator", RBMKIndicatorBlockEntity.class);
        RBMK_KEYPAD = Registrar.registerBlockEntityMenu("rbmk_keypad", RBMKKeyPadBlockEntity.class);
        RBMK_LEVER = Registrar.registerBlockEntityMenu("rbmk_lever", RBMKLeverBlockEntity.class);
        RBMK_NUMITRON =
                Registrar.registerBlockEntityMenu("rbmk_numitron", RBMKNumitronBlockEntity.class);
        RBMK_TERMINAL =
                Registrar.registerBlockEntityMenu("rbmk_terminal", RBMKTerminalBlockEntity.class);
        RADIO_TORCH = Registrar.registerBlockEntityMenu("radio_torch", RadioTorchBlockEntity.class);
        DRONE_CRATE = Registrar.registerBlockEntityMenu("drone_crate", DroneCrateBlockEntity.class);
        DRONE_DOCK = Registrar.registerBlockEntityMenu("drone_dock", DroneDockBlockEntity.class);
        DRONE_PROVIDER =
                Registrar.registerBlockEntityMenu("drone_provider", DroneProviderBlockEntity.class);
        DRONE_REQUESTER =
                Registrar.registerBlockEntityMenu(
                        "drone_requester", DroneRequesterBlockEntity.class);
        WEAPON_TABLE = Registrar.registerMenu("weapon_table", WeaponTableMenu::new);
        BOOK_OF = Registrar.registerMenu("book_of_", BookMenu::new);
        LEMEGETON = Registrar.registerMenu("lemegeton", LemegetonMenu::new);
        ARMOR_TABLE = Registrar.registerMenu("armor_table", ArmorTableMenu::new);

        RBMK_ROD = Registrar.registerBlockEntityMenu("rbmk_fuel_rod", RBMKRodBlockEntity.class);
        RBMK_CONTROL =
                Registrar.registerBlockEntityMenu(
                        "rbmk_control", RBMKManualControlBlockEntity.class);
        RBMK_CONTROL_AUTO =
                Registrar.registerBlockEntityMenu(
                        "rbmk_control_auto", RBMKControlAutoBlockEntity.class);
        RBMK_BOILER = Registrar.registerBlockEntityMenu("rbmk_boiler", RBMKBoilerBlockEntity.class);
        RBMK_STORAGE =
                Registrar.registerBlockEntityMenu("rbmk_storage", RBMKStorageBlockEntity.class);
        RBMK_HEATER = Registrar.registerBlockEntityMenu("rbmk_heater", RBMKHeaterBlockEntity.class);
        RBMK_OUTGASSER =
                Registrar.registerBlockEntityMenu(
                        "rbmk_outgasser", RBMKIrradiationColumnBlockEntity.class);
        RBMK_CONSOLE =
                Registrar.registerBlockEntityMenu("rbmk_console", RBMKConsoleBlockEntity.class);
        RBMK_AUTOLOADER =
                Registrar.registerBlockEntityMenu(
                        "rbmk_autoloader", RBMKAutoloaderBlockEntity.class);
        PWR_CONTROLLER =
                Registrar.registerBlockEntityMenu("pwr_controller", PWRControllerBlockEntity.class);

        FURNACE_IRON =
                Registrar.registerBlockEntityMenu("furnace_iron", IronFurnaceBlockEntity.class);
        FURNACE_STEEL =
                Registrar.registerBlockEntityMenu("furnace_steel", SteelFurnaceBlockEntity.class);
        FURNACE_COMBINATION =
                Registrar.registerBlockEntityMenu(
                        "furnace_combination", CombinationFurnaceBlockEntity.class);
        FURNACE_BRICK =
                Registrar.registerBlockEntityMenu(
                        "machine_furnace_brick", BrickFurnaceBlockEntity.class);
        HEATER_OVEN = Registrar.registerBlockEntityMenu("heater_oven", HeaterOvenBlockEntity.class);
        MACHINE_ASHPIT =
                Registrar.registerBlockEntityMenu("machine_ashpit", AshpitBlockEntity.class);
        MACHINE_SIREN = Registrar.registerBlockEntityMenu("machine_siren", SirenBlockEntity.class);
        HEATER_OILBURNER =
                Registrar.registerBlockEntityMenu(
                        "heater_oilburner", HeaterOilburnerBlockEntity.class);
        HEATER_HEATEX =
                Registrar.registerBlockEntityMenu("heater_heatex", CoolingHeatBlockEntity.class);
        MACHINE_CRYSTALLIZER =
                Registrar.registerBlockEntityMenu(
                        "machine_crystallizer", CrystalProcessorBlockEntity.class);
        MACHINE_COKER = Registrar.registerBlockEntityMenu("machine_coker", CokerBlockEntity.class);
        MACHINE_LIQUEFACTOR =
                Registrar.registerBlockEntityMenu(
                        "machine_liquefactor", LiquefactorBlockEntity.class);
        MACHINE_SOLIDIFIER =
                Registrar.registerBlockEntityMenu(
                        "machine_solidifier", SolidifierBlockEntity.class);
        MACHINE_PYROOVEN =
                Registrar.registerBlockEntityMenu("machine_pyrooven", PyroOvenBlockEntity.class);
        MACHINE_VACUUM_DISTILL =
                Registrar.registerBlockEntityMenu(
                        "machine_vacuum_distill", VacuumRefineryBlockEntity.class);
        MACHINE_FUNNEL =
                Registrar.registerBlockEntityMenu(
                        "machine_funnel", ItemCompressionBlockEntity.class);
        MACHINE_MICROWAVE =
                Registrar.registerBlockEntityMenu("machine_microwave", MicrowaveBlockEntity.class);
        MACHINE_SATLINKER =
                Registrar.registerBlockEntityMenu("machine_satlinker", SatLinkerBlockEntity.class);
        MACHINE_SUPERCOMPUTER =
                Registrar.registerBlockEntityMenu(
                        "machine_supercomputer", SuperComputerBlockEntity.class);
        MACHINE_TAPE_DRIVE =
                Registrar.registerBlockEntityMenu("machine_tape_drive", TapeDriveBlockEntity.class);
        MACHINE_KEYFORGE =
                Registrar.registerBlockEntityMenu("machine_keyforge", KeyForgeBlockEntity.class);
        MACHINE_AMMO_PRESS =
                Registrar.registerBlockEntityMenu("machine_ammo_press", AmmoPressBlockEntity.class);

        TRAIN_CARGO_TRAM = Registrar.registerMenu("train_cargo_tram", TrainCargoTramMenu::new);
        TRAIN_CARGO_TRAM_TRAILER =
                Registrar.registerMenu("train_cargo_tram_trailer", TrainCargoTramTrailerMenu::new);
        CART_DESTROYER = Registrar.registerMenu("cart_destroyer", CartDestroyerMenu::new);
        CONTAINMENT_BOX =
                Registrar.registerMenu(
                        "containment_box",
                        (id, inv) ->
                                new ItemBoxMenu(
                                        CONTAINMENT_BOX.get(), id, inv, LeadBoxItem.LAYOUT));
        TOOLBOX =
                Registrar.registerMenu(
                        "toolbox",
                        (id, inv) -> new ItemBoxMenu(TOOLBOX.get(), id, inv, ToolBoxItem.LAYOUT));
        AMMO_BAG =
                Registrar.registerMenu(
                        "ammo_bag",
                        (id, inv) -> new ItemBoxMenu(AMMO_BAG.get(), id, inv, AmmoBagItem.LAYOUT));
        CASING_BAG =
                Registrar.registerMenu(
                        "casing_bag",
                        (id, inv) ->
                                new ItemBoxMenu(CASING_BAG.get(), id, inv, CasingBagItem.LAYOUT));
        PLASTIC_BAG =
                Registrar.registerMenu(
                        "plastic_bag",
                        (id, inv) ->
                                new ItemBoxMenu(PLASTIC_BAG.get(), id, inv, PlasticBagItem.LAYOUT));
        REBAR_PLACER = Registrar.registerMenu("rebar_placer", RebarPlacerMenu::new);
        CATALOG =
                Registrar.registerDataMenu(
                        "catalog",
                        (id, inv, hand) -> new HeldItemMenu(CATALOG.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);
        FLUID_IDENTIFIER =
                Registrar.registerDataMenu(
                        "fluid_identifier",
                        (id, inv, hand) -> new HeldItemMenu(FLUID_IDENTIFIER.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);
        DESIGNATOR_MANUAL =
                Registrar.registerDataMenu(
                        "designator_manual",
                        (id, inv, hand) -> new HeldItemMenu(DESIGNATOR_MANUAL.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);
        RTTY_PAGER =
                Registrar.registerDataMenu(
                        "rtty_pager",
                        (id, inv, hand) -> new HeldItemMenu(RTTY_PAGER.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);
        SATELLITE_INTERFACE =
                Registrar.registerDataMenu(
                        "satellite_interface",
                        (id, inv, hand) ->
                                new HeldItemMenu(SATELLITE_INTERFACE.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);
        TOOL_PRESETS =
                Registrar.registerDataMenu(
                        "tool_presets",
                        (id, inv, hand) -> new HeldItemMenu(TOOL_PRESETS.get(), id, inv, hand),
                        InteractionHand.STREAM_CODEC);

        for (CrateType type : CrateType.values()) {
            CRATE[type.ordinal()] =
                    Registrar.registerMenu(type.id, (id, inv) -> new CrateMenu(id, inv, type));
        }
        for (int tier : ModBlocks.ANVIL_TIERS) {
            ANVIL.put(
                    tier,
                    Registrar.registerMenu(
                            "anvil_" + tier, (id, inv) -> new AnvilMenu(id, inv, tier)));
        }
    }

    public static RegistryHandle<MenuType<CrateMenu>> crateMenuType(CrateType type) {
        return CRATE[type.ordinal()];
    }

    public static RegistryHandle<MenuType<AnvilMenu>> anvilMenuType(int tier) {
        return ANVIL.get(tier);
    }

    @SuppressWarnings("unchecked")
    private static RegistryHandle<MenuType<CrateMenu>>[] newCrateArray() {
        return new RegistryHandle[CrateType.values().length];
    }
}
