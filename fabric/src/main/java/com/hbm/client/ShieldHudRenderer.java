// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.render.util.RenderScreenOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public final class ShieldHudRenderer {

    private ShieldHudRenderer() {}

    public static boolean render(GuiGraphicsExtractor graphics, int top) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer())
            return false;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        float maximum = props.getEffectiveMaxShield(player);
        if (maximum <= 0F) return false;
        int left = graphics.guiWidth() / 2 - 91;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RenderScreenOverlay.misc,
                left,
                top,
                146,
                0,
                81,
                9,
                256,
                256);
        int fill = (int) Math.ceil(props.shield * 79 / maximum);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RenderScreenOverlay.misc,
                left + 1,
                top,
                147,
                9,
                fill,
                9,
                256,
                256);
        String label = Double.toString(((int) (props.shield * 10F)) / 10D);
        int center = left + 40 - minecraft.font.width(label) / 2;
        graphics.text(minecraft.font, label, center + 1, top + 1, 0xFF000000, false);
        graphics.text(minecraft.font, label, center - 1, top + 1, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top + 2, 0xFF000000, false);
        graphics.text(minecraft.font, label, center, top + 1, 0xFFFFFF80, false);
        return true;
    }
}
