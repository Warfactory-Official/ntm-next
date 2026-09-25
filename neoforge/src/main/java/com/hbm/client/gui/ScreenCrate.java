// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCrate;
import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCrate extends AbstractContainerScreen<MenuCrate> {

    private final CrateType type;

    public ScreenCrate(MenuCrate menu, Inventory playerInventory, Component title) {
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
