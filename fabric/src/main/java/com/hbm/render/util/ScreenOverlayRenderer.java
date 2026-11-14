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

package com.hbm.render.util;

import com.hbm.config.ModeStatus;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.impl.GunStingerItem;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class ScreenOverlayRenderer {

    public static final Identifier misc = RegistryUtil.id("textures/misc/overlay_misc.png");

    public static void renderBadges(GuiGraphicsExtractor graphics) {
        int flags = ModeStatus.clientBadges;
        int x = 2;
        for (int badge = 0; badge < 4; badge++) {
            if ((flags & (1 << badge)) == 0) continue;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, misc, x, 2, 0, 218 + badge * 8, 24, 8, 256, 256);
            x += 26;
        }
    }

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

        int progress = (int) (GunStingerItem.lockon * 28);

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
