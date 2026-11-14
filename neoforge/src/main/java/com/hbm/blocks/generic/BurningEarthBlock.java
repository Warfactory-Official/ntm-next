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
import com.hbm.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BurningEarthBlock extends Block {
    public BurningEarthBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity living) {
            living.igniteForSeconds(5.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var y = pos.getY() + 1.1F;
        var flameX = pos.getX() + random.nextFloat();
        var flameZ = pos.getZ() + random.nextFloat();
        level.addParticle(ParticleTypes.FLAME, flameX, y, flameZ, 0.0, 0.0, 0.0);

        var smokeX = pos.getX() + random.nextFloat();
        var smokeZ = pos.getZ() + random.nextFloat();
        level.addParticle(ParticleTypes.SMOKE, smokeX, y, smokeZ, 0.0, 0.0, 0.0);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block changedBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        var abovePos = pos.above();
        var above = level.getBlockState(abovePos);
        if (!above.getFluidState().isEmpty() || above.isRedstoneConductor(level, abovePos)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            spread(level, pos);
        }
        level.setBlockAndUpdate(pos, ModBlocks.IMPACT_DIRT.get().defaultBlockState());
    }

    private void spread(ServerLevel level, BlockPos origin) {
        var originCoverPos = origin.above();
        for (var dx = -1; dx <= 1; dx++) {
            for (var dy = -1; dy <= 1; dy++) {
                for (var dz = -1; dz <= 1; dz++) {
                    var herePos = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(herePos)) {
                        continue;
                    }

                    var here = level.getBlockState(herePos);
                    var coverPos = herePos.above();
                    var cover = level.getBlockState(coverPos);
                    if (canBurnGround(here)
                            && !cover.isSolidRender()
                            && !level.isRainingAt(origin)) {
                        level.setBlockAndUpdate(
                                herePos, ModBlocks.BURNING_EARTH.get().defaultBlockState());
                    }
                    if (isVegetation(here)) {
                        level.setBlockAndUpdate(herePos, Blocks.AIR.defaultBlockState());
                    } else if (here.is(ModBlocks.FROZEN_DIRT.get())) {
                        level.setBlockAndUpdate(herePos, Blocks.DIRT.defaultBlockState());
                    }
                    if (!isVegetation(cover)
                            && Platform.isFlammable(level, coverPos, cover, Direction.UP)
                            && level.isEmptyBlock(originCoverPos)) {
                        level.setBlockAndUpdate(
                                originCoverPos, BaseFireBlock.getState(level, originCoverPos));
                    }
                }
            }
        }
    }

    private static boolean canBurnGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(ModBlocks.WASTE_EARTH.get())
                || state.is(ModBlocks.FROZEN_GRASS.get())
                || state.is(ModBlocks.WASTE_MYCELIUM.get());
    }

    private static boolean isVegetation(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof VegetationBlock;
    }
}
