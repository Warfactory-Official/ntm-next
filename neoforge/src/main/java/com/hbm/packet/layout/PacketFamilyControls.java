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

package com.hbm.packet.layout;

import com.hbm.packet.PayloadHandlerContext;
import com.hbm.packet.PayloadHandlerThread;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.platform.Network;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

import static com.hbm.lib.internal.UnsafeHolder.BA_BASE;
import static com.hbm.lib.internal.UnsafeHolder.U;

public final class PacketFamilyControls {
    public static final Type<EncodedPayload> TYPE = new Type<>(Identifier.parse("hbm:c"));
    public static final int MIN_BODY_BYTES = 2;
    public static final int MAX_BODY_BYTES = 32767;
    public static final StreamCodec<ByteBuf, EncodedPayload> STREAM_CODEC =
            EncodedPayload.variableCodec(TYPE, MIN_BODY_BYTES, MAX_BODY_BYTES, false);

    private PacketFamilyControls() {}

    public static void register() {
        Network.serverbound.add(
                new Network.Registration<>(
                        TYPE,
                        STREAM_CODEC,
                        PacketFamilyControls::handle,
                        PayloadHandlerThread.MAIN));
    }

    private static void handle(EncodedPayload payload, PayloadHandlerContext context) {
        try {
            receive(payload.buffer(), context);
        } finally {
            payload.close();
        }
    }

    public static void receive(ByteBuf decodedBuffer, PayloadHandlerContext context) {
        int bufferStart = decodedBuffer.readerIndex();
        int bodyBytes = decodedBuffer.readableBytes();
        Object storageBase;
        long storageAddress;
        if (decodedBuffer.hasArray()) {
            storageBase = decodedBuffer.array();
            storageAddress = BA_BASE + decodedBuffer.arrayOffset() + bufferStart;
        } else {
            storageBase = null;
            storageAddress = decodedBuffer.memoryAddress() + bufferStart;
        }
        switch (U.getByte(storageBase, storageAddress) & 255) {
            case 0 ->
                    ReactorControlFunctionLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 1 ->
                    ReactorControlCurveLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 2 ->
                    RBMKManualTargetLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 3 ->
                    RBMKManualColorLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 4 ->
                    RBMKAutoFunctionLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 5 ->
                    RBMKAutoCurveLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 6 ->
                    RBMKAutoloaderStepLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 7 ->
                    RBMKBoilerCompressorLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 8 ->
                    RBMKConsoleManualLevelLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 9 ->
                    RBMKConsoleCycleScreenLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 10 ->
                    RBMKConsoleAssignColumnsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 11 ->
                    RBMKConsoleRodColorLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 12 ->
                    RBMKConsoleCompressorsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 13 ->
                    RBMKGaugeSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 14 ->
                    RBMKGraphSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 15 ->
                    RBMKIndicatorSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 16 ->
                    RBMKKeyPadSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 17 ->
                    RBMKLeverSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 18 ->
                    RBMKNumitronSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 19 ->
                    RBMKTerminalCommandLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 20 ->
                    ZirnoxToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 21 ->
                    ZirnoxVentLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 22 ->
                    PWRRodTargetLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 23 ->
                    WatzLockLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 24 ->
                    FELToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 25 ->
                    CoreEmitterWattsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 26 ->
                    CoreEmitterToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 27 ->
                    CoreStabilizerWattsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 28 ->
                    FusionKlystronOutputLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 29 ->
                    FusionTorusRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 30 ->
                    FusionPlasmaForgeRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 31 ->
                    PADipoleDirectionLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 32 ->
                    PADipoleThresholdLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 33 ->
                    PASourceCancelLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 34 ->
                    RustLaunchPadReleaseLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 35 ->
                    LaunchTablePadSizeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 36 ->
                    BalefireStartLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 37 ->
                    BalefireTimerLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 38 ->
                    CrucibleRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 39 ->
                    ForceFieldToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 40 ->
                    HeatexCoolAmountLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 41 ->
                    HeatexDelayLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 42 ->
                    OilburnerToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 43 ->
                    SoyuzPadCargoLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 44 ->
                    SoyuzPadLaunchLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 45 ->
                    AmmoPressRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 46 ->
                    AnnihilatorPoolLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 47 ->
                    ArcFurnaceLiquidModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 48 ->
                    AssemblyFactoryRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 49 ->
                    AssemblyMachineRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 50 ->
                    ChemicalFactoryRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 51 ->
                    ChemicalPlantRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 52 ->
                    CombustionEngineSettingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 53 ->
                    CompressorSettingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 54 ->
                    DieselToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 55 ->
                    ExcavatorSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 56 ->
                    FunnelCycleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 57 ->
                    MiningLaserToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 58 ->
                    MissileAssemblyBuildLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 59 ->
                    MixerCycleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 60 ->
                    PurexRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 61 ->
                    PrecAssRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 62 ->
                    RockMillRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 63 ->
                    SolderingStationToggleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 64 ->
                    SuperComputerRecipeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 65 ->
                    TurbineGasSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 66 ->
                    WoodBurnerPowerLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 67 ->
                    WoodBurnerFuelModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 68 ->
                    MicrowaveSpeedLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 69 ->
                    RadioReceiverSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 70 ->
                    SilexVoidLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 71 ->
                    SoyuzLauncherModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 72 ->
                    SoyuzLauncherStartLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 73 ->
                    GasFlareValveLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 74 ->
                    GasFlareBurnModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 75 ->
                    BarrelModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 76 ->
                    BatterySocketRedLowLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 77 ->
                    BatterySocketRedHighLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 78 ->
                    BatterySocketPriorityLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 79 ->
                    MachineBatteryRedLowLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 80 ->
                    MachineBatteryRedHighLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 81 ->
                    MachineBatteryPriorityLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 82 ->
                    MachineFluidTankModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 83 ->
                    MassStorageOutputLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 84 ->
                    MassStorageProvideLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 85 ->
                    CableDiodeSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 86 ->
                    CraneBoxerCycleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 87 ->
                    CraneExtractorWhitelistLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 88 ->
                    CraneExtractorMaxEjectLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 89 ->
                    CraneGrabberWhitelistLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 90 ->
                    CraneInserterDestroyerLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 91 ->
                    DroneCrateSendingModeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 92 ->
                    DroneCrateItemTypeLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 93 ->
                    FluidPumpSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 94 ->
                    RadioAutocalPowerLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 95 ->
                    RadioAutocalIgnoreLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 96 ->
                    RadioAutocalRebootLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 97 ->
                    RadioAutocalScriptLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 98 ->
                    RadioTelexTransmitBufferLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 99 ->
                    RadioTelexSendLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 100 ->
                    RadioTelexPrintLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 101 ->
                    RadioTelexClearLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 102 ->
                    RadioTelexChannelsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 103 ->
                    RadioTorchSenderSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 104 ->
                    RadioTorchReceiverSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 105 ->
                    RadioTorchLogicSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 106 ->
                    RadioTorchLogicPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 107 ->
                    RadioTorchLogicDescendingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 108 ->
                    RadioTorchReaderSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 109 ->
                    RadioTorchSenderPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 110 ->
                    RadioTorchSenderCustomMapLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 111 ->
                    RadioTorchReceiverPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 112 ->
                    RadioTorchReceiverCustomMapLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 113 ->
                    RadioTorchReaderPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 114 ->
                    RadioTorchControllerSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 115 ->
                    RadioTorchControllerPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 116 ->
                    PneumaticPressureLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 117 ->
                    PneumoExporterContinuousLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 118 ->
                    PneumoExporterRequestLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 119 ->
                    PneumoExporterRorLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 120 ->
                    PneumoTubeWhitelistLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 121 ->
                    PneumoTubeRedstoneLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 122 ->
                    PneumoTubePressureLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 123 ->
                    PneumoTubeSendOrderLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 124 ->
                    PneumoTubeReceiveOrderLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 125 ->
                    TurretNameAddLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 126 ->
                    TurretNameRemoveLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 127 ->
                    TurretButtonLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 128 ->
                    GhostFilterStackLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 129 ->
                    RadioTorchCounterChannelsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 130 ->
                    RadioTorchCounterPollingLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 131 ->
                    RadarSettingsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 132 ->
                    RadarOpenMapLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 133 ->
                    RadarOpenSlotsLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 134 ->
                    RadarDispatchEntityLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 135 ->
                    RadarDispatchPositionLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 136 ->
                    ElectrolyserOpenMetalMenuLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 137 ->
                    ElectrolyserOpenFluidMenuLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 138 ->
                    CraneRouterCycleLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            case 139 ->
                    ResearchReactorTargetLayout.receiveFields(
                            decodedBuffer,
                            context,
                            storageBase,
                            storageAddress,
                            bufferStart,
                            bodyBytes);
            default -> throw new DecoderException("Unknown packet variant");
        }
    }
}
