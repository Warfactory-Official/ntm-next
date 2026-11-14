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

import com.hbm.util.FertilizerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.jspecify.annotations.Nullable;

public class FertilizerItem extends Item {

    public FertilizerItem(Properties properties) {
        super(properties);
    }

    public static boolean spread(ItemStack stack, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel)) return false;
        boolean any = sweep(level, pos, null, stack);
        if (any) stack.shrink(1);
        return any;
    }

    private static boolean sweep(
            Level level, BlockPos centre, @Nullable Player player, ItemStack stack) {
        boolean any = false;
        for (BlockPos cursor :
                BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            BlockPos pos = cursor.immutable();
            if (!FertilizerUtil.fertilize(level, pos, player, stack, pos.equals(centre))) continue;
            any = true;
            if (!level.isClientSide()) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
            }
        }
        return any;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        @Nullable Player player = ctx.getPlayer();
        if (player != null
                && !player.mayUseItemAt(pos, ctx.getClickedFace(), ctx.getItemInHand())) {
            return InteractionResult.PASS;
        }

        ItemStack stack = ctx.getItemInHand();
        if (sweep(level, pos, player, stack)) stack.consume(1, player);
        return InteractionResult.PASS;
    }
}
