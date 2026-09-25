// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.Library;
import com.hbm.world.BiomeTarget;
import com.hbm.world.NtmWorldgenFields;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BiomeCaveFeature extends Feature<NoneFeatureConfiguration> {

    private static final double THRESHOLD = 2.0D;
    private static final int RANGE_MULT = 3;
    private static final int MAX_RANGE = 4;
    private static final int Y_LEVEL = 30;
    private static final double SCALE = 0.01D;

    private static final Identifier DOMAIN = Library.id("noise/biome_cave");

    private static final Set<ResourceKey<Biome>> DESERT =
            Set.copyOf(
                    BiomeTarget.vanilla(
                                    "badlands",
                                    "desert",
                                    "eroded_badlands",
                                    "savanna",
                                    "savanna_plateau",
                                    "windswept_savanna",
                                    "wooded_badlands")
                            .biomes());
    private static final Set<ResourceKey<Biome>> WOODLAND =
            Set.copyOf(
                    BiomeTarget.vanilla(
                                    "mushroom_fields",
                                    "swamp",
                                    "mangrove_swamp",
                                    "plains",
                                    "meadow",
                                    "beach",
                                    "forest",
                                    "flower_forest",
                                    "birch_forest",
                                    "dark_forest",
                                    "pale_garden",
                                    "jungle",
                                    "stony_peaks",
                                    "river",
                                    "sunflower_plains",
                                    "old_growth_birch_forest",
                                    "sparse_jungle",
                                    "bamboo_jungle",
                                    "cherry_grove",
                                    "dripstone_caves",
                                    "lush_caves",
                                    "sulfur_caves",
                                    "deep_dark")
                            .biomes());

    public BiomeCaveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static BlockState typeFor(Holder<Biome> biome) {
        if (biome.is(DESERT::contains))
            return ModBlocks.STONE_BIOME_DESERT.get().defaultBlockState();
        if (biome.is(WOODLAND::contains))
            return ModBlocks.STONE_BIOME_WOODLAND.get().defaultBlockState();
        return null;
    }

    private static void handleColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos pos,
            BlockPos.MutableBlockPos neighbor,
            BlockState type) {
        BlockState target = level.getBlockState(pos);
        if (!target.is(BlockTags.STONE_ORE_REPLACEABLES)
                || !target.isCollisionShapeFullBlock(level, pos)) return;

        boolean shouldGen = false;
        for (Direction dir : Direction.VALUES) {
            neighbor.setWithOffset(pos, dir);
            if (level.getBlockState(neighbor).isAir()) {
                shouldGen = true;
                break;
            }
            neighbor.move(dir);
            if (level.getBlockState(neighbor).isAir()) {
                shouldGen = true;
                break;
            }
        }
        if (shouldGen) level.setBlock(pos, type, 2);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;

        FractalSimplexNoise noise = NtmWorldgenFields.get(level).fractal(DOMAIN, 2);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                Holder<Biome> biome = level.getBiome(pos.set(x, Y_LEVEL, z));
                BlockState type = typeFor(biome);
                if (type == null) continue;

                double n = noise.get(x * SCALE, z * SCALE);
                if (n <= THRESHOLD) continue;

                int range = (int) ((n - THRESHOLD) * RANGE_MULT);
                if (range > MAX_RANGE) range = MAX_RANGE * 2 - range;
                if (range < 0) continue;

                for (int y = Y_LEVEL - range; y <= Y_LEVEL + range; y++) {
                    handleColumn(level, pos.set(x, y, z), neighbor, type);
                }
            }
        }
        return true;
    }
}
