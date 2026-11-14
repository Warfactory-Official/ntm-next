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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.machine.thermal.SteamCondenserBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.RenderConfig;
import com.hbm.data.MachineData;
import com.hbm.particle.CoolingTowerParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class TowerLargeBlockEntity extends SteamCondenserBlockEntity {
    private static final double PLUME_SPREAD = 3D;

    public TowerLargeBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.TOWER_LARGE.get(),
                pos,
                state,
                MachineData.TOWER_LARGE_INPUT_TANK_SIZE.get(),
                MachineData.TOWER_LARGE_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public void tickClient() {
        if (waterTimer <= 0 || level.getGameTime() % 4 != 0) return;
        if (!RenderConfig.coolingTowerParticles) return;

        RandomSource rand = level.getRandom();
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(0.5F)
                        .setBaseScale(1F)
                        .setMaxScale(10F)
                        .setLife(750 + rand.nextInt(250))
                        .build();
        level.addParticle(
                opts,
                worldPosition.getX() + 0.5 + rand.nextDouble() * PLUME_SPREAD - PLUME_SPREAD / 2D,
                worldPosition.getY() + 1,
                worldPosition.getZ() + 0.5 + rand.nextDouble() * PLUME_SPREAD - PLUME_SPREAD / 2D,
                0,
                0,
                0);
    }
}
