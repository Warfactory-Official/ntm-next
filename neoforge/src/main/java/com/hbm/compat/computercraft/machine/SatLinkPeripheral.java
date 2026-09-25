// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityMachineSatLink;
import dan200.computercraft.api.lua.LuaFunction;

public final class SatLinkPeripheral extends SnapshotPeripheral<BlockEntityMachineSatLink> {

    private static final int CONNECTED = 0;
    private static final int FREQUENCY = 1;
    private static final int TYPE = 0;
    private static final int REPLY = 1;

    public SatLinkPeripheral(BlockEntityMachineSatLink machine) {
        super(machine, "ntm_satlink", 2, 2);
    }

    @Override
    protected void capture(BlockEntityMachineSatLink machine) {
        put(CONNECTED, machine.connected);
        put(FREQUENCY, machine.freq);
        putRef(TYPE, machine.provideRORValue(IRORInfo.PREFIX_VALUE + "type"));
        putRef(REPLY, machine.provideRORValue(IRORInfo.PREFIX_VALUE + "rx"));
    }

    @LuaFunction
    public final Object[] isConnected() {
        return read(s -> new Object[] {s.booleanAt(CONNECTED)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setFreq(int frequency) {
        machine().setFrequency(frequency);
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getFreq() {
        return read(s -> new Object[] {s.intAt(FREQUENCY)});
    }

    @LuaFunction("getType")
    public final Object[] getSatelliteType() {
        return read(s -> new Object[] {s.refAt(TYPE)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] send(String command) {
        machine().runRORFunction(IRORInfo.PREFIX_FUNCTION + "tx", new String[] {command});
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] read() {
        return read(s -> new Object[] {s.refAt(REPLY)});
    }
}
