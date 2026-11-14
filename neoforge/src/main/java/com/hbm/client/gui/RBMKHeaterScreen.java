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

import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.inventory.container.RBMKHeaterMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RBMKHeaterScreen extends InfoContainerScreen<RBMKHeaterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_heater.png");

    public RBMKHeaterScreen(RBMKHeaterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        RBMKHeaterBlockEntity be = be();
        if (be != null) {
            drawFluidBar(graphics, 68, 24, 14, 58, be.feed);
            drawFluidBar(graphics, 126, 24, 14, 58, be.steam);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 72, 72, 176, 0, 10, 10, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 130, 72, 186, 0, 10, 10, 256, 256);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 68, 24, 16, 58, be.feed);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 126, 24, 16, 58, be.steam);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private RBMKHeaterBlockEntity be() {
        return menu.blockEntity();
    }
}
