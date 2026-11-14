/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.network;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.radio.RadioSystem;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RadioReaderBlockEntity extends RadioTorchBlockEntity {
    public static final int ROW_COUNT = 8;

    private static final int CHANNEL_UNIT = 4;
    private static final int NAME_UNIT = 12;
    private static final long CHANNEL_MASK = 0xff0;
    private static final long NAME_MASK = 0xff000;

    private final String[] readKeys = emptyRows();
    private final boolean[] force = new boolean[ROW_COUNT];

    public final String[] channels = emptyRows();
    public final String[] names = emptyRows();
    public final String[] previous = emptyRows();

    public RadioReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_READER.get(), pos, state, NtmContracts.ROR_VALUE_PROVIDER);
        Arrays.fill(readKeys, RadioCommandParser.VAL);
    }

    private static String[] emptyRows() {
        var rows = new String[ROW_COUNT];
        Arrays.fill(rows, "");
        return rows;
    }

    private void readNameChanged(int index) {
        if (index < 0) {
            for (int row = 0; row < ROW_COUNT; row++) {
                updateReadKey(row);
            }
        } else {
            updateReadKey(index);
        }
    }

    private void updateReadKey(int row) {
        readKeys[row] = RadioCommandParser.VAL + names[row].toLowerCase(Locale.ROOT);
    }

    @Override
    public void tickServer() {
        var provider = attached(RadioValueProvider.class);
        if (provider != null) {
            for (int row = 0; row < ROW_COUNT; row++) {
                if (channels[row].isEmpty() || names[row].isEmpty()) continue;
                var value = provider.getRadioValue(readKeys[row]);
                if (value == null) continue;

                boolean changed = !previous[row].equals(value);
                if (polling || force[row] || changed) {
                    RadioSystem.send(level, channels[row], value);
                    previous[row] = value;
                    force[row] = false;
                    if (changed) setChanged();
                }
            }
        }
        super.tickServer();
    }

    public void setChannel(int row, String value) {
        if (channels[row].equals(value)) return;
        hbm$syncArraySet$RadioReaderBlockEntity$channels(channels, row, value);
        force[row] = true;
        setChanged();
    }

    public void setName(int row, String value) {
        if (names[row].equals(value)) return;
        hbm$syncArraySet$RadioReaderBlockEntity$names(names, row, value);
        force[row] = true;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyReader");
    }

    @Override
    protected void loadRadio(ValueInput input) {
        hbm$syncSet$RadioTorchBlockEntity$polling(input.getBooleanOr("p", false));
        for (int row = 0; row < ROW_COUNT; row++) {
            hbm$syncArraySet$RadioReaderBlockEntity$channels(
                    channels, row, input.getStringOr("c" + row, ""));
            hbm$syncArraySet$RadioReaderBlockEntity$names(
                    names, row, input.getStringOr("n" + row, ""));
            previous[row] = input.getStringOr("p" + row, "");
        }
        Arrays.fill(force, false);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        for (int row = 0; row < ROW_COUNT; row++) {
            output.putString("c" + row, channels[row]);
            output.putString("n" + row, names[row]);
            output.putString("p" + row, previous[row]);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0xffff1;
    }

    @Override
    public void syncArrayChanged(Object value, int index, int mask, long units) {
        if (value == channels) {
            long changedUnits = index < 0 ? CHANNEL_MASK : 1L << (CHANNEL_UNIT + index);
            syncUnitsChanged(mask, changedUnits);
        } else if (value == names) {
            long changedUnits = index < 0 ? NAME_MASK : 1L << (NAME_UNIT + index);
            syncUnitsChanged(mask, changedUnits);
        } else {
            super.syncArrayChanged(value, index, mask, units);
        }
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) {
            output.writeBoolean(polling);
        } else if (unit >= CHANNEL_UNIT && unit < NAME_UNIT) {
            writeString(output, channels[unit - CHANNEL_UNIT]);
        } else if (unit >= NAME_UNIT && unit < NAME_UNIT + ROW_COUNT) {
            writeString(output, names[unit - NAME_UNIT]);
        } else {
            throw new IllegalArgumentException("Radio reader unit " + unit);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) {
            hbm$syncSet$RadioTorchBlockEntity$polling(input.readBoolean());
        } else if (unit >= CHANNEL_UNIT && unit < NAME_UNIT) {
            hbm$syncArraySet$RadioReaderBlockEntity$channels(
                    channels, unit - CHANNEL_UNIT, readString(input));
        } else if (unit >= NAME_UNIT && unit < NAME_UNIT + ROW_COUNT) {
            hbm$syncArraySet$RadioReaderBlockEntity$names(
                    names, unit - NAME_UNIT, readString(input));
        } else {
            throw new IllegalArgumentException("Radio reader unit " + unit);
        }
    }

    public final String hbm$syncArraySet$RadioReaderBlockEntity$channels(
            String[] array, int index, String next) {
        if (array[index] != next) {
            boolean changed = !java.util.Objects.equals(array[index], next);
            array[index] = next;
            if (array == this.channels && changed) {
                syncArrayChanged(array, index, 3, CHANNEL_MASK);
            }
        }
        return next;
    }

    public final String hbm$syncArraySet$RadioReaderBlockEntity$names(
            String[] array, int index, String next) {
        if (array[index] != next) {
            if (array == this.names && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.names) {
                syncArrayChanged(array, index, 3, NAME_MASK);
                readNameChanged(index);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(
                RadioReaderBlockEntity.class, "channels", 3, CHANNEL_MASK);
        com.hbm.packet.SyncBindings.field(RadioReaderBlockEntity.class, "names", 3, NAME_MASK);
    }
}
