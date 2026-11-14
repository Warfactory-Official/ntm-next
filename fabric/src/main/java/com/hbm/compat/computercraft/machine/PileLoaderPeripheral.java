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

import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.PileRodMK2Item;
import dan200.computercraft.api.lua.LuaFunction;

public final class PileLoaderPeripheral extends SnapshotPeripheral<PileLoaderBlockEntity> {
    private static final int TEMP = 0;
    private static final int DEPLETION = 1;
    private static final int LIFETIME = 2;
    private static final int TYPE = 3;
    private static final int LOADING_TYPE = 4;
    private static final int LOADING = 5;

    public PileLoaderPeripheral(PileLoaderBlockEntity machine) {
        super(machine, "ntm_pile_loader", 6, 0);
    }

    @Override
    protected void capture(PileLoaderBlockEntity machine) {
        var channel = machine.observedRod;
        var staged = machine.getItem(0);
        put(TEMP, machine.observedHeat);
        put(DEPLETION, PileRodMK2Item.getDepletion(channel));
        put(LIFETIME, channel.getItem() instanceof PileRodMK2Item rod ? rod.type.life : 0D);
        put(TYPE, channel.getItem() instanceof PileRodMK2Item rod ? rod.type.ordinal() : -1);
        put(LOADING_TYPE, staged.getItem() instanceof PileRodMK2Item rod ? rod.type.ordinal() : -1);
        put(LOADING, machine.extending);
    }

    @LuaFunction
    public Object[] getTemp() {
        return new Object[] {doubleAt(TEMP)};
    }

    @LuaFunction
    public Object[] getDepletion() {
        return new Object[] {doubleAt(DEPLETION)};
    }

    @LuaFunction
    public Object[] getLifetime() {
        return new Object[] {doubleAt(LIFETIME)};
    }

    @LuaFunction("getType")
    public Object[] rodType() {
        return new Object[] {intAt(TYPE)};
    }

    @LuaFunction
    public Object[] getLoadingType() {
        return new Object[] {intAt(LOADING_TYPE)};
    }

    @LuaFunction
    public Object[] isLoading() {
        return new Object[] {booleanAt(LOADING)};
    }

    @LuaFunction(mainThread = true)
    public Object[] load() {
        machine().startLoading();
        return new Object[] {};
    }
}
