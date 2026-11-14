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

package com.hbm.compat.computercraft.albion;

import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public abstract class CooledPeripheral<BE extends CooledAcceleratorBlockEntity>
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
