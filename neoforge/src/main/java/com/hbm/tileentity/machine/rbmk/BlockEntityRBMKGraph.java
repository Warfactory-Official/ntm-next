// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKGraph extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int GRAPHS = 2;
    public static final int SAMPLES = 30;

    public static final int SAMPLE_PERIOD = 10;

    @SyncField(units = 0x3L)
    public final GraphUnit[] graphs = new GraphUnit[GRAPHS];

    public BlockEntityRBMKGraph(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GRAPH.get(), pos, state);
        for (int i = 0; i < GRAPHS; i++) graphs[i] = new GraphUnit(i);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKGraph graph) {
        if (TickPhase.every(graph, SAMPLE_PERIOD)) {
            for (GraphUnit unit : graph.graphs) unit.update(level);
        }
        graph.networkPackNT(50);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < GRAPHS; i++) graphs[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < GRAPHS; i++) graphs[i].save(output, i);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int active = data.getByteOr("active", (byte) 0);
        int polling = data.getByteOr("polling", (byte) 0);
        for (int i = 0; i < GRAPHS; i++) {
            graphs[i].active = (active & (1 << i)) != 0;
            graphs[i].polling = (polling & (1 << i)) != 0;
        }

        for (int i = 0; i < GRAPHS; i++) {
            GraphUnit graph = graphs[i];
            graph.label = data.getStringOr("label" + i, "");
            graph.rtty = data.getStringOr("rtty" + i, "");

            graph.minBound = data.getLong("min" + i).isPresent();
            if (graph.minBound) graph.min = data.getLongOr("min" + i, 0L);
            graph.maxBound = data.getLong("max" + i).isPresent();
            if (graph.maxBound) graph.max = data.getLongOr("max" + i, 0L);

            if (graph.max < graph.min) {
                long temp = graph.max;
                graph.max = graph.min;
                graph.min = temp;
            }
        }
        markChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 15 * 15;
    }

    public static class GraphUnit implements SyncSource {

        @SyncField public boolean polling;
        @SyncField public String label = "";
        @SyncField public String rtty = "";

        @SyncField public final long[] values = new long[SAMPLES];
        @SyncField public boolean active;
        @SyncField public long min;
        @SyncField public boolean minBound = false;
        @SyncField public long max;
        @SyncField public boolean maxBound = false;

        public GraphUnit(int initialIndex) {
            label = "Graph " + (initialIndex + 1);
        }

        public void update(Level level) {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;

            RTTYChannel chan = RTTYSystem.listen(level, rtty);
            long sigVal = 0;

            if (chan != null && chan.timeStamp < level.getGameTime() - 1) chan = null;

            if (chan != null && chan.signal != null) {
                try {
                    sigVal = Long.parseLong(chan.signal.toString());
                } catch (Exception ex) {
                }
                pushValue(sigVal);
            } else {
                if (polling) pushValue(0);
            }
        }

        public void pushValue(long value) {
            SyncArrays.copy(this, 3, values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            buf.writeBoolean(minBound);
            if (minBound) buf.writeLong(min);
            buf.writeBoolean(maxBound);
            if (maxBound) buf.writeLong(max);
            if (active) for (long value : values) buf.writeLong(value);
        }

        public void deserialize(ByteBuf buf) {
            active = buf.readBoolean();
            polling = buf.readBoolean();
            label = ByteBufCodecs.STRING_UTF8.decode(buf);
            rtty = ByteBufCodecs.STRING_UTF8.decode(buf);
            minBound = buf.readBoolean();
            if (minBound) min = buf.readLong();
            maxBound = buf.readBoolean();
            if (maxBound) max = buf.readLong();
            if (active) for (int i = 0; i < values.length; i++) values[i] = buf.readLong();
        }

        public void load(ValueInput input, int index) {
            this.active = input.getBooleanOr("active" + index, false);
            this.polling = input.getBooleanOr("polling" + index, false);
            this.label = input.getStringOr("label" + index, "");
            this.rtty = input.getStringOr("rtty" + index, "");
            this.minBound = input.getBooleanOr("minBound" + index, false);
            this.min = input.getLongOr("min" + index, 0L);
            this.maxBound = input.getBooleanOr("maxBound" + index, false);
            this.max = input.getLongOr("max" + index, 0L);
            for (int i = 0; i < values.length; i++)
                values[i] = input.getLongOr("value" + index + "_" + i, 0L);
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putBoolean("minBound" + index, minBound);
            output.putLong("min" + index, min);
            output.putBoolean("maxBound" + index, maxBound);
            output.putLong("max" + index, max);
            for (int i = 0; i < values.length; i++)
                output.putLong("value" + index + "_" + i, values[i]);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= graphs.length) throw new IllegalArgumentException();
        graphs[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= graphs.length) throw new IllegalArgumentException();
        graphs[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.graphs) {
            SyncBindings.bindIndexed(this, value, flags, 0, 2, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == graphs) {
            long selected = index < 0 ? 0x3L : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, graphs[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
