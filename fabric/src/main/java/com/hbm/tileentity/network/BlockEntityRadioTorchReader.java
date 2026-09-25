// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlockEntityRadioTorchReader extends BlockEntityRadioTorch {

    @SyncField(units = 0xff0L)
    public final String[] channels = new String[8];

    @SyncField(units = 0xff000L)
    public final String[] names = new String[8];

    public final String[] previous = new String[8];
    private final boolean[] forceUpdate = new boolean[8];

    public BlockEntityRadioTorchReader(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_READER.get(), pos, state, NtmContracts.ROR_VALUE_PROVIDER);
        Arrays.fill(channels, "");
        Arrays.fill(names, "");
        Arrays.fill(previous, "");
    }

    @Override
    public void tickServer() {
        IRORValueProvider provider = attached(IRORValueProvider.class);
        if (provider != null) {
            for (int i = 0; i < channels.length; i++) {
                if (channels[i].isEmpty() || names[i].isEmpty()) continue;
                String value =
                        provider.provideRORValue(
                                IRORInfo.PREFIX_VALUE + names[i].toLowerCase(Locale.US));
                if (value != null && (polling || forceUpdate[i] || !value.equals(previous[i]))) {
                    RTTYSystem.broadcast(level, channels[i], value);
                    previous[i] = value;
                    forceUpdate[i] = false;
                }
            }
        }
        super.tickServer();
    }

    @Override
    protected void loadRadio(ValueInput input) {
        polling = input.getBooleanOr("p", false);
        for (int i = 0; i < channels.length; i++) channels[i] = input.getStringOr("c" + i, "");
        for (int i = 0; i < names.length; i++) names[i] = input.getStringOr("n" + i, "");
        for (int i = 0; i < previous.length; i++) previous[i] = input.getStringOr("p" + i, "");
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        for (int i = 0; i < channels.length; i++) output.putString("c" + i, channels[i]);
        for (int i = 0; i < names.length; i++) output.putString("n" + i, names[i]);
        for (int i = 0; i < previous.length; i++) output.putString("p" + i, previous[i]);
    }

    @Override
    protected void receiveRadioControl(CompoundTag data) {
        if (data.contains("p")) polling = data.getBooleanOr("p", polling);
        for (int i = 0; i < channels.length; i++) {
            if (data.contains("c" + i)) setChannel(i, data.getStringOr("c" + i, channels[i]));
            if (data.contains("n" + i)) setName(i, data.getStringOr("n" + i, names[i]));
        }
    }

    public void setChannel(int index, String next) {
        if (next.equals(channels[index])) return;
        channels[index] = next;
        forceUpdate[index] = true;
        setChanged();
    }

    public void setName(int index, String next) {
        if (next.equals(names[index])) return;
        names[index] = next;
        forceUpdate[index] = true;
        setChanged();
    }

    private void writeChannels(int group, ByteBuf output) {
        writeString(output, channels[group]);
    }

    private void readChannels(int group, ByteBuf input) {
        channels[group] = readString(input);
    }

    private void writeNames(int group, ByteBuf output) {
        writeString(output, names[group]);
    }

    private void readNames(int group, ByteBuf input) {
        names[group] = readString(input);
    }

    @Override
    public long syncUnitMask() {
        return (super.syncUnitMask() | 0xffff0L) & ~0xeL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit >= 4 && unit < 12) writeChannels(unit - 4, output);
        else if (unit >= 12 && unit < 20) writeNames(unit - 12, output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit >= 4 && unit < 12) readChannels(unit - 4, input);
        else if (unit >= 12 && unit < 20) readNames(unit - 12, input);
        else super.readSyncUnit(unit, input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.channels) {
            SyncBindings.bindIndexed(this, value, flags, 4, 8, 0L);
            return;
        }
        if (value == this.names) {
            SyncBindings.bindIndexed(this, value, flags, 12, 8, 0L);
            return;
        }
        super.bindSyncValue(value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == channels) {
            long selected = index < 0 ? 0xff0L : 1L << (4 + index);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, channels[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (value == names) {
            long selected = index < 0 ? 0xff000L : 1L << (12 + index);
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, names[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        super.syncArrayChanged(value, index, flags, units);
    }
}
