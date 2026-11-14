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

package com.hbm.client;

import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.util.GameTime;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ToolConversionLookOverlay {
    private ToolConversionLookOverlay() {}

    public static void build(
            Level level,
            Component title,
            ToolType tool,
            List<CountIngredient> cost,
            LookOverlay info) {
        if (ToolType.getType(Minecraft.getInstance().player.getMainHandItem()) != tool) return;
        if (cost.isEmpty()) return;

        long millis = GameTime.millis(level);
        List<Item> tools = tool.getItems();
        info.title(title);
        info.row(
                Component.empty().append(Component.translatable("desc.tooling.requires")),
                0xFFAA00);
        info.row(
                Component.empty()
                        .append(
                                Component.translatable(
                                        "desc.tooling.tool",
                                        new ItemStack(
                                                        tools.get(
                                                                (int)
                                                                        (millis
                                                                                / 1000
                                                                                % tools.size())))
                                                .getHoverName())),
                0x5555FF);
        for (CountIngredient material : cost) {
            ItemStack display = material.extractForCyclingDisplay(millis, 20);
            if (display.isEmpty()) continue;
            info.row(
                    Component.empty()
                            .append(
                                    Component.translatable(
                                            "desc.tooling.material",
                                            display.getHoverName(),
                                            display.getCount())));
        }
    }
}
