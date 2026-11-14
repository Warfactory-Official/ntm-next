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

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.itempool.LootPile;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {
    public static final Identifier TEMPLATE = RegistryUtil.id("glyphid_hive/small");

    public GlyphidHiveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static BlockState base(int subtype) {
        return switch (subtype) {
            case GlyphidEntity.TYPE_INFECTED ->
                    ModBlocks.GLYPHID_BASE_INFESTED.get().defaultBlockState();
            case GlyphidEntity.TYPE_RADIOACTIVE ->
                    ModBlocks.GLYPHID_BASE_RAD.get().defaultBlockState();
            default -> ModBlocks.GLYPHID_BASE.get().defaultBlockState();
        };
    }

    private static BlockState spawner(int subtype) {
        return switch (subtype) {
            case GlyphidEntity.TYPE_INFECTED ->
                    ModBlocks.GLYPHID_SPAWNER_INFESTED.get().defaultBlockState();
            case GlyphidEntity.TYPE_RADIOACTIVE ->
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
        int subtype = infected ? GlyphidEntity.TYPE_INFECTED : GlyphidEntity.TYPE_NORMAL;
        var template = level.getLevel().getStructureManager().get(TEMPLATE).orElseThrow();
        var origin = new BlockPos(x - 5, y - 2, z - 5);
        var settings = new StructurePlaceSettings();
        var cells = new ArrayList<StructureTemplate.StructureBlockInfo>();
        cells.addAll(template.filterBlocks(origin, settings, ModBlocks.GLYPHID_BASE.get()));
        cells.addAll(template.filterBlocks(origin, settings, ModBlocks.GLYPHID_SPAWNER.get()));
        cells.addAll(template.filterBlocks(origin, settings, Blocks.SKELETON_SKULL));
        cells.sort(
                Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                                cell -> cell.pos().getX())
                        .thenComparingInt(cell -> cell.pos().getY())
                        .thenComparingInt(cell -> cell.pos().getZ()));
        var base = base(subtype);
        var spawner = spawner(subtype);
        for (var cell : cells) {
            if (cell.state().is(Blocks.SKELETON_SKULL)) {
                generateTreasureCell(level, rand, cell.pos(), subtype, loot);
            } else {
                level.setBlock(
                        cell.pos(),
                        cell.state().is(ModBlocks.GLYPHID_SPAWNER.get()) && rand.nextInt(3) == 0
                                ? spawner
                                : base,
                        2);
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
            case 1 -> LootPile.BONES.place(level, pos, rand, level.getSeed());
            default -> {
                if (loot) {
                    LootPile.GLYPHID_HIVE.place(level, pos, rand, level.getSeed());
                } else {
                    level.setBlock(pos, base(subtype), 2);
                }
            }
        }
    }
}
