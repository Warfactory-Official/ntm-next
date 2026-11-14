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

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.util.Blink;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ModShieldItem extends ArmorModItem {

    public final float shield;

    public ModShieldItem(Properties properties, float shield) {
        super(properties, ArmorModHandler.KEVLAR, false, true, false, false);
        this.shield = shield;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        ChatFormatting color = Blink.lit() ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
        adder.accept(
                Component.translatable("desc.item.armorMod.shield", Math.round(shield * 10F) * 0.1F)
                        .withStyle(color));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        ChatFormatting color = Blink.lit() ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.health.installed",
                                stack.getHoverName(),
                                Math.round(shield * 10F) * 0.1F)
                        .withStyle(color));
    }
}
