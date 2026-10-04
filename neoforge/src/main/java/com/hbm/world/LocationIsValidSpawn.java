// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
