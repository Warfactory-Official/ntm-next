// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.gui.ScreenToolAbility;
import com.hbm.config.ConfigSchema;
import com.hbm.config.HudConfig;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ToolAbilityHudRenderer {

    private static final int SIZE = 16;

    private ToolAbilityHudRenderer() {}

    private static int textureU(ToolAreaAbility area) {
        return switch (area) {
            case RECURSION -> 0;
            case HAMMER -> 16;
            case HAMMER_FLAT -> 32;
            case EXPLOSION -> 48;
            case NONE -> -1;
        };
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemToolAbility tool)) return;

        ToolPreset preset = tool.activePreset(held);
        int u = textureU(preset.area());
        if (u == -1) return;

        int x = graphics.guiWidth() / 2 - SIZE - 8 + HudConfig.toolIndicatorX;
        int y = graphics.guiHeight() / 2 + 8 + HudConfig.toolIndicatorY;
        graphics.blit(
                RenderPipelines.CROSSHAIR,
                ScreenToolAbility.TEXTURE,
                x,
                y,
                u,
                138,
                SIZE,
                SIZE,
                256,
                256);
    }
}
