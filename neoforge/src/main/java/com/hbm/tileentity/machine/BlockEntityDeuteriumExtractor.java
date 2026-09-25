// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityDeuteriumExtractor extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, FluidTankEndpoint, IFluidCopiable, SyncUnitSchema {

    public static final long MAX_POWER = 10_000L;
    public static final int RATIO = 50;
    public static final int MIN_WATER = 100;

    @SyncField(units = 1L << 1)
    public final FluidTankNTM[] tanks;

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 0)
    public long power;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityDeuteriumExtractor(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DEUTERIUM_EXTRACTOR.get(), pos, state, 1_000, 100);
    }

    protected BlockEntityDeuteriumExtractor(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int waterCapacity,
            int heavyCapacity) {
        super(type, pos, state, 0);
        tanks =
                new FluidTankNTM[] {
                    new FluidTankNTM(NTMFluids.WATER, waterCapacity),
                    new FluidTankNTM(NTMFluids.HEAVYWATER, heavyCapacity)
                };
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    public void tickServer() {
        if (hasPower() && hasEnoughWater() && tanks[1].getFill() < tanks[1].getMaxFill()) {

            int convert = Math.min(tanks[1].getMaxFill(), tanks[0].getFill()) / RATIO;
            convert = Math.min(convert, tanks[1].getMaxFill() - tanks[1].getFill());

            tanks[0].setFill(tanks[0].getFill() - convert * RATIO);
            tanks[1].setFill(tanks[1].getFill() + convert);
            power -= getMaxPower() / 20;
            setChanged();
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    public boolean hasPower() {
        return power >= getMaxPower() / 20;
    }

    public boolean hasEnoughWater() {
        return tanks[0].getFill() >= MIN_WATER;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, getMaxPower()));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
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
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("water").ifPresent(tanks[0]::deserialize);
        input.child("heavyWater").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tanks[0].serialize(output.child("water"));
        tanks[1].serialize(output.child("heavyWater"));
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
            case 0 -> output.writeLong(this.power);
            case 1 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
