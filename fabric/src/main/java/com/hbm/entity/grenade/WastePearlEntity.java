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

package com.hbm.entity.grenade;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.grenade.GrenadeBaseEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WastePearlEntity extends GrenadeBaseEntity {

    public WastePearlEntity(EntityType<? extends WastePearlEntity> type, Level level) {
        super(type, level);
    }

    public WastePearlEntity(Level level) {
        this(ModEntities.WASTE_PEARL.get(), level);
    }

    public WastePearlEntity(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    public WastePearlEntity(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    @Override
    public void explode() {

        if (level().isClientSide()) return;

        this.discard();

        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());

        Block fallout = ModBlocks.FALLOUT.get();
        Block radon = ModBlocks.GAS_RADON.get();
        Block radonDense = ModBlocks.GAS_RADON_DENSE.get();

        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iy = y - 3; iy <= y + 3; iy++) {
                for (int iz = z - 3; iz <= z + 3; iz++) {

                    BlockPos pos = new BlockPos(ix, iy, iz);
                    BlockState state = level().getBlockState(pos);
                    BlockState falloutState = fallout.defaultBlockState();

                    if (random.nextInt(3) == 0
                            && state.canBeReplaced()
                            && falloutState.canSurvive(level(), pos)) {
                        level().setBlockAndUpdate(pos, falloutState);
                    } else if (state.isAir()) {
                        level().setBlockAndUpdate(
                                        pos,
                                        random.nextBoolean()
                                                ? radon.defaultBlockState()
                                                : radonDense.defaultBlockState());
                    }
                }
            }
        }
    }
}
