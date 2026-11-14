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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class NetherCoalBlock extends OutgasBlock {

    public NetherCoalBlock(Properties properties) {
        super(properties, () -> ModBlocks.GAS_MONOXIDE.get(), true);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (var direction : Direction.VALUES) {
            if (direction == Direction.DOWN || !level.isEmptyBlock(pos.relative(direction))) {
                continue;
            }

            int stepX = direction.getStepX();
            int stepY = direction.getStepY();
            int stepZ = direction.getStepZ();
            double x = pos.getX() + 0.5F + stepX + random.nextDouble() - 0.5D;
            double y = pos.getY() + 0.5F + stepY + random.nextDouble() - 0.5D;
            double z = pos.getZ() + 0.5F + stepZ + random.nextDouble() - 0.5D;
            if (stepX != 0) {
                x = pos.getX() + 0.5F + stepX * 0.5D + random.nextDouble() * 0.125D * stepX;
            } else if (stepY != 0) {
                y = pos.getY() + 0.5F + stepY * 0.5D + random.nextDouble() * 0.125D * stepY;
            } else {
                z = pos.getZ() + 0.5F + stepZ * 0.5D + random.nextDouble() * 0.125D * stepZ;
            }

            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.1D, 0.0D);
        }
    }
}
