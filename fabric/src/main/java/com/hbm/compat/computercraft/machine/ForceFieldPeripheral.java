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

import com.hbm.blockentity.machine.RepulsionFieldBlockEntity;
import com.hbm.compat.computercraft.BlockEntityPeripheral;
import dan200.computercraft.api.lua.LuaFunction;

public final class ForceFieldPeripheral extends BlockEntityPeripheral<RepulsionFieldBlockEntity> {

    public ForceFieldPeripheral(RepulsionFieldBlockEntity machine) {
        super(machine, "ntm_forcefield");
    }

    @LuaFunction
    public Object[] getEnergyInfo() {
        return new Object[] {target.getPower(), target.getMaxPower()};
    }

    @LuaFunction
    public Object[] getHealth() {
        return new Object[] {target.shieldHealth, target.shieldCapacity};
    }

    @LuaFunction
    public double getRadius() {
        return target.fieldRadius;
    }

    @LuaFunction
    public int getCooldown() {
        return target.recoveryTicks;
    }

    @LuaFunction
    public int getPowerConsumption() {
        return target.energyPerTick;
    }

    @LuaFunction
    public boolean isOn() {
        return target.requestedOn;
    }

    @LuaFunction
    public boolean isActive() {
        return target.isOperational();
    }

    @LuaFunction(mainThread = true)
    public void setOn(boolean on) {
        machine().setRequestedOn(on);
    }
}
