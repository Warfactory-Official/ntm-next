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

package com.hbm.items.block;

import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.machine.CustomMachineDefinitions;
import com.hbm.items.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class CustomMachineItem extends BlockItem {

    public CustomMachineItem(Block block, Properties props) {
        super(block, props);
    }

    public static @Nullable ResourceKey<CustomMachineDefinition> typeOf(ItemStack stack) {
        return stack.get(ModDataComponents.CUSTOM_MACHINE_TYPE.get());
    }

    public static ItemStack of(Block block, ResourceKey<CustomMachineDefinition> type) {
        ItemStack stack = new ItemStack(block);
        stack.set(ModDataComponents.CUSTOM_MACHINE_TYPE.get(), type);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        CustomMachineDefinition definition = CustomMachineDefinitions.get(typeOf(stack));
        return definition != null
                ? definition.name()
                : Component.translatable("item.hbm.custom_machine.invalid");
    }
}
