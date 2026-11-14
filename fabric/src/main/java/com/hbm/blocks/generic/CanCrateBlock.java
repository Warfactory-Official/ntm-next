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

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.util.RegistryUtil;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CanCrateBlock extends Block {
    private static final long CONTENT_DOMAIN =
            WorldgenHash.identifier(RegistryUtil.id("crate_can"));

    public CanCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
        var pool = CrateContents.canPool();
        int count = 5 + random.nextInt(4);
        for (int index = 0; index < count; index++) {
            var selected = pool.get(random.nextInt(pool.size()));
            popResource(level, pos, selected.copy());
        }
        LootCrateBlock.finishOpening(level, pos);
        return InteractionResult.SUCCESS;
    }
}
