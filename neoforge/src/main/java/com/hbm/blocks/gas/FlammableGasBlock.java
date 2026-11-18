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

import com.hbm.particle.AshRevealParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FlammableGasBlock extends GasBlock {
    private static final AshRevealParticleOptions REVEAL_TINT =
            new AshRevealParticleOptions(0.8F, 0.8F, 0.2F);

    public FlammableGasBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected AshRevealParticleOptions revealTint() {
        return REVEAL_TINT;
    }

    @Override
    public Direction getFirstDirection(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            return Direction.VALUES[random.nextInt(2)];
        }
        return randomHorizontal(random);
    }

    @Override
    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }

    @Override
    public int getDelay(Level level) {
        return 16 + level.getRandom().nextInt(5);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (hasIgniterNeighbor(level, pos)) {
            combust(level, pos);
            return;
        }
        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level instanceof ServerLevel serverLevel && entity.isOnFire()) {
            combust(serverLevel, pos);
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
        if (level instanceof ServerLevel serverLevel && hasIgniterNeighbor(level, pos)) {
            serverLevel.scheduleTick(pos, this, 2);
        }
    }

    protected void combust(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
    }

    protected boolean isFireSource(BlockState state) {
        var block = state.getBlock();
        return block == Blocks.FIRE
                || block == Blocks.SOUL_FIRE
                || block == Blocks.LAVA
                || block == Blocks.TORCH
                || block == Blocks.WALL_TORCH
                || block == Blocks.JACK_O_LANTERN;
    }

    private boolean hasIgniterNeighbor(Level level, BlockPos pos) {
        for (var direction : Direction.VALUES) {
            if (isFireSource(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }
}
