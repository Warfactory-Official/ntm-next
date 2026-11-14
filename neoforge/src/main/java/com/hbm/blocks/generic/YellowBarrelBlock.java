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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.ExplosiveBarrelBlock;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.explosion.BlockConversionTable;
import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class YellowBarrelBlock extends ExplosiveBarrelBlock {

    private final boolean chainPrimes;

    public YellowBarrelBlock(BlockBehaviour.Properties props, boolean chainPrimes) {
        super(props, 100, false);
        this.chainPrimes = chainPrimes;
    }

    @Override
    protected void primeFromBlast(Level level, BlockPos pos, @Nullable Explosion explosion) {
        if (!chainPrimes) return;
        super.primeFromBlast(level, pos, explosion);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        level.addParticle(
                ParticleTypes.MYCELIUM,
                pos.getX() + rand.nextFloat() * 0.5F + 0.25F,
                pos.getY() + 1.1F,
                pos.getZ() + rand.nextFloat() * 0.5F + 0.25F,
                0.0D,
                0.0D,
                0.0D);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable TntNtmEntity entity) {
        BlockPos center = BlockPos.containing(x, y, z);
        if (level.getRandom().nextInt(3) == 0) {
            level.setBlockAndUpdate(center, ModBlocks.TOXIC_BLOCK.get().defaultBlockState());
        } else {
            level.explode(entity, x, y, z, 12.0F, false, Level.ExplosionInteraction.TNT);
        }
        if (level instanceof ServerLevel serverLevel) {
            BlockConversionTable.FALLOUT.sphere(level, center, 35, serverLevel.getRandom());
            RandomSource rand = serverLevel.getRandom();
            BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
            for (int i = -5; i <= 5; i++) {
                for (int j = -5; j <= 5; j++) {
                    for (int k = -5; k <= 5; k++) {
                        scan.set(center.getX() + i, center.getY() + j, center.getZ() + k);
                        if (rand.nextInt(5) == 0 && level.getBlockState(scan).is(Blocks.AIR)) {
                            level.setBlockAndUpdate(
                                    scan, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
                        }
                    }
                }
            }
            RadiationSystemNT.incrementRad(serverLevel, center, 35.0, 100.0);
        }
    }
}
