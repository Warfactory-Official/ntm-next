// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GeyserFeature extends Feature<NoneFeatureConfiguration> {

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

    public GeyserFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static BlockState yellowcake() {
        return ModBlocks.BLOCK_YELLOWCAKE.get().defaultBlockState();
    }

    private static BlockState geysirChlorine() {
        return ModBlocks.GEYSIR_CHLORINE.get().defaultBlockState();
    }

    private static void set(
            WorldGenLevel level,
            BlockPos.MutableBlockPos origin,
            int dx,
            int dy,
            int dz,
            BlockState state) {
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        level.setBlock(origin.set(x + dx, y + dy, z + dz), state, 3);
        origin.set(x, y, z);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos(origin.getX(), origin.getY() - 1, origin.getZ());
        if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) return false;
        generateR0(level, pos.set(origin.getX() - 2, origin.getY() - 11, origin.getZ() - 2));
        return true;
    }

    private void generateR0(WorldGenLevel level, BlockPos.MutableBlockPos o) {

        set(level, o, 1, 5, 0, STONE);
        set(level, o, 2, 5, 0, STONE);
        set(level, o, 3, 5, 0, STONE);
        set(level, o, 0, 5, 1, STONE);
        set(level, o, 1, 5, 1, STONE);
        set(level, o, 2, 5, 1, STONE);
        set(level, o, 3, 5, 1, STONE);
        set(level, o, 4, 5, 1, STONE);
        set(level, o, 0, 5, 2, STONE);
        set(level, o, 1, 5, 2, STONE);
        set(level, o, 2, 5, 2, STONE);
        set(level, o, 3, 5, 2, STONE);
        set(level, o, 4, 5, 2, STONE);
        set(level, o, 0, 5, 3, STONE);
        set(level, o, 1, 5, 3, STONE);
        set(level, o, 2, 5, 3, STONE);
        set(level, o, 3, 5, 3, STONE);
        set(level, o, 4, 5, 3, STONE);
        set(level, o, 1, 5, 4, STONE);
        set(level, o, 2, 5, 4, STONE);
        set(level, o, 3, 5, 4, STONE);

        set(level, o, 1, 6, 0, STONE);
        set(level, o, 2, 6, 0, STONE);
        set(level, o, 3, 6, 0, STONE);
        set(level, o, 0, 6, 1, STONE);
        set(level, o, 1, 6, 1, STONE);
        set(level, o, 2, 6, 1, STONE);
        set(level, o, 3, 6, 1, STONE);
        set(level, o, 4, 6, 1, STONE);
        set(level, o, 0, 6, 2, STONE);
        set(level, o, 1, 6, 2, STONE);
        set(level, o, 2, 6, 2, STONE);
        set(level, o, 3, 6, 2, STONE);
        set(level, o, 4, 6, 2, STONE);
        set(level, o, 0, 6, 3, STONE);
        set(level, o, 1, 6, 3, STONE);
        set(level, o, 2, 6, 3, STONE);
        set(level, o, 3, 6, 3, STONE);
        set(level, o, 4, 6, 3, STONE);
        set(level, o, 1, 6, 4, STONE);
        set(level, o, 2, 6, 4, STONE);
        set(level, o, 3, 6, 4, STONE);

        set(level, o, 1, 7, 0, STONE);
        set(level, o, 2, 7, 0, STONE);
        set(level, o, 3, 7, 0, STONE);
        set(level, o, 0, 7, 1, STONE);
        set(level, o, 1, 7, 1, WATER);
        set(level, o, 2, 7, 1, yellowcake());
        set(level, o, 3, 7, 1, WATER);
        set(level, o, 4, 7, 1, STONE);
        set(level, o, 0, 7, 2, STONE);
        set(level, o, 1, 7, 2, yellowcake());
        set(level, o, 2, 7, 2, WATER);
        set(level, o, 3, 7, 2, WATER);
        set(level, o, 4, 7, 2, STONE);
        set(level, o, 0, 7, 3, STONE);
        set(level, o, 1, 7, 3, WATER);
        set(level, o, 2, 7, 3, yellowcake());
        set(level, o, 3, 7, 3, yellowcake());
        set(level, o, 4, 7, 3, STONE);
        set(level, o, 1, 7, 4, STONE);
        set(level, o, 2, 7, 4, STONE);
        set(level, o, 3, 7, 4, STONE);

        set(level, o, 1, 8, 0, STONE);
        set(level, o, 2, 8, 0, STONE);
        set(level, o, 3, 8, 0, STONE);
        set(level, o, 0, 8, 1, STONE);
        set(level, o, 1, 8, 1, AIR);
        set(level, o, 2, 8, 1, AIR);
        set(level, o, 3, 8, 1, AIR);
        set(level, o, 4, 8, 1, STONE);
        set(level, o, 0, 8, 2, STONE);
        set(level, o, 1, 8, 2, AIR);
        set(level, o, 2, 8, 2, AIR);
        set(level, o, 3, 8, 2, AIR);
        set(level, o, 4, 8, 2, STONE);
        set(level, o, 0, 8, 3, STONE);
        set(level, o, 1, 8, 3, AIR);
        set(level, o, 2, 8, 3, AIR);
        set(level, o, 3, 8, 3, AIR);
        set(level, o, 4, 8, 3, STONE);
        set(level, o, 1, 8, 4, STONE);
        set(level, o, 2, 8, 4, STONE);
        set(level, o, 3, 8, 4, STONE);

        set(level, o, 1, 9, 0, STONE);
        set(level, o, 2, 9, 0, STONE);
        set(level, o, 3, 9, 0, STONE);
        set(level, o, 0, 9, 1, STONE);
        set(level, o, 1, 9, 1, STONE);
        set(level, o, 3, 9, 1, STONE);
        set(level, o, 4, 9, 1, STONE);
        set(level, o, 0, 9, 2, STONE);
        set(level, o, 1, 9, 2, AIR);
        set(level, o, 2, 9, 2, AIR);
        set(level, o, 3, 9, 2, AIR);
        set(level, o, 4, 9, 2, STONE);
        set(level, o, 0, 9, 3, STONE);
        set(level, o, 1, 9, 3, STONE);
        set(level, o, 3, 9, 3, STONE);
        set(level, o, 4, 9, 3, STONE);
        set(level, o, 1, 9, 4, STONE);
        set(level, o, 2, 9, 4, STONE);
        set(level, o, 3, 9, 4, STONE);

        set(level, o, 1, 10, 0, GRASS);
        set(level, o, 2, 10, 0, GRASS);
        set(level, o, 3, 10, 0, GRASS);
        set(level, o, 0, 10, 1, GRASS);
        set(level, o, 1, 10, 1, GRAVEL);
        set(level, o, 2, 10, 1, STONE);
        set(level, o, 3, 10, 1, GRASS);
        set(level, o, 4, 10, 1, STONE);
        set(level, o, 0, 10, 2, STONE);
        set(level, o, 1, 10, 2, GRASS);
        set(level, o, 2, 10, 2, geysirChlorine());
        set(level, o, 3, 10, 2, GRASS);
        set(level, o, 4, 10, 2, GRAVEL);
        set(level, o, 0, 10, 3, GRASS);
        set(level, o, 1, 10, 3, STONE);
        set(level, o, 2, 10, 3, GRASS);
        set(level, o, 3, 10, 3, GRAVEL);
        set(level, o, 4, 10, 3, GRASS);
        set(level, o, 1, 10, 4, GRASS);
        set(level, o, 2, 10, 4, GRASS);
        set(level, o, 3, 10, 4, GRASS);
    }
}
