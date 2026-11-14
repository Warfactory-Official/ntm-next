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

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.VersatileConfig;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockConversionTable {

    private static final int FLAGS = Block.UPDATE_ALL;

    public static final BlockConversionTable FALLOUT =
            new BlockConversionTable(
                    row(s -> s.is(BlockTags.DOORS), to(() -> Blocks.AIR, Block.UPDATE_CLIENTS)),
                    row(
                            s -> s.is(Blocks.GRASS_BLOCK),
                            to(() -> ModBlocks.WASTE_EARTH.get(), FLAGS)),
                    row(
                            s -> s.is(Blocks.MYCELIUM),
                            to(() -> ModBlocks.WASTE_MYCELIUM.get(), FLAGS)),
                    row(s -> s.is(Blocks.SAND), oneIn(20, () -> ModBlocks.WASTE_TRINITITE.get())),
                    row(
                            s -> s.is(Blocks.RED_SAND),
                            oneIn(20, () -> ModBlocks.WASTE_TRINITITE_RED.get())),
                    row(s -> s.is(Blocks.CLAY), to(() -> Blocks.TERRACOTTA, FLAGS)),
                    row(s -> s.is(Blocks.MOSSY_COBBLESTONE), to(() -> Blocks.COAL_ORE, FLAGS)),
                    row(s -> s.is(Blocks.COAL_ORE), gems(10, 9)),
                    row(
                            s -> s.is(BlockTags.LOGS) || s.is(Blocks.MUSHROOM_STEM),
                            to(() -> ModBlocks.WASTE_LOG.get(), FLAGS)),
                    row(
                            s ->
                                    s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                                            || s.is(Blocks.RED_MUSHROOM_BLOCK),
                            to(() -> Blocks.AIR, Block.UPDATE_CLIENTS)),
                    row(
                            s ->
                                    s.is(BlockTags.MINEABLE_WITH_AXE)
                                            && s.isSolidRender()
                                            && !s.is(ModBlocks.WASTE_LOG.get()),
                            to(() -> ModBlocks.WASTE_PLANKS.get(), FLAGS)),
                    row(
                            s ->
                                    s.is(ModBlocks.ORE_URANIUM.get())
                                            || s.is(ModBlocks.ORE_URANIUM_DEEPSLATE.get()),
                            uranium(
                                    () -> ModBlocks.ORE_SCHRABIDIUM.get(),
                                    () -> ModBlocks.ORE_URANIUM_SCORCHED.get())),
                    row(
                            s -> s.is(ModBlocks.ORE_NETHER_URANIUM.get()),
                            uranium(
                                    () -> ModBlocks.ORE_NETHER_SCHRABIDIUM.get(),
                                    () -> ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get())),
                    row(
                            s -> s.is(ModBlocks.ORE_GNEISS_URANIUM.get()),
                            uranium(
                                    () -> ModBlocks.ORE_GNEISS_SCHRABIDIUM.get(),
                                    () -> ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get())));

    public static final BlockConversionTable POISON =
            new BlockConversionTable(
                    row(
                            s ->
                                    s.is(Blocks.GLASS)
                                            || s.getBlock() instanceof StainedGlassBlock
                                            || s.is(BlockTags.DOORS)
                                            || s.is(BlockTags.LEAVES),
                            to(() -> Blocks.AIR, FLAGS)),
                    row(
                            s -> s.is(Blocks.GRASS_BLOCK),
                            to(() -> ModBlocks.WASTE_EARTH.get(), FLAGS)),
                    row(
                            s -> s.is(Blocks.MYCELIUM),
                            to(() -> ModBlocks.WASTE_MYCELIUM.get(), FLAGS)),
                    row(s -> s.is(Blocks.SAND), oneIn(20, () -> ModBlocks.WASTE_TRINITITE.get())),
                    row(
                            s -> s.is(Blocks.RED_SAND),
                            oneIn(20, () -> ModBlocks.WASTE_TRINITITE_RED.get())),
                    row(s -> s.is(Blocks.CLAY), to(() -> Blocks.TERRACOTTA, FLAGS)),
                    row(s -> s.is(Blocks.MOSSY_COBBLESTONE), to(() -> Blocks.COAL_ORE, FLAGS)),
                    row(s -> s.is(Blocks.COAL_ORE), gems(30, 29)),
                    row(
                            s -> s.is(BlockTags.LOGS) || s.is(Blocks.MUSHROOM_STEM),
                            to(() -> ModBlocks.WASTE_LOG.get(), FLAGS)),
                    row(s -> s.is(BlockTags.PLANKS), to(() -> ModBlocks.WASTE_PLANKS.get(), FLAGS)),
                    row(
                            s ->
                                    s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                                            || s.is(Blocks.RED_MUSHROOM_BLOCK),
                            to(() -> Blocks.AIR, Block.UPDATE_CLIENTS)));

    private final Row[] rows;
    private final BlockStateClassificationCache rowOf;

    private BlockConversionTable(Row... rows) {
        this.rows = rows;
        this.rowOf = new BlockStateClassificationCache(this::classify);
    }

    @FunctionalInterface
    private interface Outcome {
        void apply(Level level, BlockPos pos, RandomSource random);
    }

    private record Row(Predicate<BlockState> when, Outcome then) {}

    private static Row row(Predicate<BlockState> when, Outcome then) {
        return new Row(when, then);
    }

    private static Outcome to(Supplier<Block> result, int flags) {
        return (level, pos, _) ->
                level.setBlock(pos, result.get().defaultBlockState(), flags);
    }

    private static Outcome oneIn(int bound, Supplier<Block> result) {
        return (level, pos, random) -> {
            if (random.nextInt(bound) == 1)
                level.setBlock(pos, result.get().defaultBlockState(), FLAGS);
        };
    }

    private static Outcome gems(int bound, int emerald) {
        return (level, pos, random) -> {
            int roll = random.nextInt(bound);
            if (roll >= 1 && roll <= 3)
                level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), FLAGS);
            else if (roll == emerald)
                level.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), FLAGS);
        };
    }

    private static Outcome uranium(Supplier<Block> schrabidium, Supplier<Block> scorched) {
        return (level, pos, random) ->
                level.setBlock(
                        pos,
                        (random.nextInt(VersatileConfig.getSchrabOreChance()) == 1
                                        ? schrabidium
                                        : scorched)
                                .get()
                                .defaultBlockState(),
                        FLAGS);
    }

    private int classify(BlockState state) {
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].when.test(state)) return i;
        }
        return rows.length;
    }

    public static void forgetTags() {
        FALLOUT.rowOf.forget();
        POISON.rowOf.forget();
        SoliniumBlockConversion.KINDS.forget();
    }

    public void apply(Level level, BlockPos pos, RandomSource random) {
        apply(level, pos, level.getBlockState(pos), random);
    }

    private void apply(Level level, BlockPos pos, BlockState state, RandomSource random) {
        int row = rowOf.of(state);
        if (row < rows.length) rows[row].then.apply(level, pos, random);
    }

    public void sphere(Level level, BlockPos centre, int radius, RandomSource random) {
        if (radius < 0)
            throw new IllegalArgumentException("Transmutation radius " + radius + " < 0");
        int half = radius * radius / 2;
        int band = half / 5;
        int limit = band > 0 ? half + band - 1 : half;
        var cursor = new BlockPos.MutableBlockPos();
        int cx = centre.getX(), cy = centre.getY(), cz = centre.getZ();
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                int planar = x * x + y * y;
                if (planar >= limit) continue;
                for (int z = -radius; z < radius; z++) {
                    int distance = planar + z * z;
                    if (distance >= limit
                            || distance >= half && distance - half >= random.nextInt(band))
                        continue;
                    var state = level.getBlockState(cursor.set(cx + x, cy + y, cz + z));
                    if (!state.isAir()) apply(level, cursor, state, random);
                }
            }
        }
    }
}
