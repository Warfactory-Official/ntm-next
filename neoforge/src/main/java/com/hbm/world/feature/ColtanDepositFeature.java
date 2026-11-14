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

import com.hbm.world.NtmWorldgenFields;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class ColtanDepositFeature extends Feature<ColtanDepositConfig> {
    public ColtanDepositFeature() {
        super(ColtanDepositConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ColtanDepositConfig> ctx) {
        ColtanDepositConfig cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        NtmWorldgenFields fields = NtmWorldgenFields.get(level);
        int centerX = fields.coltanX(cfg.spread());
        int centerZ = fields.coltanZ(cfg.spread());
        int reach = cfg.baseRange();
        if (cx + 15 < centerX - reach
                || cx > centerX + reach
                || cz + 15 < centerZ - reach
                || cz > centerZ + reach) {
            return true;
        }
        BlockPos.MutableBlockPos veinPos = new BlockPos.MutableBlockPos();

        for (int k = 0; k < cfg.outerAttempts(); k++) {
            for (int r = 1; r <= cfg.rings(); r++) {
                int x = cx + rand.nextInt(16);
                int y = cfg.minY() + rand.nextInt(cfg.yRange());
                int z = cz + rand.nextInt(16);
                int range = cfg.baseRange() / r;
                if (x <= centerX + range
                        && x >= centerX - range
                        && z <= centerZ + range
                        && z >= centerZ - range) {
                    Feature.ORE.place(
                            new FeaturePlaceContext<>(
                                    Optional.empty(),
                                    level,
                                    ctx.chunkGenerator(),
                                    rand,
                                    veinPos.set(x, y, z),
                                    cfg.vein()));
                }
            }
        }
        return true;
    }
}
