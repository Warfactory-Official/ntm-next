// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class BlockEntityRadioTorchLogic extends BlockEntityRadioTorch {

    @SyncField(units = 0xffff0L)
    public final int[] conditions = new int[16];

    @SyncField(units = 1L << 20)
    public boolean descending;

    public BlockEntityRadioTorchLogic(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_LOGIC.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (!channel.isEmpty() || polling) {
            RTTYChannel received = channel.isEmpty() ? null : RTTYSystem.listen(level, channel);
            if (received != null
                    && (polling
                            || received.timeStamp > lastUpdate - 1L && received.timeStamp != -1L)) {
                String message = String.valueOf(received.signal);
                lastUpdate = level.getGameTime();
                if (polling && received.timeStamp < lastUpdate - 1L) message = "0";

                int nextState = 0;
                for (int j = 0; j < mapping.length; j++) {
                    int index = descending ? mapping.length - 1 - j : j;
                    if (!mapping[index].isEmpty() && matches(message, index)) {
                        nextState = index;
                        break;
                    }
                }
                setState(nextState);
            } else if (polling && lastState != 0) {
                setState(0);
            }
        }
        super.tickServer();
    }

    private boolean matches(String signal, int index) {
        if (conditions[index] <= 5) {
            try {
                long value = Long.parseLong(signal);
                long comparison = Long.parseLong(mapping[index]);
                return switch (conditions[index]) {
                    case 1 -> value <= comparison;
                    case 2 -> value >= comparison;
                    case 3 -> value > comparison;
                    case 4 -> value == comparison;
                    case 5 -> value != comparison;
                    default -> value < comparison;
                };
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return switch (conditions[index]) {
            case 7 -> !signal.equals(mapping[index]);
            case 8 -> signal.contains(mapping[index]);
            case 9 -> !signal.contains(mapping[index]);
            default -> signal.equals(mapping[index]);
        };
    }

    @Override
    protected void loadRadio(ValueInput input) {
        polling = input.getBooleanOr("p", false);
        descending = input.getBooleanOr("d", false);
        lastState = input.getIntOr("l", 0);
        lastUpdate = input.getLongOr("u", 0L);
        channel = input.getStringOr("c", "");
        for (int i = 0; i < mapping.length; i++) mapping[i] = input.getStringOr("m" + i, "");
        for (int i = 0; i < conditions.length; i++) conditions[i] = input.getIntOr("c" + i, 0);
    }

    @Override
    protected void saveRadio(ValueOutput output) {
        output.putBoolean("p", polling);
        output.putBoolean("d", descending);
        output.putInt("l", lastState);
        output.putLong("u", lastUpdate);
        output.putString("c", channel);
        for (int i = 0; i < mapping.length; i++) output.putString("m" + i, mapping[i]);
        for (int i = 0; i < conditions.length; i++) output.putInt("c" + i, conditions[i]);
    }

    @Override
    protected void receiveRadioControl(CompoundTag data) {
        if (data.contains("p")) polling = data.getBooleanOr("p", polling);
        if (data.contains("c")) channel = data.getStringOr("c", channel);
        if (data.contains("d")) descending = data.getBooleanOr("d", descending);
        for (int i = 0; i < mapping.length; i++) {
            if (data.contains("m" + i)) mapping[i] = data.getStringOr("m" + i, mapping[i]);
            if (data.contains("c" + i)) conditions[i] = data.getIntOr("c" + i, conditions[i]);
        }
    }

    private void writeConditions(int group, ByteBuf output) {
        output.writeInt(conditions[group]);
    }

    private void readConditions(int group, ByteBuf input) {
        conditions[group] = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return (super.syncUnitMask() | 0x1ffff0L) & ~(1L << 3);
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit >= 4 && unit < 20) writeConditions(unit - 4, output);
        else if (unit == 20) output.writeBoolean(descending);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit >= 4 && unit < 20) readConditions(unit - 4, input);
        else if (unit == 20) descending = input.readBoolean();
        else super.readSyncUnit(unit, input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.conditions) {
            SyncBindings.bindIndexed(this, value, flags, 4, 16, 0L);
            return;
        }
        super.bindSyncValue(value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == conditions) {
            long selected = index < 0 ? 0xffff0L : 1L << (4 + index);
            syncUnitsChanged(flags, selected);
            return;
        }
        super.syncArrayChanged(value, index, flags, units);
    }
}
