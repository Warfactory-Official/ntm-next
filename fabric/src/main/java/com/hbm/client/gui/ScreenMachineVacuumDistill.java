// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineVacuumDistill;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityMachineVacuumDistill;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineVacuumDistill extends ScreenInfoContainer<MenuMachineVacuumDistill> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_vacuum_distill.png");
    private static final int TEX_W = 256, TEX_H = 256;
    private static final int POWER_X = 26, BAR_TOP = 18, BAR_W = 16, BAR_H = 52;
    private static final int[] TANK_X = {44, 80, 98, 116, 134};

    public ScreenMachineVacuumDistill(
            MenuMachineVacuumDistill menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);

        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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
                TEX_W,
                TEX_H);

        BlockEntityMachineVacuumDistill be = distill();
        long power = menu.getPower();

        int j = (int) (power * 54L / BlockEntityMachineVacuumDistill.MAX_POWER);
        if (j > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    BAR_TOP + (52 - j),
                    176,
                    52 - j,
                    BAR_W,
                    j,
                    TEX_W,
                    TEX_H);
        }

        for (int i = 0; i < 5; i++)
            drawFluidBar(graphics, TANK_X[i], BAR_TOP, BAR_W, BAR_H, be.tanks[i]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                power,
                BlockEntityMachineVacuumDistill.MAX_POWER);
        for (int i = 0; i < 5; i++)
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, TANK_X[i], BAR_TOP, BAR_W, BAR_H, be.tanks[i]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineVacuumDistill distill() {
        return menu.blockEntity();
    }
}
