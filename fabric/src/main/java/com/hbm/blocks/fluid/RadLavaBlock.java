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

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.SellafieldSlakedBlock;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class RadLavaBlock extends VolcanicLavaBlock {

    public RadLavaBlock(ClassicFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected Block basaltForCheck() {
        return ModBlocks.SELLAFIELD_SLAKED.get();
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(
                    living, HazardType.RADIATION, ContaminationType.CREATIVE, 5.0F);
        }
    }

    @Override
    protected @Nullable Block mineralReaction(Level level, BlockState state) {
        if (state.is(Blocks.DIAMOND_ORE)) {
            return ModBlocks.ORE_SELLAFIELD_RADGEM.get();
        }
        if (state.is(ModBlocks.ORE_URANIUM.get()) || state.is(ModBlocks.ORE_GNEISS_URANIUM.get())) {
            return level.getRandom().nextInt(5) == 0
                    ? ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get()
                    : ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get();
        }
        return null;
    }

    @Override
    protected void onSolidify(
            ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource random) {
        var outcome = random.nextInt(400);
        Block crust = ModBlocks.SELLAFIELD_SLAKED.get();
        if (lavaCount + basaltCount == 6 && lavaCount < 3) {
            var cap = level.getBlockState(pos.above(10));
            if (cap.is(this) || cap.is(ModBlocks.SELLAFIELD_SLAKED.get())) {
                if (outcome < 2) {
                    crust = ModBlocks.ORE_SELLAFIELD_DIAMOND.get();
                } else if (outcome == 2) {
                    crust = ModBlocks.ORE_SELLAFIELD_EMERALD.get();
                } else if (outcome < 20) {
                    crust = ModBlocks.ORE_SELLAFIELD_RADGEM.get();
                }
            }
        }

        var shade = 5 + random.nextInt(3);
        var result = crust.defaultBlockState().setValue(SellafieldSlakedBlock.SHADE, shade);
        level.setBlockAndUpdate(pos, result);
    }
}
