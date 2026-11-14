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

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class SwordMeteoriteItem extends SwordAbilityItem {

    public SwordMeteoriteItem(Properties properties) {
        super(properties, AvailableAbilities.EMPTY);
    }

    public static void upgrade(
            NonNullList<ItemStack> inventory,
            int slot,
            RegistryHandle<? extends Item> from,
            RegistryHandle<? extends Item> to) {
        if (inventory.get(slot).is(from.get())) inventory.set(slot, new ItemStack(to.get()));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        for (Component line :
                TooltipStyle.lines(
                        Component.translatableWithFallback(getDescriptionId() + ".desc", ""))) {
            adder.accept(line.copy().withStyle(ChatFormatting.ITALIC));
        }
    }
}
