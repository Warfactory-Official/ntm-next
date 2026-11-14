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

import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class ResearchReactorPeripheral
        extends BlockEntityPeripheral<ResearchReactorBlockEntity> {

    public ResearchReactorPeripheral(ResearchReactorBlockEntity machine) {
        super(machine, "research_reactor");
    }

    @LuaFunction
    public Object[] getTemp() {
        return new Object[] {(int) target.heat};
    }

    @LuaFunction
    public Object[] getLevel() {
        return new Object[] {(double) (target.rodPosition) * 100};
    }

    @LuaFunction
    public Object[] getTargetLevel() {
        return new Object[] {(double) target.targetRodPosition};
    }

    @LuaFunction
    public Object[] getFlux() {
        return new Object[] {(int) target.totalFlux};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (int) target.heat,
            (double) target.rodPosition,
            (double) target.targetRodPosition,
            (int) target.totalFlux
        };
    }

    @LuaFunction(mainThread = true)
    public Object[] setLevel(double level) {
        ResearchReactorBlockEntity machine = machine();
        machine.requestRodPosition(Math.clamp(level / 100.0, 0, 1.0));
        return new Object[] {};
    }
}
