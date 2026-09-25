// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityRadioAUTOCAL;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioAutocalPeripheral extends SnapshotPeripheral<BlockEntityRadioAUTOCAL> {
    private static final int ON = 0;
    private static final int IGNORE = 1;
    private static final int REBOOT = 2;

    public RadioAutocalPeripheral(BlockEntityRadioAUTOCAL machine) {
        super(machine, "radio_autocal", 3, 0);
    }

    @Override
    protected void capture(BlockEntityRadioAUTOCAL machine) {
        put(ON, machine.isOn);
        put(IGNORE, machine.ignoreError);
        put(REBOOT, machine.autoReboot);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setBuffer(String text) {
        return new Object[] {machine().setComputerBuffer(text)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getBuffer() {
        return new Object[] {machine().ctx.readBuffer()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setScript(String text) {
        machine().setComputerScript(text);
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getScript() {
        String[] lines = machine().script;
        StringBuilder joined = new StringBuilder();
        for (String line : lines) joined.append(line).append('\n');
        return new Object[] {joined.toString()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setState(boolean on) {
        machine().setComputerState(on);
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIgnoreError(boolean ignore) {
        BlockEntityRadioAUTOCAL machine = machine();
        machine.ignoreError = ignore;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getIgnoreError() {
        return read(s -> new Object[] {s.booleanAt(IGNORE)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setAutoReboot(boolean reboot) {
        BlockEntityRadioAUTOCAL machine = machine();
        machine.autoReboot = reboot;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getAutoReboot() {
        return read(s -> new Object[] {s.booleanAt(REBOOT)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] getHistory(int index) {
        return index >= 0 && index < machine().history.length
                ? new Object[] {machine().history[index].getString()}
                : new Object[] {};
    }
}
