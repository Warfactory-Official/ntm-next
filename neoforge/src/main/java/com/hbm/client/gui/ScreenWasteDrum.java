// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuWasteDrum;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenWasteDrum extends ScreenInfoContainer<MenuWasteDrum> {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_waste_drum.png");

    public ScreenWasteDrum(MenuWasteDrum menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 189);
        this.inventoryLabelY = this.imageHeight - 96 + 5;
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
        drawInfoPanel(graphics, -16, 36, 16, 16, 2);
        drawCustomInfoStat(
                graphics, mouseX, mouseY, -16, 36, 16, 16, -8, 52, lineArray("desc.gui.wasteDrum"));
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
