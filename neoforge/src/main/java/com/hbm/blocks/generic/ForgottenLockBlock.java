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
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ForgottenLockBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public ForgottenLockBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
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
        var outward = hit.getDirection();
        if (outward == Direction.UP || outward == Direction.DOWN) {
            return InteractionResult.PASS;
        }
        boolean cracked = stack.is(ModItems.KEY_RED_CRACKED.get());
        if (!cracked && !stack.is(ModItems.KEY_RED.get())) {
            return InteractionResult.PASS;
        }
        if (cracked) {
            stack.shrink(1);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        openTunnel(level, pos, outward);
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.LOCK_OPEN.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void openTunnel(Level level, BlockPos origin, Direction outward) {
        var widthDirection = outward.getClockWise();
        var brick = ModBlocks.BRICK_FORGOTTEN.get().defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        var cursor = new BlockPos.MutableBlockPos();
        for (int width = -2; width <= 2; width++) {
            for (int height = -2; height <= 2; height++) {
                for (int depth = 0; depth < 15; depth++) {
                    cursor.set(
                            origin.getX()
                                    + widthDirection.getStepX() * width
                                    - outward.getStepX() * depth,
                            origin.getY() + height,
                            origin.getZ()
                                    + widthDirection.getStepZ() * width
                                    - outward.getStepZ() * depth);
                    boolean wall =
                            width == -2 || width == 2 || height == -2 || height == 2 || depth == 14;
                    level.setBlock(cursor, wall ? brick : air, Block.UPDATE_ALL);
                }
            }
        }
    }
}
