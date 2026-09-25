// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineLiquefactor;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityMachineLiquefactor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineLiquefactor extends ScreenInfoContainer<MenuMachineLiquefactor> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_liquefactor.png");

    public ScreenMachineLiquefactor(
            MenuMachineLiquefactor menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
    }

    @Override
    protected int titleColor() {
        return 0xFFC7C1A3;
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

        BlockEntityMachineLiquefactor be = menu.blockEntity();

        int i = (int) (be.power * 52 / BlockEntityMachineLiquefactor.MAX_POWER);
        if (i > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    134,
                    70 - i,
                    176,
                    52 - i,
                    16,
                    i,
                    256,
                    256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 138, 4, 176, 52, 9, 12, 256, 256);
        }

        if (be.processTime > 0) {
            int j = be.progress * 42 / be.processTime;
            if (j > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 42, 17, 192, 0, j, 35, 256, 256);
        }

        drawFluidBar(graphics, 71, 36, 16, 52, be.tank);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 71, 36, 16, 52, be.tank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                134,
                18,
                16,
                52,
                be.power,
                BlockEntityMachineLiquefactor.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
