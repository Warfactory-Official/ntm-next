// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.tileentity.machine.BlockEntityReactorZirnox;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class ZirnoxPeripheral extends SnapshotPeripheral<BlockEntityReactorZirnox> {

    private static final int HEAT = 0;
    private static final int PRESSURE = 1;
    private static final int WATER = 2;
    private static final int STEAM = 3;
    private static final int CO2 = 4;
    private static final int ON = 5;

    public ZirnoxPeripheral(BlockEntityReactorZirnox machine) {
        super(machine, "zirnox_reactor", 6, 0);
    }

    @Override
    protected void capture(BlockEntityReactorZirnox machine) {
        put(HEAT, machine.heat);
        put(PRESSURE, machine.pressure);
        put(WATER, machine.water.getFill());
        put(STEAM, machine.steam.getFill());
        put(CO2, machine.carbonDioxide.getFill());
        put(ON, machine.isOn);
    }

    private static long temp(int heat) {
        return Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
    }

    private static long pressure(int pressure) {
        return Math.round(pressure * 1.0E-5D * 30.0D);
    }

    @LuaFunction
    public final Object[] getTemp() {
        return read(s -> new Object[] {temp(s.intAt(HEAT))});
    }

    @LuaFunction
    public final Object[] getPressure() {
        return read(s -> new Object[] {pressure(s.intAt(PRESSURE))});
    }

    @LuaFunction
    public final Object[] getWater() {
        return read(s -> new Object[] {s.intAt(WATER)});
    }

    @LuaFunction
    public final Object[] getSteam() {
        return read(s -> new Object[] {s.intAt(STEAM)});
    }

    @LuaFunction
    public final Object[] getCarbonDioxide() {
        return read(s -> new Object[] {s.intAt(CO2)});
    }

    @LuaFunction
    public final Object[] isActive() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            temp(s.intAt(HEAT)),
                            pressure(s.intAt(PRESSURE)),
                            s.intAt(WATER),
                            s.intAt(STEAM),
                            s.intAt(CO2),
                            s.booleanAt(ON)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] getFuel(int index) {
        if (index < 0 || index >= 24) return new Object[] {"", 0, 0, 0, false};
        ItemStack stack = machine().getItem(index);
        if (!(stack.getItem() instanceof ItemZirnoxRod rod) || rod.type.breeding) {
            return new Object[] {"", 0, 0, 0, false};
        }

        return new Object[] {
            rod.type.legacyName(), ItemZirnoxRod.getLifeTime(stack), rod.type.maxLife, 0, false
        };
    }

    @LuaFunction(mainThread = true)
    public final Object[] setActive(boolean active) {
        BlockEntityReactorZirnox machine = machine();
        machine.isOn = active;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] ventCarbonDioxide(Optional<Integer> amount) {
        BlockEntityReactorZirnox machine = machine();
        int vent = Math.clamp(amount.orElse(1000), 0, machine.carbonDioxide.getMaxFill());
        machine.carbonDioxide.setFill(Math.max(machine.carbonDioxide.getFill() - vent, 0));
        return new Object[] {};
    }
}
