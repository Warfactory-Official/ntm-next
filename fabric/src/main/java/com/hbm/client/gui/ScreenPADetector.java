// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPADetector;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.BlockEntityPADetector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPADetector extends ScreenPACooled<MenuPADetector> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/particleaccelerator/gui_detector.png");

    public ScreenPADetector(MenuPADetector menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 8;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPADetector be = menu.blockEntity();

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
        drawPowerBar(graphics, TEXTURE, 8, be.power, be.getMaxPower());

        if (Math.ceil(be.temperature) <= COLD_ENOUGH) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 43, 18, 176, 8, 8, 8, 256, 256);
        }
        if (be.power >= BlockEntityPADetector.usage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 43, 43, 176, 8, 8, 8, 256, 256);
        }

        drawCoolant(graphics, mouseX, mouseY, be, 134);
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.power, be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
