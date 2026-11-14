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

package com.hbm.items.food;

import com.hbm.items.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MarshmallowItem extends Item {

    public MarshmallowItem(Properties props) {
        super(props);
    }

    public static ItemStack roasted(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.ROASTED.get(), true);
        return stack;
    }

    public static boolean isRaw(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.ROASTED.get(), false);
    }
}
