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
import com.hbm.world.feature.HugeMushFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MushBlock extends BushBlock implements BonemealableBlock {
    private static final VoxelShape SHAPE = box(4.8D, 0.0D, 4.8D, 11.2D, 6.4D, 11.2D);

    private static final int SPREAD_CHANCE = 25;
    private static final int CROWD_REACH = 4;
    private static final int CROWD_LIMIT = 3;
    private static final int MYCELIUM_CHANCE = 5;
    private static final float BONEMEAL_CHANCE = 0.4F;

    public MushBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || growsOn(level, pos);
    }

    private static boolean growsOn(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.WASTE_EARTH.get())
                || below.is(ModBlocks.WASTE_MYCELIUM.get())
                || below.is(ModBlocks.WASTE_TRINITITE.get())
                || below.is(ModBlocks.WASTE_TRINITITE_RED.get())
                || below.is(ModBlocks.BLOCK_WASTE.get())
                || below.is(ModBlocks.BLOCK_WASTE_PAINTED.get())
                || below.is(ModBlocks.BLOCK_WASTE_VITRIFIED.get());
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (WorldData.ENABLE_MYCELIUM.get()
                && level.getBlockState(pos.below()).is(ModBlocks.WASTE_EARTH.get())
                && random.nextInt(MYCELIUM_CHANCE) == 0) {
            level.setBlockAndUpdate(
                    pos.below(), ModBlocks.WASTE_MYCELIUM.get().defaultBlockState());
        }

        if (random.nextInt(SPREAD_CHANCE) != 0) return;

        int nearby = 0;
        for (int x = pos.getX() - CROWD_REACH; x <= pos.getX() + CROWD_REACH; x++) {
            for (int y = pos.getY() - 1; y <= pos.getY() + 1; y++) {
                for (int z = pos.getZ() - CROWD_REACH; z <= pos.getZ() + CROWD_REACH; z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(this)
                            && ++nearby >= CROWD_LIMIT) {
                        return;
                    }
                }
            }
        }

        BlockPos target = wander(pos, random);
        for (int attempt = 0; attempt < 4; attempt++) {
            if (level.isEmptyBlock(target) && growsOn(level, target)) break;
            target = wander(target, random);
        }
        if (level.isEmptyBlock(target) && growsOn(level, target)) {
            level.setBlock(target, defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockPos wander(BlockPos from, RandomSource random) {
        return new BlockPos(
                from.getX() + random.nextInt(5) - 2,
                from.getY() + random.nextInt(2) - random.nextInt(2),
                from.getZ() + random.nextInt(5) - 2);
    }

    @Override
    public boolean isValidBonemealTarget(
            LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return canSurvive(state, level, pos);
    }

    @Override
    public boolean isBonemealSuccess(
            Level level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource source) {
        return random.nextFloat() < BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource source) {
        level.removeBlock(pos, false);
        HugeMushFeature.generate(level, pos);
    }
}
