// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.*;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.CableDiodeBlock;
import com.hbm.capability.NtmCapabilities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.FabricCaps;
import com.hbm.uninos.graph.LevelNodeGraph;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityCableDiode extends BlockEntity
        implements Synced, GraphResident, IEnergyHandlerMK2, IControlReceiver, SyncUnitSchema {

    public static final long MAX_LIMIT = 10_000_000_000L;

    @SyncField(units = 1L)
    public long limit = 1_000L;

    @SyncField(units = 1L << 1)
    public ConnectionPriority priority = ConnectionPriority.NORMAL;

    private long stock;
    private long arriving;
    private long power;
    private int pulses;
    private long counted = Long.MIN_VALUE;
    private boolean recursionBrake;
    private @Nullable BlockLookupCache<IEnergyHandlerMK2> outputReceiver;

    public BlockEntityCableDiode(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    @Override
    public void setBlockState(BlockState state) {
        if (getBlockState().getValue(CableDiodeBlock.FACING)
                != state.getValue(CableDiodeBlock.FACING)) {
            outputReceiver = null;
        }
        super.setBlockState(state);
    }

    private void rollover() {
        long now = level.getGameTime();
        if (now == counted) return;
        counted = now;
        pulses = 0;
        power = 0;
        stock += arriving;
        arriving = 0;
    }

    public void settingsChanged() {
        setChanged();
        networkPackNT(15);
    }

    public void stepLimitUp() {
        long next = 10L;
        while (next <= limit && next < MAX_LIMIT) next *= 10L;
        limit = next;
    }

    public void stepLimitDown() {
        if (limit <= 10L) return;
        long previous = 10L;
        for (long next = 100L; next < limit && next <= MAX_LIMIT; next *= 10L) previous = next;
        limit = previous;
    }

    private Direction output() {
        return getBlockState().getValue(CableDiodeBlock.FACING).getOpposite();
    }

    public boolean acceptsFace(Direction dir, NtmCapabilities.CapRole role) {
        return (role == NtmCapabilities.CapRole.POWER_OUT) == (dir == output());
    }

    private boolean outputIsNet() {
        Direction out = output();
        long outKey = worldPosition.relative(out).asLong();
        LevelNodeGraph<CableData> graph = PowerGraph.get((ServerLevel) level);
        return graph.containsCell(outKey) && graph.isOpenAt(outKey, out.getOpposite());
    }

    @Override
    public long transferPower(long amount, boolean simulate) {
        if (simulate) return amount - Math.min(amount, getReceiverSpeed());
        if (outputIsNet()) {
            long accepted = Math.min(amount, netRoom());
            if (accepted > 0) {
                arriving += accepted;
                energyChanged();
            }
            return amount - accepted;
        }
        if (recursionBrake) return amount;

        rollover();
        pulses++;
        if (this.getPower() >= this.getMaxPower() || pulses > 10) return amount;

        recursionBrake = true;
        try {
            ServerLevel sl = (ServerLevel) level;
            Direction out = output();
            if (outputReceiver == null) {
                outputReceiver =
                        FabricCaps.createCache(
                                EnergyCaps.RECEIVER,
                                sl,
                                worldPosition.relative(out),
                                out.getOpposite());
            }
            IEnergyHandlerMK2 rec = outputReceiver.find();
            if (rec != null && rec != this) {
                long toTransfer = Math.min(amount, rec.getReceiverSpeed());
                long remainder = rec.transferPower(toTransfer, false);
                return amount - (toTransfer - remainder);
            }
            return amount;
        } finally {
            recursionBrake = false;
        }
    }

    private long netRoom() {
        rollover();
        return Math.max(0, Math.min(limit - arriving, 2 * limit - stock - arriving));
    }

    @Override
    public long getReceiverSpeed() {
        return outputIsNet() ? netRoom() : this.getMaxPower() - this.getPower();
    }

    @Override
    public long getMaxPower() {
        return limit;
    }

    @Override
    public long getPower() {
        rollover();
        return Math.min(arriving + power, this.getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getProviderSpeed() {
        rollover();
        return Math.min(stock, limit);
    }

    @Override
    public void usePower(long amount) {
        if (amount <= 0) return;
        stock = Math.max(0, stock - amount);
        energyChanged();
    }

    @Override
    public boolean passThrough() {
        return true;
    }

    @Override
    public ConnectionPriority getPriority() {
        return this.priority;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D)
                <= 128D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("limit"))
            limit = Math.clamp(data.getLongOr("limit", limit), 0L, MAX_LIMIT);
        if (data.contains("priority")) {
            priority =
                    ConnectionPriority.VALUES[
                            Math.clamp(
                                    data.getByteOr("priority", (byte) priority.ordinal()),
                                    0,
                                    ConnectionPriority.VALUES.length - 1)];
        }
        settingsChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        limit = Math.clamp(input.getLongOr("limit", limit), 0L, MAX_LIMIT);
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(
                                input.getByteOr("p", (byte) priority.ordinal()),
                                0,
                                ConnectionPriority.VALUES.length - 1)];
        stock = Math.max(0L, input.getLongOr("stock", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("limit", limit);
        output.putByte("p", (byte) priority.ordinal());
        if (stock + arriving > 0) output.putLong("stock", stock + arriving);
    }

    private void writeDiodeLevel(ByteBuf output) {
        output.writeLong(limit);
    }

    private void readDiodeLevel(ByteBuf input) {
        limit = Math.clamp(input.readLong(), 0L, MAX_LIMIT);
    }

    private void writePriority(ByteBuf output) {
        output.writeByte(priority.ordinal());
    }

    private void readPriority(ByteBuf input) {
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(input.readByte(), 0, ConnectionPriority.VALUES.length - 1)];
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeDiodeLevel(output);
            case 1 -> writePriority(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readDiodeLevel(input);
            case 1 -> readPriority(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
