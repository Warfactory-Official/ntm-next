// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.BlockEntityCoreStabilizer;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class CoreStabilizerPeripheral extends SnapshotPeripheral<BlockEntityCoreStabilizer> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int WATTS = 2;
    private static final int HAS_LENS = 3;
    private static final int LENS_DAMAGE = 4;

    public CoreStabilizerPeripheral(BlockEntityCoreStabilizer machine) {
        super(machine, "dfc_stabilizer", 5, 0);
    }

    @Override
    protected void capture(BlockEntityCoreStabilizer machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(WATTS, machine.watts);
        ItemStack lens = machine.getItem(BlockEntityCoreStabilizer.SLOT_LENS);
        boolean usable = BlockEntityCoreStabilizer.isUsableLens(lens);
        put(HAS_LENS, usable);
        put(LENS_DAMAGE, usable ? lens.getDamageValue() : 0);
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getInput() {
        return read(s -> new Object[] {s.intAt(WATTS)});
    }

    @LuaFunction
    public final Object[] getDurability() {
        return read(
                s -> new Object[] {s.booleanAt(HAS_LENS) ? (Object) s.longAt(LENS_DAMAGE) : "N/A"});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER),
                            s.longAt(MAX_POWER),
                            s.intAt(WATTS),
                            s.booleanAt(HAS_LENS) ? (Object) s.longAt(LENS_DAMAGE) : "N/A"
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setInput(int input) {
        BlockEntityCoreStabilizer machine = machine();
        machine.watts = Math.clamp(input, 0, 100);
        machine.markChanged();
        return new Object[] {};
    }
}
