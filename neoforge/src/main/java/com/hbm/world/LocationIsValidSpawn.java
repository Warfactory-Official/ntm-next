/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world;

import com.hbm.blocks.ModBlocks;
import java.util.function.Predicate;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LocationIsValidSpawn {

    private static final Predicate<BlockState> GROUND =
            state ->
                    isGrass(state)
                            || isDirtFamily(state)
                            || isSandFamily(state)
                            || isStone(state)
                            || isSandstone(state);

    private LocationIsValidSpawn() {}

    public static boolean isGrass(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK);
    }

    public static boolean isDirtFamily(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
    }

    public static boolean isSandFamily(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    public static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    public static boolean isPlant(BlockState state) {
        return state.getBlock() instanceof VegetationBlock
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.COCOA)
                || state.is(ModBlocks.PLANT_REEDS.get());
    }

    public static boolean isDryGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    public static boolean isSandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE);
    }

    public static boolean isValidGround(BlockState checkBlock, BlockState below) {
        return isValidGround(checkBlock, below, GROUND);
    }

    public static boolean isValidGround(
            BlockState checkBlock, BlockState below, Predicate<BlockState> ground) {
        if (ground.test(checkBlock)) return true;
        if (checkBlock.is(Blocks.SNOW) && ground.test(below)) return true;
        return isPlant(checkBlock) && ground.test(below);
    }
}
