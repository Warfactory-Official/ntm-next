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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class UberConcreteBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);

    public UberConcreteBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (random.nextInt(age + 1) != 0) {
            return;
        }
        if (age < 15) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            return;
        }

        level.removeBlock(pos, false);
        var brokenState = ModBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState();
        if (level.isEmptyBlock(pos.below())) {
            level.setBlockAndUpdate(pos, brokenState);
            return;
        }

        int remainingSides = 0b1111;
        while (remainingSides != 0) {
            int rank = random.nextInt(Integer.bitCount(remainingSides));
            int rankedSides = remainingSides;
            for (var skipped = 0; skipped < rank; skipped++) {
                rankedSides &= rankedSides - 1;
            }
            int selectedSide = Integer.numberOfTrailingZeros(rankedSides);
            remainingSides &= ~(1 << selectedSide);

            var direction = Direction.from2DDataValue(selectedSide);
            var destination = pos.relative(direction);
            if (level.isEmptyBlock(destination) && level.isEmptyBlock(destination.below())) {
                var debris = FallingBlockEntity.fall(level, destination, brokenState);
                debris.time = 2;
                debris.setHurtsEntities(2.0F, 40);
                debris.disableDrop();
                return;
            }
        }
        level.setBlockAndUpdate(pos, brokenState);
    }
}
