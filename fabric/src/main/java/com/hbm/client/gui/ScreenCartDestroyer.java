// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCartDestroyer;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCartDestroyer extends ScreenInfoContainer<MenuCartDestroyer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/vehicles/gui_destroyer.png");

    public ScreenCartDestroyer(MenuCartDestroyer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 4;
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

        int index = (int) (Util.getMillis() % 1000 / 128);
        if (index == 1 || index == 7)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 166, 44, 16, 256, 256);
        if (index == 2 || index == 6)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 182, 44, 16, 256, 256);
        if (index == 3 || index == 5)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 198, 44, 16, 256, 256);
        if (index == 4)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 66, 35, 0, 214, 44, 16, 256, 256);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
