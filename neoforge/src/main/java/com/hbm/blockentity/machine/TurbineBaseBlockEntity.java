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
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CoolableTrait;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class TurbineBaseBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidFlushSender,
                PlainFluidProvider,
                PlainFluidReceiver,
                FluidSettingsHandler,
                RadioValueProvider,
                SyncUnitSchema {
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    protected final double[] info = new double[3];
    public long powerBuffer;
    public boolean operational;

    {
        hbm$syncSet$TurbineBaseBlockEntity$operational(false);
    }

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    protected TurbineBaseBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inputSize,
            int outputSize) {
        super(type, pos, state, 0);
        hbm$syncArraySet$TurbineBaseBlockEntity$tanks(
                tanks, 0, new FluidTankNTM(NTMFluids.STEAM, inputSize));
        hbm$syncArraySet$TurbineBaseBlockEntity$tanks(
                tanks, 1, new FluidTankNTM(NTMFluids.SPENTSTEAM, outputSize));
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public abstract double getEfficiency();

    public abstract double consumptionPercent();

    public void generatePower(long power, int steamConsumed) {
        this.hbm$syncSet$TurbineBaseBlockEntity$powerBuffer((long) (this.powerBuffer + (power)));
    }

    protected void onServerTick() {}

    protected void onClientTick() {}

    @Override
    public void tickServer() {
        if (this.powerBuffer != 0) markChanged();
        this.hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(0);
        info[0] = info[1] = info[2] = 0;

        hbm$syncSet$TurbineBaseBlockEntity$operational(false);
        Fluid in = tanks[0].getTankType();
        boolean valid = false;
        if (in != null && NTMFluidProperties.hasTrait(in, CoolableTrait.class)) {
            CoolableTrait trait = NTMFluidProperties.getTrait(in, CoolableTrait.class);
            double eff = trait.efficiency(CoolableTrait.Mode.TURBINE) * getEfficiency();
            if (eff > 0) {
                tanks[1].setTankType(trait.product());
                int inputOps =
                        (int)
                                (Math.min(
                                                Math.ceil(
                                                        tanks[0].getFill() * consumptionPercent()),
                                                tanks[0].getFill())
                                        / trait.consumed());
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.produced();
                int ops = Math.min(inputOps, outputOps);
                if (ops > 0) {
                    markChanged();
                    tanks[0].setFill(tanks[0].getFill() - ops * trait.consumed());
                    tanks[1].setFill(tanks[1].getFill() + ops * trait.produced());
                    this.generatePower(
                            (long) ((double) ops * trait.heat() * eff), ops * trait.consumed());
                }
                info[0] = ops * trait.consumed();
                info[1] = ops * trait.produced();
                info[2] = (double) ops * trait.heat() * eff;
                valid = true;
                hbm$syncSet$TurbineBaseBlockEntity$operational(ops > 0);
            }
        }

        onServerTick();

        if (!valid) tanks[1].setTankType(null);

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        onClientTick();
    }

    public void onLeverPull() {
        Fluid type = tanks[0].getTankType();
        boolean resize = this.doesResizeCompressor();

        if (type == NTMFluids.STEAM) {
            tanks[0].setTankType(NTMFluids.HOTSTEAM);
            tanks[1].setTankType(NTMFluids.STEAM);
            if (resize) {
                tanks[0].changeTankSize(tanks[0].getMaxFill() / 10);
                tanks[1].changeTankSize(tanks[1].getMaxFill() / 10);
            }
        } else if (type == NTMFluids.HOTSTEAM) {
            tanks[0].setTankType(NTMFluids.SUPERHOTSTEAM);
            tanks[1].setTankType(NTMFluids.HOTSTEAM);
            if (resize) {
                tanks[0].changeTankSize(tanks[0].getMaxFill() / 10);
                tanks[1].changeTankSize(tanks[1].getMaxFill() / 10);
            }
        } else if (type == NTMFluids.SUPERHOTSTEAM) {
            tanks[0].setTankType(NTMFluids.ULTRAHOTSTEAM);
            tanks[1].setTankType(NTMFluids.SUPERHOTSTEAM);
            if (resize) {
                tanks[0].changeTankSize(tanks[0].getMaxFill() / 10);
                tanks[1].changeTankSize(tanks[1].getMaxFill() / 10);
            }
        } else if (type == NTMFluids.ULTRAHOTSTEAM) {
            tanks[0].setTankType(NTMFluids.STEAM);
            tanks[1].setTankType(NTMFluids.SPENTSTEAM);
            if (resize) {
                tanks[0].changeTankSize(tanks[0].getMaxFill() * 1000);
                tanks[1].changeTankSize(tanks[1].getMaxFill() * 1000);
            }
        } else {
            tanks[0].setTankType(NTMFluids.STEAM);
            tanks[1].setTankType(NTMFluids.SPENTSTEAM);
        }

        setChanged();
    }

    public boolean doesResizeCompressor() {
        return false;
    }

    protected Direction facing() {
        return MultiblockCoreBlock.coreFacing(getBlockState());
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(power);
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
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (!tanks[0].accepts(type)) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure()) return amount;
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
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return;
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

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("steam").ifPresent(tanks[1]::deserialize);
        input.getLong("power").ifPresent(v -> hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(v));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("steam"));
        output.putLong("power", powerBuffer);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            case 1 -> output.writeLong(this.powerBuffer);
            case 2 -> output.writeBoolean(this.operational);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            case 1 -> this.hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(input.readLong());
            case 2 -> this.hbm$syncSet$TurbineBaseBlockEntity$operational(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$TurbineBaseBlockEntity$tanks(
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

    public final void hbm$syncSet$TurbineBaseBlockEntity$powerBuffer(long next) {
        if (this.powerBuffer != next) {
            this.powerBuffer = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$TurbineBaseBlockEntity$operational(boolean next) {
        if (this.operational != next) {
            this.operational = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TurbineBaseBlockEntity.class, "tanks", 3, 1L << 0);
    }
}
