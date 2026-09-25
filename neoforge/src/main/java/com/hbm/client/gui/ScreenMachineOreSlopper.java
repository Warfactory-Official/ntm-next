// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineOreSlopper;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineOreSlopper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineOreSlopper extends ScreenInfoContainer<MenuMachineOreSlopper> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_ore_slopper.png");

    public ScreenMachineOreSlopper(
            MenuMachineOreSlopper menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 9;
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

        BlockEntityMachineOreSlopper be = slopper();

        int i = (int) (be.progress * 35);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    52 - i,
                    176,
                    34 - i,
                    34,
                    i,
                    256,
                    256);

        int j = (int) (be.power * 52 / BlockEntityMachineOreSlopper.maxPower);
        if (j > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 70 - j, 176, 86 - j, 16, j, 256, 256);

        if (be.power >= be.consumption) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 12, 4, 202, 34, 9, 12, 256, 256);
        }

        drawFluidBar(graphics, 26, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[1]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                8,
                18,
                16,
                52,
                be.power,
                BlockEntityMachineOreSlopper.maxPower);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 26, 18, 34, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[1]);

        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 9;
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineOreSlopper slopper() {
        return menu.blockEntity();
    }
}
