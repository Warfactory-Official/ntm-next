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

package com.hbm.compat.computercraft.storage;

import com.hbm.blockentity.machine.storage.MassStorageBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class MassStoragePeripheral extends SnapshotPeripheral<MassStorageBlockEntity> {

    private static final int FILL = 0;
    private static final int CAPACITY = 1;
    private static final int OUTPUT = 2;
    private static final int TYPE_NAME = 0;

    private ItemStack lastType = ItemStack.EMPTY;
    private String typeName = "None";

    public MassStoragePeripheral(MassStorageBlockEntity machine) {
        super(machine, "ntm_mass_storage", 3, 1);
    }

    @Override
    protected void capture(MassStorageBlockEntity machine) {
        put(FILL, machine.getStockpile());
        put(CAPACITY, machine.getCapacity());
        put(OUTPUT, machine.isOutput());
        ItemStack type = machine.getItem(MassStorageBlockEntity.SLOT_TYPE);
        if (!ItemStack.isSameItemSameComponents(type, lastType)) {
            typeName = type.isEmpty() ? "None" : type.getHoverName().getString();
            lastType = type.copy();
        }
        putRef(TYPE_NAME, typeName);
    }

    @LuaFunction
    public Object[] getFill() {
        return new Object[] {intAt(FILL)};
    }

    @LuaFunction
    public Object[] getCapacity() {
        return new Object[] {intAt(CAPACITY)};
    }

    @LuaFunction("getType")
    public Object[] storedType() {
        return new Object[] {refAt(TYPE_NAME)};
    }

    @LuaFunction
    public Object[] getOutputMode() {
        return new Object[] {booleanAt(OUTPUT)};
    }

    @LuaFunction(mainThread = true)
    public Object[] setOutputMode(boolean mode) {
        MassStorageBlockEntity machine = machine();
        machine.setOutput(mode);
        return new Object[] {};
    }
}
