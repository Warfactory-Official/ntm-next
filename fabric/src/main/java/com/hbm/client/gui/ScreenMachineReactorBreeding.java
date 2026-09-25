// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineReactorBreeding;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineReactorBreeding;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineReactorBreeding extends ScreenInfoContainer<MenuMachineReactorBreeding> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_breeder.png");

    private static final int ARROW_X = 53, ARROW_Y = 32, ARROW_W = 70, ARROW_H = 20;
    private static final int PANEL_X = -16, PANEL_Y = 16, PANEL_W = 16, PANEL_H = 16;

    public ScreenMachineReactorBreeding(
            MenuMachineReactorBreeding menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelY = 6;
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

        BlockEntityMachineReactorBreeding be = menu.blockEntity();

        int progress = (int) (be.progress * ARROW_W);
        if (progress > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    176.0F,
                    0.0F,
                    progress,
                    ARROW_H,
                    256,
                    256);
        }

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 3);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                PANEL_X + 8,
                PANEL_Y + 32,
                List.of(
                        Component.translatable("desc.gui.machineReactorBreeding.theReactorHasTo"),
                        Component.translatable(
                                "desc.gui.machineReactorBreeding.neutronFluxFromAdjacent"),
                        Component.translatable(
                                "desc.gui.machineReactorBreeding.researchReactorsToBreed")));

        String flux = String.valueOf(menu.getFlux());
        graphics.text(this.font, flux, 88 - this.font.width(flux) / 2, 21, 0xFF08FF00, false);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
