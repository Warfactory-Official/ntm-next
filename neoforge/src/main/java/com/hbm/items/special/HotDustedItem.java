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

import com.hbm.items.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class HotDustedItem extends HotItem {
    public static final int FOLDS = 10;
    private static final int HEAT_PER_FOLD = 10;

    public HotDustedItem(Properties properties, int maxHeat) {
        super(properties, maxHeat);
    }

    public static int folds(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FORGE_COUNT.get(), 0);
    }

    public int maxHeat(int folds) {
        return maxHeat() - folds * HEAT_PER_FOLD;
    }

    @Override
    public int maxHeat(ItemStack stack) {
        return maxHeat(folds(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.hot_dusted.forged", folds(stack)));
    }
}
