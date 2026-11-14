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

import com.hbm.blockentity.machine.storage.CrateType;
import com.hbm.inventory.container.CrateMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class CrateScreen extends AbstractContainerScreen<CrateMenu> {

    private final CrateType type;

    public CrateScreen(CrateMenu menu, Inventory playerInventory, Component title) {
        super(
                menu,
                playerInventory,
                title,
                menu.getCrateType().guiWidth,
                menu.getCrateType().guiHeight);
        this.type = menu.getCrateType();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                type.texture,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        graphics.text(
                font,
                title,
                (imageWidth - font.width(title)) / 2,
                titleLabelY,
                ARGB.opaque(type.titleColor),
                false);
        graphics.text(
                font,
                playerInventoryTitle,
                type.inventoryLabelX,
                imageHeight - 96 + 2,
                ARGB.opaque(type.inventoryLabelColor),
                false);
    }
}
