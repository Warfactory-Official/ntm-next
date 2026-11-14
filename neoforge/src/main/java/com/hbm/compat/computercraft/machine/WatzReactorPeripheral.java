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

import com.hbm.blockentity.machine.WatzBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class WatzReactorPeripheral extends BlockEntityPeripheral<WatzBlockEntity> {

    public WatzReactorPeripheral(WatzBlockEntity machine) {
        super(machine, "watz_reactor");
    }

    private double flux() {
        return target.fluxLastBase + target.fluxLastReaction;
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {target.heat};
    }

    @LuaFunction
    public Object[] getFlux() {
        return new Object[] {flux()};
    }

    @LuaFunction
    public Object[] getCoolantInfo() {
        var tanks = target.tanks;
        return new Object[] {
            tanks[0].getFill(), tanks[0].getMaxFill(), tanks[1].getFill(), tanks[1].getMaxFill()
        };
    }

    @LuaFunction
    public Object[] getWasteInfo() {
        var waste = target.tanks[2];
        return new Object[] {waste.getFill(), waste.getMaxFill()};
    }

    @LuaFunction
    public Object[] isOn() {
        return new Object[] {target.isOn};
    }

    @LuaFunction
    public Object[] getInfo() {
        var tanks = target.tanks;
        return new Object[] {
            tanks[0].getFill(),
            tanks[0].getMaxFill(),
            tanks[1].getFill(),
            tanks[1].getMaxFill(),
            tanks[2].getFill(),
            tanks[2].getMaxFill(),
            target.heat,
            flux(),
            target.isOn
        };
    }
}
