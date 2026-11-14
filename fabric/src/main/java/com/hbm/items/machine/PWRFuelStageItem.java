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

package com.hbm.items.machine;

import com.hbm.items.machine.PWRFuelItem.EnumPWRFuel;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class PWRFuelStageItem extends Item {

    private final boolean hot;
    public final EnumPWRFuel fuel;

    public PWRFuelStageItem(Properties props, boolean hot, EnumPWRFuel fuel) {
        super(props);
        this.hot = hot;
        this.fuel = fuel;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("item.hbm.pwr_fuel_" + fuel.id)
                        .withStyle(ChatFormatting.ITALIC));
        if (hot)
            adder.accept(
                    Component.translatable("desc.item.wasteCooling")
                            .withStyle(ChatFormatting.GOLD));
    }
}
