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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class BlockTallPlant extends DoublePlantBlock implements BonemealableBlock {
    private final Variant variant;

    public BlockTallPlant(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
    }

    public Variant variant() {
        return variant;
    }

    @Override
    protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return PlantGrowth.supports(soil);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && direction == Direction.UP
                && !(neighborState.is(this)
                        && neighborState.getValue(HALF) == DoubleBlockHalf.UPPER)) {
            var flower = variant.flower().defaultBlockState();
            if (flower.canSurvive(level, pos)) {
                return flower;
            }
        }
        return super.updateShape(
                state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.preventsBlockDrops()
                && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            var upperPos = pos.above();
            var upper = level.getBlockState(upperPos);
            if (upper.is(this) && upper.getValue(HALF) == DoubleBlockHalf.UPPER) {
                level.setBlock(
                        upperPos,
                        Blocks.AIR.defaultBlockState(),
                        UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
                dropResources(upper, level, upperPos, null, player, player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (variant == Variant.WEED) {
            var soil = level.getBlockState(pos.below());
            if (soil.is(ModBlocks.DIRT_DEAD.get()) || soil.is(ModBlocks.DIRT_OILY.get())) {
                level.setBlockAndUpdate(
                        pos, ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState());
                return;
            }
        }
        if (isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)
                && isBonemealSuccess(level, random, pos, state, BonemealSource.INTERACTION)
                && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state, BonemealSource.INTERACTION);
        }
    }

    @Override
    public boolean isValidBonemealTarget(
            LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        if (variant != Variant.CD2 && variant != Variant.CD3) {
            return false;
        }
        var lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!PlantGrowth.hasWaterAtSoilSides(level, lowerPos)) {
            return false;
        }
        if (variant == Variant.CD2) {
            return true;
        }
        var soil = level.getBlockState(lowerPos.below());
        return soil.is(ModBlocks.DIRT_DEAD.get()) || soil.is(ModBlocks.DIRT_OILY.get());
    }

    @Override
    public boolean isBonemealSuccess(
            Level level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource source) {
        return variant == Variant.CD3 || random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource source) {
        var lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        DoublePlantBlock.placeAt(
                level,
                variant.next().defaultBlockState(),
                lowerPos,
                UPDATE_ALL | UPDATE_KNOWN_SHAPE);
        if (variant == Variant.CD3) {
            level.setBlockAndUpdate(lowerPos.below(), Blocks.DIRT.defaultBlockState());
        }
    }

    public enum Variant implements StringRepresentable {
        WEED,
        CD2,
        CD3,
        CD4;

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        public Block flower() {
            return this == WEED
                    ? ModBlocks.PLANT_FLOWER_WEED.get()
                    : ModBlocks.PLANT_FLOWER_CD0.get();
        }

        public Block next() {
            return this == CD2 ? ModBlocks.PLANT_TALL_CD3.get() : ModBlocks.PLANT_TALL_CD4.get();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
