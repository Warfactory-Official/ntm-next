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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class LootCrateBlock extends FallingBlock {
    public static final MapCodec<LootCrateBlock> CODEC = simpleCodec(LootCrateBlock::new);
    private static final long CONTENT_DOMAIN = WorldgenHash.identifier(RegistryUtil.id("crate"));

    public LootCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<LootCrateBlock> codec() {
        return CODEC;
    }

    public static List<ItemStack> poolOf(Block block) {
        return CrateContents.poolOf(block);
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getMapColor(level, pos).col;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(ModItems.CROWBAR.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        var random = NtmWorldgenFields.get((ServerLevel) level).random(CONTENT_DOMAIN, pos);
        var block = state.getBlock();
        var pool = poolOf(block);
        List<ItemStack> selected;
        if (block == ModBlocks.CRATE_RED.get()) {
            selected = pool;
        } else {
            int count;
            if (block == ModBlocks.CRATE_WEAPON.get()) {
                count = 1 + random.nextInt(2);
                if (random.nextInt(100) == 34) {
                    count = 25;
                }
            } else {
                count = 3 + random.nextInt(3);
            }
            selected = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                selected.add(pool.get(random.nextInt(pool.size())));
            }
        }

        for (var prototype : selected) {
            float offsetX = random.nextFloat() * 0.8F + 0.1F;
            float offsetY = random.nextFloat() * 0.8F + 0.1F;
            float offsetZ = random.nextFloat() * 0.8F + 0.1F;
            var entity =
                    new ItemEntity(
                            level,
                            pos.getX() + offsetX,
                            pos.getY() + offsetY,
                            pos.getZ() + offsetZ,
                            prototype.copy());
            float velocityX = (float) random.nextGaussian() * 0.05F;
            float velocityY = (float) random.nextGaussian() * 0.05F + 0.2F;
            float velocityZ = (float) random.nextGaussian() * 0.05F;
            entity.setDeltaMovement(velocityX, velocityY, velocityZ);
            level.addFreshEntity(entity);
        }
        finishOpening(level, pos);
        return InteractionResult.SUCCESS;
    }

    static void finishOpening(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }
}
