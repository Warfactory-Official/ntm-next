// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKCooler extends BlockEntityRBMKBase
        implements FluidTankEndpoint, SyncUnitSchema {
    @SyncField(units = 1L << 4)
    public final FluidTankNTM cold = new FluidTankNTM(NTMFluids.PERFLUOROMETHYL_COLD, 4_000);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    @SyncField(units = 1L << 5)
    public final FluidTankNTM hot = new FluidTankNTM(NTMFluids.PERFLUOROMETHYL, 4_000);

    private final BlockEntityRBMKBase[] coolCache = new BlockEntityRBMKBase[25];
    private int timer = 0;

    public BlockEntityRBMKCooler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_COOLER.get(), pos, state, 0);
        receiving = new FluidTankNTM[] {cold};
        sending = new FluidTankNTM[] {hot};
    }

    @Override
    public void tickServer() {
        if (timer <= 0) {
            timer = 60;
            for (int i = 0; i < 25; i++) {
                BlockPos p =
                        new BlockPos(
                                worldPosition.getX() - 2 + i / 5,
                                worldPosition.getY(),
                                worldPosition.getZ() - 2 + i % 5);
                coolCache[i] =
                        level.getBlockEntity(p) instanceof BlockEntityRBMKBase base ? base : null;
            }
        } else {
            timer--;
        }

        if (cold.getFill() >= 50 && hot.getMaxFill() - hot.getFill() >= 50) {
            cold.setFill(cold.getFill() - 50);
            hot.setFill(hot.getFill() + 50);

            for (BlockEntityRBMKBase neighbor : coolCache) {
                if (neighbor != null && !neighbor.isRemoved()) {
                    double before = neighbor.heat;
                    neighbor.heat -= 200;
                    if (neighbor.heat < 20) neighbor.heat = 20;
                    if (before > neighbor.heat) neighbor.markChanged();
                }
            }
        }

        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.COOLER;
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
        out.add(hot, COLUMN_OUTPUTS);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("t0").ifPresent(cold::deserialize);
        input.child("t1").ifPresent(hot::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        cold.serialize(output.child("t0"));
        hot.serialize(output.child("t1"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        writeDiagnostics(tag, "t0", cold);
        writeDiagnostics(tag, "t1", hot);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> this.cold.packetSerialize(output);
            case 5 -> this.hot.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.cold.packetDeserialize(input);
            case 5 -> this.hot.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
