// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineElectricFurnace;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineElectricFurnace;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineElectricFurnace extends ScreenInfoContainer<MenuMachineElectricFurnace> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_electric_furnace.png");

    private static final int POWER_X = 152,
            POWER_Y = 18,
            POWER_W = 16,
            POWER_H = 34,
            POWER_V_BOTTOM = 64;
    private static final int PROG_X = 43,
            PROG_Y = 36,
            PROG_W = 28,
            PROG_H = 12,
            PROG_U = 176,
            PROG_V = 0;
    private static final int FLAME_W = 18, FLAME_H = 16, FLAME_U = 192;
    private static final int FLAME1_X = 45, FLAME1_Y = 20, FLAME1_V = 12;
    private static final int FLAME2_X = 46, FLAME2_Y = 47, FLAME2_V = 28;
    private static final int INFO_X = 115, INFO_Y = 19, INFO_W = 8, INFO_H = 8;

    public ScreenMachineElectricFurnace(
            MenuMachineElectricFurnace menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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

        long power = menu.getPower();
        int filled = (int) (power * POWER_H / BlockEntityMachineElectricFurnace.MAX_POWER);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - filled),
                    176,
                    POWER_V_BOTTOM - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        int progress = menu.getProgress();
        if (progress > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    FLAME1_X,
                    FLAME1_Y,
                    FLAME_U,
                    FLAME1_V,
                    FLAME_W,
                    FLAME_H,
                    256,
                    256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    FLAME2_X,
                    FLAME2_Y,
                    FLAME_U,
                    FLAME2_V,
                    FLAME_W,
                    FLAME_H,
                    256,
                    256);
        }

        int maxProgress = menu.getMaxProgress();
        int p = maxProgress > 0 ? progress * PROG_W / maxProgress : 0;
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROG_X,
                    PROG_Y,
                    PROG_U,
                    PROG_V,
                    p,
                    PROG_H,
                    256,
                    256);
        }

        drawInfoPanel(graphics, INFO_X, INFO_Y, INFO_W, INFO_H, 8);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                power,
                BlockEntityMachineElectricFurnace.MAX_POWER);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_W,
                INFO_H,
                resolveLines(
                        "desc.gui.upgrade", "desc.gui.upgrade.speed", "desc.gui.upgrade.power"));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
