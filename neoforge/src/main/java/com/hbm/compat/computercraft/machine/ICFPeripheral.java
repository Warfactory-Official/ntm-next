/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.compat.computercraft.machine;

import com.hbm.blockentity.machine.ICFBlockEntity;
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.ICFPelletItem;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class ICFPeripheral extends SnapshotPeripheral<ICFBlockEntity> {

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

    public ICFPeripheral(ICFBlockEntity machine) {
        super(machine, "ntm_icf_reactor", 13, 4);
    }

    @Override
    protected void capture(ICFBlockEntity machine) {
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
        ItemStack pellet = machine.getItem(ICFBlockEntity.SLOT_LOADED);
        put(PELLET, !pellet.isEmpty());
        if (!pellet.isEmpty()) {
            put(DEPLETION, ICFPelletItem.getDepletion(pellet));
            put(MAX_DEPLETION, ICFPelletItem.getMaxDepletion(pellet));
            put(DIFFICULTY, ICFPelletItem.getFusingDifficulty(pellet));
            putRef(FUEL_1, ICFPelletItem.getType(pellet, true).name());
            putRef(FUEL_2, ICFPelletItem.getType(pellet, false).name());
        }
    }

    private static String unlocalized(ICFBlockEntity machine, int tank) {
        return "hbmfluid."
                + NTMFluids.legacyName(machine.tanks[tank].getTankType()).toLowerCase(Locale.ROOT);
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {longAt(HEAT)};
    }

    @LuaFunction
    public Object[] getHeatingRate() {
        return new Object[] {longAt(HEATUP)};
    }

    @LuaFunction
    public Object[] getMaxHeat() {
        return new Object[] {ICFBlockEntity.maxHeat};
    }

    @LuaFunction
    public Object[] getPower() {
        return new Object[] {longAt(LASER)};
    }

    @LuaFunction
    public Object[] getFluid() {
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
    public Object[] getPelletStats() {
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
