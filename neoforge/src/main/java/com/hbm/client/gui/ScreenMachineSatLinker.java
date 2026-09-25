// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineSatLinker;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineSatLinker extends ScreenInfoContainer<MenuMachineSatLinker> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_sat_linker.png");

    private static final int PANEL_X = 12, PANEL_W = 16, PANEL_H = 16;
    private static final int COPY_Y = 28, RANDOMIZE_Y = 44;
    private static final int TOOLTIP_X = 20;

    public ScreenMachineSatLinker(MenuMachineSatLinker menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        drawInfoPanel(graphics, PANEL_X, COPY_Y, PANEL_W, PANEL_H, 2);
        drawInfoPanel(graphics, PANEL_X, RANDOMIZE_Y, PANEL_W, PANEL_H, 3);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                COPY_Y,
                PANEL_W,
                PANEL_H,
                TOOLTIP_X,
                COPY_Y + PANEL_H,
                lineArray("desc.gui.satlinker.chip"));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                RANDOMIZE_Y,
                PANEL_W,
                PANEL_H,
                TOOLTIP_X,
                RANDOMIZE_Y + PANEL_H,
                lineArray("desc.gui.satlinker.random"));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
