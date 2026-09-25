// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.util;

import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class RenderScreenOverlay {

    public static final Identifier misc = Library.id("textures/misc/overlay_misc.png");

    public static void renderCustomCrosshairs(GuiGraphicsExtractor graphics, Crosshair cross) {

        if (cross == Crosshair.NONE) return;

        int size = cross.size;
        graphics.blit(
                RenderPipelines.CROSSHAIR,
                misc,
                graphics.guiWidth() / 2 - (size / 2),
                graphics.guiHeight() / 2 - (size / 2),
                cross.x,
                cross.y,
                size,
                size,
                256,
                256);
    }

    public static void renderScope(GuiGraphicsExtractor graphics, Identifier texture) {
        double w = graphics.guiWidth();
        double h = graphics.guiHeight();
        double smallest = 9D / 16D;
        double largest = Math.max(w, h) / (Math.min(w, h) / smallest);
        double hHalf = (h < w ? smallest : largest) / 2D;
        double wHalf = (w < h ? smallest : largest) / 2D;
        graphics.blit(
                texture,
                0,
                0,
                graphics.guiWidth(),
                graphics.guiHeight(),
                (float) (0.5D - wHalf),
                (float) (0.5D + wHalf),
                (float) (0.5D - hHalf),
                (float) (0.5D + hHalf));
    }

    public static void renderStingerLockon(GuiGraphicsExtractor graphics) {

        int progress = (int) (ItemGunStinger.lockon * 28);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                misc,
                graphics.guiWidth() / 2 - 15,
                graphics.guiHeight() / 2 + 18,
                146,
                18,
                30,
                10,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                misc,
                graphics.guiWidth() / 2 - 14,
                graphics.guiHeight() / 2 + 19,
                147,
                29,
                progress,
                8,
                256,
                256);
    }
}
