// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.albion;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.albion.BlockEntityCooledBase;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public abstract class CooledPeripheral<BE extends BlockEntityCooledBase>
        extends SnapshotPeripheral<BE> {

    protected static final int POWER = 0;
    protected static final int MAX_POWER = 1;
    protected static final int COOLANT = 2;
    protected static final int COOLANT_MAX = 3;
    protected static final int HOT = 4;
    protected static final int HOT_MAX = 5;
    protected static final int SLOTS = 6;

    protected CooledPeripheral(BE machine, String type, int longs, int refs) {
        super(machine, type, longs, refs);
    }

    @Override
    protected void capture(BE machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(COOLANT, machine.coolantTanks[0].getFill());
        put(COOLANT_MAX, machine.coolantTanks[0].getMaxFill());
        put(HOT, machine.coolantTanks[1].getFill());
        put(HOT_MAX, machine.coolantTanks[1].getMaxFill());
    }

    protected final void captureCrafting(BE machine, int firstCount, int firstName) {
        for (int i = 0; i < 4; i++) {
            ItemStack stack = machine.getItem(i + 1);

            putRef(firstName + i, stack.isEmpty() ? "" : stack.getItem().getDescriptionId());
            put(firstCount + i, stack.isEmpty() ? 0 : stack.getCount());
        }
    }

    protected static Object[] crafting(SnapshotPeripheral<?> s, int firstCount, int firstName) {
        Object[] items = new Object[8];
        for (int i = 0; i < 4; i++) {
            items[i * 2] = s.refAt(firstName + i);
            items[i * 2 + 1] = s.intAt(firstCount + i);
        }
        return items;
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getCoolant() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(COOLANT), s.intAt(COOLANT_MAX), s.intAt(HOT), s.intAt(HOT_MAX)
                        });
    }
}
