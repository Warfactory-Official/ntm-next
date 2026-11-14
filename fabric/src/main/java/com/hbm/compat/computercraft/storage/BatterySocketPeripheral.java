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

package com.hbm.compat.computercraft.storage;

import com.hbm.api.energymk2.EnergyItem;
import com.hbm.blockentity.machine.storage.SocketBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class BatterySocketPeripheral extends StorageModesPeripheral<SocketBlockEntity> {

    private static final int POWER = MODE_NUMBERS;
    private static final int MAX_POWER = MODE_NUMBERS + 1;
    private static final int DELTA = MODE_NUMBERS + 2;
    private static final int HAS_PACK = MODE_NUMBERS + 3;
    private static final int CHARGE_RATE = MODE_NUMBERS + 4;
    private static final int DISCHARGE_RATE = MODE_NUMBERS + 5;
    private static final int PACK_NAME = 0;

    public BatterySocketPeripheral(SocketBlockEntity machine) {
        super(machine, MODE_NUMBERS + 6, 1);
    }

    @Override
    protected void capture(SocketBlockEntity machine) {
        super.capture(machine);
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(DELTA, machine.delta);
        ItemStack stack = machine.getItem(SocketBlockEntity.SLOT_BATTERY);
        boolean pack = stack.getItem() instanceof EnergyItem;
        put(HAS_PACK, pack);
        if (pack) {
            EnergyItem battery = (EnergyItem) stack.getItem();
            putRef(PACK_NAME, stack.getItem().getDescriptionId());
            put(CHARGE_RATE, battery.getChargeRate(stack));
            put(DISCHARGE_RATE, battery.getDischargeRate(stack));
        }
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER), s.longAt(DELTA)});
    }

    @LuaFunction
    public Object[] getPackInfo() {
        return read(
                s ->
                        s.booleanAt(HAS_PACK)
                                ? new Object[] {
                                    s.refAt(PACK_NAME),
                                    s.longAt(CHARGE_RATE),
                                    s.longAt(DISCHARGE_RATE)
                                }
                                : new Object[] {"", 0, 0});
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    boolean pack = s.booleanAt(HAS_PACK);
                    return new Object[] {
                        s.longAt(POWER),
                        s.longAt(MAX_POWER),
                        s.longAt(DELTA),
                        s.intAt(MODE_LOW),
                        s.intAt(MODE_HIGH),
                        s.intAt(PRIORITY),
                        pack ? s.refAt(PACK_NAME) : "",
                        pack ? (Object) s.longAt(CHARGE_RATE) : (Object) 0,
                        pack ? (Object) s.longAt(DISCHARGE_RATE) : (Object) 0
                    };
                });
    }
}
