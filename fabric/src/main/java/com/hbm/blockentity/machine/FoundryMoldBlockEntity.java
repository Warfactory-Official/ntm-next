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

package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FoundryMoldBlockEntity extends FoundryCastingBaseBlockEntity
        implements FoundryRenderProvider {

    public FoundryMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 0;
    }

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.25D / this.getCapacity();
    }

    @Override
    public Material getMat() {
        return this.type;
    }

    @Override
    public double minX() {
        return 0.125D;
    }

    @Override
    public double maxX() {
        return 0.875D;
    }

    @Override
    public double minZ() {
        return 0.125D;
    }

    @Override
    public double maxZ() {
        return 0.875D;
    }

    @Override
    public double moldHeight() {
        return 0.13D;
    }

    @Override
    public double outHeight() {
        return 0.25D;
    }
}
