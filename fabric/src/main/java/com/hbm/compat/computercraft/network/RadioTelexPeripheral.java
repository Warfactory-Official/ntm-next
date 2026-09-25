// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityRadioTelex;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioTelexPeripheral extends SnapshotPeripheral<BlockEntityRadioTelex> {

    private static final int LINES = 5;
    private static final int TX = 0;
    private static final int RX = TX + LINES;
    private static final int TX_CHANNEL = RX + LINES;
    private static final int RX_CHANNEL = TX_CHANNEL + 1;

    public RadioTelexPeripheral(BlockEntityRadioTelex machine) {
        super(machine, "ntm_telex", 0, RX_CHANNEL + 1);
    }

    @Override
    protected void capture(BlockEntityRadioTelex machine) {
        for (int i = 0; i < LINES; i++) {
            putRef(TX + i, machine.txBuffer[i]);
            putRef(RX + i, machine.rxBuffer[i]);
        }
        putRef(TX_CHANNEL, machine.txChannel);
        putRef(RX_CHANNEL, machine.rxChannel);
    }

    @LuaFunction
    public final Object[] getChannels() {
        return read(s -> new Object[] {s.refAt(TX_CHANNEL), s.refAt(RX_CHANNEL)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannels(String rx, String tx) {
        BlockEntityRadioTelex machine = machine();
        Object[] old = {machine.txChannel, machine.rxChannel};
        machine.rxChannel = rx;
        machine.txChannel = tx;
        machine.setChanged();
        return old;
    }

    @LuaFunction
    public final Object[] getSendingTexts() {
        return read(
                s ->
                        new Object[] {
                            s.refAt(TX),
                            s.refAt(TX + 1),
                            s.refAt(TX + 2),
                            s.refAt(TX + 3),
                            s.refAt(TX + 4)
                        });
    }

    @LuaFunction
    public final Object[] getReceivingText() {
        return read(
                s ->
                        new Object[] {
                            s.refAt(RX),
                            s.refAt(RX + 1),
                            s.refAt(RX + 2),
                            s.refAt(RX + 3),
                            s.refAt(RX + 4)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setSendingText(IArguments args) throws LuaException {
        BlockEntityRadioTelex machine = machine();
        for (int i = 0; i < LINES; i++) {

            if (args.count() <= i)
                throw new LuaException(
                        "bad arguments #" + (i + 1) + " (value expected, got no value)");
            if (args.get(i) == null || args.getString(i).isEmpty()) {
                machine.txBuffer[i] = "";
                continue;
            }
            String line = args.getString(i);
            machine.txBuffer[i] =
                    line.length() > BlockEntityRadioTelex.LINE_WIDTH
                            ? line.substring(0, BlockEntityRadioTelex.LINE_WIDTH)
                            : line;
        }
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] printMessage() {
        machine().print();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] clearAll() {
        machine().clearReceiveBuffer();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] sendMessage() {
        BlockEntityRadioTelex machine = machine();
        if (machine.isSending) return new Object[] {false};
        machine.isSending = true;
        machine.sendingLine = 0;
        machine.sendingIndex = 0;
        return new Object[] {true};
    }
}
