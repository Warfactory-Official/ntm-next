// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.data.MobData;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.platform.Services;
import com.hbm.util.LootGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {

    private static final int[][][] SCHEMATIC = {
        {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 1, 1, 1, 3, 3, 3, 1, 1, 1, 0},
            {0, 1, 1, 1, 3, 3, 3, 1, 1, 1, 0},
            {0, 1, 1, 1, 3, 3, 3, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 2, 2, 2, 1, 1, 0, 0},
            {0, 1, 1, 2, 2, 2, 2, 2, 1, 1, 0},
            {0, 1, 1, 2, 2, 2, 2, 2, 1, 1, 0},
            {0, 1, 1, 2, 2, 2, 2, 2, 1, 1, 0},
            {0, 0, 1, 1, 2, 2, 2, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0},
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        },
    };

    public GlyphidHiveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static BlockState base(int subtype) {
        return switch (subtype) {
            case EntityGlyphid.TYPE_INFECTED ->
                    ModBlocks.GLYPHID_BASE_INFESTED.get().defaultBlockState();
            case EntityGlyphid.TYPE_RADIOACTIVE ->
                    ModBlocks.GLYPHID_BASE_RAD.get().defaultBlockState();
            default -> ModBlocks.GLYPHID_BASE.get().defaultBlockState();
        };
    }

    private static BlockState spawner(int subtype) {
        return switch (subtype) {
            case EntityGlyphid.TYPE_INFECTED ->
                    ModBlocks.GLYPHID_SPAWNER_INFESTED.get().defaultBlockState();
            case EntityGlyphid.TYPE_RADIOACTIVE ->
                    ModBlocks.GLYPHID_SPAWNER_RAD.get().defaultBlockState();
            default -> ModBlocks.GLYPHID_SPAWNER.get().defaultBlockState();
        };
    }

    private static boolean isNormalCube(BlockState state) {
        return state.isSolidRender();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!MobData.ENABLE_HIVES.get()) return false;
        if (ctx.random().nextInt(MobData.HIVE_SPAWN.get()) != 0) return false;

        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();

        for (int k = 3; k >= -1; k--) {
            if (isNormalCube(level.getBlockState(check.setWithOffset(origin, 0, k - 1, 0)))) {
                boolean infected = rand.nextInt(10) == 0;
                generateSmall(
                        level,
                        origin.getX(),
                        origin.getY() + k,
                        origin.getZ(),
                        rand,
                        infected,
                        true);
                return true;
            }
        }
        return false;
    }

    public static void generateSmall(
            WorldGenLevel level,
            int x,
            int y,
            int z,
            RandomSource rand,
            boolean infected,
            boolean loot) {
        int subtype = infected ? EntityGlyphid.TYPE_INFECTED : EntityGlyphid.TYPE_NORMAL;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    int block = SCHEMATIC[4 - j][i][k];
                    if (block == 0) continue;
                    pos.set(x + i - 5, y + j - 2, z + k - 5);

                    switch (block) {
                        case 1 -> level.setBlock(pos, base(subtype), 2);
                        case 2 ->
                                level.setBlock(
                                        pos,
                                        rand.nextInt(3) == 0 ? spawner(subtype) : base(subtype),
                                        2);
                        case 3 -> generateTreasureCell(level, rand, pos, subtype, loot);
                        default -> {}
                    }
                }
            }
        }
    }

    private static void generateTreasureCell(
            WorldGenLevel level, RandomSource rand, BlockPos pos, int subtype, boolean loot) {
        switch (rand.nextInt(3)) {
            case 0 ->
                    level.setBlock(
                            pos,
                            Blocks.SKELETON_SKULL
                                    .defaultBlockState()
                                    .setValue(SkullBlock.ROTATION, rand.nextInt(16)),
                            3);
            case 1 -> placeLootPile(level, rand, pos, LootGenerator.LOOT_BONES);
            default -> {
                if (loot) {
                    placeLootPile(level, rand, pos, LootGenerator.LOOT_GLYPHID_HIVE);
                } else {
                    level.setBlock(pos, base(subtype), 2);
                }
            }
        }
    }

    private static void placeLootPile(
            WorldGenLevel level, RandomSource rand, BlockPos pos, String pool) {
        BlockLoot.place(level, pos, LootGenerator.roll(pool, rand, level.getSeed()));
    }
}
