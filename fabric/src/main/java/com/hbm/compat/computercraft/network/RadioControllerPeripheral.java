// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.network;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.BlockEntityRadioTorchController;
import com.hbm.tileentity.network.RTTYSystem;
import dan200.computercraft.api.lua.LuaFunction;

public final class RadioControllerPeripheral
        extends SnapshotPeripheral<BlockEntityRadioTorchController> {
    public RadioControllerPeripheral(BlockEntityRadioTorchController machine) {
        super(machine, "radio_controller", 1, 1);
    }

    @Override
    protected void capture(BlockEntityRadioTorchController machine) {
        put(0, machine.polling);
        putRef(0, machine.channel);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannel(String channel) {
        machine().setChannelName(channel);
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getChannel() {
        return read(s -> new Object[] {s.refAt(0)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPolling(boolean polling) {
        BlockEntityRadioTorchController machine = machine();
        machine.polling = polling;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getPolling() {
        return read(s -> new Object[] {s.booleanAt(0)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] send(String command) {
        BlockEntityRadioTorchController machine = machine();
        if (!machine.channel.isEmpty() && !command.isEmpty()) {
            RTTYSystem.broadcast(machine.getLevel(), machine.channel, command);
        }
        return new Object[] {};
    }
}
