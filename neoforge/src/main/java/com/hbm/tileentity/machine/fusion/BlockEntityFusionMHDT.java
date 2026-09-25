// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityFusionMHDT extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IFusionPowerReceiver,
                SyncUnitSchema {
    public static final float ROTOR_ACCELERATION = 0.125F;
    public static final double PLASMA_EFFICIENCY = 1.35D;
    public static final int COOLANT_USE = 50;
    public static Consumer<BlockEntityFusionMHDT> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    public long plasmaEnergy;

    @SyncField(units = 1L << 0)
    public long plasmaEnergySync;

    public long power;
    public float rotor;
    public float prevRotor;
    public float rotorSpeed;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityFusionMHDT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_MHDT.get(), pos, state);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL_COLD, 4_000),
                    new FluidTankNTM(NTMFluids.PERFLUOROMETHYL, 4_000)
                };
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    public static List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        Direction dir = facing.getOpposite();
        return List.of(
                new FusionPorts.Port(
                        FusionPorts.Kind.PLASMA,
                        core.offset(dir.getStepX() * 6, 2, dir.getStepZ() * 6),
                        dir));
    }

    public boolean hasMinimumPlasma() {
        return plasmaEnergy >= MachineData.MHD_TURBINE_MINIMUM_PLASMA.get();
    }

    public boolean isCool() {
        return tanks[0].getFill() >= COOLANT_USE
                && tanks[1].getFill() + COOLANT_USE <= tanks[1].getMaxFill();
    }

    public void tickServer() {
        plasmaEnergySync = plasmaEnergy;

        if (isCool()) {
            power = (long) Math.floor(plasmaEnergy * PLASMA_EFFICIENCY);
            if (!hasMinimumPlasma()) power /= 2;
            tanks[0].setFill(tanks[0].getFill() - COOLANT_USE);
            tanks[1].setFill(tanks[1].getFill() + COOLANT_USE);
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
        plasmaEnergy = 0;
    }

    public void tickClient() {
        CLIENT_SOUND.accept(this);

        if (plasmaEnergy > 0 && isCool()) rotorSpeed += ROTOR_ACCELERATION;
        else rotorSpeed -= ROTOR_ACCELERATION;

        rotorSpeed = Mth.clamp(rotorSpeed, 0F, hasMinimumPlasma() ? 15F : 10F);

        prevRotor = rotor;
        rotor += rotorSpeed;

        if (rotor >= 360F) {
            rotor -= 360F;
            prevRotor -= 360F;
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        float speed = rotorSpeed / 15F;
        return AudioSystem.getLoopedSound(
                ModSounds.LARGE_TURBINE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 1.5F,
                worldPosition.getZ() + 0.5F,
                getVolume(speed),
                20F,
                speed,
                20);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(
            long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return power;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    private void readPlasmaEnergy(ByteBuf input) {
        plasmaEnergy = input.readLong();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.plasmaEnergySync);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readPlasmaEnergy(input);
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
