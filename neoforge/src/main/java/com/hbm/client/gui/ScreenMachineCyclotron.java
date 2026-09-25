// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCyclotron;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineCyclotron;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineCyclotron extends ScreenInfoContainer<MenuMachineCyclotron> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_cyclotron.png");

    private static final int POWER_X = 168, POWER_Y = 18, POWER_W = 16, POWER_H = 63;
    private static final int ARROW_X = 48, ARROW_Y = 27, ARROW_W = 34, ARROW_H = 34;
    private static final int WATER_X = 11, WATER_Y = 81, STEAM_Y = 90, TANK_W = 34, TANK_H = 7;
    private static final int AMAT_X = 107, AMAT_Y = 81, AMAT_W = 34, AMAT_H = 16;
    private static final int PANEL_X = 49, PANEL_Y = 85, PANEL_W = 8, PANEL_H = 8;

    public ScreenMachineCyclotron(MenuMachineCyclotron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 190, 215);
        this.inventoryLabelX = 15;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 79;
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

        BlockEntityMachineCyclotron be = menu.blockEntity();

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    80 - p,
                    190.0F,
                    62.0F - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        int arrow = menu.getProgressScaled(ARROW_W);
        if (arrow > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ARROW_X,
                    ARROW_Y,
                    206.0F,
                    0.0F,
                    arrow,
                    ARROW_H,
                    256,
                    256);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 172, 4, 190.0F, 63.0F, 9, 12, 256, 256);
        }

        drawFluidBarH(graphics, WATER_X, WATER_Y, TANK_W, TANK_H, be.water);
        drawFluidBarH(graphics, WATER_X, STEAM_Y, TANK_W, TANK_H, be.steam);
        drawFluidBarH(graphics, AMAT_X, AMAT_Y, AMAT_W, AMAT_H, be.amat);

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 8);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                upgradeInfo(be, UpgradeType.SPEED, UpgradeType.POWER, UpgradeType.EFFECT));

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                BlockEntityMachineCyclotron.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, WATER_X, WATER_Y, TANK_W, TANK_H, be.water);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, WATER_X, STEAM_Y, TANK_W, TANK_H, be.steam);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, AMAT_X, AMAT_Y, AMAT_W, AMAT_H, be.amat);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
