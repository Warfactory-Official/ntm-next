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

package com.hbm.explosion.vanillant.standard;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.ExplosionBlockTransform;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMutatorDigamma implements ExplosionBlockTransform {

    private final boolean circuit;
    private final LongOpenHashSet solidCubes = new LongOpenHashSet();

    public BlockMutatorDigamma(boolean circuit) {
        this.circuit = circuit;
    }

    private static void ash(Level world, BlockPos pos, RandomSource rand) {
        world.setBlock(pos, ModBlocks.ASH_DIGAMMA.get().defaultBlockState(), 3);
        BlockPos above = pos.above();
        if (rand.nextInt(5) == 0 && world.getBlockState(above).isAir()) {
            world.setBlock(above, ModBlocks.FIRE_DIGAMMA.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void afterDestroy(ComposableExplosion blast, BlockState blockState, BlockPos pos) {
        if (blockState.isRedstoneConductor(blast.level(), pos)) solidCubes.add(pos.asLong());
    }

    @Override
    public void afterPass(ComposableExplosion blast, BlockPos pos) {
        if (!solidCubes.contains(pos.asLong())) return;
        Level world = blast.level();
        RandomSource rand = world.getRandom();

        if (circuit) {
            int x = pos.getX(), z = pos.getZ();
            boolean onGrid = x % 3 == 0 && z % 3 == 0;
            boolean nearGrid = (x % 3 == 0 || z % 3 == 0) && rand.nextBoolean();
            if (onGrid || nearGrid) {
                world.setBlock(pos, ModBlocks.RBMK_DEBRIS_DIGAMMA.get().defaultBlockState(), 3);
                return;
            }
        }
        ash(world, pos, rand);
    }
}
