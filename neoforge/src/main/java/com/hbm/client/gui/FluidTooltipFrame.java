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

package com.hbm.client.gui;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public record FluidTooltipFrame(int top, int bottom) implements TooltipComponent {
    private static final int NONE_COLOR = 0x888888;

    public static FluidTooltipFrame of(@Nullable Fluid fluid) {
        NTMFluidProperty prop = NTMFluidProperties.get(fluid);
        int color = prop != null ? prop.color() : NONE_COLOR;
        int r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;
        int add = (r + g + b) / 3 > 0x80 ? -0x40 : 0x40;
        int shade =
                Mth.clamp(r + add, 0, 255) << 16
                        | Mth.clamp(g + add, 0, 255) << 8
                        | Mth.clamp(b + add, 0, 255);
        return new FluidTooltipFrame(0xFF000000 | color, 0xFF000000 | shade);
    }

    public static final class Renderer implements ClientTooltipComponent {
        private static final int GAP = 4;
        private final FluidTooltipFrame frame;

        public Renderer(FluidTooltipFrame frame) {
            this.frame = frame;
        }

        @Override
        public int getHeight(Font font) {
            return GAP;
        }

        @Override
        public int getWidth(Font font) {
            return 0;
        }

        @Override
        public void extractImage(
                Font font, int x, int localY, int w, int h, GuiGraphicsExtractor graphics) {
            int y = localY - 12;
            graphics.fillGradient(x - 3, y - 2, x - 2, y + h + 2, frame.top, frame.bottom);
            graphics.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, frame.top, frame.bottom);
            graphics.fill(x - 3, y - 3, x + w + 3, y - 2, frame.top);
            graphics.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, frame.bottom);
        }
    }
}
