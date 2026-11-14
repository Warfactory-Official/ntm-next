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
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class OutgasBlock extends Block {

    private final Supplier<Block> gas;
    private final boolean ventAfterBreak;
    private final boolean ventOnNeighbor;

    public OutgasBlock(Properties properties, Supplier<Block> gas, boolean ventAfterBreak) {
        this(properties, gas, ventAfterBreak, false);
    }

    public OutgasBlock(
            Properties properties,
            Supplier<Block> gas,
            boolean ventAfterBreak,
            boolean ventOnNeighbor) {
        super(properties);
        this.gas = gas;
        this.ventAfterBreak = ventAfterBreak;
        this.ventOnNeighbor = ventOnNeighbor;
    }

    protected Block getGas() {
        return gas.get();
    }

    private void ventIntoAir(Level level, BlockPos target) {
        if (level.isEmptyBlock(target)) {
            level.setBlockAndUpdate(target, getGas().defaultBlockState());
        }
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var direction = Direction.VALUES[random.nextInt(6)];
        ventIntoAir(level, pos.relative(direction));
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
        if (ventOnNeighbor
                && level instanceof ServerLevel serverLevel
                && serverLevel.getRandom().nextInt(3) == 0) {
            for (var direction : Direction.VALUES) {
                ventIntoAir(serverLevel, pos.relative(direction));
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.isRandomlyTicking()) {
            var emittedGas = getGas();
            if (emittedGas == ModBlocks.GAS_ASBESTOS.get()) {
                var above = pos.above();
                if (level.isEmptyBlock(above)) {
                    var random = level.getRandom();
                    if (level.isClientSide()) {
                        for (var index = 0; index < 5; index++) {
                            level.addParticle(
                                    ParticleTypes.MYCELIUM,
                                    pos.getX() + random.nextFloat(),
                                    pos.getY() + 1.1D,
                                    pos.getZ() + random.nextFloat(),
                                    0.0D,
                                    0.0D,
                                    0.0D);
                        }
                    } else if (random.nextInt(10) == 0) {
                        level.setBlockAndUpdate(above, emittedGas.defaultBlockState());
                    }
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void spawnAfterBreak(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ItemStack tool,
            boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (ventAfterBreak) {
            level.setBlockAndUpdate(pos, getGas().defaultBlockState());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (this != ModBlocks.ANCIENT_SCRAP.get()) {
            return;
        }
        for (var xOffset = -2; xOffset <= 2; xOffset++) {
            for (var yOffset = -2; yOffset <= 2; yOffset++) {
                for (var zOffset = -2; zOffset <= 2; zOffset++) {
                    int coordinateSum = Math.abs(xOffset + yOffset + zOffset);
                    if (coordinateSum > 0 && coordinateSum < 5) {
                        ventIntoAir(level, pos.offset(xOffset, yOffset, zOffset));
                    }
                }
            }
        }
    }
}
