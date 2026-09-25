// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import dan200.computercraft.api.lua.LuaFunction;

public final class PWRControllerPeripheral
        extends SnapshotPeripheral<BlockEntityMachinePWRController> {

    private static final int CORE_HEAT = 0;
    private static final int HULL_HEAT = 1;
    private static final int CORE_HEAT_CAPACITY = 2;
    private static final int FLUX = 3;
    private static final int ROD_TARGET = 4;
    private static final int ROD_LEVEL = 5;
    private static final int AMOUNT_LOADED = 6;
    private static final int PROGRESS = 7;
    private static final int PROCESS_TIME = 8;
    private static final int COLD_FILL = 9;
    private static final int COLD_MAX = 10;
    private static final int HOT_FILL = 11;
    private static final int HOT_MAX = 12;

    public PWRControllerPeripheral(BlockEntityMachinePWRController machine) {
        super(machine, "ntm_pwr_control", 13, 0);
    }

    @Override
    protected void capture(BlockEntityMachinePWRController machine) {
        put(CORE_HEAT, machine.coreHeat);
        put(HULL_HEAT, machine.hullHeat);
        put(CORE_HEAT_CAPACITY, machine.coreHeatCapacity);
        put(FLUX, machine.flux);
        put(ROD_TARGET, machine.rodTarget);
        put(ROD_LEVEL, machine.rodLevel);
        put(AMOUNT_LOADED, machine.amountLoaded);
        put(PROGRESS, machine.progress);
        put(PROCESS_TIME, machine.processTime);
        put(COLD_FILL, machine.tanks[0].getFill());
        put(COLD_MAX, machine.tanks[0].getMaxFill());
        put(HOT_FILL, machine.tanks[1].getFill());
        put(HOT_MAX, machine.tanks[1].getMaxFill());
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(CORE_HEAT),
                            s.longAt(HULL_HEAT),
                            s.longAt(CORE_HEAT_CAPACITY),
                            BlockEntityMachinePWRController.hullHeatCapacityBase
                        });
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[] {s.doubleAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[] {s.doubleAt(ROD_TARGET), s.doubleAt(ROD_LEVEL)});
    }

    @LuaFunction
    public final Object[] getCoolantInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(COLD_FILL),
                            s.intAt(COLD_MAX),
                            s.intAt(HOT_FILL),
                            s.intAt(HOT_MAX)
                        });
    }

    @LuaFunction
    public final Object[] getFuelInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(AMOUNT_LOADED), s.doubleAt(PROGRESS), s.doubleAt(PROCESS_TIME)
                        });
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(CORE_HEAT),
                            s.longAt(HULL_HEAT),
                            s.longAt(CORE_HEAT_CAPACITY),
                            BlockEntityMachinePWRController.hullHeatCapacityBase,
                            s.doubleAt(FLUX),
                            s.doubleAt(ROD_TARGET),
                            s.doubleAt(ROD_LEVEL),
                            s.intAt(AMOUNT_LOADED),
                            s.doubleAt(PROGRESS),
                            s.doubleAt(PROCESS_TIME),
                            s.intAt(COLD_FILL),
                            s.intAt(COLD_MAX),
                            s.intAt(HOT_FILL),
                            s.intAt(HOT_MAX)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        BlockEntityMachinePWRController machine = machine();
        machine.rodTarget = Math.clamp(level, 0, 100);
        machine.markChanged();
        return new Object[] {true};
    }
}
