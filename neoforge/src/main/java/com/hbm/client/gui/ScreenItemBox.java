// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuItemBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenItemBox extends AbstractContainerScreen<MenuItemBox> {

    public ScreenItemBox(MenuItemBox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.layout().width(), menu.layout().height());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                menu.layout().texture(),
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (menu.layout().titled()) {
            graphics.text(
                    font,
                    title,
                    (imageWidth - font.width(title)) / 2,
                    menu.layout().titleY(),
                    ARGB.opaque(menu.layout().titleColor()),
                    false);
        }
        graphics.text(
                font,
                playerInventoryTitle,
                8,
                menu.layout().inventoryLabelY(),
                ARGB.opaque(0x404040),
                false);
    }
}
