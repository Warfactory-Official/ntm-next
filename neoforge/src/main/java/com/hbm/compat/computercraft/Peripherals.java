// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blockentity.bomb.BlockEntityLaunchPad;
import com.hbm.blockentity.bomb.BlockEntityLaunchPadLarge;
import com.hbm.blockentity.bomb.BlockEntityLaunchTable;
import com.hbm.blockentity.machine.BlockEntityChungus;
import com.hbm.blockentity.machine.BlockEntityCoreEmitter;
import com.hbm.blockentity.machine.BlockEntityCoreInjector;
import com.hbm.blockentity.machine.BlockEntityCoreReceiver;
import com.hbm.blockentity.machine.BlockEntityCoreStabilizer;
import com.hbm.blockentity.machine.BlockEntityGeiger;
import com.hbm.blockentity.machine.BlockEntityICF;
import com.hbm.blockentity.machine.BlockEntityMachineCombustionEngine;
import com.hbm.blockentity.machine.BlockEntityMachineIndustrialTurbine;
import com.hbm.blockentity.machine.BlockEntityMachineLargeTurbine;
import com.hbm.blockentity.machine.BlockEntityMachinePWRController;
import com.hbm.blockentity.machine.BlockEntityMachineRadar;
import com.hbm.blockentity.machine.BlockEntityMachineRadarLarge;
import com.hbm.blockentity.machine.BlockEntityMachineReactorBreeding;
import com.hbm.blockentity.machine.BlockEntityMachineSatLink;
import com.hbm.blockentity.machine.BlockEntityMachineTurbine;
import com.hbm.blockentity.machine.BlockEntityMachineTurbineGas;
import com.hbm.blockentity.machine.BlockEntityMicrowave;
import com.hbm.blockentity.machine.BlockEntityReactorControl;
import com.hbm.blockentity.machine.BlockEntityReactorResearch;
import com.hbm.blockentity.machine.BlockEntityReactorZirnox;
import com.hbm.blockentity.machine.BlockEntityWatz;
import com.hbm.blockentity.machine.albion.BlockEntityPADetector;
import com.hbm.blockentity.machine.albion.BlockEntityPADipole;
import com.hbm.blockentity.machine.albion.BlockEntityPAQuadrupole;
import com.hbm.blockentity.machine.albion.BlockEntityPARFC;
import com.hbm.blockentity.machine.albion.BlockEntityPASource;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionBoiler;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionBreeder;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionMHDT;
import com.hbm.blockentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.blockentity.machine.oil.BlockEntityMachineCoker;
import com.hbm.blockentity.machine.pile.BlockEntityPileControl;
import com.hbm.blockentity.machine.pile.BlockEntityPileLoader;
import com.hbm.blockentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKGauge;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKIndicator;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKOutgasser;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKRodReaSim;
import com.hbm.blockentity.machine.rbmk.BlockEntityRBMKTerminal;
import com.hbm.blockentity.machine.storage.BlockEntityBarrel;
import com.hbm.blockentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.blockentity.machine.storage.BlockEntityBatterySocket;
import com.hbm.blockentity.machine.storage.BlockEntityMachineBattery;
import com.hbm.blockentity.machine.storage.BlockEntityMachineBigAssTank;
import com.hbm.blockentity.machine.storage.BlockEntityMachineCapacitor;
import com.hbm.blockentity.machine.storage.BlockEntityMachineFENSU;
import com.hbm.blockentity.machine.storage.BlockEntityMachineFluidTank;
import com.hbm.blockentity.machine.storage.BlockEntityMachineOrbus;
import com.hbm.blockentity.machine.storage.BlockEntityMassStorage;
import com.hbm.blockentity.network.BlockEntityCableGauge;
import com.hbm.blockentity.network.BlockEntityFluidCounterValve;
import com.hbm.blockentity.network.BlockEntityFluidPump;
import com.hbm.blockentity.network.BlockEntityPipeGauge;
import com.hbm.blockentity.network.BlockEntityRadioAUTOCAL;
import com.hbm.blockentity.network.BlockEntityRadioTelex;
import com.hbm.blockentity.network.BlockEntityRadioTorchController;
import com.hbm.blockentity.network.BlockEntityRadioTorchReader;
import com.hbm.blockentity.network.BlockEntityRadioTorchReceiver;
import com.hbm.blockentity.network.BlockEntityRadioTorchSender;
import com.hbm.blockentity.turret.BlockEntityTurretArty;
import com.hbm.blockentity.turret.BlockEntityTurretChekhov;
import com.hbm.blockentity.turret.BlockEntityTurretFriendly;
import com.hbm.blockentity.turret.BlockEntityTurretFritz;
import com.hbm.blockentity.turret.BlockEntityTurretHIMARS;
import com.hbm.blockentity.turret.BlockEntityTurretHoward;
import com.hbm.blockentity.turret.BlockEntityTurretHowardDamaged;
import com.hbm.blockentity.turret.BlockEntityTurretJeremy;
import com.hbm.blockentity.turret.BlockEntityTurretMaxwell;
import com.hbm.blockentity.turret.BlockEntityTurretRichard;
import com.hbm.blockentity.turret.BlockEntityTurretSentry;
import com.hbm.blockentity.turret.BlockEntityTurretSentryDamaged;
import com.hbm.blockentity.turret.BlockEntityTurretTauon;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts.OwnerLookup;
import com.hbm.compat.computercraft.albion.PADetectorPeripheral;
import com.hbm.compat.computercraft.albion.PADipolePeripheral;
import com.hbm.compat.computercraft.albion.PAQuadrupolePeripheral;
import com.hbm.compat.computercraft.albion.PARFCPeripheral;
import com.hbm.compat.computercraft.albion.PASourcePeripheral;
import com.hbm.compat.computercraft.bomb.LaunchPadPeripheral;
import com.hbm.compat.computercraft.bomb.LaunchTablePeripheral;
import com.hbm.compat.computercraft.fusion.FusionBoilerPeripheral;
import com.hbm.compat.computercraft.fusion.FusionBreederPeripheral;
import com.hbm.compat.computercraft.fusion.FusionKlystronPeripheral;
import com.hbm.compat.computercraft.fusion.FusionMHDTPeripheral;
import com.hbm.compat.computercraft.fusion.FusionTorusPeripheral;
import com.hbm.compat.computercraft.machine.BreedingReactorPeripheral;
import com.hbm.compat.computercraft.machine.ChungusPeripheral;
import com.hbm.compat.computercraft.machine.CokerPeripheral;
import com.hbm.compat.computercraft.machine.CombustionEnginePeripheral;
import com.hbm.compat.computercraft.machine.CoreEmitterPeripheral;
import com.hbm.compat.computercraft.machine.CoreInjectorPeripheral;
import com.hbm.compat.computercraft.machine.CoreReceiverPeripheral;
import com.hbm.compat.computercraft.machine.CoreStabilizerPeripheral;
import com.hbm.compat.computercraft.machine.GasTurbinePeripheral;
import com.hbm.compat.computercraft.machine.GeigerPeripheral;
import com.hbm.compat.computercraft.machine.ICFPeripheral;
import com.hbm.compat.computercraft.machine.IndustrialTurbinePeripheral;
import com.hbm.compat.computercraft.machine.LargeTurbinePeripheral;
import com.hbm.compat.computercraft.machine.MicrowavePeripheral;
import com.hbm.compat.computercraft.machine.PWRControllerPeripheral;
import com.hbm.compat.computercraft.machine.PileControlPeripheral;
import com.hbm.compat.computercraft.machine.PileLoaderPeripheral;
import com.hbm.compat.computercraft.machine.RadarPeripheral;
import com.hbm.compat.computercraft.machine.ReactorControlPeripheral;
import com.hbm.compat.computercraft.machine.ResearchReactorPeripheral;
import com.hbm.compat.computercraft.machine.SatLinkPeripheral;
import com.hbm.compat.computercraft.machine.SmallTurbinePeripheral;
import com.hbm.compat.computercraft.machine.WatzPeripheral;
import com.hbm.compat.computercraft.machine.ZirnoxPeripheral;
import com.hbm.compat.computercraft.network.CableGaugePeripheral;
import com.hbm.compat.computercraft.network.FluidCounterValvePeripheral;
import com.hbm.compat.computercraft.network.FluidPumpPeripheral;
import com.hbm.compat.computercraft.network.PipeGaugePeripheral;
import com.hbm.compat.computercraft.network.RadioAutocalPeripheral;
import com.hbm.compat.computercraft.network.RadioControllerPeripheral;
import com.hbm.compat.computercraft.network.RadioReaderPeripheral;
import com.hbm.compat.computercraft.network.RadioTelexPeripheral;
import com.hbm.compat.computercraft.network.RadioTorchPeripheral;
import com.hbm.compat.computercraft.rbmk.CraneConsolePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKBoilerPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKConsolePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlManualPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKCoolerPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKGaugePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKGraphPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKHeaterPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKIndicatorPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKKeyPadPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKLeverPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKNumitronPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKOutgasserPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKRodPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKTerminalPeripheral;
import com.hbm.compat.computercraft.storage.BarrelPeripheral;
import com.hbm.compat.computercraft.storage.BatteryREDDPeripheral;
import com.hbm.compat.computercraft.storage.BatterySocketPeripheral;
import com.hbm.compat.computercraft.storage.CapacitorPeripheral;
import com.hbm.compat.computercraft.storage.MachineBatteryPeripheral;
import com.hbm.compat.computercraft.storage.MachineFluidTankPeripheral;
import com.hbm.compat.computercraft.storage.MassStoragePeripheral;
import com.hbm.compat.computercraft.turret.ArtyPeripheral;
import com.hbm.compat.computercraft.turret.HimarsPeripheral;
import com.hbm.compat.computercraft.turret.TurretPeripheral;
import com.hbm.platform.NeoForgeCaps;
import com.hbm.platform.NeoForgeServerEvents;
import com.hbm.registration.RegistryHandle;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

final class Peripherals {

    private static final BlockCapability<IPeripheral, Direction> TOKEN = PeripheralCapability.get();

    private static final Map<BlockEntity, SnapshotPeripheral<?>> BY_MACHINE =
            new IdentityHashMap<>();
    private static final ArrayList<SnapshotPeripheral<?>> LIVE = new ArrayList<>();

    record Row<BE extends BlockEntity>(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            @Nullable OwnerLookup proxies,
            @Nullable Predicate<Direction> faces,
            List<RegistryHandle<? extends Block>> blocks) {}

    private static final OwnerLookup PROXY_CELLS = MultiblockSurface::proxyCoreOfCell;
    private static final OwnerLookup POWER_FLUID_CELLS =
            (level, pos, state) ->
                    MultiblockSurface.proxyCoreOfCell(
                            level,
                            pos,
                            state,
                            BlockMultiblockCore.PASSIVE_POWER_IN
                                    | BlockMultiblockCore.PASSIVE_FLUID_IN);
    private static final OwnerLookup RBMK_TOP =
            (level, pos, state) ->
                    state.hasProperty(RBMKBase.PART)
                                    && state.getValue(RBMKBase.PART) == RBMKBase.Part.TOP
                            ? MultiblockSurface.coreOfFoldedCell(level, pos, state)
                            : null;
    private static final Predicate<Direction> DOWN = side -> side == Direction.DOWN;

    static final List<Row<?>> ROWS =
            List.of(
                    block(
                            BlockEntityPileLoader.class,
                            PileLoaderPeripheral::new,
                            ModBlocks.PILE_LOADER),
                    block(
                            BlockEntityPileControl.class,
                            PileControlPeripheral::new,
                            ModBlocks.PILE_CONTROL),
                    block(
                            BlockEntityMachinePWRController.class,
                            PWRControllerPeripheral::new,
                            ModBlocks.PWR_CONTROLLER),
                    block(
                            BlockEntityMachineTurbine.class,
                            SmallTurbinePeripheral::new,
                            ModBlocks.MACHINE_TURBINE),
                    proxied(
                            BlockEntityMachineLargeTurbine.class,
                            LargeTurbinePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_LARGE_TURBINE),
                    proxied(
                            BlockEntityChungus.class,
                            ChungusPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_CHUNGUS),
                    proxied(
                            BlockEntityMachineIndustrialTurbine.class,
                            IndustrialTurbinePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_INDUSTRIAL_TURBINE),
                    proxied(
                            BlockEntityMachineTurbineGas.class,
                            GasTurbinePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_TURBINEGAS),
                    block(
                            BlockEntityBarrel.class,
                            BarrelPeripheral::new,
                            ModBlocks.BARREL_PLASTIC,
                            ModBlocks.BARREL_STEEL,
                            ModBlocks.BARREL_TCALLOY,
                            ModBlocks.BARREL_ANTIMATTER),
                    proxied(
                            BlockEntityMachineFluidTank.class,
                            MachineFluidTankPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_FLUID_TANK),
                    proxied(
                            BlockEntityMachineOrbus.class,
                            BarrelPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_ORBUS),
                    proxied(
                            BlockEntityMachineBigAssTank.class,
                            BarrelPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BIGASSTANK),
                    block(
                            BlockEntityMachineBattery.class,
                            MachineBatteryPeripheral::new,
                            ModBlocks.MACHINE_BATTERY),
                    block(
                            BlockEntityMassStorage.class,
                            MassStoragePeripheral::new,
                            ModBlocks.MASS_STORAGE_WOOD,
                            ModBlocks.MASS_STORAGE_IRON,
                            ModBlocks.MASS_STORAGE_DESH,
                            ModBlocks.MASS_STORAGE_TCALLOY),
                    block(
                            BlockEntityMachineFENSU.class,
                            MachineBatteryPeripheral::new,
                            ModBlocks.MACHINE_FENSU),
                    proxied(
                            BlockEntityBatterySocket.class,
                            BatterySocketPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BATTERY_SOCKET),
                    proxied(
                            BlockEntityBatteryREDD.class,
                            BatteryREDDPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BATTERY_REDD),
                    block(
                            BlockEntityMachineCapacitor.class,
                            CapacitorPeripheral::new,
                            ModBlocks.CAPACITOR_COPPER),
                    block(
                            BlockEntityCableGauge.class,
                            CableGaugePeripheral::new,
                            ModBlocks.RED_CABLE_GAUGE),
                    block(
                            BlockEntityPipeGauge.class,
                            PipeGaugePeripheral::new,
                            ModBlocks.FLUID_DUCT_GAUGE),
                    block(
                            BlockEntityFluidPump.class,
                            FluidPumpPeripheral::new,
                            ModBlocks.FLUID_PUMP),
                    block(
                            BlockEntityFluidCounterValve.class,
                            FluidCounterValvePeripheral::new,
                            ModBlocks.FLUID_COUNTER_VALVE),
                    block(
                            BlockEntityRadioTelex.class,
                            RadioTelexPeripheral::new,
                            ModBlocks.RADIO_TELEX),
                    block(
                            BlockEntityRadioTorchSender.class,
                            RadioTorchPeripheral::new,
                            ModBlocks.RADIO_TORCH_SENDER),
                    block(
                            BlockEntityRadioTorchReceiver.class,
                            RadioTorchPeripheral::new,
                            ModBlocks.RADIO_TORCH_RECEIVER),
                    block(
                            BlockEntityRadioTorchController.class,
                            RadioControllerPeripheral::new,
                            ModBlocks.RADIO_TORCH_CONTROLLER),
                    block(
                            BlockEntityRadioTorchReader.class,
                            RadioReaderPeripheral::new,
                            ModBlocks.RADIO_TORCH_READER),
                    block(
                            BlockEntityRadioAUTOCAL.class,
                            RadioAutocalPeripheral::new,
                            ModBlocks.RADIO_AUTOCAL),
                    block(
                            BlockEntityCoreEmitter.class,
                            CoreEmitterPeripheral::new,
                            ModBlocks.DFC_EMITTER),
                    block(
                            BlockEntityCoreReceiver.class,
                            CoreReceiverPeripheral::new,
                            ModBlocks.DFC_RECEIVER),
                    block(
                            BlockEntityCoreInjector.class,
                            CoreInjectorPeripheral::new,
                            ModBlocks.DFC_INJECTOR),
                    block(
                            BlockEntityCoreStabilizer.class,
                            CoreStabilizerPeripheral::new,
                            ModBlocks.DFC_STABILIZER),
                    block(BlockEntityGeiger.class, GeigerPeripheral::new, ModBlocks.GEIGER),
                    proxied(
                            BlockEntityICF.class,
                            ICFPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_ICF),
                    proxied(
                            BlockEntityMachineCombustionEngine.class,
                            CombustionEnginePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_COMBUSTION_ENGINE),
                    block(
                            BlockEntityMachineReactorBreeding.class,
                            BreedingReactorPeripheral::new,
                            ModBlocks.MACHINE_REACTOR_BREEDING),
                    block(
                            BlockEntityMachineRadar.class,
                            RadarPeripheral::new,
                            ModBlocks.MACHINE_RADAR),
                    proxied(
                            BlockEntityMachineRadarLarge.class,
                            RadarPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_RADAR_LARGE),
                    proxied(
                            BlockEntityMachineSatLink.class,
                            SatLinkPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_SATLINK),
                    block(
                            BlockEntityMicrowave.class,
                            MicrowavePeripheral::new,
                            ModBlocks.MACHINE_MICROWAVE),
                    block(
                            BlockEntityReactorResearch.class,
                            ResearchReactorPeripheral::new,
                            ModBlocks.REACTOR_RESEARCH),
                    block(
                            BlockEntityReactorControl.class,
                            ReactorControlPeripheral::new,
                            ModBlocks.MACHINE_CONTROLLER),
                    proxied(
                            BlockEntityReactorZirnox.class,
                            ZirnoxPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.REACTOR_ZIRNOX),
                    proxied(
                            BlockEntityWatz.class,
                            WatzPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.WATZ),
                    proxied(
                            BlockEntityPADetector.class,
                            PADetectorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_DETECTOR),
                    proxied(
                            BlockEntityPADipole.class,
                            PADipolePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_DIPOLE),
                    proxied(
                            BlockEntityPAQuadrupole.class,
                            PAQuadrupolePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_QUADRUPOLE),
                    proxied(
                            BlockEntityPARFC.class,
                            PARFCPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_RFC),
                    proxied(
                            BlockEntityPASource.class,
                            PASourcePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_SOURCE),
                    proxied(
                            BlockEntityFusionBoiler.class,
                            FusionBoilerPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_BOILER),
                    proxied(
                            BlockEntityFusionBreeder.class,
                            FusionBreederPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_BREEDER),
                    proxied(
                            BlockEntityFusionKlystron.class,
                            FusionKlystronPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_KLYSTRON),
                    proxied(
                            BlockEntityFusionMHDT.class,
                            FusionMHDTPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_MHDT),
                    proxied(
                            BlockEntityFusionTorus.class,
                            FusionTorusPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_TORUS),
                    proxied(
                            BlockEntityMachineCoker.class,
                            CokerPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_COKER),
                    proxied(
                            BlockEntityLaunchPad.class,
                            LaunchPadPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.LAUNCH_PAD),
                    proxied(
                            BlockEntityLaunchPadLarge.class,
                            LaunchPadPeripheral::new,
                            POWER_FLUID_CELLS,
                            ModBlocks.LAUNCH_PAD_LARGE),
                    block(
                            BlockEntityLaunchTable.class,
                            LaunchTablePeripheral::new,
                            ModBlocks.LAUNCH_TABLE),
                    block(
                            BlockEntityRBMKRod.class,
                            RBMKRodPeripheral::new,
                            ModBlocks.RBMK_ROD,
                            ModBlocks.RBMK_ROD_MODERATED),
                    block(
                            BlockEntityRBMKRodReaSim.class,
                            RBMKRodPeripheral::new,
                            ModBlocks.RBMK_ROD_REASIM,
                            ModBlocks.RBMK_ROD_REASIM_MODERATED),
                    block(
                            BlockEntityRBMKControlManual.class,
                            RBMKControlManualPeripheral::new,
                            ModBlocks.RBMK_CONTROL,
                            ModBlocks.RBMK_CONTROL_MOD,
                            ModBlocks.RBMK_CONTROL_REASIM),
                    block(
                            BlockEntityRBMKControlAuto.class,
                            RBMKControlPeripheral::new,
                            ModBlocks.RBMK_CONTROL_AUTO,
                            ModBlocks.RBMK_CONTROL_REASIM_AUTO),
                    proxied(
                            BlockEntityRBMKBoiler.class,
                            RBMKBoilerPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_BOILER),
                    proxied(
                            BlockEntityRBMKCooler.class,
                            RBMKCoolerPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_COOLER),
                    proxied(
                            BlockEntityRBMKHeater.class,
                            RBMKHeaterPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_HEATER),
                    proxied(
                            BlockEntityRBMKOutgasser.class,
                            RBMKOutgasserPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_OUTGASSER),
                    block(
                            BlockEntityRBMKConsole.class,
                            RBMKConsolePeripheral::new,
                            ModBlocks.RBMK_CONSOLE),
                    block(
                            BlockEntityCraneConsole.class,
                            CraneConsolePeripheral::new,
                            ModBlocks.RBMK_CRANE_CONSOLE),
                    block(
                            BlockEntityRBMKTerminal.class,
                            RBMKTerminalPeripheral::new,
                            ModBlocks.RBMK_TERMINAL),
                    block(
                            BlockEntityRBMKNumitron.class,
                            RBMKNumitronPeripheral::new,
                            ModBlocks.RBMK_NUMITRON),
                    block(
                            BlockEntityRBMKLever.class,
                            RBMKLeverPeripheral::new,
                            ModBlocks.RBMK_LEVER),
                    block(
                            BlockEntityRBMKKeyPad.class,
                            RBMKKeyPadPeripheral::new,
                            ModBlocks.RBMK_KEY_PAD),
                    block(
                            BlockEntityRBMKIndicator.class,
                            RBMKIndicatorPeripheral::new,
                            ModBlocks.RBMK_INDICATOR),
                    block(
                            BlockEntityRBMKGraph.class,
                            RBMKGraphPeripheral::new,
                            ModBlocks.RBMK_GRAPH),
                    block(
                            BlockEntityRBMKGauge.class,
                            RBMKGaugePeripheral::new,
                            ModBlocks.RBMK_GAUGE),
                    turret(
                            BlockEntityTurretChekhov.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_CHEKHOV),
                    turret(
                            BlockEntityTurretFriendly.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_FRIENDLY),
                    turret(
                            BlockEntityTurretJeremy.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_JEREMY),
                    turret(
                            BlockEntityTurretTauon.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_TAUON),
                    turret(
                            BlockEntityTurretRichard.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_RICHARD),
                    turret(
                            BlockEntityTurretHoward.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_HOWARD),
                    turret(
                            BlockEntityTurretHowardDamaged.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_HOWARD_DAMAGED),
                    turret(
                            BlockEntityTurretMaxwell.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_MAXWELL),
                    turret(
                            BlockEntityTurretFritz.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_FRITZ),
                    turret(
                            BlockEntityTurretSentry.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_SENTRY),
                    turret(
                            BlockEntityTurretSentryDamaged.class,
                            TurretPeripheral::new,
                            ModBlocks.TURRET_SENTRY_DAMAGED),
                    turret(BlockEntityTurretArty.class, ArtyPeripheral::new, ModBlocks.TURRET_ARTY),
                    turret(
                            BlockEntityTurretHIMARS.class,
                            HimarsPeripheral::new,
                            ModBlocks.TURRET_HIMARS));

    private static final List<Row<?>> ADDON_ROWS = new ArrayList<>();
    private static final Map<Class<?>, List<RegistryHandle<? extends Block>>> EXTENDED =
            new IdentityHashMap<>();

    private Peripherals() {}

    static <BE extends BlockEntity> void addon(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            boolean proxied,
            List<RegistryHandle<? extends Block>> blocks) {
        ADDON_ROWS.add(new Row<>(type, factory, proxied ? PROXY_CELLS : null, null, blocks));
    }

    static void extend(
            Class<? extends BlockEntity> type, List<RegistryHandle<? extends Block>> blocks) {
        EXTENDED.computeIfAbsent(type, ignored -> new ArrayList<>()).addAll(blocks);
    }

    static List<Row<?>> rows() {
        List<Row<?>> rows = new ArrayList<>(ROWS.size() + ADDON_ROWS.size());
        for (Row<?> row : ROWS) rows.add(extended(row));
        for (Row<?> row : ADDON_ROWS) rows.add(extended(row));
        return rows;
    }

    private static <BE extends BlockEntity> Row<BE> extended(Row<BE> row) {
        List<RegistryHandle<? extends Block>> more = EXTENDED.get(row.type);
        if (more == null) return row;
        List<RegistryHandle<? extends Block>> blocks = new ArrayList<>(row.blocks);
        blocks.addAll(more);
        return new Row<>(row.type, row.factory, row.proxies, row.faces, List.copyOf(blocks));
    }

    @SafeVarargs
    private static <BE extends BlockEntity> Row<BE> block(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, null, null, List.of(blocks));
    }

    @SafeVarargs
    private static <BE extends BlockEntity> Row<BE> proxied(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            OwnerLookup proxies,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, proxies, null, List.of(blocks));
    }

    @SafeVarargs
    private static <BE extends BlockEntity> Row<BE> turret(
            Class<BE> type,
            Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, null, DOWN, List.of(blocks));
    }

    static void init() {
        NeoForgeServerEvents.onServerTickPost(Peripherals::tick);
        NeoForgeServerEvents.onServerStopping(
                server -> {
                    BY_MACHINE.clear();
                    LIVE.clear();
                });
    }

    static void declare(List<RegistryHandle<? extends Block>> roster) {
        Map<Class<?>, Row<?>> byType = new IdentityHashMap<>();
        for (Row<?> row : rows()) {
            if (byType.put(row.type, row) != null) {
                throw new IllegalStateException(row.type.getName() + " has two peripheral rows");
            }
            for (RegistryHandle<? extends Block> block : row.blocks) {
                NeoForgeCaps.pendingBlocks.add(
                        new NeoForgeCaps.BlockRegistration<>(
                                TOKEN,
                                (Supplier<? extends Block>) block,
                                (level, pos, state, be, side) ->
                                        atBlock(row, level, pos, state, be, side)));
            }
        }
        for (Class<?> type : EXTENDED.keySet()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException(type.getName() + " extends no peripheral row");
            }
        }
        for (RegistryHandle<? extends Block> cell : ModBlocks.cellHandles()) {
            NeoForgeCaps.pendingBlocks.add(
                    new NeoForgeCaps.BlockRegistration<>(
                            TOKEN,
                            (Supplier<? extends Block>) cell,
                            (level, pos, state, be, side) ->
                                    atCell(byType, level, pos, state, side)));
        }
    }

    private static boolean opens(Row<?> row, @Nullable Direction side) {
        return row.faces == null || side == null || row.faces.test(side);
    }

    private static @Nullable IPeripheral atBlock(
            Row<?> row,
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity be,
            @Nullable Direction side) {
        if (level.isClientSide() || !opens(row, side)) return null;
        if (MultiblockSurface.isFoldedCell(state)) return proxy(row, level, pos, state);
        return row.type.isInstance(be) ? of(row, be) : null;
    }

    private static @Nullable IPeripheral atCell(
            Map<Class<?>, Row<?>> byType,
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side) {
        if (level.isClientSide()) return null;
        BlockPos core = MultiblockSurface.coreOfFoldedCell(level, pos, state);
        BlockEntity owner = core == null ? null : level.getBlockEntity(core);
        Row<?> row = owner == null ? null : byType.get(owner.getClass());
        return row == null || !opens(row, side) ? null : proxy(row, level, pos, state);
    }

    private static @Nullable IPeripheral proxy(
            Row<?> row, Level level, BlockPos pos, BlockState state) {
        if (row.proxies == null) return null;
        BlockPos core = row.proxies.ownerOf(level, pos, state);
        boolean interactiveOnly = core == null;
        if (interactiveOnly) core = MultiblockSurface.rorCoreOfCell(level, pos, state);
        BlockEntity owner = core == null ? null : level.getBlockEntity(core);
        return row.type.isInstance(owner) && (!interactiveOnly || owner instanceof IRORInteractive)
                ? of(row, owner)
                : null;
    }

    private static <BE extends BlockEntity> SnapshotPeripheral<?> of(Row<BE> row, BlockEntity be) {
        SnapshotPeripheral<?> known = BY_MACHINE.get(be);
        if (known != null) return known;
        SnapshotPeripheral<?> made = row.factory.apply(row.type.cast(be));
        made.refresh();
        BY_MACHINE.put(be, made);
        LIVE.add(made);
        return made;
    }

    private static void tick(MinecraftServer server) {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            SnapshotPeripheral<?> peripheral = LIVE.get(i);
            if (peripheral.target().isRemoved()) {
                BY_MACHINE.remove(peripheral.target());
                int last = LIVE.size() - 1;
                LIVE.set(i, LIVE.get(last));
                LIVE.remove(last);
            } else {
                peripheral.refresh();
            }
        }
    }
}
