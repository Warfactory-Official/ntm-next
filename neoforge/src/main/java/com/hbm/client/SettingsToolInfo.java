// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemSettingsTool;
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
        CompoundTag data = ItemSettingsTool.getData(held);
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
