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

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.compat.computercraft.SteamTypes;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKBoilerColumnPeripheral extends RBMKColumnPeripheral<RBMKBoilerBlockEntity> {

    private static final int STEAM = SLOTS;
    private static final int STEAM_MAX = SLOTS + 1;
    private static final int FEED = SLOTS + 2;
    private static final int FEED_MAX = SLOTS + 3;
    private static final int GRADE = SLOTS + 4;

    public RBMKBoilerColumnPeripheral(RBMKBoilerBlockEntity machine) {
        super(machine, "rbmk_boiler", SLOTS + 5, 0);
    }

    @Override
    protected void capture(RBMKBoilerBlockEntity machine) {
        super.capture(machine);
        put(STEAM, machine.steam.getFill());
        put(STEAM_MAX, machine.steam.getMaxFill());
        put(FEED, machine.feed.getFill());
        put(FEED_MAX, machine.feed.getMaxFill());
        put(GRADE, SteamTypes.toInt(machine.steam.getTankType()));
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {doubleAt(HEAT)};
    }

    @LuaFunction
    public Object[] getSteam() {
        return new Object[] {intAt(STEAM)};
    }

    @LuaFunction
    public Object[] getSteamMax() {
        return new Object[] {intAt(STEAM_MAX)};
    }

    @LuaFunction
    public Object[] getWater() {
        return new Object[] {intAt(FEED)};
    }

    @LuaFunction
    public Object[] getWaterMax() {
        return new Object[] {intAt(FEED_MAX)};
    }

    @LuaFunction
    public Object[] getSteamType() {
        return new Object[] {intAt(GRADE)};
    }

    @LuaFunction(mainThread = true)
    public Object[] setSteamType(int grade) {
        var machine = machine();
        var before = machine.steam.getTankType();
        machine.steam.setTankType(SteamTypes.fromInt(grade));
        if (machine.steam.getTankType() != before) machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            s.intAt(STEAM),
                            s.intAt(STEAM_MAX),
                            s.intAt(FEED),
                            s.intAt(FEED_MAX),
                            s.intAt(GRADE),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }
}
