// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuTrainCargoTram;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenTrainCargoTram extends ScreenInfoContainer<MenuTrainCargoTram> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/vehicles/gui_cargo_tram.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int POWER_BOTTOM = 70, POWER_U = 176, POWER_V_BOTTOM = 52;
    private static final int LIT_X = 156, LIT_Y = 4;

    public ScreenTrainCargoTram(MenuTrainCargoTram menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
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

        int fill = menu.getPower() * POWER_H / menu.getMaxPower();
        if (fill > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - fill,
                    POWER_U,
                    POWER_V_BOTTOM - fill,
                    POWER_W,
                    fill,
                    256,
                    256);
        }

        if (menu.getPower() > menu.getPowerConsumption()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LIT_X,
                    LIT_Y,
                    POWER_U,
                    POWER_V_BOTTOM,
                    9,
                    12,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                menu.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
