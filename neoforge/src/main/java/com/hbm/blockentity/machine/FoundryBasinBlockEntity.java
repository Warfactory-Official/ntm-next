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
import com.hbm.inventory.material.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class FoundryBasinBlockEntity extends FoundryCastingBaseBlockEntity
        implements FoundryRenderProvider {

    public FoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN.get(), pos, state);
    }

    @Override
    public int getMoldSize() {
        return 1;
    }

    @Override
    public boolean canAcceptFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return false;
    }

    @Override
    public MaterialStack acceptFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return stack;
    }

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.75D / this.getCapacity();
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
        return 0.875D;
    }
}
