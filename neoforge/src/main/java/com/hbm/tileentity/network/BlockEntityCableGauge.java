// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityCableGauge extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IRORValueProvider, SyncUnitSchema {

    @SyncField(units = 1L << 0)
    private long deltaTick;

    private long deltaSecond;

    @SyncField(units = 1L << 1)
    private long deltaLastSecond;

    public BlockEntityCableGauge(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CABLE_GAUGE.get(), pos, state);
    }

    public BlockEntityCableGauge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void tickServer() {
        ServerLevel sl = (ServerLevel) level;
        NodeNetwork<CableData> net = PowerGraph.get(sl).networkAt(worldPosition.asLong());
        if (net != null) {
            this.deltaTick = net.tracker;
            if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                this.deltaLastSecond = this.deltaSecond;
                this.deltaSecond = 0;
            }
            this.deltaSecond += deltaTick;
        }
        networkPackNT(25);
    }

    public long deltaTick() {
        return deltaTick;
    }

    public long deltaLastSecond() {
        return deltaLastSecond;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {PREFIX_VALUE + "deltatick", PREFIX_VALUE + "deltasecond"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaLastSecond;
        return null;
    }

    private void readDeltaTick(ByteBuf input) {
        deltaTick = Math.max(input.readLong(), 0);
    }

    private void readDeltaLastSecond(ByteBuf input) {
        deltaLastSecond = Math.max(input.readLong(), 0);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.deltaTick);
            case 1 -> output.writeLong(this.deltaLastSecond);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readDeltaTick(input);
            case 1 -> readDeltaLastSecond(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
