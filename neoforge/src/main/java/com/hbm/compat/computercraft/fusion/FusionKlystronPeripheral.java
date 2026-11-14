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

import com.hbm.blockentity.machine.fusion.FusionKlystronBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class FusionKlystronPeripheral
        extends BlockEntityPeripheral<FusionKlystronBlockEntity> {

    public FusionKlystronPeripheral(FusionKlystronBlockEntity machine) {
        super(machine, "ntm_fusion_klystron");
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return new Object[] {(long) target.getPower(), (long) target.getMaxPower()};
    }

    @LuaFunction
    public Object[] getAir() {
        return new Object[] {(int) target.compair.getFill(), (int) target.compair.getMaxFill()};
    }

    @LuaFunction
    public Object[] getOutput() {
        return new Object[] {(long) target.output, (long) target.outputTarget};
    }

    @LuaFunction(mainThread = true)
    public Object[] setOutput(double output) {
        FusionKlystronBlockEntity machine = machine();
        machine.hbm$syncSet$FusionKlystronBlockEntity$outputTarget(
                (long) Math.clamp(output, 0.0, FusionKlystronBlockEntity.MAX_OUTPUT));
        machine.setChanged();
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getInfo() {
        return new Object[] {
            (long) target.getPower(),
            (long) target.getMaxPower(),
            (int) target.compair.getFill(),
            (int) target.compair.getMaxFill(),
            (long) target.output,
            (long) target.outputTarget
        };
    }
}
