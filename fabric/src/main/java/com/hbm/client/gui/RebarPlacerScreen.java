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

import com.hbm.inventory.container.RebarPlacerMenu;
import com.hbm.items.tool.RebarPlacerItem;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RebarPlacerScreen extends InfoContainerScreen<RebarPlacerMenu> {

    private static final Identifier TEXTURE = RegistryUtil.id("textures/gui/gui_rebar.png");

    private final List<ItemStack> concrete =
            RebarPlacerItem.acceptableConcrete().stream().map(ItemStack::new).toList();

    public RebarPlacerScreen(RebarPlacerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 182);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
        if (!RebarPlacerItem.isValidConcrete(menu.pattern().getItem())) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 87,
                    topPos + 17,
                    176.0F,
                    0.0F,
                    56,
                    56,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Slot pattern = menu.pattern();
        if (!isHovering(pattern.x, pattern.y, 16, 16, mouseX, mouseY) || pattern.hasItem()) return;

        GUIElements.drawCyclingStackText(
                graphics, font, null, concrete, mouseX, mouseY, width, height);
    }
}
