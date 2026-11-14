/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.hazard.modifier;

import java.util.function.ToDoubleFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierFuelRadiation implements HazardModifier {

    private final double target;
    private final ToDoubleFunction<ItemStack> depletion;

    public HazardModifierFuelRadiation(double target) {
        this(target, HazardModifierFuelRadiation::damageDepletion);
    }

    public HazardModifierFuelRadiation(double target, ToDoubleFunction<ItemStack> depletion) {
        this.target = target;
        this.depletion = depletion;
    }

    private static double damageDepletion(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 0D : (double) stack.getDamageValue() / max;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        double spent = depletion.applyAsDouble(stack);
        if (spent <= 0D) return level;
        return level + (target - level) * Math.pow(Math.min(1D, spent), 0.4D);
    }
}
