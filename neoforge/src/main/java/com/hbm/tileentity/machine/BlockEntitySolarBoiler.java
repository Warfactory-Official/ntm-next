// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntitySolarBoiler extends BlockEntityMachineBase
        implements FluidTankEndpoint, IFluidCopiable, SyncUnitSchema {

    @SyncField(units = 1L << 1)
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 100);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 2)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.STEAM, 10_000);

    public final Set<BlockPos> primary = new HashSet<>();
    public final Set<BlockPos> secondary = new HashSet<>();

    @SyncField(units = 1L << 0)
    public int display;

    public int heat;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntitySolarBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLARBOILER.get(), pos, state, 0);
        receiving = new FluidTankNTM[] {water};
        sending = new FluidTankNTM[] {steam};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_solar_boiler");
    }

    @Override
    public void tickServer() {
        int process = heat / 50;
        this.display = process;
        process = Math.min(process, water.getFill());
        process = Math.min(process, (steam.getMaxFill() - steam.getFill()) / 100);

        if (process < 0) process = 0;

        water.setFill(water.getFill() - process);
        steam.setFill(steam.getFill() + process * 100);

        heat = 0;

        flush.provide((ServerLevel) level, this);

        networkPackNTTracking();
    }

    @Override
    public void tickClient() {

        secondary.clear();
        secondary.addAll(primary);
        primary.clear();
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
    public void declareFlush(FlushLanes out) {
        out.add(
                steam,
                (server, pos, contacts) -> {
                    contacts.contact(pos.above(3), Direction.DOWN);
                    contacts.contact(pos.below(), Direction.UP);
                });
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {water, steam};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("water").ifPresent(water::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        water.serialize(output.child("water"));
        steam.serialize(output.child("steam"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.display);
            case 1 -> this.water.packetSerialize(output);
            case 2 -> this.steam.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.display = input.readInt();
            case 1 -> this.water.packetDeserialize(input);
            case 2 -> this.steam.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
