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

package com.hbm.blocks.gas;

import com.hbm.blocks.ModBlocks;
import com.hbm.extprop.LivingHazards;
import com.hbm.particle.AshRevealParticleOptions;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TombRadonGasBlock extends GasBlock {
    public static final MapCodec<TombRadonGasBlock> CODEC = simpleCodec(TombRadonGasBlock::new);

    private static final AshRevealParticleOptions REVEAL_TINT =
            new AshRevealParticleOptions(0.1F, 0.3F, 0.1F);

    public TombRadonGasBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends TombRadonGasBlock> codec() {
        return CODEC;
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return REVEAL_TINT;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }

        living.removeEffect(HbmPotion.radaway());
        living.removeEffect(HbmPotion.radx());
        ContaminationUtil.contaminate(
                living, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 0.5F);
        LivingHazards.addAsbestos(living, 10);
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return Direction.UP;
        }
        return Direction.DOWN;
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            var belowPos = pos.below();
            var belowState = level.getBlockState(belowPos);
            if (belowState.is(Blocks.GRASS_BLOCK)) {
                var replacement =
                        random.nextInt(5) == 0 ? Blocks.COARSE_DIRT : ModBlocks.WASTE_EARTH.get();
                level.setBlockAndUpdate(belowPos, replacement.defaultBlockState());
            }

            var belowBlock = belowState.getBlock();
            if (belowBlock instanceof VegetationBlock
                    || belowBlock instanceof VineBlock
                    || belowState.is(BlockTags.LEAVES)) {
                level.removeBlock(belowPos, false);
            }
        }

        if (random.nextInt(600) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }
}
