// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.items.ItemPistons.EnumPistonType;
import com.hbm.items.ItemPistons;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class CombustionEnginePeripheral
        extends SnapshotPeripheral<BlockEntityMachineCombustionEngine> {

    private static final int FILL = 0;
    private static final int MAX = 1;
    private static final int POWER = 2;
    private static final int SETTING = 3;
    private static final int ON = 4;
    private static final int HAS_EFFICIENCY = 5;
    private static final int EFFICIENCY = 6;
    private static final int TYPE = 0;

    public CombustionEnginePeripheral(BlockEntityMachineCombustionEngine machine) {
        super(machine, "ntm_combustion_engine", 7, 1);
    }

    @Override
    protected void capture(BlockEntityMachineCombustionEngine machine) {
        put(FILL, machine.tank.getFill());
        put(MAX, machine.tank.getMaxFill());
        put(POWER, machine.power);
        put(SETTING, machine.setting);
        put(ON, machine.isOn);
        putRef(TYPE, NTMFluids.legacyName(machine.tank.getTankType()));
        ItemStack stack = machine.getItem(BlockEntityMachineCombustionEngine.SLOT_PISTON);
        FT_Combustible trait =
                NTMFluidProperties.getTrait(machine.tank.getTankType(), FT_Combustible.class);

        boolean known = !stack.isEmpty() && trait != null;
        put(HAS_EFFICIENCY, known);
        if (known) {
            EnumPistonType piston =
                    stack.getItem() instanceof ItemPistons pistons
                            ? pistons.type
                            : EnumPistonType.VALUES[0];
            put(EFFICIENCY, piston.eff[trait.getGrade().ordinal()]);
        }
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[] {s.intAt(FILL), s.intAt(MAX)});
    }

    @LuaFunction("getType")
    public final Object[] fluidType() {
        return read(s -> new Object[] {s.refAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[] {s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getThrottle() {
        return read(s -> new Object[] {s.intAt(SETTING)});
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[] {s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getEfficiency() {
        return read(
                s ->
                        s.booleanAt(HAS_EFFICIENCY)
                                ? new Object[] {s.doubleAt(EFFICIENCY)}
                                : OpenComputers.unknownError());
    }

    @LuaFunction(mainThread = true)
    public final Object[] setThrottle(int throttle) {
        if (throttle < 0 || throttle > 30)
            return new Object[] {false, "Throttle request outside of range 0-30"};
        BlockEntityMachineCombustionEngine machine = machine();
        machine.setting = throttle;
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] start() {
        BlockEntityMachineCombustionEngine machine = machine();
        machine.isOn = true;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] stop() {
        BlockEntityMachineCombustionEngine machine = machine();
        machine.isOn = false;
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        s.booleanAt(HAS_EFFICIENCY)
                                ? new Object[] {
                                    s.intAt(SETTING),
                                    s.booleanAt(ON),
                                    s.longAt(POWER),
                                    s.doubleAt(EFFICIENCY),
                                    s.intAt(FILL),
                                    s.intAt(MAX),
                                    s.refAt(TYPE)
                                }
                                : OpenComputers.unknownError());
    }
}
