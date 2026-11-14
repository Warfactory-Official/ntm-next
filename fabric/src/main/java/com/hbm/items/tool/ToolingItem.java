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

import com.hbm.interfaces.ToolType;
import com.hbm.interfaces.ToolTypeProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ToolingItem extends CraftingDegradationItem implements ToolTypeProvider {

    protected final ToolType type;

    public ToolingItem(Item.Properties properties, ToolType type) {
        super(properties);
        this.type = type;
    }

    public static Item.Properties weaponProperties(int durability, float attackDamage) {
        return new Item.Properties()
                .stacksTo(1)
                .durability(durability)
                .attributes(ToolAbilityItem.attributes(attackDamage, 0));
    }

    @Override
    public ToolType getToolType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ToolInteractions.useOn(ctx, type)) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (!ctx.getLevel().isClientSide() && stack.isDamageableItem() && player != null) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
