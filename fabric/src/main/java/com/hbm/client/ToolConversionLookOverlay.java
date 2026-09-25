// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.interfaces.ILookOverlay.LookInfo;
import com.hbm.interfaces.IToolable.ToolType;
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
            Level level, String title, ToolType tool, List<CountIngredient> cost, LookInfo info) {
        if (ToolType.getType(Minecraft.getInstance().player.getMainHandItem()) != tool) return;
        if (cost.isEmpty()) return;

        long millis = GameTime.millis(level);
        List<Item> tools = tool.items();
        info.title(title, 0xFFFF00, 0x404000);
        info.line(Component.translatable("desc.tooling.requires").getString(), 0xFFAA00);
        info.line(
                Component.translatable(
                                "desc.tooling.tool",
                                new ItemStack(tools.get((int) (millis / 1000 % tools.size())))
                                        .getHoverName())
                        .getString(),
                0x5555FF);
        for (CountIngredient material : cost) {
            ItemStack display = material.extractForCyclingDisplay(millis, 20);
            if (display.isEmpty()) continue;
            info.line(
                    Component.translatable(
                                    "desc.tooling.material",
                                    display.getHoverName(),
                                    display.getCount())
                            .getString());
        }
    }
}
