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

package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VendingMachineBlock extends MultiblockCoreBlock {
    public final String pool;

    public VendingMachineBlock(Properties properties, String pool) {
        super(properties);
        this.pool = pool;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (held.getItem() != ModItems.COIN_TOKEN.get()) {
            return InteractionResult.PASS;
        }

        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        var reward = ItemPool.getStack(pool, level.getRandom());
        var clicked = hit.getBlockPos();
        if (!reward.isEmpty()) {
            var facing = state.getValue(FACING);
            double x = clicked.getX() + 0.5 + facing.getStepX() * 0.75;
            double y = core.getY() + 0.25;
            double z = clicked.getZ() + 0.5 + facing.getStepZ() * 0.75;
            level.addFreshEntity(new ItemEntity(level, x, y, z, reward));
        }
        level.playSound(
                null, clicked, ModSounds.GUN_BOLT_OPEN.get(), SoundSource.BLOCKS, 1.0F, 0.75F);
        return InteractionResult.SUCCESS;
    }
}
