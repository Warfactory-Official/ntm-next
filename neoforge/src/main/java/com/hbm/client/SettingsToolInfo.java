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

import com.hbm.items.ModItems;
import com.hbm.items.tool.SettingsToolItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class SettingsToolInfo {
    private static final int FIRST_ID = 897;
    private static final int MILLIS = 4_000;

    private SettingsToolInfo() {}

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.level.getGameTime() % 5L != 0L)
            return;
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.SETTINGS_TOOL.get())) return;
        CompoundTag data = SettingsToolItem.getData(held);
        if (data == null) return;

        ListTag displayInfo = data.getListOrEmpty("displayInfo");
        int selected = data.getIntOr("copyIndex", 0);
        for (int i = 0; i < displayInfo.size(); i++) {
            String key = displayInfo.getCompoundOrEmpty(i).getStringOr("info", "");
            Component line =
                    Component.translatable(key)
                            .withStyle(i == selected ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
            InfoSystem.push(new InfoSystem.InfoEntry(line, MILLIS), FIRST_ID + i);
        }
    }
}
