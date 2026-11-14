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

package com.hbm.items.special;

import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.items.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RagItem extends CustomLoreItem implements DroppedItemUpdateHandler {

    public RagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER)) return false;

        entity.setItem(new ItemStack(ModItems.RAG_DAMP.get(), entity.getItem().getCount()));
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(ModItems.RAG_PISS), Prediction.SERVER_ONLY);
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
