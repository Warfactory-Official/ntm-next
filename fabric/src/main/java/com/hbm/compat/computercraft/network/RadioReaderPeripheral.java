// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityRadioTorchReader;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioReaderPeripheral extends SnapshotPeripheral<BlockEntityRadioTorchReader> {
    private static final int CHANNELS = 0;
    private static final int NAMES = CHANNELS + 8;
    private static final int PREVIOUS = NAMES + 8;

    public RadioReaderPeripheral(BlockEntityRadioTorchReader machine) {
        super(machine, "radio_reader", 1, PREVIOUS + 8);
    }

    @Override
    protected void capture(BlockEntityRadioTorchReader machine) {
        put(0, machine.polling);
        for (int i = 0; i < 8; i++) {
            putRef(CHANNELS + i, machine.channels[i]);
            putRef(NAMES + i, machine.names[i]);
            putRef(PREVIOUS + i, machine.previous[i]);
        }
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannel(IArguments args) throws LuaException {
        int index = args.getInt(0);
        if (index >= 0 && index < 8) machine().setChannel(index, args.getString(1));
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getChannel(int index) {
        return index >= 0 && index < 8
                ? read(s -> new Object[] {s.refAt(CHANNELS + index)})
                : new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setName(IArguments args) throws LuaException {
        int index = args.getInt(0);
        if (index >= 0 && index < 8) machine().setName(index, args.getString(1));
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getName(int index) {
        return index >= 0 && index < 8
                ? read(s -> new Object[] {s.refAt(NAMES + index)})
                : new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPolling(boolean polling) {
        BlockEntityRadioTorchReader machine = machine();
        machine.polling = polling;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getPolling() {
        return read(s -> new Object[] {s.booleanAt(0)});
    }

    @LuaFunction
    public final Object[] read(int index) {
        return index >= 0 && index < 8
                ? read(s -> new Object[] {s.refAt(PREVIOUS + index)})
                : new Object[] {};
    }
}
