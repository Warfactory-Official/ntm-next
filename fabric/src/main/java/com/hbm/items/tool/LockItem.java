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

package com.hbm.items.tool;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LockItem extends KeyPinItem {

    private final double lockMod;

    public LockItem(Properties properties, double lockMod) {
        super(properties);
        this.lockMod = lockMod;
    }

    private static BlockEntity lockableAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (MultiblockSurface.isFoldedCell(state)) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            return core == null ? null : level.getBlockEntity(core);
        }
        return level.getBlockEntity(pos);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer().isSpectator()) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (getPins(stack) == 0) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockEntity blockEntity = lockableAt(level, context.getClickedPos());
        if (!(blockEntity instanceof LockableBlockEntity lock)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (lock.isLocked()) return InteractionResult.FAIL;

        lock.setPinCode(getPins(stack));
        lock.lock();
        lock.setPickModifier(lockMod);
        level.playSound(
                null,
                context.getPlayer().getX(),
                context.getPlayer().getY(),
                context.getPlayer().getZ(),
                ModSounds.LOCK_HANG.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        stack.consume(1, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
