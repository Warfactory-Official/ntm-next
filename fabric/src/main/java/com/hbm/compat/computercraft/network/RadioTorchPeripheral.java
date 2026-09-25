// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityRadioTorch;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Map;

public final class RadioTorchPeripheral extends SnapshotPeripheral<BlockEntityRadioTorch> {

    private static final int POLLING = 0;
    private static final int CUSTOM_MAP = 1;
    private static final int CHANNEL = 0;
    private static final int MAPPING = 1;

    public RadioTorchPeripheral(BlockEntityRadioTorch machine) {
        super(machine, "radio_torch", 2, MAPPING + 16);
    }

    @Override
    protected void capture(BlockEntityRadioTorch machine) {
        put(POLLING, machine.polling);
        put(CUSTOM_MAP, machine.customMap);
        putRef(CHANNEL, machine.channel);
        for (int i = 0; i < machine.mapping.length; i++) putRef(MAPPING + i, machine.mapping[i]);
    }

    @LuaFunction
    public final Object[] getChannel() {
        return read(snapshot -> new Object[] {snapshot.refAt(CHANNEL)});
    }

    @LuaFunction
    public final Object[] getPolling() {
        return read(snapshot -> new Object[] {snapshot.booleanAt(POLLING)});
    }

    @LuaFunction
    public final Object[] getCustomMap() {
        return read(snapshot -> new Object[] {snapshot.booleanAt(CUSTOM_MAP)});
    }

    @LuaFunction
    public final Object[] getCustomMapValues() {
        return read(
                snapshot -> {
                    String[] values = new String[16];
                    for (int i = 0; i < values.length; i++)
                        values[i] = (String) snapshot.refAt(MAPPING + i);
                    return new Object[] {values};
                });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannel(String channel) {
        BlockEntityRadioTorch machine = machine();
        machine.channel = channel;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPolling(boolean polling) {
        BlockEntityRadioTorch machine = machine();
        machine.polling = polling;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setCustomMap(boolean customMap) {
        BlockEntityRadioTorch machine = machine();
        machine.customMap = customMap;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setCustomMapValues(Map<?, ?> values) {
        BlockEntityRadioTorch machine = machine();
        for (int i = 1; i <= 16; i++) {

            if (values.get((double) i) instanceof String signal) machine.mapping[i - 1] = signal;
        }
        machine.setChanged();
        return new Object[] {};
    }
}
