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

package com.hbm.compat.computercraft;

import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blockentity.bomb.LaunchPadBlockEntity;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.*;
import com.hbm.blockentity.machine.albion.PADetectorBlockEntity;
import com.hbm.blockentity.machine.albion.PADipoleBlockEntity;
import com.hbm.blockentity.machine.albion.PAQuadrupoleBlockEntity;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity;
import com.hbm.blockentity.machine.albion.RFCavityBlockEntity;
import com.hbm.blockentity.machine.dfc.*;
import com.hbm.blockentity.machine.fusion.FusionBoilerBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.blockentity.machine.oil.CokerBlockEntity;
import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKCoolerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKGaugeBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIndicatorBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodReaSimBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKTerminalBlockEntity;
import com.hbm.blockentity.machine.reactor.ReactorControlBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blockentity.machine.storage.BatteryBlockEntity;
import com.hbm.blockentity.machine.storage.BigAssTankBlockEntity;
import com.hbm.blockentity.machine.storage.CapacitorBlockEntity;
import com.hbm.blockentity.machine.storage.FensuBlockEntity;
import com.hbm.blockentity.machine.storage.FluidBarrelBlockEntity;
import com.hbm.blockentity.machine.storage.FluidTankBlockEntity;
import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.blockentity.machine.storage.OrbusBlockEntity;
import com.hbm.blockentity.machine.storage.ReddBlockEntity;
import com.hbm.blockentity.machine.storage.SocketBlockEntity;
import com.hbm.blockentity.machine.thermal.CompactTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.IronFurnaceBlockEntity;
import com.hbm.blockentity.machine.thermal.LeviathanTurbineBlockEntity;
import com.hbm.blockentity.machine.thermal.SteelFurnaceBlockEntity;
import com.hbm.blockentity.network.CableGaugeBlockEntity;
import com.hbm.blockentity.network.FluidCounterValveBlockEntity;
import com.hbm.blockentity.network.FluidPumpBlockEntity;
import com.hbm.blockentity.network.PipeGaugeBlockEntity;
import com.hbm.blockentity.network.RadioAutocalBlockEntity;
import com.hbm.blockentity.network.RadioReaderBlockEntity;
import com.hbm.blockentity.network.RadioTelexBlockEntity;
import com.hbm.blockentity.network.RadioTorchControllerBlockEntity;
import com.hbm.blockentity.network.RadioTorchReceiverBlockEntity;
import com.hbm.blockentity.network.RadioTorchSenderBlockEntity;
import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.blockentity.ordnance.launch.MissileErectorBlockEntity;
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
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmContracts.OwnerLookup;
import com.hbm.compat.computercraft.albion.AcceleratorDetectorPeripheral;
import com.hbm.compat.computercraft.albion.AcceleratorDipolePeripheral;
import com.hbm.compat.computercraft.albion.AcceleratorSourcePeripheral;
import com.hbm.compat.computercraft.albion.PAQuadrupolePeripheral;
import com.hbm.compat.computercraft.albion.PARFCPeripheral;
import com.hbm.compat.computercraft.bomb.CustomMissileLauncherPeripheral;
import com.hbm.compat.computercraft.bomb.MissileLauncherPeripheral;
import com.hbm.compat.computercraft.fusion.FusionBoilerPeripheral;
import com.hbm.compat.computercraft.fusion.FusionBreederPeripheral;
import com.hbm.compat.computercraft.fusion.FusionKlystronPeripheral;
import com.hbm.compat.computercraft.fusion.FusionMHDTPeripheral;
import com.hbm.compat.computercraft.fusion.FusionReactorPeripheral;
import com.hbm.compat.computercraft.machine.BreedingReactorPeripheral;
import com.hbm.compat.computercraft.machine.ChungusPeripheral;
import com.hbm.compat.computercraft.machine.CokerPeripheral;
import com.hbm.compat.computercraft.machine.CombustionGeneratorPeripheral;
import com.hbm.compat.computercraft.machine.CoreEmitterPeripheral;
import com.hbm.compat.computercraft.machine.CoreInjectorPeripheral;
import com.hbm.compat.computercraft.machine.CoreReceiverPeripheral;
import com.hbm.compat.computercraft.machine.CoreStabilizerPeripheral;
import com.hbm.compat.computercraft.machine.ForceFieldPeripheral;
import com.hbm.compat.computercraft.machine.GasTurbineGeneratorPeripheral;
import com.hbm.compat.computercraft.machine.GeigerPeripheral;
import com.hbm.compat.computercraft.machine.ICFPeripheral;
import com.hbm.compat.computercraft.machine.IndustrialTurbinePeripheral;
import com.hbm.compat.computercraft.machine.LargeTurbinePeripheral;
import com.hbm.compat.computercraft.machine.MicrowavePeripheral;
import com.hbm.compat.computercraft.machine.PileControlPeripheral;
import com.hbm.compat.computercraft.machine.PileLoaderPeripheral;
import com.hbm.compat.computercraft.machine.PressurizedReactorPeripheral;
import com.hbm.compat.computercraft.machine.RadarPeripheral;
import com.hbm.compat.computercraft.machine.ResearchReactorControlPeripheral;
import com.hbm.compat.computercraft.machine.ResearchReactorPeripheral;
import com.hbm.compat.computercraft.machine.SatLinkPeripheral;
import com.hbm.compat.computercraft.machine.SmallTurbinePeripheral;
import com.hbm.compat.computercraft.machine.WatzReactorPeripheral;
import com.hbm.compat.computercraft.machine.ZirnoxReactorPeripheral;
import com.hbm.compat.computercraft.network.AutocalControllerPeripheral;
import com.hbm.compat.computercraft.network.CableGaugePeripheral;
import com.hbm.compat.computercraft.network.CountingValvePeripheral;
import com.hbm.compat.computercraft.network.PipeGaugePeripheral;
import com.hbm.compat.computercraft.network.PumpPeripheral;
import com.hbm.compat.computercraft.network.RadioControllerPeripheral;
import com.hbm.compat.computercraft.network.RadioSignalTorchPeripheral;
import com.hbm.compat.computercraft.network.RadioValueReaderPeripheral;
import com.hbm.compat.computercraft.network.TelexPeripheral;
import com.hbm.compat.computercraft.rbmk.CraneConsolePeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKBoilerColumnPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlManualPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKControlRoomPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKCoolerPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKFuelColumnPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKHeaterColumnPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKOutgasserColumnPeripheral;
import com.hbm.compat.computercraft.rbmk.RBMKPanels;
import com.hbm.compat.computercraft.rbmk.RBMKTerminalScreenPeripheral;
import com.hbm.compat.computercraft.storage.BarrelPeripheral;
import com.hbm.compat.computercraft.storage.BatteryREDDPeripheral;
import com.hbm.compat.computercraft.storage.BatterySocketPeripheral;
import com.hbm.compat.computercraft.storage.CapacitorFlowPeripheral;
import com.hbm.compat.computercraft.storage.MachineBatteryPeripheral;
import com.hbm.compat.computercraft.storage.MachineFluidTankPeripheral;
import com.hbm.compat.computercraft.storage.MassStoragePeripheral;
import com.hbm.compat.computercraft.turret.ArtyPeripheral;
import com.hbm.compat.computercraft.turret.HimarsPeripheral;
import com.hbm.compat.computercraft.turret.TurretControlPeripheral;
import com.hbm.neoforge.compat.computercraft.machine.SmeltingFurnacePeripheral;
import com.hbm.platform.Caps;
import com.hbm.platform.ServerEvents;
import com.hbm.registration.RegistryHandle;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

    private static final Map<BlockEntity, BlockEntityPeripheral<?>> BY_MACHINE =
            new IdentityHashMap<>();
    private static final ArrayList<BlockEntityPeripheral<?>> LIVE = new ArrayList<>();

    record Row<BE extends BlockEntity>(
            Class<BE> type,
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
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
                            MultiblockCoreBlock.PASSIVE_POWER_IN
                                    | MultiblockCoreBlock.PASSIVE_FLUID_IN);
    private static final OwnerLookup RBMK_TOP =
            (level, pos, state) ->
                    state.hasProperty(RBMKBlock.PART)
                                    && state.getValue(RBMKBlock.PART) == RBMKBlock.Part.TOP
                            ? MultiblockSurface.coreOfFoldedCell(level, pos, state)
                            : null;
    private static final Predicate<Direction> DOWN = side -> side == Direction.DOWN;

    static final List<Row<?>> ROWS =
            List.of(
                    proxied(
                            IronFurnaceBlockEntity.class,
                            SmeltingFurnacePeripheral<IronFurnaceBlockEntity>::new,
                            MultiblockSurface::coreOfFoldedCell,
                            ModBlocks.FURNACE_IRON),
                    proxied(
                            SteelFurnaceBlockEntity.class,
                            SmeltingFurnacePeripheral<SteelFurnaceBlockEntity>::new,
                            MultiblockSurface::coreOfFoldedCell,
                            ModBlocks.FURNACE_STEEL),
                    block(
                            PileLoaderBlockEntity.class,
                            PileLoaderPeripheral::new,
                            ModBlocks.PILE_LOADER),
                    block(
                            PileControlBlockEntity.class,
                            PileControlPeripheral::new,
                            ModBlocks.PILE_CONTROL),
                    block(
                            PWRControllerBlockEntity.class,
                            PressurizedReactorPeripheral::new,
                            ModBlocks.PWR_CONTROLLER),
                    block(
                            CompactTurbineBlockEntity.class,
                            SmallTurbinePeripheral::new,
                            ModBlocks.MACHINE_TURBINE),
                    proxied(
                            LargeTurbineBlockEntity.class,
                            LargeTurbinePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_LARGE_TURBINE),
                    proxied(
                            LeviathanTurbineBlockEntity.class,
                            ChungusPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_CHUNGUS),
                    proxied(
                            IndustrialTurbineBlockEntity.class,
                            IndustrialTurbinePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_INDUSTRIAL_TURBINE),
                    proxied(
                            TurbineGasBlockEntity.class,
                            GasTurbineGeneratorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_TURBINEGAS),
                    block(
                            FluidBarrelBlockEntity.class,
                            BarrelPeripheral::new,
                            ModBlocks.BARREL_PLASTIC,
                            ModBlocks.BARREL_STEEL,
                            ModBlocks.BARREL_TCALLOY,
                            ModBlocks.BARREL_ANTIMATTER),
                    proxied(
                            FluidTankBlockEntity.class,
                            MachineFluidTankPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_FLUID_TANK),
                    proxied(
                            OrbusBlockEntity.class,
                            BarrelPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_ORBUS),
                    proxied(
                            BigAssTankBlockEntity.class,
                            BarrelPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BIGASSTANK),
                    block(
                            BatteryBlockEntity.class,
                            MachineBatteryPeripheral::new,
                            ModBlocks.MACHINE_BATTERY),
                    block(
                            MassStorageBlockEntity.class,
                            MassStoragePeripheral::new,
                            ModBlocks.MASS_STORAGE_WOOD,
                            ModBlocks.MASS_STORAGE_IRON,
                            ModBlocks.MASS_STORAGE_DESH,
                            ModBlocks.MASS_STORAGE_TCALLOY),
                    block(
                            FensuBlockEntity.class,
                            MachineBatteryPeripheral::new,
                            ModBlocks.MACHINE_FENSU),
                    proxied(
                            SocketBlockEntity.class,
                            BatterySocketPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BATTERY_SOCKET),
                    proxied(
                            ReddBlockEntity.class,
                            BatteryREDDPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_BATTERY_REDD),
                    block(
                            CapacitorBlockEntity.class,
                            CapacitorFlowPeripheral::new,
                            ModBlocks.CAPACITOR_COPPER),
                    block(
                            CableGaugeBlockEntity.class,
                            CableGaugePeripheral::new,
                            ModBlocks.RED_CABLE_GAUGE),
                    block(
                            PipeGaugeBlockEntity.class,
                            PipeGaugePeripheral::new,
                            ModBlocks.FLUID_DUCT_GAUGE),
                    block(FluidPumpBlockEntity.class, PumpPeripheral::new, ModBlocks.FLUID_PUMP),
                    block(
                            FluidCounterValveBlockEntity.class,
                            CountingValvePeripheral::new,
                            ModBlocks.FLUID_COUNTER_VALVE),
                    block(RadioTelexBlockEntity.class, TelexPeripheral::new, ModBlocks.RADIO_TELEX),
                    block(
                            RadioTorchSenderBlockEntity.class,
                            RadioSignalTorchPeripheral::new,
                            ModBlocks.RADIO_TORCH_SENDER),
                    block(
                            RadioTorchReceiverBlockEntity.class,
                            RadioSignalTorchPeripheral::new,
                            ModBlocks.RADIO_TORCH_RECEIVER),
                    block(
                            RadioTorchControllerBlockEntity.class,
                            RadioControllerPeripheral::new,
                            ModBlocks.RADIO_TORCH_CONTROLLER),
                    block(
                            RadioReaderBlockEntity.class,
                            RadioValueReaderPeripheral::new,
                            ModBlocks.RADIO_TORCH_READER),
                    block(
                            RadioAutocalBlockEntity.class,
                            AutocalControllerPeripheral::new,
                            ModBlocks.RADIO_AUTOCAL),
                    block(
                            DfcEmitterBlockEntity.class,
                            CoreEmitterPeripheral::new,
                            ModBlocks.DFC_EMITTER),
                    block(
                            DfcReceiverBlockEntity.class,
                            CoreReceiverPeripheral::new,
                            ModBlocks.DFC_RECEIVER),
                    block(
                            DfcInjectorBlockEntity.class,
                            CoreInjectorPeripheral::new,
                            ModBlocks.DFC_INJECTOR),
                    block(
                            DfcStabilizerBlockEntity.class,
                            CoreStabilizerPeripheral::new,
                            ModBlocks.DFC_STABILIZER),
                    block(GeigerBlockEntity.class, GeigerPeripheral::new, ModBlocks.GEIGER),
                    proxied(
                            ICFBlockEntity.class,
                            ICFPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_ICF),
                    proxied(
                            CombustionEngineBlockEntity.class,
                            CombustionGeneratorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_COMBUSTION_ENGINE),
                    block(
                            ReactorBreedingBlockEntity.class,
                            BreedingReactorPeripheral::new,
                            ModBlocks.MACHINE_REACTOR_BREEDING),
                    block(RadarBlockEntity.class, RadarPeripheral::new, ModBlocks.MACHINE_RADAR),
                    proxied(
                            RadarLargeBlockEntity.class,
                            RadarPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_RADAR_LARGE),
                    proxied(
                            SatelliteLinkerBlockEntity.class,
                            SatLinkPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_SATLINK),
                    block(
                            RepulsionFieldBlockEntity.class,
                            ForceFieldPeripheral::new,
                            ModBlocks.MACHINE_FORCEFIELD),
                    block(
                            MicrowaveBlockEntity.class,
                            MicrowavePeripheral::new,
                            ModBlocks.MACHINE_MICROWAVE),
                    block(
                            ResearchReactorBlockEntity.class,
                            ResearchReactorPeripheral::new,
                            ModBlocks.REACTOR_RESEARCH),
                    block(
                            ReactorControlBlockEntity.class,
                            ResearchReactorControlPeripheral::new,
                            ModBlocks.MACHINE_CONTROLLER),
                    proxied(
                            ReactorZirnoxBlockEntity.class,
                            ZirnoxReactorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.REACTOR_ZIRNOX),
                    proxied(
                            WatzBlockEntity.class,
                            WatzReactorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.WATZ),
                    proxied(
                            PADetectorBlockEntity.class,
                            AcceleratorDetectorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_DETECTOR),
                    proxied(
                            PADipoleBlockEntity.class,
                            AcceleratorDipolePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_DIPOLE),
                    proxied(
                            PAQuadrupoleBlockEntity.class,
                            PAQuadrupolePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_QUADRUPOLE),
                    proxied(
                            RFCavityBlockEntity.class,
                            PARFCPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_RFC),
                    proxied(
                            PASourceBlockEntity.class,
                            AcceleratorSourcePeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.PA_SOURCE),
                    proxied(
                            FusionBoilerBlockEntity.class,
                            FusionBoilerPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_BOILER),
                    proxied(
                            FusionNeutronBlockEntity.class,
                            FusionBreederPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_BREEDER),
                    proxied(
                            FusionKlystronBlockEntity.class,
                            FusionKlystronPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_KLYSTRON),
                    proxied(
                            FusionMHDTBlockEntity.class,
                            FusionMHDTPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_MHDT),
                    proxied(
                            FusionTorusBlockEntity.class,
                            FusionReactorPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.FUSION_TORUS),
                    proxied(
                            CokerBlockEntity.class,
                            CokerPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.MACHINE_COKER),
                    proxied(
                            LaunchPadBlockEntity.class,
                            MissileLauncherPeripheral::new,
                            PROXY_CELLS,
                            ModBlocks.LAUNCH_PAD),
                    proxied(
                            MissileErectorBlockEntity.class,
                            MissileLauncherPeripheral::new,
                            POWER_FLUID_CELLS,
                            ModBlocks.LAUNCH_PAD_LARGE),
                    block(
                            LaunchTableBlockEntity.class,
                            CustomMissileLauncherPeripheral::new,
                            ModBlocks.LAUNCH_TABLE),
                    block(
                            RBMKRodBlockEntity.class,
                            RBMKFuelColumnPeripheral::new,
                            ModBlocks.RBMK_ROD,
                            ModBlocks.RBMK_ROD_MODERATED),
                    block(
                            RBMKRodReaSimBlockEntity.class,
                            RBMKFuelColumnPeripheral::new,
                            ModBlocks.RBMK_ROD_REASIM,
                            ModBlocks.RBMK_ROD_REASIM_MODERATED),
                    block(
                            RBMKManualControlBlockEntity.class,
                            RBMKControlManualPeripheral::new,
                            ModBlocks.RBMK_CONTROL,
                            ModBlocks.RBMK_CONTROL_MOD,
                            ModBlocks.RBMK_CONTROL_REASIM),
                    block(
                            RBMKControlAutoBlockEntity.class,
                            RBMKControlPeripheral::new,
                            ModBlocks.RBMK_CONTROL_AUTO,
                            ModBlocks.RBMK_CONTROL_REASIM_AUTO),
                    proxied(
                            RBMKBoilerBlockEntity.class,
                            RBMKBoilerColumnPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_BOILER),
                    proxied(
                            RBMKCoolerBlockEntity.class,
                            RBMKCoolerPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_COOLER),
                    proxied(
                            RBMKHeaterBlockEntity.class,
                            RBMKHeaterColumnPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_HEATER),
                    proxied(
                            RBMKIrradiationColumnBlockEntity.class,
                            RBMKOutgasserColumnPeripheral::new,
                            RBMK_TOP,
                            ModBlocks.RBMK_OUTGASSER),
                    block(
                            RBMKConsoleBlockEntity.class,
                            RBMKControlRoomPeripheral::new,
                            ModBlocks.RBMK_CONSOLE),
                    block(
                            RBMKCraneConsoleBlockEntity.class,
                            CraneConsolePeripheral::new,
                            ModBlocks.RBMK_CRANE_CONSOLE),
                    block(
                            RBMKTerminalBlockEntity.class,
                            RBMKTerminalScreenPeripheral::new,
                            ModBlocks.RBMK_TERMINAL),
                    block(
                            RBMKNumitronBlockEntity.class,
                            RBMKPanels::numitron,
                            ModBlocks.RBMK_NUMITRON),
                    block(RBMKLeverBlockEntity.class, RBMKPanels::lever, ModBlocks.RBMK_LEVER),
                    block(RBMKKeyPadBlockEntity.class, RBMKPanels::keyPad, ModBlocks.RBMK_KEY_PAD),
                    block(
                            RBMKIndicatorBlockEntity.class,
                            RBMKPanels::indicator,
                            ModBlocks.RBMK_INDICATOR),
                    block(RBMKGraphBlockEntity.class, RBMKPanels::graph, ModBlocks.RBMK_GRAPH),
                    block(RBMKGaugeBlockEntity.class, RBMKPanels::gauge, ModBlocks.RBMK_GAUGE),
                    turret(
                            TurretChekhovBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_CHEKHOV),
                    turret(
                            TurretFriendlyBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_FRIENDLY),
                    turret(
                            TurretJeremyBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_JEREMY),
                    turret(
                            TurretTauonBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_TAUON),
                    turret(
                            TurretRichardBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_RICHARD),
                    turret(
                            TurretHowardBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_HOWARD),
                    turret(
                            TurretHowardDamagedBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_HOWARD_DAMAGED),
                    turret(
                            TurretMaxwellBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_MAXWELL),
                    turret(
                            TurretFritzBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_FRITZ),
                    turret(
                            TurretSentryBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_SENTRY),
                    turret(
                            TurretSentryDamagedBlockEntity.class,
                            TurretControlPeripheral::new,
                            ModBlocks.TURRET_SENTRY_DAMAGED),
                    turret(TurretArtyBlockEntity.class, ArtyPeripheral::new, ModBlocks.TURRET_ARTY),
                    turret(
                            TurretHIMARSBlockEntity.class,
                            HimarsPeripheral::new,
                            ModBlocks.TURRET_HIMARS));

    private static final List<Row<?>> ADDON_ROWS = new ArrayList<>();
    private static final Map<Class<?>, List<RegistryHandle<? extends Block>>> EXTENDED =
            new IdentityHashMap<>();

    private Peripherals() {}

    static <BE extends BlockEntity> void addon(
            Class<BE> type,
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
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
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, null, null, List.of(blocks));
    }

    @SafeVarargs
    private static <BE extends BlockEntity> Row<BE> proxied(
            Class<BE> type,
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
            OwnerLookup proxies,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, proxies, null, List.of(blocks));
    }

    @SafeVarargs
    private static <BE extends BlockEntity> Row<BE> turret(
            Class<BE> type,
            Function<? super BE, ? extends BlockEntityPeripheral<?>> factory,
            RegistryHandle<? extends Block>... blocks) {
        return new Row<>(type, factory, null, DOWN, List.of(blocks));
    }

    static void init() {
        final Consumer<MinecraftServer> callback = Peripherals::tick;
        ServerEvents.TICK_POST.add(callback);
        final Consumer<MinecraftServer> callback2 =
                server -> {
                    BY_MACHINE.clear();
                    LIVE.clear();
                };
        ServerEvents.STOPPING.add(callback2);
    }

    static void declare(List<RegistryHandle<? extends Block>> roster) {
        Map<Class<?>, Row<?>> byType = new IdentityHashMap<>();
        for (Row<?> row : rows()) {
            if (byType.put(row.type, row) != null) {
                throw new IllegalStateException(row.type.getName() + " has two peripheral rows");
            }
            for (RegistryHandle<? extends Block> block : row.blocks) {
                Caps.pendingBlocks.add(
                        new Caps.BlockRegistration<>(
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
            Caps.pendingBlocks.add(
                    new Caps.BlockRegistration<>(
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
        return row.type.isInstance(owner)
                        && (!interactiveOnly || owner instanceof RadioControlHandler)
                ? of(row, owner)
                : null;
    }

    private static <BE extends BlockEntity> BlockEntityPeripheral<?> of(
            Row<BE> row, BlockEntity be) {
        BlockEntityPeripheral<?> known = BY_MACHINE.get(be);
        if (known != null) return known;
        BlockEntityPeripheral<?> made = row.factory.apply(row.type.cast(be));
        if (made instanceof SnapshotPeripheral<?> snapshot) snapshot.refresh();
        BY_MACHINE.put(be, made);
        LIVE.add(made);
        return made;
    }

    private static void tick(MinecraftServer server) {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            BlockEntityPeripheral<?> peripheral = LIVE.get(i);
            if (peripheral.target.isRemoved()) {
                BY_MACHINE.remove(peripheral.target);
                int last = LIVE.size() - 1;
                LIVE.set(i, LIVE.get(last));
                LIVE.remove(last);
            } else if (peripheral instanceof SnapshotPeripheral<?> snapshot) {
                snapshot.refresh();
            }
        }
    }
}
