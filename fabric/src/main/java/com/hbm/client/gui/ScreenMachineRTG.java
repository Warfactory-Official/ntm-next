// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRTG;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineRTG;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRTG extends ScreenInfoContainer<MenuMachineRTG> {

    private static final Identifier TEXTURE = Library.id("textures/gui/generators/gui_rtg.png");

    private static final int BAR_H = 51, BAR_W = 16, BAR_BOTTOM = 61, BAR_TOP = 9;
    private static final int HEAT_X = 124, HEAT_U = 176;
    private static final int POWER_X = 146, POWER_U = 192;
    private static final int BAR_V = 10;
    private static final int INFO_X = -12, INFO_Y = 25, INFO_W = 16, INFO_H = 16;
    private static final String PELLET_KEY = "desc.gui.rtg.pelletPower";

    public ScreenMachineRTG(MenuMachineRTG menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 188);
        this.titleLabelY = 7;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 60;
    }

    @Override
    protected int titleColor() {
        return 0xFFA6B5AE;
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

        if (menu.blockEntity().hasHeat()) {
            int i = menu.getHeatScaled(BAR_H);
            if (i > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        HEAT_X,
                        BAR_BOTTOM - i,
                        HEAT_U,
                        BAR_V + (BAR_H - i),
                        BAR_W,
                        i,
                        256,
                        256);
            }
        }

        long power = menu.getPower();
        if (power > 0) {
            int i = (int) menu.getPowerScaled(BAR_H);
            if (i > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        BAR_BOTTOM - i,
                        POWER_U,
                        BAR_V + (BAR_H - i),
                        BAR_W,
                        i,
                        256,
                        256);
            }
        }

        drawInfoPanel(graphics, INFO_X, INFO_Y, INFO_W, INFO_H, 2);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                power,
                BlockEntityMachineRTG.MAX_POWER);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEAT_X,
                BAR_TOP,
                BAR_W,
                BAR_H,
                List.of(
                        Component.literal(
                                I18nUtil.resolveKey("desc.gui.rtg.heat", menu.getHeat()))));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_W,
                INFO_H,
                INFO_X + 4,
                INFO_Y + 27,
                pelletLines(PELLET_KEY, BlockEntityMachineRTG.POWER_PER_HEAT));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
