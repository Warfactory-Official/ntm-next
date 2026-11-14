/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.util;

import java.util.List;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStack;

public class WeightedRandomObject {
    public static final int PRIME = 137;

    private WeightedRandomObject() {}

    public static ItemStack pickDeterministic(WeightedList<ItemStack> outputs, int[] cursor) {

        List<Weighted<ItemStack>> entries = outputs.unwrap();

        int totalWeight = 0;
        for (Weighted<ItemStack> weighted : entries) totalWeight += weighted.weight();

        cursor[0] %= Math.max(totalWeight, 1);

        ItemStack result = null;
        int weight = 0;

        for (Weighted<ItemStack> weighted : entries) {
            weight += weighted.weight();

            if (cursor[0] < weight) {
                result = weighted.value().copy();
                break;
            }
        }

        cursor[0] += PRIME;

        return result;
    }
}
