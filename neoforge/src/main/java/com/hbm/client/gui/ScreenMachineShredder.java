// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineShredder;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineShredder;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineShredder extends ScreenInfoContainer<MenuMachineShredder> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_shredder.png");

    private static final int POWER_X = 8, POWER_H = 88, POWER_W = 16, POWER_BOTTOM = 106;
    private static final int POWER_U = 176, POWER_V_BOTTOM = 160;
    private static final int PROGRESS_X = 63, PROGRESS_Y = 89, PROGRESS_U = 176, PROGRESS_V = 54;
    private static final int PROGRESS_LENGTH = 34;
    private static final int GEAR_LEFT_X = 43, GEAR_RIGHT_X = 79, GEAR_Y = 71;
    private static final int GEAR_LEFT_U = 176, GEAR_RIGHT_U = 194;
    private static final int PANEL_X = -16, PANEL_Y = 36, PANEL_SIZE = 16;

    public ScreenMachineShredder(MenuMachineShredder menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 233);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 106;
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
        if (power > 0) {
            int filled = (int) menu.getPowerScaled(POWER_H);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - filled,
                    POWER_U,
                    POWER_V_BOTTOM - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        int progress = menu.getProgressScaled(PROGRESS_LENGTH);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                PROGRESS_X,
                PROGRESS_Y,
                PROGRESS_U,
                PROGRESS_V,
                progress + 1,
                18,
                256,
                256);

        int left = menu.getGearLeft();
        int right = menu.getGearRight();
        drawGear(graphics, GEAR_LEFT_X, GEAR_LEFT_U, left);
        drawGear(graphics, GEAR_RIGHT_X, GEAR_RIGHT_U, right);

        boolean stalled = left == 0 || left == 3 || right == 0 || right == 3;
        if (stalled) {
            drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    -8,
                    PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable(
                                    "desc.gui.machineShredder.errorShredderBladesAre")));
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_BOTTOM - POWER_H,
                POWER_W,
                POWER_H,
                power,
                BlockEntityMachineShredder.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawGear(GuiGraphicsExtractor graphics, int x, int u, int gear) {
        if (gear == 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                GEAR_Y,
                u,
                (gear - 1) * 18,
                18,
                18,
                256,
                256);
    }
}
