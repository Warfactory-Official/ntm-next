// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.*;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.FluidPumpBlock;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.BlockLookupCache;
import com.hbm.platform.Services;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFluidPump extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                IFluidHandlerMK2,
                IControlReceiver,
                SyncUnitSchema {

    public static final int MAX_RATE = 10_000;
    public static final int DEFAULT_RATE = 100;

    @SyncField(units = 1L << 0)
    public @Nullable Fluid type;

    public boolean carries(@Nullable Fluid fluid) {
        return fluid == null || fluid == type;
    }

    @SyncField(units = 1L << 1)
    public int pressure;

    @SyncField(units = 1L << 2)
    public int rate = DEFAULT_RATE;

    @SyncField(units = 1L << 3)
    public ConnectionPriority priority = ConnectionPriority.NORMAL;

    private long moved;

    @SyncField(units = 1L << 4)
    private long lastMoved;

    private int pulses;
    private boolean recursionBrake;
    private boolean redstone;

    public void refreshRedstone() {
        redstone = level.hasNeighborSignal(worldPosition);
    }

    private @Nullable BlockLookupCache<IFluidHandlerMK2> outputReceiver;

    public BlockEntityFluidPump(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_PUMP.get(), pos, state);
    }

    public void tickServer() {
        lastMoved = moved;
        moved = 0;
        pulses = 0;
        networkPackNT(15);
    }

    public long lastMoved() {
        return lastMoved;
    }

    public Direction intake() {
        return getBlockState().getValue(FluidPumpBlock.FACING).getClockWise();
    }

    public Direction output() {
        return intake().getOpposite();
    }

    @Override
    public long getDemand(Fluid fluid, int lane) {
        if (redstone || fluid != type || fluid == null || lane != pressure) return 0;
        return Math.max(0, rate - moved);
    }

    @Override
    public long getReceiverSpeed(Fluid fluid, int lane) {
        return Math.max(0, rate - moved);
    }

    @Override
    public int[] getReceivingPressureRange(Fluid fluid) {
        return new int[] {pressure, pressure};
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return priority;
    }

    @Override
    public long transferFluid(Fluid fluid, int lane, long amount) {
        if (recursionBrake || redstone || fluid != type || lane != pressure) return amount;
        pulses++;
        if (pulses > 10) return amount;
        long toMove = Math.min(amount, rate - moved);
        if (toMove <= 0) return amount;

        recursionBrake = true;
        try {
            ServerLevel sl = (ServerLevel) level;
            Direction out = output();
            long outKey = worldPosition.relative(out).asLong();
            LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(sl, outKey);
            GraphNode<PipeData> node = graph == null ? null : graph.getNode(outKey);

            if (node != null && node.isOpen(out.getOpposite()) && node.data.fluid() == fluid) {
                NodeNetwork<PipeData> net = graph.networkAt(outKey);
                if (net != null) {
                    long leftover = FluidNetwork.injectDiode(sl, graph, net, fluid, lane, toMove);
                    long transferred = toMove - leftover;
                    moved += transferred;
                    if (transferred > 0) setChanged();
                    return amount - transferred;
                }
            }

            if (outputReceiver == null) {
                outputReceiver =
                        Services.CAPS.createCache(
                                FluidCaps.RECEIVER,
                                sl,
                                worldPosition.relative(out),
                                FluidFace.any(out.getOpposite()));
            }
            IFluidHandlerMK2 rec = outputReceiver.find();
            if (rec != null && rec != this) {
                int[] range = rec.getReceivingPressureRange(fluid);
                if (lane >= range[0] && lane <= range[1]) {
                    long grant =
                            Math.min(
                                    toMove,
                                    Math.min(
                                            rec.getDemand(fluid, lane),
                                            rec.getReceiverSpeed(fluid, lane)));
                    if (grant > 0) {
                        long transferred = grant - rec.transferFluid(fluid, lane, grant);
                        moved += transferred;
                        if (transferred > 0) setChanged();
                        return amount - transferred;
                    }
                }
            }
            return amount;
        } finally {
            recursionBrake = false;
        }
    }

    @Override
    public long getFluidAvailable(Fluid fluid, int lane) {
        return 0;
    }

    @Override
    public void useUpFluid(Fluid fluid, int lane, long amount) {}

    @Override
    public int[] getProvidingPressureRange(Fluid fluid) {
        return new int[] {pressure, pressure};
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("rate")) {
            this.rate = Math.clamp(data.getIntOr("rate", rate), 0, MAX_RATE);
        }
        if (data.contains("pressure")) {
            this.pressure = Math.clamp(data.getByteOr("pressure", (byte) pressure), 0, 5);
        }
        if (data.contains("priority")) {
            this.priority =
                    ConnectionPriority.VALUES[
                            Math.clamp(
                                    data.getByteOr("priority", (byte) priority.ordinal()),
                                    0,
                                    ConnectionPriority.VALUES.length - 1)];
        }
        setChanged();
    }

    private void writeType(ByteBuf output) {
        output.writeInt(type == null ? -1 : BuiltInRegistries.FLUID.getId(type));
    }

    private void readType(ByteBuf input) {
        int id = input.readInt();
        Fluid decoded = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
        type = (decoded == null || decoded == Fluids.EMPTY) ? null : decoded;
    }

    private void writePressure(ByteBuf output) {
        output.writeByte(pressure);
    }

    private void readPressure(ByteBuf input) {
        pressure = Math.clamp(input.readByte(), 0, 5);
    }

    private void readRate(ByteBuf input) {
        rate = Math.clamp(input.readInt(), 0, MAX_RATE);
    }

    private void writePriority(ByteBuf output) {
        output.writeByte(priority.ordinal());
    }

    private void readPriority(ByteBuf input) {
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(input.readByte(), 0, ConnectionPriority.VALUES.length - 1)];
    }

    private void readLastMoved(ByteBuf input) {
        lastMoved = Math.max(input.readLong(), 0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String id = input.getStringOr("type", "");
        if (!id.isEmpty()) {
            Fluid decoded = BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
            type = decoded == Fluids.EMPTY ? null : decoded;
        }
        pressure = Math.clamp(input.getIntOr("pressure", pressure), 0, 5);
        rate = Math.clamp(input.getIntOr("rate", rate), 0, MAX_RATE);
        priority =
                ConnectionPriority.VALUES[
                        Math.clamp(
                                input.getByteOr("p", (byte) priority.ordinal()),
                                0,
                                ConnectionPriority.VALUES.length - 1)];
        redstone = input.getBooleanOr("redstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (type != null) output.putString("type", BuiltInRegistries.FLUID.getKey(type).toString());
        output.putInt("pressure", pressure);
        output.putInt("rate", rate);
        output.putByte("p", (byte) priority.ordinal());
        output.putBoolean("redstone", redstone);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeType(output);
            case 1 -> writePressure(output);
            case 2 -> output.writeInt(this.rate);
            case 3 -> writePriority(output);
            case 4 -> output.writeLong(this.lastMoved);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readType(input);
            case 1 -> readPressure(input);
            case 2 -> readRate(input);
            case 3 -> readPriority(input);
            case 4 -> readLastMoved(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
