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

package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class NetherSurfaceFeature extends Feature<NetherSurfaceConfig> {
    public NetherSurfaceFeature() {
        super(NetherSurfaceConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NetherSurfaceConfig> ctx) {
        WorldGenLevel level = ctx.level();
        NetherSurfaceConfig cfg = ctx.config();
        RandomSource rand = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int k = 0; k < cfg.attempts(); k++) {
            int x = cx + rand.nextInt(16);
            int z = cz + rand.nextInt(16);
            int d = cfg.minD() + rand.nextInt(cfg.dRange());
            for (int y = d - cfg.scanDown(); y <= d; y++) {
                pos.set(x, y + 1, z);
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.setY(y)).is(Blocks.NETHERRACK)) {
                    level.setBlock(pos, cfg.state(), 2);
                }
            }
        }
        return true;
    }
}
