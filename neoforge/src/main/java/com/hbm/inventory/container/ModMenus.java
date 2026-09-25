// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.container;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemPlasticBag;
import com.hbm.items.tool.ItemToolBox;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.bomb.BlockEntityBombMulti;
import com.hbm.tileentity.bomb.BlockEntityCompactLauncher;
import com.hbm.tileentity.bomb.BlockEntityLaunchPad;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadLarge;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import com.hbm.tileentity.bomb.BlockEntityNukeBalefire;
import com.hbm.tileentity.bomb.BlockEntityNukeCustom;
import com.hbm.tileentity.machine.*;
import com.hbm.tileentity.machine.albion.*;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.tileentity.machine.oil.*;
import com.hbm.tileentity.machine.rbmk.*;
import com.hbm.tileentity.machine.storage.*;
import com.hbm.tileentity.network.BlockEntityCraneBoxer;
import com.hbm.tileentity.network.BlockEntityCraneExtractor;
import com.hbm.tileentity.network.BlockEntityCraneGrabber;
import com.hbm.tileentity.network.BlockEntityCraneInserter;
import com.hbm.tileentity.network.BlockEntityCraneRouter;
import com.hbm.tileentity.network.BlockEntityCraneUnboxer;
import com.hbm.tileentity.network.BlockEntityDroneCrate;
import com.hbm.tileentity.network.BlockEntityDroneDock;
import com.hbm.tileentity.network.BlockEntityDroneProvider;
import com.hbm.tileentity.network.BlockEntityDroneRequester;
import com.hbm.tileentity.network.BlockEntityRadioTorchCounter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageAccess;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageClutter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageExporter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageImporter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageMono;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import com.hbm.tileentity.turret.BlockEntityTurretBaseNT;
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

    public static void register(IRegistrar r) {
        MACHINE_CENTRIFUGE = r.registerMenu("machine_centrifuge", MenuMachineCentrifuge::new);

        MACHINE_GASCENT =
                r.registerBlockEntityMenu("machine_gascent", BlockEntityMachineGasCent.class);
        MACHINE_ELECTRIC_FURNACE =
                r.registerMenu("machine_electric_furnace", MenuMachineElectricFurnace::new);
        MACHINE_PRESS = r.registerBlockEntityMenu("machine_press", BlockEntityMachinePress.class);

        MACHINE_AUTOCRAFTER =
                r.registerBlockEntityMenu(
                        "machine_autocrafter", BlockEntityMachineAutocrafter.class);

        PNEUMATIC_TUBE = r.registerBlockEntityMenu("pneumatic_tube", BlockEntityPneumoTube.class);

        CRANE_INSERTER =
                r.registerBlockEntityMenu("crane_inserter", BlockEntityCraneInserter.class);
        CRANE_EXTRACTOR =
                r.registerBlockEntityMenu("crane_extractor", BlockEntityCraneExtractor.class);
        CRANE_GRABBER = r.registerBlockEntityMenu("crane_grabber", BlockEntityCraneGrabber.class);
        CRANE_BOXER = r.registerBlockEntityMenu("crane_boxer", BlockEntityCraneBoxer.class);
        CRANE_UNBOXER = r.registerBlockEntityMenu("crane_unboxer", BlockEntityCraneUnboxer.class);
        CRANE_ROUTER = r.registerBlockEntityMenu("crane_router", BlockEntityCraneRouter.class);
        PNEUMATIC_STORAGE_ACCESS =
                r.registerBlockEntityMenu(
                        "pneumatic_storage_access", BlockEntityPneumoStorageAccess.class);
        PNEUMATIC_STORAGE_CLUTTER =
                r.registerBlockEntityMenu(
                        "pneumatic_storage_clutter", BlockEntityPneumoStorageClutter.class);
        PNEUMATIC_STORAGE_MONO =
                r.registerBlockEntityMenu(
                        "pneumatic_storage_mono", BlockEntityPneumoStorageMono.class);
        PNEUMATIC_STORAGE_IMPORTER =
                r.registerBlockEntityMenu(
                        "pneumatic_storage_importer", BlockEntityPneumoStorageImporter.class);
        PNEUMATIC_STORAGE_EXPORTER =
                r.registerBlockEntityMenu(
                        "pneumatic_storage_exporter", BlockEntityPneumoStorageExporter.class);
        MACHINE_ASSEMBLY_MACHINE =
                r.registerBlockEntityMenu(
                        "machine_assembly_machine", BlockEntityMachineAssemblyMachine.class);
        MACHINE_ASSEMBLY_FACTORY =
                r.registerBlockEntityMenu(
                        "machine_assembly_factory", BlockEntityMachineAssemblyFactory.class);
        MACHINE_CUSTOM =
                r.registerBlockEntityMenu("machine_custom", BlockEntityCustomMachine.class);
        MACHINE_CHEMICAL_PLANT =
                r.registerBlockEntityMenu(
                        "machine_chemical_plant", BlockEntityMachineChemicalPlant.class);
        MACHINE_ROCK_MILL =
                r.registerBlockEntityMenu("machine_rockmill", BlockEntityMachineRockMill.class);
        MACHINE_RADIOLYSIS =
                r.registerBlockEntityMenu("machine_radiolysis", BlockEntityMachineRadiolysis.class);
        FUSION_TORUS = r.registerBlockEntityMenu("fusion_torus", BlockEntityFusionTorus.class);
        CORE_CORE = r.registerBlockEntityMenu("dfc_core", BlockEntityCore.class);
        CORE_EMITTER = r.registerBlockEntityMenu("dfc_emitter", BlockEntityCoreEmitter.class);
        CORE_RECEIVER = r.registerBlockEntityMenu("dfc_receiver", BlockEntityCoreReceiver.class);
        CORE_INJECTOR = r.registerBlockEntityMenu("dfc_injector", BlockEntityCoreInjector.class);
        CORE_STABILIZER =
                r.registerBlockEntityMenu("dfc_stabilizer", BlockEntityCoreStabilizer.class);
        FUSION_KLYSTRON =
                r.registerBlockEntityMenu("fusion_klystron", BlockEntityFusionKlystron.class);
        FUSION_BREEDER =
                r.registerBlockEntityMenu("fusion_breeder", BlockEntityFusionBreeder.class);
        MACHINE_PLASMA_FORGE =
                r.registerBlockEntityMenu(
                        "machine_plasma_forge", BlockEntityFusionPlasmaForge.class);
        MACHINE_CHEMICAL_FACTORY =
                r.registerBlockEntityMenu(
                        "machine_chemical_factory", BlockEntityMachineChemicalFactory.class);
        MACHINE_ELECTROLYSER_FLUID =
                r.registerBlockEntityMenu(
                        "machine_electrolyser_fluid", BlockEntityMachineElectrolyser.class);
        MACHINE_ELECTROLYSER_METAL =
                r.registerBlockEntityMenu(
                        "machine_electrolyser_metal", BlockEntityMachineElectrolyser.class);
        MACHINE_REFINERY =
                r.registerBlockEntityMenu("machine_refinery", BlockEntityMachineRefinery.class);
        MACHINE_CATALYTIC_REFORMER =
                r.registerBlockEntityMenu(
                        "machine_catalytic_reformer", BlockEntityMachineCatalyticReformer.class);
        MACHINE_HYDROTREATER =
                r.registerBlockEntityMenu(
                        "machine_hydrotreater", BlockEntityMachineHydrotreater.class);
        MACHINE_PUREX = r.registerBlockEntityMenu("machine_purex", BlockEntityMachinePUREX.class);
        MACHINE_MIXER = r.registerBlockEntityMenu("machine_mixer", BlockEntityMachineMixer.class);
        MACHINE_SILEX = r.registerBlockEntityMenu("machine_silex", BlockEntitySILEX.class);
        MACHINE_ICF = r.registerBlockEntityMenu("icf", BlockEntityICF.class);
        MACHINE_ICF_PRESS =
                r.registerBlockEntityMenu("machine_icf_press", BlockEntityICFPress.class);
        MACHINE_ORE_SLOPPER =
                r.registerBlockEntityMenu(
                        "machine_ore_slopper", BlockEntityMachineOreSlopper.class);
        MACHINE_ANNIHILATOR =
                r.registerBlockEntityMenu(
                        "machine_annihilator", BlockEntityMachineAnnihilator.class);
        MACHINE_BLAST_FURNACE =
                r.registerBlockEntityMenu(
                        "machine_blast_furnace", BlockEntityMachineBlastFurnace.class);
        MACHINE_CRUCIBLE = r.registerBlockEntityMenu("machine_crucible", BlockEntityCrucible.class);
        MACHINE_STRAND_CASTER =
                r.registerBlockEntityMenu(
                        "machine_strand_caster", BlockEntityMachineStrandCaster.class);
        MACHINE_PRECASS =
                r.registerBlockEntityMenu("machine_precass", BlockEntityMachinePrecAss.class);
        MACHINE_MINING_LASER =
                r.registerBlockEntityMenu(
                        "machine_mining_laser", BlockEntityMachineMiningLaser.class);
        FORCEFIELD = r.registerBlockEntityMenu("machine_forcefield", BlockEntityForceField.class);
        MACHINE_EXCAVATOR =
                r.registerBlockEntityMenu("machine_excavator", BlockEntityMachineExcavator.class);
        MACHINE_CYCLOTRON =
                r.registerBlockEntityMenu("machine_cyclotron", BlockEntityMachineCyclotron.class);
        MACHINE_EXPOSURE_CHAMBER =
                r.registerBlockEntityMenu(
                        "machine_exposure_chamber", BlockEntityMachineExposureChamber.class);
        MACHINE_RADGEN =
                r.registerBlockEntityMenu("machine_radgen", BlockEntityMachineRadGen.class);
        MACHINE_RADAR = r.registerBlockEntityMenu("machine_radar", BlockEntityMachineRadar.class);
        MACHINE_RADAR_SLOTS =
                r.registerBlockEntityMenu("machine_radar_slots", BlockEntityMachineRadar.class);
        MACHINE_REACTOR_BREEDING =
                r.registerBlockEntityMenu(
                        "machine_reactor", BlockEntityMachineReactorBreeding.class);
        REACTOR_RESEARCH =
                r.registerBlockEntityMenu(
                        "machine_reactor_small", BlockEntityReactorResearch.class);
        REACTOR_CONTROL =
                r.registerBlockEntityMenu("machine_controller", BlockEntityReactorControl.class);
        MACHINE_ARC_FURNACE =
                r.registerBlockEntityMenu(
                        "machine_arc_furnace", BlockEntityMachineArcFurnace.class);
        MACHINE_ROTARY_FURNACE =
                r.registerBlockEntityMenu(
                        "machine_rotary_furnace", BlockEntityMachineRotaryFurnace.class);
        HEATER_FIREBOX =
                r.registerBlockEntityMenu("heater_firebox", BlockEntityHeaterFirebox.class);

        MACHINE_WELL = r.registerBlockEntityMenu("machine_well", BlockEntityOilDrillBase.class);
        MACHINE_COMPRESSOR =
                r.registerBlockEntityMenu("machine_compressor", BlockEntityMachineCompressor.class);
        MACHINE_GAS_FLARE =
                r.registerBlockEntityMenu("machine_flare", BlockEntityMachineGasFlare.class);
        REACTOR_ZIRNOX =
                r.registerBlockEntityMenu("reactor_zirnox", BlockEntityReactorZirnox.class);
        WATZ = r.registerBlockEntityMenu("watz", BlockEntityWatz.class);
        MACHINE_FEL = r.registerBlockEntityMenu("machine_fel", BlockEntityFEL.class);
        PA_SOURCE = r.registerBlockEntityMenu("pa_source", BlockEntityPASource.class);
        PA_RFC = r.registerBlockEntityMenu("pa_rfc", BlockEntityPARFC.class);
        PA_QUADRUPOLE = r.registerBlockEntityMenu("pa_quadrupole", BlockEntityPAQuadrupole.class);
        PA_DIPOLE = r.registerBlockEntityMenu("pa_dipole", BlockEntityPADipole.class);
        PA_DETECTOR = r.registerBlockEntityMenu("pa_detector", BlockEntityPADetector.class);
        MACHINE_SOLDERING_STATION =
                r.registerBlockEntityMenu(
                        "machine_soldering_station", BlockEntityMachineSolderingStation.class);
        MACHINE_ARC_WELDER =
                r.registerBlockEntityMenu("machine_arc_welder", BlockEntityMachineArcWelder.class);
        MACHINE_EPRESS =
                r.registerBlockEntityMenu("machine_epress", BlockEntityMachineEPress.class);
        MACHINE_SHREDDER = r.registerMenu("machine_shredder", MenuMachineShredder::new);
        MACHINE_FLUID_TANK =
                r.registerBlockEntityMenu("machine_fluidtank", BlockEntityMachineFluidTank.class);
        TURRET_BASE = r.registerBlockEntityMenu("turret_base", BlockEntityTurretBaseNT.class);
        BARREL = r.registerBlockEntityMenu("barrel", BlockEntityBarrel.class);
        MASS_STORAGE = r.registerBlockEntityMenu("mass_storage", BlockEntityMassStorage.class);
        FILE_CABINET = r.registerBlockEntityMenu("filing_cabinet", BlockEntityFileCabinet.class);
        MACHINE_WOOD_BURNER =
                r.registerBlockEntityMenu(
                        "machine_wood_burner", BlockEntityMachineWoodBurner.class);
        MACHINE_DIESEL =
                r.registerBlockEntityMenu("machine_diesel", BlockEntityMachineDiesel.class);
        MACHINE_RTG = r.registerBlockEntityMenu("machine_rtg", BlockEntityMachineRTG.class);
        MACHINE_TURBINE =
                r.registerBlockEntityMenu("machine_turbine", BlockEntityMachineTurbine.class);
        MACHINE_LARGE_TURBINE =
                r.registerBlockEntityMenu(
                        "machine_large_turbine", BlockEntityMachineLargeTurbine.class);
        MACHINE_COMBUSTION_ENGINE =
                r.registerBlockEntityMenu(
                        "machine_combustion_engine", BlockEntityMachineCombustionEngine.class);
        MACHINE_TURBINEGAS =
                r.registerBlockEntityMenu("machine_turbinegas", BlockEntityMachineTurbineGas.class);
        MACHINE_TURBOFAN =
                r.registerBlockEntityMenu("machine_turbofan", BlockEntityMachineTurbofan.class);
        WASTE_DRUM = r.registerMenu("machine_waste_drum", MenuWasteDrum::new);
        STORAGE_DRUM =
                r.registerBlockEntityMenu("machine_storage_drum", BlockEntityStorageDrum.class);
        BATTERY_SOCKET =
                r.registerBlockEntityMenu("machine_battery_socket", BlockEntityBatterySocket.class);
        MACHINE_BATTERY =
                r.registerBlockEntityMenu("machine_battery", BlockEntityMachineBattery.class);
        BATTERY_REDD = r.registerBlockEntityMenu("battery_redd", BlockEntityBatteryREDD.class);
        NUKE_MAN = r.registerMenu("nuke_man", MenuNukeMan::new);
        NUKE_GADGET = r.registerMenu("nuke_gadget", MenuNukeGadget::new);
        NUKE_BOY = r.registerMenu("nuke_boy", MenuNukeBoy::new);
        NUKE_MIKE = r.registerMenu("nuke_mike", MenuNukeMike::new);
        NUKE_TSAR = r.registerMenu("nuke_tsar", MenuNukeTsar::new);
        NUKE_FLEIJA = r.registerMenu("nuke_fleija", MenuNukeFleija::new);
        NUKE_PROTOTYPE = r.registerMenu("nuke_prototype", MenuNukePrototype::new);
        NUKE_SOLINIUM = r.registerMenu("nuke_solinium", MenuNukeSolinium::new);
        NUKE_N2 = r.registerMenu("nuke_n2", MenuNukeN2::new);
        MACHINE_MISSILE_ASSEMBLY =
                r.registerBlockEntityMenu(
                        "machine_missile_assembly", BlockEntityMachineMissileAssembly.class);
        LAUNCH_PAD = r.registerBlockEntityMenu("launch_pad", BlockEntityLaunchPad.class);
        LAUNCH_PAD_RUSTED =
                r.registerBlockEntityMenu("launch_pad_rusted", BlockEntityLaunchPadRusted.class);
        LAUNCH_PAD_LARGE =
                r.registerBlockEntityMenu("launch_pad_large", BlockEntityLaunchPadLarge.class);
        LAUNCH_TABLE = r.registerBlockEntityMenu("launch_table", BlockEntityLaunchTable.class);
        COMPACT_LAUNCHER =
                r.registerBlockEntityMenu("compact_launcher", BlockEntityCompactLauncher.class);
        NUKE_CUSTOM = r.registerBlockEntityMenu("nuke_custom", BlockEntityNukeCustom.class);
        NUKE_FSTBMB = r.registerBlockEntityMenu("nuke_fstbmb", BlockEntityNukeBalefire.class);
        BOMB_MULTI = r.registerBlockEntityMenu("bomb_multi", BlockEntityBombMulti.class);
        SOYUZ_CAPSULE = r.registerBlockEntityMenu("soyuz_capsule", BlockEntitySoyuzCapsule.class);
        SOYUZ_LAUNCHER =
                r.registerBlockEntityMenu("soyuz_launcher", BlockEntitySoyuzLauncher.class);
        LAUNCHPAD_SOYUZ =
                r.registerBlockEntityMenu("launchpad_soyuz", BlockEntityLaunchpadSoyuz.class);
        SAT_DOCK = r.registerBlockEntityMenu("sat_dock", BlockEntityMachineSatDock.class);
        RADIO_TORCH_COUNTER =
                r.registerBlockEntityMenu(
                        "radio_torch_counter", BlockEntityRadioTorchCounter.class);
        DRONE_CRATE = r.registerBlockEntityMenu("drone_crate", BlockEntityDroneCrate.class);
        DRONE_DOCK = r.registerBlockEntityMenu("drone_dock", BlockEntityDroneDock.class);
        DRONE_PROVIDER =
                r.registerBlockEntityMenu("drone_provider", BlockEntityDroneProvider.class);
        DRONE_REQUESTER =
                r.registerBlockEntityMenu("drone_requester", BlockEntityDroneRequester.class);
        WEAPON_TABLE = r.registerMenu("weapon_table", MenuWeaponTable::new);
        BOOK_OF = r.registerMenu("book_of_", MenuBook::new);
        LEMEGETON = r.registerMenu("lemegeton", MenuLemegeton::new);
        ARMOR_TABLE = r.registerMenu("armor_table", MenuArmorTable::new);

        RBMK_ROD = r.registerBlockEntityMenu("rbmk_fuel_rod", BlockEntityRBMKRod.class);
        RBMK_CONTROL =
                r.registerBlockEntityMenu("rbmk_control", BlockEntityRBMKControlManual.class);
        RBMK_CONTROL_AUTO =
                r.registerBlockEntityMenu("rbmk_control_auto", BlockEntityRBMKControlAuto.class);
        RBMK_BOILER = r.registerBlockEntityMenu("rbmk_boiler", BlockEntityRBMKBoiler.class);
        RBMK_STORAGE = r.registerBlockEntityMenu("rbmk_storage", BlockEntityRBMKStorage.class);
        RBMK_HEATER = r.registerBlockEntityMenu("rbmk_heater", BlockEntityRBMKHeater.class);
        RBMK_OUTGASSER =
                r.registerBlockEntityMenu("rbmk_outgasser", BlockEntityRBMKOutgasser.class);
        RBMK_CONSOLE = r.registerBlockEntityMenu("rbmk_console", BlockEntityRBMKConsole.class);
        RBMK_AUTOLOADER =
                r.registerBlockEntityMenu("rbmk_autoloader", BlockEntityRBMKAutoloader.class);
        PWR_CONTROLLER =
                r.registerBlockEntityMenu("pwr_controller", BlockEntityMachinePWRController.class);

        FURNACE_IRON = r.registerBlockEntityMenu("furnace_iron", BlockEntityFurnaceIron.class);
        FURNACE_STEEL = r.registerBlockEntityMenu("furnace_steel", BlockEntityFurnaceSteel.class);
        FURNACE_COMBINATION =
                r.registerBlockEntityMenu(
                        "furnace_combination", BlockEntityFurnaceCombination.class);
        FURNACE_BRICK =
                r.registerBlockEntityMenu("machine_furnace_brick", BlockEntityFurnaceBrick.class);
        HEATER_OVEN = r.registerBlockEntityMenu("heater_oven", BlockEntityHeaterOven.class);
        MACHINE_ASHPIT = r.registerBlockEntityMenu("machine_ashpit", BlockEntityAshpit.class);
        MACHINE_SIREN = r.registerBlockEntityMenu("machine_siren", BlockEntityMachineSiren.class);
        HEATER_OILBURNER =
                r.registerBlockEntityMenu("heater_oilburner", BlockEntityHeaterOilburner.class);
        HEATER_HEATEX = r.registerBlockEntityMenu("heater_heatex", BlockEntityHeaterHeatex.class);
        MACHINE_CRYSTALLIZER =
                r.registerBlockEntityMenu(
                        "machine_crystallizer", BlockEntityMachineCrystallizer.class);
        MACHINE_COKER = r.registerBlockEntityMenu("machine_coker", BlockEntityMachineCoker.class);
        MACHINE_LIQUEFACTOR =
                r.registerBlockEntityMenu(
                        "machine_liquefactor", BlockEntityMachineLiquefactor.class);
        MACHINE_SOLIDIFIER =
                r.registerBlockEntityMenu("machine_solidifier", BlockEntityMachineSolidifier.class);
        MACHINE_PYROOVEN =
                r.registerBlockEntityMenu("machine_pyrooven", BlockEntityMachinePyroOven.class);
        MACHINE_VACUUM_DISTILL =
                r.registerBlockEntityMenu(
                        "machine_vacuum_distill", BlockEntityMachineVacuumDistill.class);
        MACHINE_FUNNEL =
                r.registerBlockEntityMenu("machine_funnel", BlockEntityMachineFunnel.class);
        MACHINE_MICROWAVE =
                r.registerBlockEntityMenu("machine_microwave", BlockEntityMicrowave.class);
        MACHINE_SATLINKER =
                r.registerBlockEntityMenu("machine_satlinker", BlockEntityMachineSatLinker.class);
        MACHINE_SUPERCOMPUTER =
                r.registerBlockEntityMenu(
                        "machine_supercomputer", BlockEntityMachineSuperComputer.class);
        MACHINE_TAPE_DRIVE =
                r.registerBlockEntityMenu("machine_tape_drive", BlockEntityMachineTapeDrive.class);
        MACHINE_KEYFORGE =
                r.registerBlockEntityMenu("machine_keyforge", BlockEntityMachineKeyForge.class);
        MACHINE_AMMO_PRESS =
                r.registerBlockEntityMenu("machine_ammo_press", BlockEntityMachineAmmoPress.class);

        TRAIN_CARGO_TRAM = r.registerMenu("train_cargo_tram", MenuTrainCargoTram::new);
        TRAIN_CARGO_TRAM_TRAILER =
                r.registerMenu("train_cargo_tram_trailer", MenuTrainCargoTramTrailer::new);
        CART_DESTROYER = r.registerMenu("cart_destroyer", MenuCartDestroyer::new);
        CONTAINMENT_BOX =
                r.registerMenu(
                        "containment_box",
                        (id, inv) ->
                                new MenuItemBox(
                                        CONTAINMENT_BOX.get(), id, inv, ItemLeadBox.LAYOUT));
        TOOLBOX =
                r.registerMenu(
                        "toolbox",
                        (id, inv) -> new MenuItemBox(TOOLBOX.get(), id, inv, ItemToolBox.LAYOUT));
        AMMO_BAG =
                r.registerMenu(
                        "ammo_bag",
                        (id, inv) -> new MenuItemBox(AMMO_BAG.get(), id, inv, ItemAmmoBag.LAYOUT));
        CASING_BAG =
                r.registerMenu(
                        "casing_bag",
                        (id, inv) ->
                                new MenuItemBox(CASING_BAG.get(), id, inv, ItemCasingBag.LAYOUT));
        PLASTIC_BAG =
                r.registerMenu(
                        "plastic_bag",
                        (id, inv) ->
                                new MenuItemBox(PLASTIC_BAG.get(), id, inv, ItemPlasticBag.LAYOUT));
        REBAR_PLACER = r.registerMenu("rebar_placer", MenuRebarPlacer::new);

        for (CrateType type : CrateType.values()) {
            CRATE[type.ordinal()] =
                    r.registerMenu(type.id, (id, inv) -> new MenuCrate(id, inv, type));
        }
        for (int tier : ModBlocks.ANVIL_TIERS) {
            ANVIL.put(
                    tier,
                    r.registerMenu("anvil_" + tier, (id, inv) -> new MenuAnvil(id, inv, tier)));
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
