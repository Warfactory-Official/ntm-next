// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.turret;

import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.tileentity.turret.BlockEntityTurretBaseNT;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class TurretPeripheral<BE extends BlockEntityTurretBaseNT> extends SnapshotPeripheral<BE> {

    protected static final int ON = 0;
    protected static final int POWER = 1;
    protected static final int MAX_POWER = 2;
    protected static final int PLAYERS = 3;
    protected static final int ANIMALS = 4;
    protected static final int MOBS = 5;
    protected static final int MACHINES = 6;
    protected static final int HAS_TARGET = 7;
    protected static final int PITCH = 8;
    protected static final int YAW = 9;
    protected static final int ALIGNED = 10;
    protected static final int X = 11;
    protected static final int Y = 12;
    protected static final int Z = 13;
    protected static final int SLOTS = 14;

    public TurretPeripheral(BE machine) {
        this(machine, "ntm_turret", SLOTS, 0);
    }

    protected TurretPeripheral(BE machine, String type, int longs, int refs) {
        super(machine, type, longs, refs);
    }

    @Override
    protected void capture(BE machine) {
        put(ON, machine.isOn);
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(PLAYERS, machine.targetPlayers);
        put(ANIMALS, machine.targetAnimals);
        put(MOBS, machine.targetMobs);
        put(MACHINES, machine.targetMachines);
        put(HAS_TARGET, machine.target != null);
        put(PITCH, Math.toDegrees(machine.rotationPitch));
        put(YAW, Math.toDegrees(machine.rotationYaw));
        put(ALIGNED, machine.aligned);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction(mainThread = true)
    public final Object[] setActive(boolean active) {
        BE machine = machine();
        machine.isOn = active;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] isActive() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] getWhitelisted() {
        ItemStack chip = machine().getItem(BlockEntityTurretBaseNT.SLOT_CHIP);
        if (!chip.is(ModItems.TURRET_CHIP.get())) return new Object[] {};
        return new Object[] {ItemTurretBiometry.getNames(chip)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] addWhitelist(String name) {
        BE machine = machine();
        List<String> names = machine.getWhitelist();
        if (names != null && names.contains(name)) return new Object[] {false};
        machine.addName(name);
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] removeWhitelist(String name) {
        BE machine = machine();
        List<String> names = machine.getWhitelist();

        if (names == null) return OpenComputers.unknownError();
        if (!names.contains(name)) return new Object[] {false};
        machine.removeName(names.indexOf(name));
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setTargeting(
            boolean players, boolean animals, boolean mobs, boolean machines) {
        BE machine = machine();
        machine.targetPlayers = players;
        machine.targetAnimals = animals;
        machine.targetMobs = mobs;
        machine.targetMachines = machines;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getTargeting() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(PLAYERS),
                            s.booleanAt(ANIMALS),
                            s.booleanAt(MOBS),
                            s.booleanAt(MACHINES)
                        });
    }

    @LuaFunction
    public final Object[] hasTarget() {
        return read(s -> new Object[] {s.booleanAt(HAS_TARGET)});
    }

    @LuaFunction
    public final Object[] getAngle() {
        return read(s -> new Object[] {s.doubleAt(PITCH), s.doubleAt(YAW)});
    }

    @LuaFunction
    public final Object[] isAligned() {
        return read(s -> new Object[] {s.booleanAt(ALIGNED)});
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[] {s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
