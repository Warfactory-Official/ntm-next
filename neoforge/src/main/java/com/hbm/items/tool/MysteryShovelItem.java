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

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class MysteryShovelItem extends Item {

    public MysteryShovelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide() || !level.getBlockState(pos).is(ModBlocks.NTM_DIRT.get()))
            return InteractionResult.PASS;

        level.destroyBlock(pos, false);
        RandomSource random = RandomSource.create();
        for (var collectible :
                List.of(ModItems.HS_ELEMENTS, ModItems.HS_ARSENIC, ModItems.HS_VAULT)) {
            float x = random.nextFloat() * 0.8F + 0.1F;
            float y = random.nextFloat() * 0.8F + 0.1F;
            float z = random.nextFloat() * 0.8F + 0.1F;
            ItemEntity item =
                    new ItemEntity(
                            level,
                            pos.getX() + x,
                            pos.getY() + y,
                            pos.getZ() + z,
                            new ItemStack(collectible.get()));
            item.setDeltaMovement(
                    random.nextGaussian() * 0.05F,
                    random.nextGaussian() * 0.05F + 0.2F,
                    random.nextGaussian() * 0.05F);
            level.addFreshEntity(item);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.mysteryShovel.lostButNotForgotten"));
    }
}
