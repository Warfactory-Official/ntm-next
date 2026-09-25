// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuStorageDrum;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityStorageDrum;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenStorageDrum extends ScreenInfoContainer<MenuStorageDrum> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_drum.png");

    private static final int BAR_HEIGHT = 106;
    private static final int BAR_WIDTH = 7;
    private static final int BAR_BOTTOM = 130;

    public ScreenStorageDrum(MenuStorageDrum menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 234);
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

        BlockEntityStorageDrum be = menu.blockEntity();

        drawBar(graphics, be.tanks[0].getFill(), be.tanks[0].getMaxFill(), 17, 176);
        drawBar(graphics, be.tanks[1].getFill(), be.tanks[1].getMaxFill(), 152, 183);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 16, 23, 9, 108, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 151, 23, 9, 108, be.tanks[1]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private static void drawBar(GuiGraphicsExtractor graphics, int fill, int max, int x, int u) {
        int height = fill * BAR_HEIGHT / max;
        if (height <= 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                BAR_BOTTOM - height,
                u,
                BAR_HEIGHT - height,
                BAR_WIDTH,
                height,
                256,
                256);
    }
}
