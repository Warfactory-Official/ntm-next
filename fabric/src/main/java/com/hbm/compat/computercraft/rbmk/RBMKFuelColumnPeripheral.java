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

import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKRodBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.RBMKFuelData;
import com.hbm.items.machine.RBMKRodItem;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class RBMKFuelColumnPeripheral extends RBMKColumnPeripheral<RBMKRodBlockEntity> {

    private static final int FLUX_QUANTITY = SLOTS;
    private static final int FLUX_RATIO = SLOTS + 1;
    private static final int MODERATED = SLOTS + 2;
    private static final int LOADED = SLOTS + 3;
    private static final int ENRICHMENT = SLOTS + 4;
    private static final int XENON = SLOTS + 5;
    private static final int CORE_HEAT = SLOTS + 6;
    private static final int HULL_HEAT = SLOTS + 7;
    private static final int ROD_NAME = 0;
    private static final String ABSENT = "N/A";

    private @Nullable Item freshFor;
    private @Nullable RBMKFuelData fresh;

    public RBMKFuelColumnPeripheral(RBMKRodBlockEntity machine) {
        super(machine, "rbmk_fuel_rod", SLOTS + 8, 1);
    }

    @Override
    protected void capture(RBMKRodBlockEntity machine) {
        super.capture(machine);
        put(FLUX_QUANTITY, machine.lastFluxQuantity);
        put(FLUX_RATIO, machine.lastFluxRatio);
        put(
                MODERATED,
                machine.getBlockState().getBlock() instanceof RBMKRodBlock column
                        && column.moderated);
        var fuel = machine.getItem(0);
        var loaded = fuel.getItem() instanceof RBMKRodItem;
        put(LOADED, loaded);
        if (!loaded) return;
        var rod = (RBMKRodItem) fuel.getItem();
        var data = fuel.get(ModDataComponents.RBMK_FUEL.get());
        if (data == null) {
            if (freshFor != rod) {
                freshFor = rod;
                fresh = RBMKRodItem.getData(fuel);
            }
            data = fresh;
        }
        put(ENRICHMENT, data.yield() / rod.yield);
        put(XENON, data.xenon());
        put(CORE_HEAT, data.coreHeat());
        put(HULL_HEAT, data.hullHeat());
        putRef(ROD_NAME, rod.getDescriptionId());
    }

    @LuaFunction
    public Object[] getHeat() {
        return new Object[] {doubleAt(HEAT)};
    }

    @LuaFunction
    public Object[] getFluxQuantity() {
        return new Object[] {doubleAt(FLUX_QUANTITY)};
    }

    @LuaFunction
    public Object[] getFluxRatio() {
        return new Object[] {doubleAt(FLUX_RATIO)};
    }

    @LuaFunction
    public Object[] getDepletion() {
        return read(s -> new Object[] {s.booleanAt(LOADED) ? s.doubleAt(ENRICHMENT) : ABSENT});
    }

    @LuaFunction
    public Object[] getXenonPoison() {
        return read(s -> new Object[] {s.booleanAt(LOADED) ? s.doubleAt(XENON) : ABSENT});
    }

    @LuaFunction
    public Object[] getCoreHeat() {
        return read(s -> new Object[] {s.booleanAt(LOADED) ? s.doubleAt(CORE_HEAT) : ABSENT});
    }

    @LuaFunction
    public Object[] getSkinHeat() {
        return read(s -> new Object[] {s.booleanAt(LOADED) ? s.doubleAt(HULL_HEAT) : ABSENT});
    }

    @LuaFunction("getType")
    public Object[] rodName() {
        return read(s -> new Object[] {s.booleanAt(LOADED) ? s.refAt(ROD_NAME) : ABSENT});
    }

    @LuaFunction
    public Object[] getModerated() {
        return new Object[] {booleanAt(MODERATED)};
    }

    @LuaFunction
    public Object[] getInfo() {
        return read(
                s -> {
                    var loaded = s.booleanAt(LOADED);
                    return new Object[] {
                        s.doubleAt(HEAT),
                        loaded ? s.doubleAt(HULL_HEAT) : ABSENT,
                        loaded ? s.doubleAt(CORE_HEAT) : ABSENT,
                        s.doubleAt(FLUX_QUANTITY),
                        s.doubleAt(FLUX_RATIO),
                        loaded ? s.doubleAt(ENRICHMENT) : ABSENT,
                        loaded ? s.doubleAt(XENON) : ABSENT,
                        loaded ? s.refAt(ROD_NAME) : ABSENT,
                        s.booleanAt(MODERATED),
                        s.intAt(X),
                        s.intAt(Y),
                        s.intAt(Z)
                    };
                });
    }
}
