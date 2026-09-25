// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.fusion;

import com.hbm.compat.computercraft.albion.CooledPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;

public final class FusionTorusPeripheral extends CooledPeripheral<BlockEntityFusionTorus> {

    private static final int FILL = SLOTS;
    private static final int KLYSTRON = SLOTS + 8;
    private static final int PLASMA = SLOTS + 9;
    private static final int CONSUMPTION = SLOTS + 10;
    private static final int PROGRESS = SLOTS + 11;
    private static final int BONUS = SLOTS + 12;

    public FusionTorusPeripheral(BlockEntityFusionTorus machine) {
        super(machine, "ntm_fusion_torus", SLOTS + 13, 4);
    }

    @Override
    protected void capture(BlockEntityFusionTorus machine) {
        super.capture(machine);
        for (int i = 0; i < 4; i++) {
            put(FILL + 2 * i, machine.tanks[i].getFill());
            put(FILL + 2 * i + 1, machine.tanks[i].getMaxFill());
            putRef(
                    i,
                    "hbmfluid."
                            + NTMFluids.legacyName(machine.tanks[i].getTankType())
                                    .toLowerCase(Locale.US));
        }
        put(KLYSTRON, machine.klystronEnergy);
        put(PLASMA, machine.plasmaEnergy);
        put(CONSUMPTION, machine.fuelConsumption);
        put(PROGRESS, machine.module.progress);
        put(BONUS, machine.module.bonus);
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL),
                            s.intAt(FILL + 1),
                            s.refAt(0),
                            s.intAt(FILL + 2),
                            s.intAt(FILL + 3),
                            s.refAt(1),
                            s.intAt(FILL + 4),
                            s.intAt(FILL + 5),
                            s.refAt(2),
                            s.intAt(FILL + 6),
                            s.intAt(FILL + 7),
                            s.refAt(3)
                        });
    }

    @LuaFunction
    public final Object[] getKlystronEnergy() {
        return read(s -> new Object[] {s.longAt(KLYSTRON)});
    }

    @LuaFunction
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[] {s.longAt(PLASMA)});
    }

    @LuaFunction
    public final Object[] getFuelConsumption() {
        return read(s -> new Object[] {s.doubleAt(CONSUMPTION)});
    }

    @LuaFunction
    public final Object[] getRecipeProgress() {
        return read(s -> new Object[] {s.doubleAt(PROGRESS), s.doubleAt(BONUS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.longAt(POWER),
                            s.longAt(MAX_POWER),
                            s.intAt(FILL),
                            s.intAt(FILL + 1),
                            s.refAt(0),
                            s.intAt(FILL + 2),
                            s.intAt(FILL + 3),
                            s.refAt(1),
                            s.intAt(FILL + 4),
                            s.intAt(FILL + 5),
                            s.refAt(2),
                            s.intAt(FILL + 6),
                            s.intAt(FILL + 7),
                            s.refAt(3),
                            s.intAt(COOLANT),
                            s.intAt(COOLANT_MAX),
                            s.intAt(HOT),
                            s.intAt(HOT_MAX),
                            s.longAt(KLYSTRON),
                            s.longAt(PLASMA),
                            s.doubleAt(CONSUMPTION),
                            s.doubleAt(PROGRESS),
                            s.doubleAt(BONUS)
                        });
    }
}
