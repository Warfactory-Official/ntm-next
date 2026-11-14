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

import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.MaterialTooltip;
import com.hbm.inventory.material.SmeltingBehavior;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

record MoltenFluidBar(
        Identifier texture, int u, int additiveU, int width, int height, int vBottom) {

    private static final int SHEEN = 0x4CFFFFFF;

    void draw(
            GuiGraphicsExtractor graphics,
            List<MaterialStack> stacks,
            int capacity,
            int x,
            int bottom) {
        int filled = 0;
        int edge = 0;
        for (MaterialStack stack : stacks) {
            filled += stack.amount;
            int top = filled * height / capacity;
            if (top != edge) band(graphics, stack.material, x, bottom, edge, top);
            edge = top;
        }
    }

    private void band(
            GuiGraphicsExtractor graphics, Material material, int x, int bottom, int from, int to) {
        int sheet = material.behaviour() == SmeltingBehavior.ADDITIVE ? additiveU : u;
        int y = bottom - to;
        int v = vBottom - to;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                sheet,
                v,
                width,
                to - from,
                256,
                256,
                ARGB.opaque(material.moltenColor()));
        graphics.blit(
                HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                texture,
                x,
                y,
                sheet,
                v,
                width,
                to - from,
                256,
                256,
                SHEEN);
    }

    static List<Component> describe(List<MaterialStack> stacks) {
        List<Component> lines = new ArrayList<>(Math.max(stacks.size(), 1));
        if (stacks.isEmpty())
            lines.add(Component.translatable("desc.shared.empty").withStyle(ChatFormatting.RED));
        boolean millibuckets = ModifierKeys.detailsHeld();
        for (MaterialStack stack : stacks) {
            lines.add(
                    Component.empty()
                            .append(stack.material.displayName())
                            .append(Component.literal(": "))
                            .append(MaterialTooltip.of(stack.amount, millibuckets))
                            .withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }
}
