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

import com.hbm.blockentity.machine.dfc.DfcStabilizerBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class CoreStabilizerPeripheral extends SnapshotPeripheral<DfcStabilizerBlockEntity> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int WATTS = 2;
    private static final int HAS_LENS = 3;
    private static final int LENS_DAMAGE = 4;

    public CoreStabilizerPeripheral(DfcStabilizerBlockEntity machine) {
        super(machine, "dfc_stabilizer", 5, 0);
    }

    @Override
    protected void capture(DfcStabilizerBlockEntity machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(WATTS, machine.watts);
        ItemStack lens = machine.getItem(DfcStabilizerBlockEntity.SLOT_LENS);
        boolean usable = DfcStabilizerBlockEntity.usableLens(lens);
        put(HAS_LENS, usable);
        put(LENS_DAMAGE, usable ? lens.getDamageValue() : 0);
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public Object[] getInput() {
        return new Object[] {intAt(WATTS)};
    }

    @LuaFunction
    public Object[] getDurability() {
        return read(
                s -> new Object[] {s.booleanAt(HAS_LENS) ? (Object) s.longAt(LENS_DAMAGE) : "N/A"});
    }

    @LuaFunction
    public Object[] getInfo() {
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
    public Object[] setInput(int input) {
        DfcStabilizerBlockEntity machine = machine();
        machine.setInputPercent(input);
        return new Object[] {};
    }
}
