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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

public class NtmFlowerBlock extends VegetationBlock implements BonemealableBlock {
    public static final MapCodec<NtmFlowerBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            propertiesCodec(),
                                            Variant.CODEC
                                                    .fieldOf("variant")
                                                    .forGetter(block -> block.variant))
                                    .apply(instance, NtmFlowerBlock::new));

    private final Variant variant;

    public NtmFlowerBlock(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends NtmFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return PlantGrowth.supports(soil);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return variant == Variant.WEED || variant == Variant.CD0 || variant == Variant.CD1;
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isValidBonemealTarget(level, pos, state)
                && isBonemealSuccess(level, random, pos, state)
                && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return switch (variant) {
            case WEED -> level.isEmptyBlock(pos.above());
            case CD0 -> PlantGrowth.hasWaterAtSoilSides(level, pos);
            case CD1 ->
                    PlantGrowth.hasWaterAtSoilSides(level, pos) && level.isEmptyBlock(pos.above());
            default -> true;
        };
    }

    @Override
    public boolean isBonemealSuccess(
            Level level, RandomSource random, BlockPos pos, BlockState state) {
        return switch (variant) {
            case WEED, CD0, CD1 -> random.nextFloat() < 0.33F;
            default -> true;
        };
    }

    @Override
    public void performBonemeal(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        switch (variant) {
            case WEED -> {
                var soil = level.getBlockState(pos.below());
                if (soil.is(ModBlocks.DIRT_DEAD.get()) || soil.is(ModBlocks.DIRT_OILY.get())) {
                    level.setBlockAndUpdate(pos, ModBlocks.PLANT_DEAD.get().defaultBlockState());
                    return;
                }
                DoublePlantBlock.placeAt(
                        level,
                        ModBlocks.PLANT_TALL_WEED.get().defaultBlockState(),
                        pos,
                        UPDATE_ALL);
            }
            case CD0 ->
                    level.setBlockAndUpdate(
                            pos, ModBlocks.PLANT_FLOWER_CD1.get().defaultBlockState());
            case CD1 ->
                    DoublePlantBlock.placeAt(
                            level,
                            ModBlocks.PLANT_TALL_CD2.get().defaultBlockState(),
                            pos,
                            UPDATE_ALL);
            default -> dropResources(state, level, pos);
        }
    }

    public enum Variant implements StringRepresentable {
        FOXGLOVE,
        TOBACCO,
        NIGHTSHADE,
        WEED,
        CD0,
        CD1;

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
