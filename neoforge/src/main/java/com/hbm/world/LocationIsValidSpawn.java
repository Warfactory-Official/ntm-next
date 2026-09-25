// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LocationIsValidSpawn {

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
        return state.is(Blocks.STONE);
    }

    public static boolean isSandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE);
    }

    public static boolean isTerracottaFamily(BlockState state) {
        return state.is(BlockTags.TERRACOTTA);
    }

    public static boolean isValidGround(
            BlockState checkBlock, BlockState below, boolean includeTerracotta) {
        if (matches(checkBlock, includeTerracotta)) return true;

        if (checkBlock.is(Blocks.SNOW) && matches(below, includeTerracotta)) return true;

        return checkBlock.getBlock() instanceof VegetationBlock
                && matches(below, includeTerracotta);
    }

    private static boolean matches(BlockState state, boolean includeTerracotta) {
        return isGrass(state)
                || isDirtFamily(state)
                || isSandFamily(state)
                || isStone(state)
                || isSandstone(state)
                || (includeTerracotta && isTerracottaFamily(state));
    }
}
