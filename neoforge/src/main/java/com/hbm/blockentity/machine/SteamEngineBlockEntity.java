/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.fluidmk2.PlainFluidProvider;
import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SteamEngineBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                MachineSoundProvider,
                EnergyHandler,
                FluidFlushSender,
                PlainFluidProvider,
                PlainFluidReceiver,
                FluidSettingsHandler,
                SyncUnitSchema {
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public long powerBuffer;
    public float rotor;
    public float lastRotor;
    private float syncRotor;
    private float acceleration = 0F;
    private int turnProgress;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state, 0);
        hbm$syncArraySet$SteamEngineBlockEntity$tanks(
                tanks,
                0,
                new FluidTankNTM(NTMFluids.STEAM, MachineData.STEAM_ENGINE_STEAM_CAP.get()));
        hbm$syncArraySet$SteamEngineBlockEntity$tanks(
                tanks,
                1,
                new FluidTankNTM(NTMFluids.SPENTSTEAM, MachineData.STEAM_ENGINE_LDS_CAP.get()));
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    public void tickServer() {
        hbm$syncSet$SteamEngineBlockEntity$powerBuffer(0);

        tanks[0].setTankType(NTMFluids.STEAM);
        tanks[1].setTankType(NTMFluids.SPENTSTEAM);

        CoolableTrait trait =
                NTMFluidProperties.getTrait(tanks[0].getTankType(), CoolableTrait.class);
        double eff =
                trait.efficiency(CoolableTrait.Mode.TURBINE)
                        * MachineData.STEAM_ENGINE_EFFICIENCY.get();

        int inputOps = tanks[0].getFill() / trait.consumed();
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.produced();
        int ops = Math.min(inputOps, outputOps);
        tanks[0].setFill(tanks[0].getFill() - ops * trait.consumed());
        tanks[1].setFill(tanks[1].getFill() + ops * trait.produced());
        this.hbm$syncSet$SteamEngineBlockEntity$powerBuffer(
                (long) (this.powerBuffer + ((long) (ops * trait.heat() * eff))));

        if (ops > 0) {
            this.acceleration += 0.1F;
            this.markChanged();
        } else {
            if (this.acceleration > 0F) this.markChanged();
            this.acceleration -= 0.1F;
        }

        this.acceleration = Mth.clamp(this.acceleration, 0F, 40F);
        this.hbm$syncSet$SteamEngineBlockEntity$rotor((float) (this.rotor + (this.acceleration)));

        if (this.rotor >= 360D) {
            this.hbm$syncSet$SteamEngineBlockEntity$rotor((float) (this.rotor - (360D)));
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.STEAM_ENGINE_OPERATE.get(),
                    SoundSource.BLOCKS,
                    getVolume(1.0F),
                    0.5F + (acceleration / 80F));
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        this.lastRotor = this.rotor;

        if (this.turnProgress > 0) {
            double d = Mth.wrapDegrees(this.syncRotor - (double) this.rotor);
            this.hbm$syncSet$SteamEngineBlockEntity$rotor(
                    (float) ((double) this.rotor + d / (double) this.turnProgress));
            --this.turnProgress;
        } else {
            this.hbm$syncSet$SteamEngineBlockEntity$rotor(this.syncRotor);
        }
        if (this.rotor >= 360F) {
            this.hbm$syncSet$SteamEngineBlockEntity$rotor((float) (this.rotor - (360F)));
            this.lastRotor -= 360F;
        } else if (this.rotor < 0F) {
            this.hbm$syncSet$SteamEngineBlockEntity$rotor((float) (this.rotor + (360F)));
            this.lastRotor += 360F;
        }
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.hbm$syncSet$SteamEngineBlockEntity$powerBuffer(power);
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tanks[0].joins(type);
    }

    @Override
    public boolean canProvideFluid(Fluid type) {
        return tanks[1].joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (type != NTMFluids.STEAM) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure() || type != NTMFluids.STEAM) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (type != NTMFluids.SPENTSTEAM || pressure != tanks[1].getPressure()) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (type != NTMFluids.SPENTSTEAM || pressure != tanks[1].getPressure()) return;
        if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("powerBuffer")
                .ifPresent(v -> hbm$syncSet$SteamEngineBlockEntity$powerBuffer(v));
        acceleration = input.getFloatOr("acceleration", 0F);
        input.child("s").ifPresent(tanks[0]::deserialize);
        input.child("w").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("powerBuffer", powerBuffer);
        output.putFloat("acceleration", acceleration);
        tanks[0].serialize(output.child("s"));
        tanks[1].serialize(output.child("w"));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    private void readRotor(ByteBuf input) {
        syncRotor = input.readFloat();
    }

    @Override
    public void afterSyncUnits(long units) {
        turnProgress = 3;
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            case 1 -> output.writeLong(this.powerBuffer);
            case 2 -> output.writeFloat(this.rotor);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            case 1 -> this.hbm$syncSet$SteamEngineBlockEntity$powerBuffer(input.readLong());
            case 2 -> readRotor(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$SteamEngineBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 0);
            }
        }
        return next;
    }

    public final void hbm$syncSet$SteamEngineBlockEntity$powerBuffer(long next) {
        if (this.powerBuffer != next) {
            this.powerBuffer = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$SteamEngineBlockEntity$rotor(float next) {
        if (Float.floatToRawIntBits(this.rotor) != Float.floatToRawIntBits(next)) {
            this.rotor = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(SteamEngineBlockEntity.class, "tanks", 3, 1L << 0);
    }
}
