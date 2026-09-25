// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.tileentity.network.RTTYSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKGauge extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int GAUGES = 4;

    @SyncField(units = 0xfL)
    public final GaugeUnit[] gauges = new GaugeUnit[GAUGES];

    public BlockEntityRBMKGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_GAUGE.get(), pos, state);
        for (int i = 0; i < GAUGES; i++) gauges[i] = new GaugeUnit(i);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKGauge gauge) {
        for (GaugeUnit unit : gauge.gauges) unit.update(level);
        gauge.networkPackNT(50);
    }

    public void tickClient() {
        for (GaugeUnit unit : gauges) unit.updateClient();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < GAUGES; i++) gauges[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < GAUGES; i++) gauges[i].save(output, i);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int active = data.getByteOr("active", (byte) 0);
        int polling = data.getByteOr("polling", (byte) 0);
        for (int i = 0; i < GAUGES; i++) {
            gauges[i].active = (active & (1 << i)) != 0;
            gauges[i].polling = (polling & (1 << i)) != 0;
        }

        for (int i = 0; i < GAUGES; i++) {
            GaugeUnit gauge = gauges[i];
            gauge.color = Mth.clamp(data.getIntOr("color" + i, 0), 0, 0xffffff);
            gauge.label = data.getStringOr("label" + i, "");
            gauge.rtty = data.getStringOr("rtty" + i, "");
            gauge.min = data.getIntOr("min" + i, 0);
            gauge.max = data.getIntOr("max" + i, 0);
        }
        setChanged();
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

    public static class GaugeUnit implements SyncSource {

        @SyncField public boolean polling;
        @SyncField public int color;
        @SyncField public String label = "";
        @SyncField public String rtty = "";
        @SyncField public long min = 0;

        @SyncField public long max = 100;
        @SyncField public long value;
        public double renderValue;
        public double lastRenderValue;

        @SyncField public boolean active;

        public GaugeUnit(int initialIndex) {
            if (initialIndex == 0) color = 0x800000;
            if (initialIndex == 1) color = 0x804000;
            if (initialIndex == 2) color = 0x808000;
            if (initialIndex == 3) color = 0x000080;
            label = "Gauge " + (initialIndex + 1);
        }

        public void updateClient() {
            this.lastRenderValue = this.renderValue;
            double delta = value - renderValue;
            this.renderValue += delta * 0.1D;
        }

        public void update(Level level) {
            if (!active) return;
            if (rtty == null || rtty.isEmpty()) return;

            RTTYChannel chan = RTTYSystem.listen(level, rtty);
            int sigVal = 0;

            if (chan != null && chan.timeStamp < level.getGameTime() - 1) chan = null;

            if (chan != null && chan.signal != null) {
                try {
                    sigVal = Integer.parseInt(chan.signal.toString());
                } catch (Exception ex) {
                }
                this.value = sigVal;
            } else {
                if (polling) this.value = 0;
            }
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            buf.writeInt(color);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            buf.writeLong(min);
            buf.writeLong(max);
            buf.writeLong(value);
        }

        public void deserialize(ByteBuf buf) {
            active = buf.readBoolean();
            polling = buf.readBoolean();
            color = buf.readInt();
            label = ByteBufCodecs.STRING_UTF8.decode(buf);
            rtty = ByteBufCodecs.STRING_UTF8.decode(buf);
            min = buf.readLong();
            max = buf.readLong();
            value = buf.readLong();
        }

        public void load(ValueInput input, int index) {
            this.active = input.getBooleanOr("active" + index, false);
            this.polling = input.getBooleanOr("polling" + index, false);
            this.color = input.getIntOr("color" + index, 0);
            this.label = input.getStringOr("label" + index, "");
            this.rtty = input.getStringOr("rtty" + index, "");
            this.min = input.getLongOr("min" + index, 0L);
            this.max = input.getLongOr("max" + index, 0L);
            this.value = input.getLongOr("value" + index, 0L);
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putInt("color" + index, color);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putLong("min" + index, min);
            output.putLong("max" + index, max);
            output.putLong("value" + index, value);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= gauges.length) throw new IllegalArgumentException();
        gauges[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= gauges.length) throw new IllegalArgumentException();
        gauges[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.gauges) {
            SyncBindings.bindIndexed(this, value, flags, 0, 4, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == gauges) {
            long selected = index < 0 ? 0xfL : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, gauges[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
