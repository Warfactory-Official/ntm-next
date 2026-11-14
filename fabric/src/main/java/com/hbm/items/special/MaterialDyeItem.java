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

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public final class MaterialDyeItem extends MaterialShapeItem implements SignApplicator {

    public MaterialDyeItem(
            Properties properties, Material material, MaterialShape shape, DyeColor dye) {
        super(properties.component(DataComponents.DYE, dye), material, shape);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return Items.DYE.white().interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public boolean tryApplyToSign(
            Level level, SignBlockEntity sign, boolean front, ItemStack stack, Player player) {
        return ((SignApplicator) Items.DYE.white())
                .tryApplyToSign(level, sign, front, stack, player);
    }
}
