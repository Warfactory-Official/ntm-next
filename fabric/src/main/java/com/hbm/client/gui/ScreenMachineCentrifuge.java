// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuMachineCentrifuge;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineCentrifuge extends ScreenInfoContainer<MenuMachineCentrifuge> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_centrifuge.png");

    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 37;
    private static final int PROGRESS_TOTAL = 145, BAR_W = 12, BAR_MAX_H = 36;
    private static final int BAR_X0 = 72, BAR_Y_BOTTOM = 57, BAR_STRIDE = 20;

    public ScreenMachineCentrifuge(
            MenuMachineCentrifuge menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 182, 189);
        this.inventoryLabelX = 11;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return imageWidth / 2 + 18;
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
                256,
                256);

        long power = menu.getPower();
        if (power > 0) {
            int filled = (int) (power * POWER_H / MachineData.CENTRIFUGE_MAX_POWER.get());
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        182,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
        }

        int progress = menu.getProgress();
        if (progress > 0) {
            int p = progress * PROGRESS_TOTAL / MachineData.CENTRIFUGE_PROCESS_TIME.get();
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, BAR_MAX_H);
                if (h <= 0) break;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        BAR_X0 + i * BAR_STRIDE,
                        BAR_Y_BOTTOM - h,
                        182,
                        73 - h,
                        BAR_W,
                        h,
                        256,
                        256);
                p -= h;
                if (p <= 0) break;
            }
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
                MachineData.CENTRIFUGE_MAX_POWER.get());
        drawInfoPanel(graphics, 160, 16, 8, 8, 8);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                160,
                16,
                8,
                8,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.power",
                        "desc.gui.upgrade.overdrive"));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
