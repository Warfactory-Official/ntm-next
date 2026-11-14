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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.WorldData;
import com.hbm.potion.HbmPotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WasteMyceliumBlock extends WasteEarthBlock {

    public WasteMyceliumBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (WorldData.ENABLE_MYCELIUM.get()) {
            var candidate = new BlockPos.MutableBlockPos();
            var replacement = defaultBlockState();
            for (var offsetX = -1; offsetX <= 1; offsetX++) {
                for (var offsetY = -1; offsetY <= 1; offsetY++) {
                    for (var offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        candidate.set(
                                pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);
                        var target = level.getBlockState(candidate);
                        if (!target.is(Blocks.DIRT)
                                && !target.is(Blocks.GRASS_BLOCK)
                                && !target.is(Blocks.MYCELIUM)
                                && !target.is(ModBlocks.WASTE_EARTH.get())) {
                            continue;
                        }
                        if (level.getBlockState(candidate.above()).isSolidRender()) {
                            continue;
                        }
                        level.setBlockAndUpdate(candidate, replacement);
                    }
                }
            }
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 600, 3));
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var particleX = pos.getX() + random.nextFloat();
        var particleY = pos.getY() + 1.1F;
        var particleZ = pos.getZ() + random.nextFloat();
        level.addParticle(ParticleTypes.MYCELIUM, particleX, particleY, particleZ, 0.0, 0.0, 0.0);
    }
}
