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

import com.hbm.items.StackRemainderItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public class CraftingDegradationItem extends StackRemainderItem {

    public CraftingDegradationItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected @Nullable ItemStackTemplate remainder(ItemStack consumed) {
        ItemStack tool = consumed.copyWithCount(1);
        if (tool.isDamageableItem()) {
            int damage = tool.getDamageValue() + 1;
            if (damage > tool.getMaxDamage()) return null;
            tool.setDamageValue(damage);
        }
        return ItemStackTemplate.fromNonEmptyStack(tool);
    }
}
