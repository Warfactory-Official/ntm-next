// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.tileentity.machine.BlockEntityICF;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class ICFPeripheral extends SnapshotPeripheral<BlockEntityICF> {

    private static final int HEAT = 0;
    private static final int HEATUP = 1;
    private static final int LASER = 2;
    private static final int FILL_0 = 3;
    private static final int MAX_0 = 4;
    private static final int FILL_1 = 5;
    private static final int MAX_1 = 6;
    private static final int FILL_2 = 7;
    private static final int MAX_2 = 8;
    private static final int PELLET = 9;
    private static final int DEPLETION = 10;
    private static final int MAX_DEPLETION = 11;
    private static final int DIFFICULTY = 12;
    private static final int NAME_0 = 0;
    private static final int NAME_1 = 1;
    private static final int FUEL_1 = 2;
    private static final int FUEL_2 = 3;

    public ICFPeripheral(BlockEntityICF machine) {
        super(machine, "ntm_icf_reactor", 13, 4);
    }

    @Override
    protected void capture(BlockEntityICF machine) {
        put(HEAT, machine.heat);
        put(HEATUP, machine.heatup);
        put(LASER, machine.laser);
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
        put(FILL_2, machine.tanks[2].getFill());
        put(MAX_2, machine.tanks[2].getMaxFill());
        putRef(NAME_0, unlocalized(machine, 0));
        putRef(NAME_1, unlocalized(machine, 1));
        ItemStack pellet = machine.getItem(BlockEntityICF.SLOT_LOADED);
        put(PELLET, !pellet.isEmpty());
        if (!pellet.isEmpty()) {
            put(DEPLETION, ItemICFPellet.getDepletion(pellet));
            put(MAX_DEPLETION, ItemICFPellet.getMaxDepletion(pellet));
            put(DIFFICULTY, ItemICFPellet.getFusingDifficulty(pellet));
            putRef(FUEL_1, ItemICFPellet.getType(pellet, true).name());
            putRef(FUEL_2, ItemICFPellet.getType(pellet, false).name());
        }
    }

    private static String unlocalized(BlockEntityICF machine, int tank) {
        return "hbmfluid."
                + NTMFluids.legacyName(machine.tanks[tank].getTankType()).toLowerCase(Locale.US);
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[] {s.longAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getHeatingRate() {
        return read(s -> new Object[] {s.longAt(HEATUP)});
    }

    @LuaFunction
    public final Object[] getMaxHeat() {
        return new Object[] {BlockEntityICF.maxHeat};
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[] {s.longAt(LASER)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.refAt(NAME_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1),
                            s.refAt(NAME_1),
                            s.intAt(FILL_2),
                            s.intAt(MAX_2)
                        });
    }

    @LuaFunction
    public final Object[] getPelletStats() {
        return read(
                s ->
                        s.booleanAt(PELLET)
                                ? new Object[] {
                                    s.longAt(DEPLETION),
                                    s.longAt(MAX_DEPLETION),
                                    s.longAt(DIFFICULTY),
                                    s.refAt(FUEL_1),
                                    s.refAt(FUEL_2)
                                }
                                : OpenComputers.unknownError());
    }
}
