// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineTurbofan;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineTurbofan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineTurbofan extends ScreenInfoContainer<MenuMachineTurbofan> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/generators/gui_turbofan.png");

    private static final int POWER_X = 143, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 34, TANK_H = 52;
    private static final int BLOOD_X = 98, BLOOD_Y = 17, BLOOD_W = 16, BLOOD_H = 16;
    private static final int GAUGE_X = 97, GAUGE_Y = 16;
    private static final int AFTERBURNER_X = 98, AFTERBURNER_Y = 44, AFTERBURNER_SIZE = 16;
    private static final int AFTERBURNER_FRAMES = 6;

    public ScreenMachineTurbofan(MenuMachineTurbofan menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 43;
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

        BlockEntityMachineTurbofan be = menu.blockEntity();

        int p = (int) (be.power * POWER_H / BlockEntityMachineTurbofan.MAX_POWER);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    192,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        if (be.afterburner > 0) {
            int frame = Math.min(be.afterburner, AFTERBURNER_FRAMES);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    AFTERBURNER_X,
                    AFTERBURNER_Y,
                    176,
                    (frame - 1) * AFTERBURNER_SIZE,
                    AFTERBURNER_SIZE,
                    AFTERBURNER_SIZE,
                    256,
                    256);
        }

        if (be.showBlood) {
            drawRoundGauge(
                    graphics, GAUGE_X, GAUGE_Y, (float) be.blood.getFill() / be.blood.getMaxFill());
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        if (be.showBlood) {
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, BLOOD_X, BLOOD_Y, BLOOD_W, BLOOD_H, be.blood);
        }
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                BlockEntityMachineTurbofan.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
