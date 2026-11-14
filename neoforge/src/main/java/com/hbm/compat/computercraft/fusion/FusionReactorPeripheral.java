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

package com.hbm.compat.computercraft.fusion;

import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.compat.computercraft.albion.CooledPeripheral;
import com.hbm.compat.computercraft.albion.CooledValues;
import dan200.computercraft.api.lua.LuaFunction;

public final class FusionReactorPeripheral extends CooledPeripheral<FusionTorusBlockEntity> {

    private static final int TANKS = 4;
    private static final int FILLS = SLOTS;
    private static final int KLYSTRON = SLOTS + 2 * TANKS;
    private static final int PLASMA = KLYSTRON + 1;
    private static final int CONSUMPTION = KLYSTRON + 2;
    private static final int PROGRESS = KLYSTRON + 3;
    private static final int BONUS = KLYSTRON + 4;

    public FusionReactorPeripheral(FusionTorusBlockEntity machine) {
        super(machine, "ntm_fusion_torus", BONUS + 1, TANKS);
    }

    @Override
    protected void capture(FusionTorusBlockEntity machine) {
        super.capture(machine);
        for (var i = 0; i < TANKS; i++) {
            var tank = machine.tanks[i];
            put(FILLS + 2 * i, tank.getFill());
            put(FILLS + 2 * i + 1, tank.getMaxFill());
            putRef(i, FluidKeys.of(tank.getTankType()));
        }
        put(KLYSTRON, machine.klystronEnergy);
        put(PLASMA, machine.plasmaEnergy);
        put(CONSUMPTION, machine.fuelConsumption);
        put(PROGRESS, machine.module.progress);
        put(BONUS, machine.module.bonus);
    }

    private static int tanks(SnapshotPeripheral<?> s, Object[] out, int at) {
        for (var i = 0; i < TANKS; i++) {
            out[at + 3 * i] = s.intAt(FILLS + 2 * i);
            out[at + 3 * i + 1] = s.intAt(FILLS + 2 * i + 1);
            out[at + 3 * i + 2] = s.refAt(i);
        }
        return at + 3 * TANKS;
    }

    @LuaFunction
    public Object[] getFluid() {
        return read(
                s -> {
                    var out = new Object[3 * TANKS];
                    tanks(s, out, 0);
                    return out;
                });
    }

    @LuaFunction
    public Object[] getKlystronEnergy() {
        return new Object[] {longAt(KLYSTRON)};
    }

    @LuaFunction
    public Object[] getPlasmaEnergy() {
        return new Object[] {longAt(PLASMA)};
    }

    @LuaFunction
    public Object[] getFuelConsumption() {
        return new Object[] {doubleAt(CONSUMPTION)};
    }

    @LuaFunction
    public Object[] getRecipeProgress() {
        return read(s -> new Object[] {s.doubleAt(PROGRESS), s.doubleAt(BONUS)});
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    var out = new Object[23];
                    var at = CooledValues.energy(s, out, 0);
                    at = tanks(s, out, at);
                    at = CooledValues.coolant(s, out, at);
                    out[at] = s.longAt(KLYSTRON);
                    out[at + 1] = s.longAt(PLASMA);
                    out[at + 2] = s.doubleAt(CONSUMPTION);
                    out[at + 3] = s.doubleAt(PROGRESS);
                    out[at + 4] = s.doubleAt(BONUS);
                    return out;
                });
    }
}
