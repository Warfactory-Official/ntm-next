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

package com.hbm.compat.computercraft.network;

import com.hbm.blockentity.network.FluidPumpBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.LuaEnums;
import dan200.computercraft.api.lua.LuaFunction;

public final class PumpPeripheral extends BlockEntityPeripheral<FluidPumpBlockEntity> {

    public PumpPeripheral(FluidPumpBlockEntity machine) {
        super(machine, "ntm_fluid_pump");
    }

    @LuaFunction
    public Object[] getFluid() {
        return new Object[] {FluidKeys.of(target.type)};
    }

    @LuaFunction
    public Object[] getPressure() {
        return new Object[] {target.pressure};
    }

    @LuaFunction
    public Object[] getFlow() {
        return new Object[] {target.rate};
    }

    @LuaFunction
    public Object[] getPriority() {
        return new Object[] {LuaEnums.priority(target.priority)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            FluidKeys.of(target.type),
            target.pressure,
            target.rate,
            LuaEnums.priority(target.priority)
        };
    }

    @LuaFunction(mainThread = true)
    public Object[] setPriority(int index) {
        var priority = LuaEnums.priority(index);
        if (priority == null) return new Object[] {null, "Not a valid Priority."};
        var machine = machine();
        if (machine.priority != priority) {
            machine.hbm$syncSet$FluidPumpBlockEntity$priority(priority);
            machine.setChanged();
        }
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public Object[] setFlow(int rate) {
        if (rate < 0 || rate > FluidPumpBlockEntity.MAX_RATE)
            return new Object[] {null, "Number outside of bounds."};
        var machine = machine();
        if (machine.rate != rate) {
            machine.hbm$syncSet$FluidPumpBlockEntity$rate(rate);
            machine.setChanged();
        }
        return new Object[] {true};
    }
}
