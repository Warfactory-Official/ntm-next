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

import com.hbm.blockentity.machine.fusion.FusionNeutronBlockEntity;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class FusionBreederPeripheral extends SnapshotPeripheral<FusionNeutronBlockEntity> {

    private static final int NEUTRON = 0;
    private static final int PROGRESS = 1;
    private static final int FILL_0 = 2;
    private static final int MAX_0 = 3;
    private static final int FILL_1 = 4;
    private static final int MAX_1 = 5;
    private static final int INPUT_SIZE = 6;
    private static final int OUTPUT_SIZE = 7;
    private static final int NAME_0 = 0;
    private static final int NAME_1 = 1;
    private static final int INPUT_NAME = 2;
    private static final int OUTPUT_NAME = 3;

    public FusionBreederPeripheral(FusionNeutronBlockEntity machine) {
        super(machine, "ntm_fusion_breeder", 8, 4);
    }

    @Override
    protected void capture(FusionNeutronBlockEntity machine) {
        put(NEUTRON, machine.neutronEnergySync);
        put(PROGRESS, machine.progress / FusionNeutronBlockEntity.CAPACITY);
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
        putRef(NAME_0, FluidKeys.of(machine.tanks[0].getTankType()));
        putRef(NAME_1, FluidKeys.of(machine.tanks[1].getTankType()));
        ItemStack input = machine.getItem(FusionNeutronBlockEntity.SLOT_INPUT);
        ItemStack output = machine.getItem(FusionNeutronBlockEntity.SLOT_OUTPUT);
        putRef(INPUT_NAME, input.isEmpty() ? "" : input.getItem().getDescriptionId());
        put(INPUT_SIZE, input.isEmpty() ? 0 : input.getCount());
        putRef(OUTPUT_NAME, output.isEmpty() ? "" : output.getItem().getDescriptionId());
        put(OUTPUT_SIZE, output.isEmpty() ? 0 : output.getCount());
    }

    @LuaFunction
    public Object[] getNeutronEnergy() {
        return new Object[] {doubleAt(NEUTRON)};
    }

    @LuaFunction
    public Object[] getProgress() {
        return new Object[] {doubleAt(PROGRESS)};
    }

    @LuaFunction
    public Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.refAt(NAME_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1),
                            s.refAt(NAME_1)
                        });
    }

    @LuaFunction
    public Object[] getCrafting() {
        return read(
                s ->
                        new Object[] {
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_SIZE),
                            s.refAt(OUTPUT_NAME),
                            s.intAt(OUTPUT_SIZE)
                        });
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(NEUTRON),
                            s.doubleAt(PROGRESS),
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.refAt(NAME_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1),
                            s.refAt(NAME_1),
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_SIZE),
                            s.refAt(OUTPUT_NAME),
                            s.intAt(OUTPUT_SIZE)
                        });
    }
}
