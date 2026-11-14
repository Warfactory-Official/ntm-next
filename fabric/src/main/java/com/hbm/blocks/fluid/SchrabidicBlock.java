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
import com.hbm.particle.HbmParticles;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SchrabidicBlock extends ClassicFluidBlock {

    private static final Vec3 STUCK_MULTIPLIER = new Vec3(0.25, 0.05, 0.25);

    public SchrabidicBlock(ClassicFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        entity.makeStuckInBlock(state, STUCK_MULTIPLIER);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(
                    living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block changedBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, changedBlock, orientation, movedByPiston);
        if (level.isClientSide()) {
            return;
        }

        for (var direction : Direction.VALUES) {
            var neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.getBlock() != this && FiniteFluidBlock.isLiquid(neighbor)) {
                level.setBlockAndUpdate(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
                return;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var x = pos.getX() + 0.5D + random.nextDouble() * 2.0D + -1.0D;
        var y = pos.getY() + 0.5D + random.nextDouble() * 2.0D + -1.0D;
        var z = pos.getZ() + 0.5D + random.nextDouble() * 2.0D + -1.0D;
        level.addParticle(HbmParticles.SCHRAB_FOG.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
