// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCoreInjector;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityCoreInjector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCoreInjector extends ScreenInfoContainer<MenuCoreInjector> {

    private static final Identifier TEXTURE = Library.id("textures/gui/dfc/gui_injector.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17;
    private static final int TANK_A_X = 44, TANK_B_X = 116;

    public ScreenCoreInjector(MenuCoreInjector menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCoreInjector be = menu.blockEntity();

        drawFluidBar(graphics, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
