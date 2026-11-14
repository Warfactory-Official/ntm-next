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

import com.hbm.hazard.HazardRegistry;
import com.hbm.items.machine.RBMKPelletItem;
import com.hbm.items.machine.RBMKRodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardModifierRBMKRadiation implements HazardModifier {

    private final double target;
    private final boolean linear;

    public HazardModifierRBMKRadiation(double target, boolean linear) {
        this.target = target;
        this.linear = linear;
    }

    @Override
    public double modify(ItemStack stack, @Nullable LivingEntity entity, double level) {
        if (stack.getItem() instanceof RBMKRodItem) {
            double enrichment = RBMKRodItem.getEnrichment(stack);
            double depletion = linear ? 1D - enrichment : 1D - Math.pow(enrichment, 2D);
            level = level + (target - level) * depletion;
            level += HazardRegistry.xe135 * RBMKRodItem.getPoisonLevel(stack);
        } else if (stack.getItem() instanceof RBMKPelletItem) {
            RBMKPelletItem.Stage stage = RBMKPelletItem.stage(stack);
            level = level + (target - level) * (stage.depletion() / 4D);
            if (stage.xenon()) level += HazardRegistry.xe135 * HazardRegistry.mult_nugget;
        }
        return level;
    }
}
