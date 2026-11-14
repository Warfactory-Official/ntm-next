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

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukePrototypeBlockEntity;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class NukePrototypeBlock extends NukeAssemblyBlock {

    public NukePrototypeBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukePrototypeBlockEntity(pos, state);
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
        if (player.isSecondaryUseActive() || !stack.is(ModItems.IGNITER.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) trigger(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        NukeExplosionMK3Entity blast =
                NukeExplosionMK3Entity.statFacFleija(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yield);
        if (blast.isRemoved()) return;

        playBoom(level, pos);
        level.addFreshEntity(blast);
        level.addFreshEntity(
                CloudFleijaEntity.statFac(level, yield, pos.getX(), pos.getY(), pos.getZ()));
    }
}
