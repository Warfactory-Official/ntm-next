/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RadioLogicBlockEntity extends RadioTorchBlockEntity {

    private static final int CONDITION_UNIT = 4;
    private static final long CONDITION_MASK = 0xffff0L;

    public final int[] conditionIndices = new int[16];
    public boolean descending;

    public RadioLogicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_LOGIC.get(), pos, state);
    }

    @Override
    public void tickServer() {
        RadioChannel frame = null;
        if (!channel.isEmpty()) frame = RadioSystem.read(level, channel);
        if (frame == null) {
            if (polling) setState(0);
        } else if (polling || frame.stamp() > lastUpdate - 1) {
            long gameTime = level.getGameTime();
            if (lastUpdate != gameTime) {
                lastUpdate = gameTime;
                setChanged();
            }

            String message;
            if (polling && frame.stamp() < gameTime - 1) {
                message = "0";
            } else {
                message = frame.text();
            }
            setState(selectOutput(message));
        }
        super.tickServer();
    }

    private int selectOutput(String message) {
        for (int offset = 0; offset < mapping.length; offset++) {
            int index = descending ? mapping.length - 1 - offset : offset;
            String entry = mapping[index];
            if (!entry.isEmpty() && matches(message, entry, conditionIndices[index])) return index;
        }
        return 0;
    }

    private static boolean matches(String message, String entry, int condition) {
        if (condition <= 5) {
            long receivedValue;
            long configuredValue;
            try {
                receivedValue = Long.parseLong(message);
                configuredValue = Long.parseLong(entry);
            } catch (NumberFormatException _) {
                return false;
            }
            return switch (condition) {
                case 1 -> receivedValue <= configuredValue;
                case 2 -> receivedValue >= configuredValue;
                case 3 -> receivedValue > configuredValue;
                case 4 -> receivedValue == configuredValue;
                case 5 -> receivedValue != configuredValue;
                default -> receivedValue < configuredValue;
            };
        }
        return switch (condition) {
            case 7 -> !message.equals(entry);
            case 8 -> message.contains(entry);
            case 9 -> !message.contains(entry);
            default -> message.equals(entry);
        };
    }

    public void setEntry(int index, String entry, int condition) {
        boolean mappingChanged = !mapping[index].equals(entry);
        boolean conditionChanged = conditionIndices[index] != condition;
        if (!mappingChanged && !conditionChanged) return;
        if (mappingChanged) setMapping(index, entry);
        if (conditionChanged) {
            hbm$syncArraySet$RadioLogicBlockEntity$conditionIndices(
                    conditionIndices, index, condition);
        }
        setChanged();
    }

    public void setDescending(boolean descending) {
        if (this.descending == descending) return;
        this.hbm$syncSet$RadioLogicBlockEntity$descending(descending);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyLogic");
    }

    @Override
    protected void loadRadio(ValueInput input) {
        hbm$syncSet$RadioTorchBlockEntity$polling(input.getBooleanOr("p", false));
        hbm$syncSet$RadioLogicBlockEntity$descending(input.getBooleanOr("d", false));
        hbm$syncSet$RadioTorchBlockEntity$lastState(input.getIntOr("l", 0));
        lastUpdate = input.getLongOr("u", 0L);
        hbm$syncSet$RadioTorchBlockEntity$channel(input.getStringOr("c", ""));
        for (int index = 0; index < mapping.length; index++) {
            hbm$syncArraySet$RadioTorchBlockEntity$mapping(
                    mapping, index, input.getStringOr("m" + index, ""));
            hbm$syncArraySet$RadioLogicBlockEntity$conditionIndices(
                    conditionIndices, index, input.getIntOr("c" + index, 0));
        }
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        output.putBoolean("d", descending);
        output.putInt("l", lastState);
        output.putLong("u", lastUpdate);
        output.putString("c", channel);
        for (int index = 0; index < mapping.length; index++) {
            output.putString("m" + index, mapping[index]);
            output.putInt("c" + index, conditionIndices[index]);
        }
    }

    @Override
    public void syncArrayChanged(Object value, int index, int mask, long units) {
        if (value == conditionIndices) {
            long changedUnits = index < 0 ? CONDITION_MASK : 1L << (CONDITION_UNIT + index);
            syncUnitsChanged(mask, changedUnits);
        } else {
            super.syncArrayChanged(value, index, mask, units);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0x1001ffff7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit >= CONDITION_UNIT && unit < CONDITION_UNIT + conditionIndices.length) {
            output.writeInt(conditionIndices[unit - CONDITION_UNIT]);
        } else if (unit == 20) {
            output.writeBoolean(descending);
        } else {
            super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit >= CONDITION_UNIT && unit < CONDITION_UNIT + conditionIndices.length) {
            hbm$syncArraySet$RadioLogicBlockEntity$conditionIndices(
                    conditionIndices, unit - CONDITION_UNIT, input.readInt());
        } else if (unit == 20) {
            hbm$syncSet$RadioLogicBlockEntity$descending(input.readBoolean());
        } else {
            super.readSyncUnit(unit, input);
        }
    }

    public final int hbm$syncArraySet$RadioLogicBlockEntity$conditionIndices(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.conditionIndices) {
                syncArrayChanged(array, index, 3, CONDITION_MASK);
            }
        }
        return next;
    }

    public final void hbm$syncSet$RadioLogicBlockEntity$descending(boolean next) {
        if (this.descending != next) {
            this.descending = next;
            if ((1L << 20) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 20);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                RadioLogicBlockEntity.class, "conditionIndices", 3, CONDITION_MASK);
    }
}
