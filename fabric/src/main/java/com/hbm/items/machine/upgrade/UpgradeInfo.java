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

package com.hbm.items.machine.upgrade;

import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.util.GameTime;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public final class UpgradeInfo {
    private UpgradeInfo() {}

    public static void append(
            Block block,
            UpgradeInfoProvider provider,
            MachineUpgradeItem upgrade,
            List<Component> tooltip) {
        if (!appendHeader(block, provider.upgradeCaps(), upgrade, tooltip)) return;
        if (upgrade.type == UpgradeType.OVERDRIVE) overdrive(tooltip);
        else provider.appendUpgradeInfo(upgrade.type, upgrade.tier, tooltip);
    }

    public static boolean appendHeader(
            Block block, UpgradeLimits caps, MachineUpgradeItem upgrade, List<Component> tooltip) {
        if (caps.max(upgrade.type) <= 0) return false;
        tooltip.add(
                Component.translatable("tooltip.hbm.upgrade.machine", block.getName())
                        .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    private static void overdrive(List<Component> tooltip) {
        tooltip.add(
                Component.translatable("tooltip.hbm.upgrade.overdrive")
                        .withStyle(
                                GameTime.millis() % 1000 < 500
                                        ? ChatFormatting.RED
                                        : ChatFormatting.DARK_GRAY));
    }

    public static void electricFurnace(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) speedAndPower(tooltip, -level * 25, level * 100);
        else powerAndDelay(tooltip, -level * 30, level * 10);
    }

    public static void centrifuge(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.OVERDRIVE) overdrive(tooltip);
        else if (type == UpgradeType.SPEED)
            speedAndPower(tooltip, reduction(level + 1), level * 100);
        else percent(tooltip, "upgrade.consumption", reduction(level + 1), true);
    }

    public static void soldering(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) speedAndPower(tooltip, -level * 100 / 6, level * 100);
        else powerAndDelay(tooltip, -level * 100 / 6, level * 100 / 3);
    }

    public static void processor(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) {
            percent(tooltip, "upgrade.speed", level * 100 / 3, true);
            percent(tooltip, "upgrade.consumption", level * 100, false);
        } else percent(tooltip, "upgrade.consumption", -level * 25, true);
    }

    public static int reduction(int divisor) {
        return 100 / divisor - 100;
    }

    public static Component percentage(int value) {
        return Component.translatable(
                "tooltip.hbm.upgrade.percent", (value >= 0 ? "+" : "") + value);
    }

    public static void percent(List<Component> tooltip, String key, int value, boolean beneficial) {
        tooltip.add(
                Component.translatable(key, percentage(value))
                        .withStyle(beneficial ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    public static void speedAndPower(List<Component> tooltip, int delay, int power) {
        percent(tooltip, "upgrade.delay", delay, true);
        percent(tooltip, "upgrade.consumption", power, false);
    }

    public static void powerAndDelay(List<Component> tooltip, int power, int delay) {
        percent(tooltip, "upgrade.consumption", power, true);
        percent(tooltip, "upgrade.delay", delay, false);
    }

    public static void range(List<Component> tooltip, int meters) {
        tooltip.add(
                Component.translatable(
                                "upgrade.range",
                                Component.translatable("tooltip.hbm.upgrade.meters", meters))
                        .withStyle(ChatFormatting.GREEN));
    }
}
