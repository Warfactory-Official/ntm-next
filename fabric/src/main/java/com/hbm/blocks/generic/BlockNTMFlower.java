// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockNTMFlower extends VegetationBlock implements BonemealableBlock {

    public static final MapCodec<BlockNTMFlower> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            propertiesCodec(),
                                            Variant.CODEC
                                                    .fieldOf("variant")
                                                    .forGetter(flower -> flower.variant))
                                    .apply(i, BlockNTMFlower::new));

    private final Variant variant;

    public BlockNTMFlower(BlockBehaviour.Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends BlockNTMFlower> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.FARMLAND)
                || state.is(ModBlocks.DIRT_DEAD.get())
                || state.is(ModBlocks.DIRT_OILY.get());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return variant.grows;
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
        if (variant.needsWater && !hasWaterBeside(level, pos.below())) return false;
        if (variant.needsHeadroom) return level.isEmptyBlock(pos.above());
        return true;
    }

    private static boolean hasWaterBeside(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.WATER)) return true;
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(
            Level level, RandomSource random, BlockPos pos, BlockState state) {
        return !variant.grows || random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (variant == Variant.WEED) {
            BlockState ground = level.getBlockState(pos.below());
            if (ground.is(ModBlocks.DIRT_DEAD.get()) || ground.is(ModBlocks.DIRT_OILY.get())) {
                level.setBlock(
                        pos, ModBlocks.PLANT_DEAD.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            grow(level, pos, ModBlocks.PLANT_TALL_WEED.get());
            return;
        }
        if (variant == Variant.CD0) {
            level.setBlock(
                    pos, ModBlocks.PLANT_FLOWER_CD1.get().defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        if (variant == Variant.CD1) {
            grow(level, pos, ModBlocks.PLANT_TALL_CD2.get());
            return;
        }
        Block.popResource(level, pos, new ItemStack(this));
    }

    private static void grow(ServerLevel level, BlockPos pos, Block tall) {
        DoublePlantBlock.placeAt(level, tall.defaultBlockState(), pos, Block.UPDATE_ALL);
    }

    public enum Variant implements StringRepresentable {
        FOXGLOVE(false, false, false),
        TOBACCO(false, false, false),
        NIGHTSHADE(false, false, false),

        WEED(true, false, true),
        CD0(true, true, false),
        CD1(true, true, true);

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        private final boolean grows;
        private final boolean needsWater;
        private final boolean needsHeadroom;

        Variant(boolean grows, boolean needsWater, boolean needsHeadroom) {
            this.grows = grows;
            this.needsWater = needsWater;
            this.needsHeadroom = needsHeadroom;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
