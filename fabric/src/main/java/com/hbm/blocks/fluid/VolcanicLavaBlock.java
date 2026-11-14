/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.util.RegistryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class VolcanicLavaBlock extends ClassicFluidBlock {

    public VolcanicLavaBlock(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    protected Block basaltForCheck() {
        return Crust.BASALT;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected boolean displaces(BlockState below) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(below) > 0;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        level.scheduleTick(pos, state.getFluidState().getType(), fluid.getTickDelay(level));
        if (level.isClientSide()) return;
        for (Direction dir : Direction.VALUES) {
            BlockPos touched = pos.relative(dir);
            Block scorched = getReaction(level, touched);
            if (scorched != null) level.setBlockAndUpdate(touched, scorched.defaultBlockState());
        }
    }

    protected @Nullable Block getReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isWater(state)) return Blocks.STONE;
        if (state.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get();
        if (state.is(BlockTags.PLANKS)) return ModBlocks.WASTE_PLANKS.get();
        if (state.is(BlockTags.LEAVES)) return Blocks.FIRE;
        return mineralReaction(level, state);
    }

    protected @Nullable Block mineralReaction(Level level, BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) ? ModBlocks.ORE_BASALT_GEM.get() : null;
    }

    protected static boolean isWater(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    protected void flowTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block crust = basaltForCheck();
        int lava = 0;
        int crusted = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.VALUES) {
            Block near = level.getBlockState(cursor.setWithOffset(pos, dir)).getBlock();
            if (near == this) lava++;
            else if (near == crust) crusted++;
        }
        boolean thin = !state.getFluidState().isSource() && lava < 2;
        if (!thin && (random.nextInt(5) != 0 || lava >= 5)) return;
        if (level.getBlockState(pos.below()).is(this)) return;
        onSolidify(level, pos, lava, crusted, random);
    }

    protected void onSolidify(
            ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int roll = rand.nextInt(200);
        Block result;
        if (roll < Crust.VEINS.length) {
            result = Crust.VEINS[roll];
        } else {
            Block crust = basaltForCheck();
            Block capped = level.getBlockState(pos.above(10)).getBlock();
            boolean gem =
                    roll < 15
                            && lavaCount + basaltCount == 6
                            && lavaCount < 3
                            && (capped == crust || capped == this);
            result = gem ? ModBlocks.ORE_BASALT_GEM.get() : crust;
        }
        level.setBlockAndUpdate(pos, result.defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (level.getBlockState(pos.above()).isAir()) {
            if (rand.nextInt(100) == 0) {
                double x = pos.getX() + rand.nextFloat(), z = pos.getZ() + rand.nextFloat();
                double top = pos.getY() + 1.0D;
                level.addParticle(ParticleTypes.LAVA, x, top, z, 0.0D, 0.0D, 0.0D);
                ambient(level, x, top, z, SoundEvents.LAVA_POP, rand);
            }
            if (rand.nextInt(200) == 0)
                ambient(level, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, rand);
        }

        BlockPos below = pos.below();
        if (rand.nextInt(10) != 0
                || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || level.getBlockState(below.below()).is(BlockTags.BLOCKS_MOTION)) return;
        double x = pos.getX() + rand.nextFloat(), z = pos.getZ() + rand.nextFloat();
        level.addParticle(ParticleTypes.DRIPPING_LAVA, x, pos.getY() - 1.05D, z, 0.0D, 0.0D, 0.0D);
    }

    private static void ambient(
            Level level, double x, double y, double z, SoundEvent sound, RandomSource rand) {
        float volume = 0.2F + rand.nextFloat() * 0.2F;
        level.playLocalSound(
                x,
                y,
                z,
                sound,
                SoundSource.AMBIENT,
                volume,
                0.9F + rand.nextFloat() * 0.15F,
                false);
    }

    private static final class Crust {

        static final Block BASALT = named("basalt");
        static final Block SULFUR = named("ore_basalt_sulfur");
        static final Block[] VEINS = {
            SULFUR,
            SULFUR,
            named("ore_basalt_fluorite"),
            ModBlocks.ORE_BASALT_ASBESTOS.get(),
            named("ore_basalt_molysite")
        };

        private static Block named(String name) {
            return BuiltInRegistries.BLOCK.getOptional(RegistryUtil.id(name)).orElseThrow();
        }
    }
}
