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

package com.hbm.blockentity.machine.oil;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

final class FractionColumn {

    static final int FEED_CAPACITY = 64_000;
    static final int FRACTION_CAPACITY = 24_000;
    static final int SLOT_FEED_IN = 1;
    static final int SLOT_FEED_OUT = 2;
    private static final int SLOT_FRACTIONS = 3;
    private static final int DRAW = 100;

    final FluidTankNTM[] tanks;
    final FluidTankNTM[] feed;
    final FluidTankNTM[] fractions;
    private final FractionSplit split;

    FractionColumn(FluidTankNTM feedTank, Fluid first, Fluid second, Fluid third, Fluid fourth) {
        fractions =
                new FluidTankNTM[] {
                    new FluidTankNTM(first, FRACTION_CAPACITY),
                    new FluidTankNTM(second, FRACTION_CAPACITY),
                    new FluidTankNTM(third, FRACTION_CAPACITY),
                    new FluidTankNTM(fourth, FRACTION_CAPACITY)
                };
        feed = new FluidTankNTM[] {feedTank};
        tanks =
                new FluidTankNTM[] {
                    feedTank, fractions[0], fractions[1], fractions[2], fractions[3]
                };
        split = new FractionSplit(feedTank, fractions, DRAW);
    }

    boolean fillFeed(NonNullList<ItemStack> slots, int identifierSlot) {
        boolean changed = feed[0].setType(identifierSlot, identifierSlot, slots);
        changed |= feed[0].loadTank(SLOT_FEED_IN, SLOT_FEED_OUT, slots);
        return changed;
    }

    boolean refine(FluidStackNTM @Nullable [] split, boolean powered) {
        return this.split.step(split, powered);
    }

    boolean drainFractions(NonNullList<ItemStack> slots) {
        boolean changed = false;
        for (int i = 0; i < fractions.length; i++) {
            changed |=
                    fractions[i].unloadTank(
                            SLOT_FRACTIONS + 2 * i, SLOT_FRACTIONS + 2 * i + 1, slots);
        }
        return changed;
    }

    boolean holdsFluid() {
        for (FluidTankNTM tank : tanks) {
            if (tank.getFill() > 0) return true;
        }
        return false;
    }
}
