// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCrystallizer;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineCrystallizer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineCrystallizer extends ScreenInfoContainer<MenuMachineCrystallizer> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_crystallizer_alt.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 35, TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int PROGRESS_X = 80, PROGRESS_Y = 47;

    public ScreenMachineCrystallizer(
            MenuMachineCrystallizer menu, Inventory inventory, Component title) {
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

        long power = menu.getPower();
        int filled =
                (int) Math.min(POWER_H, power * POWER_H / BlockEntityMachineCrystallizer.MAX_POWER);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + POWER_H - filled,
                    176,
                    64 - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        BlockEntityMachineCrystallizer be = menu.blockEntity();
        if (be.duration > 0 && be.progress > 0) {
            int j = Math.min(28, be.progress * 28 / be.duration);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_X,
                    PROGRESS_Y,
                    176,
                    0,
                    j,
                    12,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawInfoPanel(graphics, 117, 22, 8, 8, 8);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                power,
                BlockEntityMachineCrystallizer.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                117,
                22,
                8,
                8,
                200,
                45,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.effectiveness",
                        "desc.gui.upgrade.overdrive"));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
