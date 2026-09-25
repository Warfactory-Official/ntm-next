// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKTerminalPeripheral extends SnapshotPeripheral<BlockEntityRBMKTerminal> {

    private static final int OC_MODE = 0;

    public RBMKTerminalPeripheral(BlockEntityRBMKTerminal machine) {
        super(machine, "rbmk_terminal", 1, BlockEntityRBMKTerminal.HISTORY);
    }

    @Override
    protected void capture(BlockEntityRBMKTerminal machine) {
        put(OC_MODE, machine.ocMode);
        for (int i = 0; i < BlockEntityRBMKTerminal.HISTORY; i++) {
            putRef(i, machine.history[i] == null ? "" : machine.history[i]);
        }
    }

    @LuaFunction(mainThread = true)
    public final Object[] enableOCMode(boolean enabled) {
        BlockEntityRBMKTerminal machine = machine();
        machine.ocMode = enabled;
        if (enabled) {
            for (int i = 0; i < machine.history.length; i++) machine.history[i] = "";
            machine.push("OC MODE ENABLED");
            machine.push("Terminal ready.");
        }
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction
    public final Object[] isOCMode() {
        return read(s -> new Object[] {s.booleanAt(OC_MODE)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] write(String text) {
        BlockEntityRBMKTerminal machine = machine();
        if (!machine.ocMode) return new Object[] {false, "OC mode not enabled"};
        machine.push(text);
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] writeln(String text) {
        BlockEntityRBMKTerminal machine = machine();
        if (!machine.ocMode) return new Object[] {false, "OC mode not enabled"};
        machine.push(text);
        return new Object[] {true};
    }

    @LuaFunction
    public final Object[] readInput() {
        return read(
                s ->
                        s.booleanAt(OC_MODE)
                                ? new Object[] {s.refAt(0)}
                                : new Object[] {"", "OC mode not enabled"});
    }

    @LuaFunction
    public final Object[] getAllHistory() {
        return read(
                s -> {
                    if (!s.booleanAt(OC_MODE)) return new Object[] {null, "OC mode not enabled"};
                    String[] copy = new String[BlockEntityRBMKTerminal.HISTORY];
                    for (int i = 0; i < copy.length; i++) copy[i] = (String) s.refAt(i);
                    return new Object[] {copy};
                });
    }

    @LuaFunction(mainThread = true)
    public final Object[] clearScreen() {
        BlockEntityRBMKTerminal machine = machine();
        if (!machine.ocMode) return new Object[] {false, "OC mode not enabled"};
        for (int i = 0; i < machine.history.length; i++) machine.history[i] = "";
        machine.setChanged();
        return new Object[] {true};
    }
}
