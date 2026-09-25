// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.RadarEntry;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.List;

public final class RadarPeripheral extends SnapshotPeripheral<BlockEntityMachineRadar> {

    private static final int MISSILES = 0;
    private static final int SHELLS = 1;
    private static final int PLAYERS = 2;
    private static final int SMART = 3;
    private static final int RANGE = 4;
    private static final int POWER = 5;
    private static final int MAX_POWER = 6;
    private static final int JAMMED = 7;
    private static final int AMOUNT = 8;
    private static final int X = 9;
    private static final int Y = 10;
    private static final int Z = 11;

    public RadarPeripheral(BlockEntityMachineRadar machine) {
        super(machine, "ntm_radar", 12, 0);
    }

    @Override
    protected void capture(BlockEntityMachineRadar machine) {
        put(MISSILES, machine.scanMissiles);
        put(SHELLS, machine.scanShells);
        put(PLAYERS, machine.scanPlayers);
        put(SMART, machine.smartMode);
        put(RANGE, machine.getRange());
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(JAMMED, machine.jammed);
        put(AMOUNT, machine.entries.size());
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getSettings() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(MISSILES),
                            s.booleanAt(SHELLS),
                            s.booleanAt(PLAYERS),
                            s.booleanAt(SMART)
                        });
    }

    @LuaFunction
    public final Object[] getRange() {
        return read(s -> new Object[] {s.intAt(RANGE)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setSettings(
            boolean missiles, boolean shells, boolean players, boolean smart) {
        BlockEntityMachineRadar machine = machine();
        machine.scanMissiles = missiles;
        machine.scanShells = shells;
        machine.scanPlayers = players;
        machine.smartMode = smart;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] isJammed() {
        return read(s -> new Object[] {s.booleanAt(JAMMED)});
    }

    @LuaFunction
    public final Object[] getAmount() {
        return read(s -> new Object[] {s.intAt(AMOUNT)});
    }

    private Object[] noEntity() {
        return new Object[] {null, "No entity exists at that index."};
    }

    private RadarEntry entry(int index) {
        List<RadarEntry> entries = machine().entries;
        return index >= entries.size() || index < 0 ? null : entries.get(index);
    }

    @LuaFunction(mainThread = true)
    public final Object[] isIndexPlayer(int index) {
        RadarEntry e = entry(index - 1);
        return e == null ? noEntity() : new Object[] {e.blipLevel == IRadarDetectableNT.PLAYER};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getIndexType(int index) {
        RadarEntry e = entry(index - 1);
        return e == null ? noEntity() : new Object[] {e.blipLevel};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getEntityAtIndex(int index) {
        RadarEntry e = entry(index - 1);
        if (e == null) return noEntity();
        if (e.blipLevel == IRadarDetectableNT.PLAYER) {
            return new Object[] {true, e.posX, e.posY, e.posZ, e.blipLevel, e.radarName};
        }
        return new Object[] {false, e.posX, e.posY, e.posZ, e.blipLevel};
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[] {s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
