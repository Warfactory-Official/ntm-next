// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.FluidValveBlock;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityFluidCounterValve extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                IRORValueProvider,
                IRORInteractive,
                SyncUnitSchema {

    @SyncField(units = 1L << 0)
    private long counter;

    private long lastCounterUpdate = -1L;

    public BlockEntityFluidCounterValve(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_COUNTER_VALVE.get(), pos, state);
    }

    public void tickServer() {
        updateCounter();
        networkPackNT(25);
    }

    private NodeNetwork<PipeData> pendingNetwork() {
        if (lastCounterUpdate == level.getGameTime()) return null;
        ServerLevel sl = (ServerLevel) level;
        long key = worldPosition.asLong();
        LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(sl, key);
        if (graph == null) return null;
        PipeData data = graph.dataAt(key);
        if (data == null || data.fluid() == Fluids.EMPTY) return null;
        return graph.networkAt(key);
    }

    public void updateCounter() {
        NodeNetwork<PipeData> net = pendingNetwork();
        if (net == null) return;
        counter += net.tracker;
        lastCounterUpdate = level.getGameTime();
        if (net.tracker != 0) setChanged();
    }

    private long currentCounter() {
        NodeNetwork<PipeData> net = pendingNetwork();
        return counter + (net == null ? 0L : net.tracker);
    }

    public long getCounter() {
        return counter;
    }

    public void resetCounter() {
        updateCounter();
        counter = 0;
        setChanged();
    }

    public boolean isOpen() {
        BlockState state = getBlockState();
        return state.hasProperty(FluidValveBlock.OPEN) && state.getValue(FluidValveBlock.OPEN);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "value",
            PREFIX_VALUE + "state",
            PREFIX_FUNCTION + "reset",
            PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "value").equals(name)) return String.valueOf(currentCounter());
        if ((PREFIX_VALUE + "state").equals(name)) return String.valueOf(isOpen() ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "reset").equals(name)) {
            resetCounter();
        } else if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            setState(IRORInteractive.parseInt(params[0], 0, 1));
        }
        return null;
    }

    public void setState(int state) {
        BlockState current = getBlockState();
        if (current.getBlock() instanceof FluidValveBlock valve) {
            valve.setOpen(level, worldPosition, current, state == 1, 1.0F);
        }
    }

    private void readCounter(ByteBuf input) {
        counter = Math.max(input.readLong(), 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        counter = input.getLongOr("counter", counter);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("counter", counter);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.counter);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readCounter(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
