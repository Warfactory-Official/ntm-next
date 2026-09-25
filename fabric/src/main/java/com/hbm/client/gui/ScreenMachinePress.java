// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachinePress;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachinePress;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachinePress extends ScreenInfoContainer<MenuMachinePress> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_press.png");

    private static final int BAR_X = 79,
            BAR_Y = 35,
            BAR_U = 15,
            BAR_V = 214,
            BAR_W = 18,
            BAR_MAX_H = 16;
    private static final int BURN_X = 26,
            BURN_Y = 36,
            BURN_U = 0,
            BURN_V = 214,
            BURN_W = 14,
            BURN_H = 14;
    private static final int GAUGE_X = 34, GAUGE_Y = 25;
    private static final int SPEED_STAT_X = 25,
            SPEED_STAT_Y = 16,
            SPEED_STAT_W = 18,
            SPEED_STAT_H = 18;
    private static final int BURN_STAT_X = 25, BURN_STAT_Y = 34, BURN_STAT_W = 18, BURN_STAT_H = 18;

    public ScreenMachinePress(MenuMachinePress menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.titleLabelY = 5;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityMachinePress press = menu.blockEntity();

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

        if (press.burnTime >= 20) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BURN_X,
                    BURN_Y,
                    BURN_U,
                    BURN_V,
                    BURN_W,
                    BURN_H,
                    256,
                    256);
        }

        int h = (int) (press.renderPress * BAR_MAX_H / BlockEntityMachinePress.MAX_PROGRESS);
        if (h > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BAR_X,
                    BAR_Y,
                    BAR_U,
                    BAR_V,
                    BAR_W,
                    h,
                    256,
                    256);
        }

        SmoothGaugeElement.draw(
                graphics,
                GAUGE_X,
                GAUGE_Y,
                (double) press.speed / BlockEntityMachinePress.MAX_SPEED,
                5,
                2,
                1,
                0x7F0000,
                0x000000);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                SPEED_STAT_X,
                SPEED_STAT_Y,
                SPEED_STAT_W,
                SPEED_STAT_H,
                List.of(Component.literal(menu.getSpeedPercent() + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BURN_STAT_X,
                BURN_STAT_Y,
                BURN_STAT_W,
                BURN_STAT_H,
                List.of(Component.literal(menu.getOperationsLeft() + " operations left")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
