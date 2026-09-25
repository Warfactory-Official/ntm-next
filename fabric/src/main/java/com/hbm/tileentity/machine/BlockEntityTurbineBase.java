// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityTurbineBase extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidFlushSender,
                IFluidCopiable,
                IRORValueProvider,
                SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    protected final double[] info = new double[3];

    @SyncField(units = 1L << 1)
    public long powerBuffer;

    @SyncField(units = 1L << 2)
    public boolean operational = false;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    protected BlockEntityTurbineBase(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inputSize,
            int outputSize) {
        super(type, pos, state, 0);
        tanks[0] = new FluidTankNTM(NTMFluids.STEAM, inputSize);
        tanks[1] = new FluidTankNTM(NTMFluids.SPENTSTEAM, outputSize);
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public abstract double getEfficiency();

    public abstract double consumptionPercent();

    public void generatePower(long power, int steamConsumed) {
        this.powerBuffer += power;
    }

    protected void onServerTick() {}

    protected void onClientTick() {}

    @Override
    public void tickServer() {
        this.powerBuffer = 0;
        info[0] = info[1] = info[2] = 0;

        operational = false;
        Fluid in = tanks[0].getTankType();
        boolean valid = false;
        if (in != null && NTMFluidProperties.hasTrait(in, FT_Coolable.class)) {
            FT_Coolable trait = NTMFluidProperties.getTrait(in, FT_Coolable.class);
            double eff = trait.getEfficiency(CoolingType.TURBINE) * getEfficiency();
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo());

                int inputOps =
                        (int)
                                (Math.min(
                                                Math.ceil(
                                                        tanks[0].getFill() * consumptionPercent()),
                                                tanks[0].getFill())
                                        / trait.amountReq);
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
                int ops = Math.min(inputOps, outputOps);
                if (ops > 0) {
                    tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                    tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);

                    this.generatePower(
                            (long) ((double) ops * trait.heatEnergy * eff), ops * trait.amountReq);
                }
                info[0] = ops * trait.amountReq;
                info[1] = ops * trait.amountProduced;
                info[2] = (double) ops * trait.heatEnergy * eff;
                valid = true;
                operational = ops > 0;
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
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.powerBuffer = power;
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
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
    public FluidTankNTM[] getAllTanks() {
        return tanks;
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
        input.getLong("power").ifPresent(v -> powerBuffer = v);
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
            case 1 -> this.powerBuffer = input.readLong();
            case 2 -> this.operational = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
