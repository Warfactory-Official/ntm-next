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

import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.inventory.fluid.NTMFluids;
import dan200.computercraft.api.lua.LuaFunction;

public final class RBMKOutgasserColumnPeripheral
        extends RBMKColumnPeripheral<RBMKIrradiationColumnBlockEntity> {

    private static final int GAS = SLOTS;
    private static final int GAS_MAX = SLOTS + 1;
    private static final int PROGRESS = SLOTS + 2;
    private static final int GAS_ID = SLOTS + 3;
    private static final int INPUT_COUNT = SLOTS + 4;
    private static final int GAS_NAME = 0;
    private static final int INPUT_NAME = 1;

    public RBMKOutgasserColumnPeripheral(RBMKIrradiationColumnBlockEntity machine) {
        super(machine, "rbmk_outgasser", SLOTS + 5, 2);
    }

    @Override
    protected void capture(RBMKIrradiationColumnBlockEntity machine) {
        super.capture(machine);
        var gas = machine.gas.getTankType();
        put(GAS, machine.gas.getFill());
        put(GAS_MAX, machine.gas.getMaxFill());
        put(PROGRESS, machine.progress);
        put(GAS_ID, NTMFluids.legacyId(gas));
        putRef(GAS_NAME, NTMFluids.legacyName(gas));
        var input = machine.getItem(RBMKIrradiationColumnBlockEntity.SLOT_INPUT);
        putRef(INPUT_NAME, input.isEmpty() ? "" : input.getItem().getDescriptionId());
        put(INPUT_COUNT, input.getCount());
    }

    @LuaFunction
    public Object[] getGas() {
        return new Object[] {intAt(GAS)};
    }

    @LuaFunction
    public Object[] getGasMax() {
        return new Object[] {intAt(GAS_MAX)};
    }

    @LuaFunction
    public Object[] getGasType() {
        return new Object[] {refAt(GAS_NAME)};
    }

    @LuaFunction
    public Object[] getProgress() {
        return new Object[] {doubleAt(PROGRESS)};
    }

    @LuaFunction
    public Object[] getCrafting() {
        return read(s -> new Object[] {s.refAt(INPUT_NAME), s.intAt(INPUT_COUNT)});
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(GAS),
                            s.intAt(GAS_MAX),
                            s.doubleAt(PROGRESS),
                            s.intAt(GAS_ID),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z),
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_COUNT)
                        });
    }
}
