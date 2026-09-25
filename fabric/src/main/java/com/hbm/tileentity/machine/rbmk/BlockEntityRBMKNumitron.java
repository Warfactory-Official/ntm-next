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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKNumitron extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int DISPLAYS = 2;

    @SyncField(units = 0x3L)
    public final DisplayUnit[] displays = new DisplayUnit[DISPLAYS];

    public BlockEntityRBMKNumitron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_NUMITRON.get(), pos, state);
        for (int i = 0; i < DISPLAYS; i++) displays[i] = new DisplayUnit(i);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKNumitron numitron) {
        for (DisplayUnit unit : numitron.displays) unit.update(level);
        numitron.networkPackNT(50);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < DISPLAYS; i++) displays[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < DISPLAYS; i++) displays[i].save(output, i);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int active = data.getByteOr("active", (byte) 0);
        int polling = data.getByteOr("polling", (byte) 0);
        int shorten = data.getByteOr("shorten_number", (byte) 0);
        int leading = data.getByteOr("leading_zeroes", (byte) 0);
        for (int i = 0; i < DISPLAYS; i++) {
            displays[i].active = (active & (1 << i)) != 0;
            displays[i].polling = (polling & (1 << i)) != 0;
            displays[i].shortenNumber = (shorten & (1 << i)) != 0;
            displays[i].leadingZeroes = (leading & (1 << i)) != 0;
        }

        for (int i = 0; i < DISPLAYS; i++) {
            displays[i].label = data.getStringOr("label" + i, "");
            displays[i].rtty = data.getStringOr("rtty" + i, "");
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

    public static class DisplayUnit implements SyncSource {

        @SyncField public boolean polling;
        @SyncField public String label = "";
        @SyncField public String rtty = "";
        @SyncField public long value;
        @SyncField public boolean active;

        @SyncField public boolean leadingZeroes;

        @SyncField public long activeDigits;
        @SyncField public boolean shortenNumber;

        public DisplayUnit(int initialIndex) {
            label = "Display " + (initialIndex + 1);
            activeDigits = 0b01111111;
            shortenNumber = true;
            leadingZeroes = true;
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
                this.value = sigVal;
            } else {
                if (polling) this.value = 0;
            }
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(shortenNumber);
            buf.writeLong(activeDigits);
            buf.writeBoolean(leadingZeroes);
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            buf.writeLong(value);
        }

        public void deserialize(ByteBuf buf) {
            shortenNumber = buf.readBoolean();
            activeDigits = buf.readLong();
            leadingZeroes = buf.readBoolean();
            active = buf.readBoolean();
            polling = buf.readBoolean();
            label = ByteBufCodecs.STRING_UTF8.decode(buf);
            rtty = ByteBufCodecs.STRING_UTF8.decode(buf);
            value = buf.readLong();
        }

        public void load(ValueInput input, int index) {
            this.shortenNumber = input.getBooleanOr("shorten_number" + index, false);
            this.activeDigits = input.getLongOr("active_digits" + index, 0L);
            this.leadingZeroes = input.getBooleanOr("leading_zeroes" + index, false);
            this.active = input.getBooleanOr("active" + index, false);
            this.polling = input.getBooleanOr("polling" + index, false);
            this.label = input.getStringOr("label" + index, "");
            this.rtty = input.getStringOr("rtty" + index, "");
            this.value = input.getLongOr("value" + index, 0L);
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("shorten_number" + index, shortenNumber);
            output.putLong("active_digits" + index, activeDigits);
            output.putBoolean("leading_zeroes" + index, leadingZeroes);
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putLong("value" + index, value);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= displays.length) throw new IllegalArgumentException();
        displays[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= displays.length) throw new IllegalArgumentException();
        displays[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.displays) {
            SyncBindings.bindIndexed(this, value, flags, 0, 2, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == displays) {
            long selected = index < 0 ? 0x3L : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, displays[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
