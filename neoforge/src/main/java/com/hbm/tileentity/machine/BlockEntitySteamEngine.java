// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntitySteamEngine extends BlockEntityMachineBase
        implements Audible, IEnergyHandlerMK2, FluidFlushSender, IFluidCopiable, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    @SyncField(units = 1L << 1)
    public long powerBuffer;

    @SyncField(units = 1L << 2)
    public float rotor;

    public float lastRotor;
    private float syncRotor;
    private float acceleration = 0F;
    private int turnProgress;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntitySteamEngine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state, 0);
        tanks[0] = new FluidTankNTM(NTMFluids.STEAM, MachineData.STEAM_ENGINE_STEAM_CAP.get());
        tanks[1] = new FluidTankNTM(NTMFluids.SPENTSTEAM, MachineData.STEAM_ENGINE_LDS_CAP.get());
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    public void tickServer() {
        powerBuffer = 0;

        tanks[0].setTankType(NTMFluids.STEAM);
        tanks[1].setTankType(NTMFluids.SPENTSTEAM);

        FT_Coolable trait = NTMFluidProperties.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        double eff =
                trait.getEfficiency(CoolingType.TURBINE)
                        * MachineData.STEAM_ENGINE_EFFICIENCY.get();

        int inputOps = tanks[0].getFill() / trait.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
        int ops = Math.min(inputOps, outputOps);
        tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
        tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
        this.powerBuffer += (long) (ops * trait.heatEnergy * eff);

        if (ops > 0) {
            this.acceleration += 0.1F;
        } else {
            this.acceleration -= 0.1F;
        }

        this.acceleration = Mth.clamp(this.acceleration, 0F, 40F);
        this.rotor += this.acceleration;

        if (this.rotor >= 360D) {
            this.rotor -= 360D;

            level.playSound(
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
            this.rotor = (float) ((double) this.rotor + d / (double) this.turnProgress);
            --this.turnProgress;
        } else {
            this.rotor = this.syncRotor;
        }

        if (this.rotor >= 360F) {
            this.rotor -= 360F;
            this.lastRotor -= 360F;
        } else if (this.rotor < 0F) {
            this.rotor += 360F;
            this.lastRotor += 360F;
        }
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
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("powerBuffer").ifPresent(v -> powerBuffer = v);
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
            case 1 -> this.powerBuffer = input.readLong();
            case 2 -> readRotor(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
