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

import com.hbm.hazard.HazardSystem;
import com.hbm.particle.HbmParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class HazardBlock extends Block {
    private @Nullable ExtDisplayEffect displayEffect;

    public static final MapCodec<HazardBlock> CODEC = simpleCodec(HazardBlock::new);

    @Override
    protected MapCodec<? extends HazardBlock> codec() {
        return CODEC;
    }

    public HazardBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public HazardBlock setDisplayEffect(@Nullable ExtDisplayEffect effect) {
        displayEffect = effect;
        return this;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()
                && entity instanceof LivingEntity living
                && !(living instanceof Player player && player.isCreative())) {
            HazardSystem.applyHazards(this, living);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var effect = displayEffect;
        if (effect == null) {
            return;
        }
        var particle =
                switch (effect) {
                    case RADFOG -> ParticleTypes.MYCELIUM;
                    case SCHRAB -> HbmParticles.SCHRAB_FOG.get();
                };

        for (var direction : Direction.VALUES) {
            if (!level.getBlockState(pos.relative(direction)).isAir()) {
                continue;
            }

            var stepX = direction.getStepX();
            var stepY = direction.getStepY();
            var stepZ = direction.getStepZ();
            var x = pos.getX() + 0.5 + stepX + 3.0D * random.nextDouble() + -1.5D;
            var y = pos.getY() + 0.5 + stepY + 3.0D * random.nextDouble() + -1.5D;
            var z = pos.getZ() + 0.5 + stepZ + 3.0D * random.nextDouble() + -1.5D;

            if (stepX != 0) {
                x = pos.getX() + 0.5 + 0.5 * stepX + random.nextDouble() * stepX;
            } else if (stepY != 0) {
                y = pos.getY() + 0.5 + 0.5 * stepY + random.nextDouble() * stepY;
            } else {
                z = pos.getZ() + 0.5 + 0.5 * stepZ + random.nextDouble() * stepZ;
            }
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    public enum ExtDisplayEffect {
        RADFOG,
        SCHRAB
    }
}
