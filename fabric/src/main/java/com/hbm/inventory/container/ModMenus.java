// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.blockentity.bomb.BlockEntityBombMulti;
import com.hbm.blockentity.bomb.BlockEntityCompactLauncher;
import com.hbm.blockentity.bomb.BlockEntityLaunchPad;
import com.hbm.blockentity.bomb.BlockEntityLaunchPadLarge;
import com.hbm.blockentity.bomb.BlockEntityLaunchPadRusted;
import com.hbm.blockentity.bomb.BlockEntityLaunchTable;
import com.hbm.blockentity.bomb.BlockEntityNukeBalefire;
import com.hbm.blockentity.bomb.BlockEntityNukeCustom;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.albion.*;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionBreeder;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.blockentity.machine.oil.*;
import com.hbm.blockentity.machine.rbmk.*;
import com.hbm.blockentity.machine.storage.*;
import com.hbm.blockentity.network.BlockEntityCraneBoxer;
import com.hbm.blockentity.network.BlockEntityCraneExtractor;
import com.hbm.blockentity.network.BlockEntityCraneGrabber;
import com.hbm.blockentity.network.BlockEntityCraneInserter;
import com.hbm.blockentity.network.BlockEntityCraneRouter;
import com.hbm.blockentity.network.BlockEntityCraneUnboxer;
import com.hbm.blockentity.network.BlockEntityDroneCrate;
import com.hbm.blockentity.network.BlockEntityDroneDock;
import com.hbm.blockentity.network.BlockEntityDroneProvider;
import com.hbm.blockentity.network.BlockEntityDroneRequester;
import com.hbm.blockentity.network.BlockEntityRadioTorchCounter;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageAccess;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageClutter;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageExporter;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageImporter;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoStorageMono;
import com.hbm.blockentity.network.pneumatic.BlockEntityPneumoTube;
import com.hbm.blockentity.turret.BlockEntityTurretBaseNT;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemPlasticBag;
import com.hbm.items.tool.ItemToolBox;
import com.hbm.registration.FabricRegistrar;
import com.hbm.registration.RegistryHandle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {

    private static final RegistryHandle<MenuType<MenuCrate>>[] CRATE = newCrateArray();
    private static final Map<Integer, RegistryHandle<MenuType<MenuAnvil>>> ANVIL = new HashMap<>();
    public static RegistryHandle<MenuType<MenuTrainCargoTram>> TRAIN_CARGO_TRAM;
    public static RegistryHandle<MenuType<MenuTrainCargoTramTrailer>> TRAIN_CARGO_TRAM_TRAILER;
    public static RegistryHandle<MenuType<MenuCartDestroyer>> CART_DESTROYER;
    public static RegistryHandle<MenuType<MenuItemBox>> CONTAINMENT_BOX;
    public static RegistryHandle<MenuType<MenuItemBox>> TOOLBOX;
    public static RegistryHandle<MenuType<MenuItemBox>> AMMO_BAG;
    public static RegistryHandle<MenuType<MenuItemBox>> CASING_BAG;
    public static RegistryHandle<MenuType<MenuItemBox>> PLASTIC_BAG;
    public static RegistryHandle<MenuType<MenuRebarPlacer>> REBAR_PLACER;
    public static RegistryHandle<MenuType<MenuMachineCentrifuge>> MACHINE_CENTRIFUGE;
    public static RegistryHandle<MenuType<MenuMachineGasCent>> MACHINE_GASCENT;
    public static RegistryHandle<MenuType<MenuMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE;
    public static RegistryHandle<MenuType<MenuMachinePress>> MACHINE_PRESS;
    public static RegistryHandle<MenuType<MenuMachineAutocrafter>> MACHINE_AUTOCRAFTER;
    public static RegistryHandle<MenuType<MenuPneumoTube>> PNEUMATIC_TUBE;
    public static RegistryHandle<MenuType<MenuCraneInserter>> CRANE_INSERTER;
    public static RegistryHandle<MenuType<MenuCraneExtractor>> CRANE_EXTRACTOR;
    public static RegistryHandle<MenuType<MenuCraneGrabber>> CRANE_GRABBER;
    public static RegistryHandle<MenuType<MenuCraneBoxer>> CRANE_BOXER;
    public static RegistryHandle<MenuType<MenuCraneUnboxer>> CRANE_UNBOXER;
    public static RegistryHandle<MenuType<MenuCraneRouter>> CRANE_ROUTER;
    public static RegistryHandle<MenuType<MenuPneumoStorageAccess>> PNEUMATIC_STORAGE_ACCESS;
    public static RegistryHandle<MenuType<MenuPneumoStorageClutter>> PNEUMATIC_STORAGE_CLUTTER;
    public static RegistryHandle<MenuType<MenuPneumoStorageMono>> PNEUMATIC_STORAGE_MONO;
    public static RegistryHandle<MenuType<MenuPneumoStorageImporter>> PNEUMATIC_STORAGE_IMPORTER;
    public static RegistryHandle<MenuType<MenuPneumoStorageExporter>> PNEUMATIC_STORAGE_EXPORTER;
    public static RegistryHandle<MenuType<MenuMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE;
    public static RegistryHandle<MenuType<MenuMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY;
    public static RegistryHandle<MenuType<MenuMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT;
    public static RegistryHandle<MenuType<MenuMachineRockMill>> MACHINE_ROCK_MILL;
    public static RegistryHandle<MenuType<MenuMachineCustom>> MACHINE_CUSTOM;
    public static RegistryHandle<MenuType<MenuMachineRadiolysis>> MACHINE_RADIOLYSIS;
    public static RegistryHandle<MenuType<MenuFusionTorus>> FUSION_TORUS;
    public static RegistryHandle<MenuType<MenuCore>> CORE_CORE;
    public static RegistryHandle<MenuType<MenuCoreEmitter>> CORE_EMITTER;
    public static RegistryHandle<MenuType<MenuCoreReceiver>> CORE_RECEIVER;
    public static RegistryHandle<MenuType<MenuCoreInjector>> CORE_INJECTOR;
    public static RegistryHandle<MenuType<MenuCoreStabilizer>> CORE_STABILIZER;
    public static RegistryHandle<MenuType<MenuFusionKlystron>> FUSION_KLYSTRON;
    public static RegistryHandle<MenuType<MenuFusionBreeder>> FUSION_BREEDER;
    public static RegistryHandle<MenuType<MenuMachinePlasmaForge>> MACHINE_PLASMA_FORGE;
    public static RegistryHandle<MenuType<MenuMachineChemicalFactory>> MACHINE_CHEMICAL_FACTORY;
    public static RegistryHandle<MenuType<MenuMachineElectrolyserFluid>> MACHINE_ELECTROLYSER_FLUID;
    public static RegistryHandle<MenuType<MenuMachineElectrolyserMetal>> MACHINE_ELECTROLYSER_METAL;
    public static RegistryHandle<MenuType<MenuMachineRefinery>> MACHINE_REFINERY;
    public static RegistryHandle<MenuType<MenuMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER;
    public static RegistryHandle<MenuType<MenuMachineHydrotreater>> MACHINE_HYDROTREATER;
    public static RegistryHandle<MenuType<MenuMachinePUREX>> MACHINE_PUREX;
    public static RegistryHandle<MenuType<MenuMachineMixer>> MACHINE_MIXER;
    public static RegistryHandle<MenuType<MenuMachineSILEX>> MACHINE_SILEX;
    public static RegistryHandle<MenuType<MenuICF>> MACHINE_ICF;
    public static RegistryHandle<MenuType<MenuICFPress>> MACHINE_ICF_PRESS;
    public static RegistryHandle<MenuType<MenuMachineOreSlopper>> MACHINE_ORE_SLOPPER;
    public static RegistryHandle<MenuType<MenuMachineAnnihilator>> MACHINE_ANNIHILATOR;
    public static RegistryHandle<MenuType<MenuMachineArcFurnace>> MACHINE_ARC_FURNACE;
    public static RegistryHandle<MenuType<MenuMachineBlastFurnace>> MACHINE_BLAST_FURNACE;
    public static RegistryHandle<MenuType<MenuCrucible>> MACHINE_CRUCIBLE;
    public static RegistryHandle<MenuType<MenuMachineStrandCaster>> MACHINE_STRAND_CASTER;
    public static RegistryHandle<MenuType<MenuMachinePrecAss>> MACHINE_PRECASS;
    public static RegistryHandle<MenuType<MenuMachineMiningLaser>> MACHINE_MINING_LASER;
    public static RegistryHandle<MenuType<MenuForceField>> FORCEFIELD;
    public static RegistryHandle<MenuType<MenuMachineExcavator>> MACHINE_EXCAVATOR;
    public static RegistryHandle<MenuType<MenuMachineCyclotron>> MACHINE_CYCLOTRON;
    public static RegistryHandle<MenuType<MenuMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER;
    public static RegistryHandle<MenuType<MenuMachineRadGen>> MACHINE_RADGEN;
    public static RegistryHandle<MenuType<MenuMachineRadar>> MACHINE_RADAR;
    public static RegistryHandle<MenuType<MenuMachineRadarSlots>> MACHINE_RADAR_SLOTS;
    public static RegistryHandle<MenuType<MenuMachineReactorBreeding>> MACHINE_REACTOR_BREEDING;
    public static RegistryHandle<MenuType<MenuReactorResearch>> REACTOR_RESEARCH;
    public static RegistryHandle<MenuType<MenuReactorControl>> REACTOR_CONTROL;
    public static RegistryHandle<MenuType<MenuMachineRotaryFurnace>> MACHINE_ROTARY_FURNACE;
    public static RegistryHandle<MenuType<MenuFirebox>> HEATER_FIREBOX;
    public static RegistryHandle<MenuType<MenuMachineOilWell>> MACHINE_WELL;
    public static RegistryHandle<MenuType<MenuMachineCompressor>> MACHINE_COMPRESSOR;
    public static RegistryHandle<MenuType<MenuMachineGasFlare>> MACHINE_GAS_FLARE;
    public static RegistryHandle<MenuType<MenuReactorZirnox>> REACTOR_ZIRNOX;
    public static RegistryHandle<MenuType<MenuWatz>> WATZ;
    public static RegistryHandle<MenuType<MenuFEL>> MACHINE_FEL;
    public static RegistryHandle<MenuType<MenuPASource>> PA_SOURCE;
    public static RegistryHandle<MenuType<MenuPARFC>> PA_RFC;
    public static RegistryHandle<MenuType<MenuPAQuadrupole>> PA_QUADRUPOLE;
    public static RegistryHandle<MenuType<MenuPADipole>> PA_DIPOLE;
    public static RegistryHandle<MenuType<MenuPADetector>> PA_DETECTOR;
    public static RegistryHandle<MenuType<MenuMachineSolderingStation>> MACHINE_SOLDERING_STATION;
    public static RegistryHandle<MenuType<MenuMachineArcWelder>> MACHINE_ARC_WELDER;
    public static RegistryHandle<MenuType<MenuMachineEPress>> MACHINE_EPRESS;
    public static RegistryHandle<MenuType<MenuMachineShredder>> MACHINE_SHREDDER;
    public static RegistryHandle<MenuType<MenuMachineFluidTank>> MACHINE_FLUID_TANK;
    public static RegistryHandle<MenuType<MenuTurretBase>> TURRET_BASE;
    public static RegistryHandle<MenuType<MenuBarrel>> BARREL;
    public static RegistryHandle<MenuType<MenuMassStorage>> MASS_STORAGE;
    public static RegistryHandle<MenuType<MenuFileCabinet>> FILE_CABINET;
    public static RegistryHandle<MenuType<MenuMachineWoodBurner>> MACHINE_WOOD_BURNER;
    public static RegistryHandle<MenuType<MenuMachineDiesel>> MACHINE_DIESEL;
    public static RegistryHandle<MenuType<MenuMachineRTG>> MACHINE_RTG;
    public static RegistryHandle<MenuType<MenuMachineTurbine>> MACHINE_TURBINE;
    public static RegistryHandle<MenuType<MenuMachineLargeTurbine>> MACHINE_LARGE_TURBINE;
    public static RegistryHandle<MenuType<MenuMachineCombustionEngine>> MACHINE_COMBUSTION_ENGINE;
    public static RegistryHandle<MenuType<MenuMachineTurbineGas>> MACHINE_TURBINEGAS;
    public static RegistryHandle<MenuType<MenuMachineTurbofan>> MACHINE_TURBOFAN;
    public static RegistryHandle<MenuType<MenuWasteDrum>> WASTE_DRUM;
    public static RegistryHandle<MenuType<MenuStorageDrum>> STORAGE_DRUM;
    public static RegistryHandle<MenuType<MenuBatterySocket>> BATTERY_SOCKET;
    public static RegistryHandle<MenuType<MenuMachineBattery>> MACHINE_BATTERY;
    public static RegistryHandle<MenuType<MenuBatteryREDD>> BATTERY_REDD;
    public static RegistryHandle<MenuType<MenuNukeMan>> NUKE_MAN;
    public static RegistryHandle<MenuType<MenuNukeGadget>> NUKE_GADGET;
    public static RegistryHandle<MenuType<MenuNukeBoy>> NUKE_BOY;
    public static RegistryHandle<MenuType<MenuNukeMike>> NUKE_MIKE;
    public static RegistryHandle<MenuType<MenuNukeTsar>> NUKE_TSAR;
    public static RegistryHandle<MenuType<MenuNukeFleija>> NUKE_FLEIJA;
    public static RegistryHandle<MenuType<MenuNukePrototype>> NUKE_PROTOTYPE;
    public static RegistryHandle<MenuType<MenuNukeSolinium>> NUKE_SOLINIUM;
    public static RegistryHandle<MenuType<MenuNukeN2>> NUKE_N2;
    public static RegistryHandle<MenuType<MenuMachineMissileAssembly>> MACHINE_MISSILE_ASSEMBLY;
    public static RegistryHandle<MenuType<MenuLaunchPad>> LAUNCH_PAD;
    public static RegistryHandle<MenuType<MenuLaunchPadRusted>> LAUNCH_PAD_RUSTED;
    public static RegistryHandle<MenuType<MenuLaunchPadLarge>> LAUNCH_PAD_LARGE;
    public static RegistryHandle<MenuType<MenuLaunchTable>> LAUNCH_TABLE;
    public static RegistryHandle<MenuType<MenuCompactLauncher>> COMPACT_LAUNCHER;
    public static RegistryHandle<MenuType<MenuNukeCustom>> NUKE_CUSTOM;
    public static RegistryHandle<MenuType<MenuNukeFstbmb>> NUKE_FSTBMB;
    public static RegistryHandle<MenuType<MenuBombMulti>> BOMB_MULTI;
    public static RegistryHandle<MenuType<MenuSoyuzCapsule>> SOYUZ_CAPSULE;
    public static RegistryHandle<MenuType<MenuSoyuzLauncher>> SOYUZ_LAUNCHER;
    public static RegistryHandle<MenuType<MenuLaunchpadSoyuz>> LAUNCHPAD_SOYUZ;
    public static RegistryHandle<MenuType<MenuSatDock>> SAT_DOCK;
    public static RegistryHandle<MenuType<MenuRadioTorchCounter>> RADIO_TORCH_COUNTER;
    public static RegistryHandle<MenuType<MenuDroneCrate>> DRONE_CRATE;
    public static RegistryHandle<MenuType<MenuDroneDock>> DRONE_DOCK;
    public static RegistryHandle<MenuType<MenuDroneProvider>> DRONE_PROVIDER;
    public static RegistryHandle<MenuType<MenuDroneRequester>> DRONE_REQUESTER;
    public static RegistryHandle<MenuType<MenuWeaponTable>> WEAPON_TABLE;
    public static RegistryHandle<MenuType<MenuBook>> BOOK_OF;
    public static RegistryHandle<MenuType<MenuLemegeton>> LEMEGETON;
    public static RegistryHandle<MenuType<MenuArmorTable>> ARMOR_TABLE;
    public static RegistryHandle<MenuType<MenuRBMKRod>> RBMK_ROD;
    public static RegistryHandle<MenuType<MenuRBMKControl>> RBMK_CONTROL;
    public static RegistryHandle<MenuType<MenuRBMKControlAuto>> RBMK_CONTROL_AUTO;
    public static RegistryHandle<MenuType<MenuRBMKBoiler>> RBMK_BOILER;
    public static RegistryHandle<MenuType<MenuRBMKStorage>> RBMK_STORAGE;
    public static RegistryHandle<MenuType<MenuRBMKHeater>> RBMK_HEATER;
    public static RegistryHandle<MenuType<MenuRBMKOutgasser>> RBMK_OUTGASSER;
    public static RegistryHandle<MenuType<MenuRBMKConsole>> RBMK_CONSOLE;
    public static RegistryHandle<MenuType<MenuRBMKAutoloader>> RBMK_AUTOLOADER;
    public static RegistryHandle<MenuType<MenuPWR>> PWR_CONTROLLER;
    public static RegistryHandle<MenuType<MenuFurnaceIron>> FURNACE_IRON;
    public static RegistryHandle<MenuType<MenuFurnaceSteel>> FURNACE_STEEL;
    public static RegistryHandle<MenuType<MenuFurnaceCombination>> FURNACE_COMBINATION;
    public static RegistryHandle<MenuType<MenuFurnaceBrick>> FURNACE_BRICK;
    public static RegistryHandle<MenuType<MenuHeaterOven>> HEATER_OVEN;
    public static RegistryHandle<MenuType<MenuMachineAshpit>> MACHINE_ASHPIT;
    public static RegistryHandle<MenuType<MenuMachineSiren>> MACHINE_SIREN;
    public static RegistryHandle<MenuType<MenuMachineOilburner>> HEATER_OILBURNER;
    public static RegistryHandle<MenuType<MenuMachineHeatex>> HEATER_HEATEX;
    public static RegistryHandle<MenuType<MenuMachineCrystallizer>> MACHINE_CRYSTALLIZER;
    public static RegistryHandle<MenuType<MenuMachineCoker>> MACHINE_COKER;
    public static RegistryHandle<MenuType<MenuMachineLiquefactor>> MACHINE_LIQUEFACTOR;
    public static RegistryHandle<MenuType<MenuMachineSolidifier>> MACHINE_SOLIDIFIER;
    public static RegistryHandle<MenuType<MenuMachinePyroOven>> MACHINE_PYROOVEN;
    public static RegistryHandle<MenuType<MenuMachineVacuumDistill>> MACHINE_VACUUM_DISTILL;
    public static RegistryHandle<MenuType<MenuMachineFunnel>> MACHINE_FUNNEL;
    public static RegistryHandle<MenuType<MenuMachineMicrowave>> MACHINE_MICROWAVE;
    public static RegistryHandle<MenuType<MenuMachineSatLinker>> MACHINE_SATLINKER;
    public static RegistryHandle<MenuType<MenuMachineSuperComputer>> MACHINE_SUPERCOMPUTER;
    public static RegistryHandle<MenuType<MenuTapeDrive>> MACHINE_TAPE_DRIVE;
    public static RegistryHandle<MenuType<MenuMachineKeyForge>> MACHINE_KEYFORGE;
    public static RegistryHandle<MenuType<MenuMachineAmmoPress>> MACHINE_AMMO_PRESS;

    private ModMenus() {}

    public static void register() {
        MACHINE_CENTRIFUGE =
                FabricRegistrar.registerMenu("machine_centrifuge", MenuMachineCentrifuge::new);
        MACHINE_GASCENT =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_gascent", BlockEntityMachineGasCent.class);
        MACHINE_ELECTRIC_FURNACE =
                FabricRegistrar.registerMenu(
                        "machine_electric_furnace", MenuMachineElectricFurnace::new);
        MACHINE_PRESS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_press", BlockEntityMachinePress.class);
        MACHINE_AUTOCRAFTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_autocrafter", BlockEntityMachineAutocrafter.class);
        PNEUMATIC_TUBE =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_tube", BlockEntityPneumoTube.class);
        CRANE_INSERTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "crane_inserter", BlockEntityCraneInserter.class);
        CRANE_EXTRACTOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "crane_extractor", BlockEntityCraneExtractor.class);
        CRANE_GRABBER =
                FabricRegistrar.registerBlockEntityMenu(
                        "crane_grabber", BlockEntityCraneGrabber.class);
        CRANE_BOXER =
                FabricRegistrar.registerBlockEntityMenu("crane_boxer", BlockEntityCraneBoxer.class);
        CRANE_UNBOXER =
                FabricRegistrar.registerBlockEntityMenu(
                        "crane_unboxer", BlockEntityCraneUnboxer.class);
        CRANE_ROUTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "crane_router", BlockEntityCraneRouter.class);
        PNEUMATIC_STORAGE_ACCESS =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_storage_access", BlockEntityPneumoStorageAccess.class);
        PNEUMATIC_STORAGE_CLUTTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_storage_clutter", BlockEntityPneumoStorageClutter.class);
        PNEUMATIC_STORAGE_MONO =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_storage_mono", BlockEntityPneumoStorageMono.class);
        PNEUMATIC_STORAGE_IMPORTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_storage_importer", BlockEntityPneumoStorageImporter.class);
        PNEUMATIC_STORAGE_EXPORTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "pneumatic_storage_exporter", BlockEntityPneumoStorageExporter.class);
        MACHINE_ASSEMBLY_MACHINE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_assembly_machine", BlockEntityMachineAssemblyMachine.class);
        MACHINE_ASSEMBLY_FACTORY =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_assembly_factory", BlockEntityMachineAssemblyFactory.class);
        MACHINE_CUSTOM =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_custom", BlockEntityCustomMachine.class);
        MACHINE_CHEMICAL_PLANT =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_chemical_plant", BlockEntityMachineChemicalPlant.class);
        MACHINE_ROCK_MILL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_rockmill", BlockEntityMachineRockMill.class);
        MACHINE_RADIOLYSIS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_radiolysis", BlockEntityMachineRadiolysis.class);
        FUSION_TORUS =
                FabricRegistrar.registerBlockEntityMenu(
                        "fusion_torus", BlockEntityFusionTorus.class);
        CORE_CORE = FabricRegistrar.registerBlockEntityMenu("dfc_core", BlockEntityCore.class);
        CORE_EMITTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "dfc_emitter", BlockEntityCoreEmitter.class);
        CORE_RECEIVER =
                FabricRegistrar.registerBlockEntityMenu(
                        "dfc_receiver", BlockEntityCoreReceiver.class);
        CORE_INJECTOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "dfc_injector", BlockEntityCoreInjector.class);
        CORE_STABILIZER =
                FabricRegistrar.registerBlockEntityMenu(
                        "dfc_stabilizer", BlockEntityCoreStabilizer.class);
        FUSION_KLYSTRON =
                FabricRegistrar.registerBlockEntityMenu(
                        "fusion_klystron", BlockEntityFusionKlystron.class);
        FUSION_BREEDER =
                FabricRegistrar.registerBlockEntityMenu(
                        "fusion_breeder", BlockEntityFusionBreeder.class);
        MACHINE_PLASMA_FORGE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_plasma_forge", BlockEntityFusionPlasmaForge.class);
        MACHINE_CHEMICAL_FACTORY =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_chemical_factory", BlockEntityMachineChemicalFactory.class);
        MACHINE_ELECTROLYSER_FLUID =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_electrolyser_fluid", BlockEntityMachineElectrolyser.class);
        MACHINE_ELECTROLYSER_METAL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_electrolyser_metal", BlockEntityMachineElectrolyser.class);
        MACHINE_REFINERY =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_refinery", BlockEntityMachineRefinery.class);
        MACHINE_CATALYTIC_REFORMER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_catalytic_reformer", BlockEntityMachineCatalyticReformer.class);
        MACHINE_HYDROTREATER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_hydrotreater", BlockEntityMachineHydrotreater.class);
        MACHINE_PUREX =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_purex", BlockEntityMachinePUREX.class);
        MACHINE_MIXER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_mixer", BlockEntityMachineMixer.class);
        MACHINE_SILEX =
                FabricRegistrar.registerBlockEntityMenu("machine_silex", BlockEntitySILEX.class);
        MACHINE_ICF = FabricRegistrar.registerBlockEntityMenu("icf", BlockEntityICF.class);
        MACHINE_ICF_PRESS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_icf_press", BlockEntityICFPress.class);
        MACHINE_ORE_SLOPPER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_ore_slopper", BlockEntityMachineOreSlopper.class);
        MACHINE_ANNIHILATOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_annihilator", BlockEntityMachineAnnihilator.class);
        MACHINE_BLAST_FURNACE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_blast_furnace", BlockEntityMachineBlastFurnace.class);
        MACHINE_CRUCIBLE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_crucible", BlockEntityCrucible.class);
        MACHINE_STRAND_CASTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_strand_caster", BlockEntityMachineStrandCaster.class);
        MACHINE_PRECASS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_precass", BlockEntityMachinePrecAss.class);
        MACHINE_MINING_LASER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_mining_laser", BlockEntityMachineMiningLaser.class);
        FORCEFIELD =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_forcefield", BlockEntityForceField.class);
        MACHINE_EXCAVATOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_excavator", BlockEntityMachineExcavator.class);
        MACHINE_CYCLOTRON =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_cyclotron", BlockEntityMachineCyclotron.class);
        MACHINE_EXPOSURE_CHAMBER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_exposure_chamber", BlockEntityMachineExposureChamber.class);
        MACHINE_RADGEN =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_radgen", BlockEntityMachineRadGen.class);
        MACHINE_RADAR =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_radar", BlockEntityMachineRadar.class);
        MACHINE_RADAR_SLOTS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_radar_slots", BlockEntityMachineRadar.class);
        MACHINE_REACTOR_BREEDING =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_reactor", BlockEntityMachineReactorBreeding.class);
        REACTOR_RESEARCH =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_reactor_small", BlockEntityReactorResearch.class);
        REACTOR_CONTROL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_controller", BlockEntityReactorControl.class);
        MACHINE_ARC_FURNACE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_arc_furnace", BlockEntityMachineArcFurnace.class);
        MACHINE_ROTARY_FURNACE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_rotary_furnace", BlockEntityMachineRotaryFurnace.class);
        HEATER_FIREBOX =
                FabricRegistrar.registerBlockEntityMenu(
                        "heater_firebox", BlockEntityHeaterFirebox.class);
        MACHINE_WELL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_well", BlockEntityOilDrillBase.class);
        MACHINE_COMPRESSOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_compressor", BlockEntityMachineCompressor.class);
        MACHINE_GAS_FLARE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_flare", BlockEntityMachineGasFlare.class);
        REACTOR_ZIRNOX =
                FabricRegistrar.registerBlockEntityMenu(
                        "reactor_zirnox", BlockEntityReactorZirnox.class);
        WATZ = FabricRegistrar.registerBlockEntityMenu("watz", BlockEntityWatz.class);
        MACHINE_FEL = FabricRegistrar.registerBlockEntityMenu("machine_fel", BlockEntityFEL.class);
        PA_SOURCE = FabricRegistrar.registerBlockEntityMenu("pa_source", BlockEntityPASource.class);
        PA_RFC = FabricRegistrar.registerBlockEntityMenu("pa_rfc", BlockEntityPARFC.class);
        PA_QUADRUPOLE =
                FabricRegistrar.registerBlockEntityMenu(
                        "pa_quadrupole", BlockEntityPAQuadrupole.class);
        PA_DIPOLE = FabricRegistrar.registerBlockEntityMenu("pa_dipole", BlockEntityPADipole.class);
        PA_DETECTOR =
                FabricRegistrar.registerBlockEntityMenu("pa_detector", BlockEntityPADetector.class);
        MACHINE_SOLDERING_STATION =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_soldering_station", BlockEntityMachineSolderingStation.class);
        MACHINE_ARC_WELDER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_arc_welder", BlockEntityMachineArcWelder.class);
        MACHINE_EPRESS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_epress", BlockEntityMachineEPress.class);
        MACHINE_SHREDDER =
                FabricRegistrar.registerMenu("machine_shredder", MenuMachineShredder::new);
        MACHINE_FLUID_TANK =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_fluidtank", BlockEntityMachineFluidTank.class);
        TURRET_BASE =
                FabricRegistrar.registerBlockEntityMenu(
                        "turret_base", BlockEntityTurretBaseNT.class);
        BARREL = FabricRegistrar.registerBlockEntityMenu("barrel", BlockEntityBarrel.class);
        MASS_STORAGE =
                FabricRegistrar.registerBlockEntityMenu(
                        "mass_storage", BlockEntityMassStorage.class);
        FILE_CABINET =
                FabricRegistrar.registerBlockEntityMenu(
                        "filing_cabinet", BlockEntityFileCabinet.class);
        MACHINE_WOOD_BURNER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_wood_burner", BlockEntityMachineWoodBurner.class);
        MACHINE_DIESEL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_diesel", BlockEntityMachineDiesel.class);
        MACHINE_RTG =
                FabricRegistrar.registerBlockEntityMenu("machine_rtg", BlockEntityMachineRTG.class);
        MACHINE_TURBINE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_turbine", BlockEntityMachineTurbine.class);
        MACHINE_LARGE_TURBINE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_large_turbine", BlockEntityMachineLargeTurbine.class);
        MACHINE_COMBUSTION_ENGINE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_combustion_engine", BlockEntityMachineCombustionEngine.class);
        MACHINE_TURBINEGAS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_turbinegas", BlockEntityMachineTurbineGas.class);
        MACHINE_TURBOFAN =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_turbofan", BlockEntityMachineTurbofan.class);
        WASTE_DRUM = FabricRegistrar.registerMenu("machine_waste_drum", MenuWasteDrum::new);
        STORAGE_DRUM =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_storage_drum", BlockEntityStorageDrum.class);
        BATTERY_SOCKET =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_battery_socket", BlockEntityBatterySocket.class);
        MACHINE_BATTERY =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_battery", BlockEntityMachineBattery.class);
        BATTERY_REDD =
                FabricRegistrar.registerBlockEntityMenu(
                        "battery_redd", BlockEntityBatteryREDD.class);
        NUKE_MAN = FabricRegistrar.registerMenu("nuke_man", MenuNukeMan::new);
        NUKE_GADGET = FabricRegistrar.registerMenu("nuke_gadget", MenuNukeGadget::new);
        NUKE_BOY = FabricRegistrar.registerMenu("nuke_boy", MenuNukeBoy::new);
        NUKE_MIKE = FabricRegistrar.registerMenu("nuke_mike", MenuNukeMike::new);
        NUKE_TSAR = FabricRegistrar.registerMenu("nuke_tsar", MenuNukeTsar::new);
        NUKE_FLEIJA = FabricRegistrar.registerMenu("nuke_fleija", MenuNukeFleija::new);
        NUKE_PROTOTYPE = FabricRegistrar.registerMenu("nuke_prototype", MenuNukePrototype::new);
        NUKE_SOLINIUM = FabricRegistrar.registerMenu("nuke_solinium", MenuNukeSolinium::new);
        NUKE_N2 = FabricRegistrar.registerMenu("nuke_n2", MenuNukeN2::new);
        MACHINE_MISSILE_ASSEMBLY =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_missile_assembly", BlockEntityMachineMissileAssembly.class);
        LAUNCH_PAD =
                FabricRegistrar.registerBlockEntityMenu("launch_pad", BlockEntityLaunchPad.class);
        LAUNCH_PAD_RUSTED =
                FabricRegistrar.registerBlockEntityMenu(
                        "launch_pad_rusted", BlockEntityLaunchPadRusted.class);
        LAUNCH_PAD_LARGE =
                FabricRegistrar.registerBlockEntityMenu(
                        "launch_pad_large", BlockEntityLaunchPadLarge.class);
        LAUNCH_TABLE =
                FabricRegistrar.registerBlockEntityMenu(
                        "launch_table", BlockEntityLaunchTable.class);
        COMPACT_LAUNCHER =
                FabricRegistrar.registerBlockEntityMenu(
                        "compact_launcher", BlockEntityCompactLauncher.class);
        NUKE_CUSTOM =
                FabricRegistrar.registerBlockEntityMenu("nuke_custom", BlockEntityNukeCustom.class);
        NUKE_FSTBMB =
                FabricRegistrar.registerBlockEntityMenu(
                        "nuke_fstbmb", BlockEntityNukeBalefire.class);
        BOMB_MULTI =
                FabricRegistrar.registerBlockEntityMenu("bomb_multi", BlockEntityBombMulti.class);
        SOYUZ_CAPSULE =
                FabricRegistrar.registerBlockEntityMenu(
                        "soyuz_capsule", BlockEntitySoyuzCapsule.class);
        SOYUZ_LAUNCHER =
                FabricRegistrar.registerBlockEntityMenu(
                        "soyuz_launcher", BlockEntitySoyuzLauncher.class);
        LAUNCHPAD_SOYUZ =
                FabricRegistrar.registerBlockEntityMenu(
                        "launchpad_soyuz", BlockEntityLaunchpadSoyuz.class);
        SAT_DOCK =
                FabricRegistrar.registerBlockEntityMenu(
                        "sat_dock", BlockEntityMachineSatDock.class);
        RADIO_TORCH_COUNTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "radio_torch_counter", BlockEntityRadioTorchCounter.class);
        DRONE_CRATE =
                FabricRegistrar.registerBlockEntityMenu("drone_crate", BlockEntityDroneCrate.class);
        DRONE_DOCK =
                FabricRegistrar.registerBlockEntityMenu("drone_dock", BlockEntityDroneDock.class);
        DRONE_PROVIDER =
                FabricRegistrar.registerBlockEntityMenu(
                        "drone_provider", BlockEntityDroneProvider.class);
        DRONE_REQUESTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "drone_requester", BlockEntityDroneRequester.class);
        WEAPON_TABLE = FabricRegistrar.registerMenu("weapon_table", MenuWeaponTable::new);
        BOOK_OF = FabricRegistrar.registerMenu("book_of_", MenuBook::new);
        LEMEGETON = FabricRegistrar.registerMenu("lemegeton", MenuLemegeton::new);
        ARMOR_TABLE = FabricRegistrar.registerMenu("armor_table", MenuArmorTable::new);

        RBMK_ROD =
                FabricRegistrar.registerBlockEntityMenu("rbmk_fuel_rod", BlockEntityRBMKRod.class);
        RBMK_CONTROL =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_control", BlockEntityRBMKControlManual.class);
        RBMK_CONTROL_AUTO =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_control_auto", BlockEntityRBMKControlAuto.class);
        RBMK_BOILER =
                FabricRegistrar.registerBlockEntityMenu("rbmk_boiler", BlockEntityRBMKBoiler.class);
        RBMK_STORAGE =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_storage", BlockEntityRBMKStorage.class);
        RBMK_HEATER =
                FabricRegistrar.registerBlockEntityMenu("rbmk_heater", BlockEntityRBMKHeater.class);
        RBMK_OUTGASSER =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_outgasser", BlockEntityRBMKOutgasser.class);
        RBMK_CONSOLE =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_console", BlockEntityRBMKConsole.class);
        RBMK_AUTOLOADER =
                FabricRegistrar.registerBlockEntityMenu(
                        "rbmk_autoloader", BlockEntityRBMKAutoloader.class);
        PWR_CONTROLLER =
                FabricRegistrar.registerBlockEntityMenu(
                        "pwr_controller", BlockEntityMachinePWRController.class);

        FURNACE_IRON =
                FabricRegistrar.registerBlockEntityMenu(
                        "furnace_iron", BlockEntityFurnaceIron.class);
        FURNACE_STEEL =
                FabricRegistrar.registerBlockEntityMenu(
                        "furnace_steel", BlockEntityFurnaceSteel.class);
        FURNACE_COMBINATION =
                FabricRegistrar.registerBlockEntityMenu(
                        "furnace_combination", BlockEntityFurnaceCombination.class);
        FURNACE_BRICK =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_furnace_brick", BlockEntityFurnaceBrick.class);
        HEATER_OVEN =
                FabricRegistrar.registerBlockEntityMenu("heater_oven", BlockEntityHeaterOven.class);
        MACHINE_ASHPIT =
                FabricRegistrar.registerBlockEntityMenu("machine_ashpit", BlockEntityAshpit.class);
        MACHINE_SIREN =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_siren", BlockEntityMachineSiren.class);
        HEATER_OILBURNER =
                FabricRegistrar.registerBlockEntityMenu(
                        "heater_oilburner", BlockEntityHeaterOilburner.class);
        HEATER_HEATEX =
                FabricRegistrar.registerBlockEntityMenu(
                        "heater_heatex", BlockEntityHeaterHeatex.class);
        MACHINE_CRYSTALLIZER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_crystallizer", BlockEntityMachineCrystallizer.class);
        MACHINE_COKER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_coker", BlockEntityMachineCoker.class);
        MACHINE_LIQUEFACTOR =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_liquefactor", BlockEntityMachineLiquefactor.class);
        MACHINE_SOLIDIFIER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_solidifier", BlockEntityMachineSolidifier.class);
        MACHINE_PYROOVEN =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_pyrooven", BlockEntityMachinePyroOven.class);
        MACHINE_VACUUM_DISTILL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_vacuum_distill", BlockEntityMachineVacuumDistill.class);
        MACHINE_FUNNEL =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_funnel", BlockEntityMachineFunnel.class);
        MACHINE_MICROWAVE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_microwave", BlockEntityMicrowave.class);
        MACHINE_SATLINKER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_satlinker", BlockEntityMachineSatLinker.class);
        MACHINE_SUPERCOMPUTER =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_supercomputer", BlockEntityMachineSuperComputer.class);
        MACHINE_TAPE_DRIVE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_tape_drive", BlockEntityMachineTapeDrive.class);
        MACHINE_KEYFORGE =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_keyforge", BlockEntityMachineKeyForge.class);
        MACHINE_AMMO_PRESS =
                FabricRegistrar.registerBlockEntityMenu(
                        "machine_ammo_press", BlockEntityMachineAmmoPress.class);

        TRAIN_CARGO_TRAM =
                FabricRegistrar.registerMenu("train_cargo_tram", MenuTrainCargoTram::new);
        TRAIN_CARGO_TRAM_TRAILER =
                FabricRegistrar.registerMenu(
                        "train_cargo_tram_trailer", MenuTrainCargoTramTrailer::new);
        CART_DESTROYER = FabricRegistrar.registerMenu("cart_destroyer", MenuCartDestroyer::new);
        CONTAINMENT_BOX =
                FabricRegistrar.registerMenu(
                        "containment_box",
                        (id, inv) ->
                                new MenuItemBox(
                                        CONTAINMENT_BOX.get(), id, inv, ItemLeadBox.LAYOUT));
        TOOLBOX =
                FabricRegistrar.registerMenu(
                        "toolbox",
                        (id, inv) -> new MenuItemBox(TOOLBOX.get(), id, inv, ItemToolBox.LAYOUT));
        AMMO_BAG =
                FabricRegistrar.registerMenu(
                        "ammo_bag",
                        (id, inv) -> new MenuItemBox(AMMO_BAG.get(), id, inv, ItemAmmoBag.LAYOUT));
        CASING_BAG =
                FabricRegistrar.registerMenu(
                        "casing_bag",
                        (id, inv) ->
                                new MenuItemBox(CASING_BAG.get(), id, inv, ItemCasingBag.LAYOUT));
        PLASTIC_BAG =
                FabricRegistrar.registerMenu(
                        "plastic_bag",
                        (id, inv) ->
                                new MenuItemBox(PLASTIC_BAG.get(), id, inv, ItemPlasticBag.LAYOUT));
        REBAR_PLACER = FabricRegistrar.registerMenu("rebar_placer", MenuRebarPlacer::new);

        for (CrateType type : CrateType.values()) {
            CRATE[type.ordinal()] =
                    FabricRegistrar.registerMenu(
                            type.id, (id, inv) -> new MenuCrate(id, inv, type));
        }
        for (int tier : ModBlocks.ANVIL_TIERS) {
            ANVIL.put(
                    tier,
                    FabricRegistrar.registerMenu(
                            "anvil_" + tier, (id, inv) -> new MenuAnvil(id, inv, tier)));
        }
    }

    public static RegistryHandle<MenuType<MenuCrate>> crateMenuType(CrateType type) {
        return CRATE[type.ordinal()];
    }

    public static RegistryHandle<MenuType<MenuAnvil>> anvilMenuType(int tier) {
        return ANVIL.get(tier);
    }

    @SuppressWarnings("unchecked")
    private static RegistryHandle<MenuType<MenuCrate>>[] newCrateArray() {
        return new RegistryHandle[CrateType.values().length];
    }
}
