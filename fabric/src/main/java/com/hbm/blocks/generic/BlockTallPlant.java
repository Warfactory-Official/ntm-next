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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class BlockTallPlant extends DoublePlantBlock implements BonemealableBlock {

    public static final MapCodec<BlockTallPlant> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            propertiesCodec(),
                                            Variant.CODEC
                                                    .fieldOf("variant")
                                                    .forGetter(plant -> plant.variant))
                                    .apply(i, BlockTallPlant::new));

    private final Variant variant;

    public BlockTallPlant(BlockBehaviour.Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
    }

    public Variant variant() {
        return variant;
    }

    @Override
    public MapCodec<? extends BlockTallPlant> codec() {
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
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && directionToNeighbour == Direction.UP
                && !neighbourState.is(this)
                && state.canSurvive(level, pos)) {
            BlockState flower = variant.flower().defaultBlockState();
            if (flower.canSurvive(level, pos)) return flower;
        }
        return super.updateShape(
                state,
                level,
                ticks,
                pos,
                directionToNeighbour,
                neighbourPos,
                neighbourState,
                random);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && !player.preventsBlockDrops()) {
            BlockPos above = pos.above();
            BlockState crown = level.getBlockState(above);
            if (crown.is(this) && crown.getValue(HALF) == DoubleBlockHalf.UPPER) {

                level.setBlock(
                        above,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                dropResources(crown, level, above, null, player, player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!variant.needsOil && deadGround(level, pos)) {
            level.setBlock(
                    pos,
                    ModBlocks.PLANT_DEAD_BIGFLOWER.get().defaultBlockState(),
                    Block.UPDATE_ALL);
            return;
        }
        if (isValidBonemealTarget(level, pos, state)
                && isBonemealSuccess(level, random, pos, state)
                && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        if (!variant.grows) return false;
        BlockPos soil = lower(state, pos).below();
        if (!hasWaterBeside(level, soil)) return false;
        return variant != Variant.CD3 || deadGroundAt(level, soil);
    }

    @Override
    public boolean isBonemealSuccess(
            Level level, RandomSource random, BlockPos pos, BlockState state) {
        return variant == Variant.CD3 || random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(
            ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!variant.grows) return;
        BlockPos lower = lower(state, pos);

        placeAt(
                level,
                variant.next().defaultBlockState(),
                lower,
                Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        if (variant == Variant.CD3) {
            level.setBlock(lower.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private boolean deadGround(LevelReader level, BlockPos pos) {
        return deadGroundAt(level, pos.below());
    }

    private static boolean deadGroundAt(LevelReader level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos);
        return ground.is(ModBlocks.DIRT_DEAD.get()) || ground.is(ModBlocks.DIRT_OILY.get());
    }

    private static boolean hasWaterBeside(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.WATER)) return true;
        }
        return false;
    }

    public enum Variant implements StringRepresentable {
        WEED(false, false),
        CD2(true, true),
        CD3(true, true),
        CD4(false, true);

        public static final Codec<Variant> CODEC = StringRepresentable.fromEnum(Variant::values);

        private final boolean grows;
        private final boolean needsOil;

        Variant(boolean grows, boolean needsOil) {
            this.grows = grows;
            this.needsOil = needsOil;
        }

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
