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

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.inventory.fluid.NTMFluids;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKHeaterColumnPeripheral extends RBMKColumnPeripheral<RBMKHeaterBlockEntity> {

    private static final int FEED = SLOTS;
    private static final int FEED_MAX = SLOTS + 1;
    private static final int OUTPUT = SLOTS + 2;
    private static final int OUTPUT_MAX = SLOTS + 3;
    private static final int FEED_NAME = 0;
    private static final int OUTPUT_NAME = 1;

    public RBMKHeaterColumnPeripheral(RBMKHeaterBlockEntity machine) {
        super(machine, "rbmk_heater", SLOTS + 4, 2);
    }

    @Override
    protected void capture(RBMKHeaterBlockEntity machine) {
        super.capture(machine);
        put(FEED, machine.feed.getFill());
        put(FEED_MAX, machine.feed.getMaxFill());
        put(OUTPUT, machine.steam.getFill());
        put(OUTPUT_MAX, machine.steam.getMaxFill());
        putRef(FEED_NAME, NTMFluids.legacyName(machine.feed.getTankType()));
        putRef(OUTPUT_NAME, NTMFluids.legacyName(machine.steam.getTankType()));
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {doubleAt(HEAT)};
    }

    @LuaFunction
    public Object[] getFill() {
        return new Object[] {intAt(FEED)};
    }

    @LuaFunction
    public Object[] getFillMax() {
        return new Object[] {intAt(FEED_MAX)};
    }

    @LuaFunction
    public Object[] getExport() {
        return new Object[] {intAt(OUTPUT)};
    }

    @LuaFunction
    public Object[] getExportMax() {
        return new Object[] {intAt(OUTPUT_MAX)};
    }

    @LuaFunction
    public Object[] getFillType() {
        return new Object[] {refAt(FEED_NAME)};
    }

    @LuaFunction
    public Object[] getExportType() {
        return new Object[] {refAt(OUTPUT_NAME)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            s.intAt(FEED),
                            s.intAt(FEED_MAX),
                            s.intAt(OUTPUT),
                            s.intAt(OUTPUT_MAX),
                            s.refAt(FEED_NAME),
                            s.refAt(OUTPUT_NAME),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }
}
