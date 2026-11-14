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

import com.hbm.blockentity.machine.TurbineGasBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import dan200.computercraft.api.lua.LuaFunction;

public final class GasTurbineGeneratorPeripheral
        extends BlockEntityPeripheral<TurbineGasBlockEntity> {

    private static final int OFFLINE = 0;
    private static final int STARTING = -1;
    private static final int ONLINE = 1;
    private static final int SLIDER_STEPS = 60;

    public GasTurbineGeneratorPeripheral(TurbineGasBlockEntity machine) {
        super(machine, "ntm_gas_turbine");
    }

    private Object[] tanks(Object[] out, int at) {
        for (var i = 0; i < 4; i++) {
            out[at + 2 * i] = target.tanks[i].getFill();
            out[at + 2 * i + 1] = target.tanks[i].getMaxFill();
        }
        return out;
    }

    @LuaFunction
    public Object[] getFluid() {
        return tanks(new Object[8], 0);
    }

    @LuaFunction("getType")
    public Object[] fuelName() {
        return new Object[] {NTMFluids.legacyName(target.tanks[0].getTankType())};
    }

    @LuaFunction
    public Object[] getPower() {
        return new Object[] {target.power};
    }

    @LuaFunction
    public Object[] getThrottle() {
        return new Object[] {target.throttle};
    }

    @LuaFunction
    public Object[] getState() {
        return new Object[] {target.state};
    }

    @LuaFunction
    public Object[] getAuto() {
        return new Object[] {target.autoMode};
    }

    @LuaFunction(mainThread = true)
    public Object[] setThrottle(int percent) {
        if (percent < 0 || percent > 100) return new Object[] {null, "Input out of range."};
        var slider = percent * SLIDER_STEPS / 100;
        var machine = machine();
        if (machine.powerSliderPos != slider) {
            machine.hbm$syncSet$TurbineGasBlockEntity$powerSliderPos(slider);
            machine.setChanged();
        }
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public void setAuto(boolean on) {
        var machine = machine();
        if (machine.autoMode == on) return;
        machine.hbm$syncSet$TurbineGasBlockEntity$autoMode(on);
        machine.setChanged();
    }

    @LuaFunction(mainThread = true)
    public void start() {
        transition(OFFLINE, STARTING);
    }

    @LuaFunction(mainThread = true)
    public void stop() {
        transition(ONLINE, OFFLINE);
    }

    private void transition(int from, int to) {
        var machine = machine();
        if (machine.state != from) return;
        machine.hbm$syncSet$TurbineGasBlockEntity$state(to);
        machine.setChanged();
    }

    @LuaFunction
    public Object[] getInfo() {
        var out = new Object[10];
        out[0] = target.throttle;
        out[1] = target.state;
        return tanks(out, 2);
    }
}
