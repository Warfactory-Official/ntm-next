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

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class DepthDepositFeature extends Feature<DepthDepositConfig> {
    public DepthDepositFeature() {
        super(DepthDepositConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<DepthDepositConfig> ctx) {
        WorldGenLevel level = ctx.level();
        DepthDepositConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();

        int size = cfg.size();
        double coreRadius = size * cfg.fill();
        WorldGenerationContext generation = new WorldGenerationContext(ctx.chunkGenerator(), level);
        int floor = generation.getMinGenY();
        int ceiling = floor + generation.getGenDepth() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                int y = origin.getY() + dy;
                if (y <= floor || y >= ceiling) continue;
                for (int dz = -size; dz <= size; dz++) {
                    if (dx * dx + dy * dy + dz * dz > size * size) continue;
                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState cur = level.getBlockState(pos);
                    if (!cfg.replaceable().test(cur, rand) && !cur.is(Blocks.BEDROCK)) continue;

                    double len = Mth.length(dx, dy, dz);
                    if (len + rand.nextInt(2) < coreRadius) {
                        level.setBlock(pos, cfg.core(), 2);
                    } else if (len + rand.nextInt(2) <= size) {
                        level.setBlock(pos, cfg.filler(), 2);
                    }
                }
            }
        }
        return true;
    }
}
