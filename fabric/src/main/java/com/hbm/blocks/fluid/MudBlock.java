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
import com.hbm.blocks.bomb.DigammaMatterBlock;
import com.hbm.blocks.generic.TritiumLampBlock;
import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MudBlock extends ClassicFluidBlock {

    public MudBlock(ClassicFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        if (level.isClientSide()) return;
        if (entity instanceof Player p && (p.isSpectator() || p.getAbilities().instabuild)) return;
        if (entity instanceof LivingEntity living && ArmorUtil.checkForHazmat(living)) return;
        entity.hurt(level.damageSources().source(ModDamageTypes.MUD_POISONING), 8.0F);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void flowTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.VALUES) erode(level, pos.relative(dir), random);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);

        for (Direction dir : Direction.VALUES) {
            BlockPos side = pos.relative(dir);
            BlockState neighbour = level.getBlockState(side);
            if (!neighbour.is(this) && FiniteFluidBlock.isLiquid(neighbour)) {
                level.setBlockAndUpdate(side, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private void erode(ServerLevel level, BlockPos pos, RandomSource rand) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this)) return;
        if (state.isAir()) return;

        if (state.is(Blocks.STONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.STONE_BRICK_STAIRS)
                || state.is(Blocks.STONE_SLAB)) {
            if (rand.nextInt(20) == 0)
                level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
            return;
        }
        if (state.is(Blocks.COBBLESTONE)) {
            if (rand.nextInt(15) == 0)
                level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
            return;
        }
        if (state.is(Blocks.SANDSTONE)) {
            if (rand.nextInt(5) == 0) level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
            return;
        }
        if (state.is(BlockTags.TERRACOTTA)) {
            if (rand.nextInt(10) == 0)
                level.setBlockAndUpdate(pos, Blocks.CLAY.defaultBlockState());
            return;
        }

        if (eroded(state)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static boolean eroded(BlockState state) {
        Block block = state.getBlock();
        return block.getExplosionResistance() < 1.2F
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.instrument() == NoteBlockInstrument.HAT
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.PORTALS)
                || state.is(BlockTags.RAILS)
                || block instanceof WebBlock
                || block instanceof LadderBlock
                || state.is(Blocks.PISTON)
                || state.is(Blocks.STICKY_PISTON)
                || state.is(Blocks.PISTON_HEAD)
                || state.is(Blocks.MOVING_PISTON)
                || block instanceof RadioTorchBlock
                || block instanceof DroneWaypointBlock
                || block instanceof TritiumLampBlock
                || block instanceof DigammaMatterBlock
                || block == ModBlocks.GLASS_QUARTZ.get()
                || block == ModBlocks.BLOCK_INSULATOR.get()
                || block == ModBlocks.BLOCK_FIBERGLASS.get()
                || block == ModBlocks.BLOCK_ASBESTOS.get()
                || block == ModBlocks.DECO_ASBESTOS.get();
    }
}
