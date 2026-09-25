// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRadiolysis;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineRadiolysis;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRadiolysis extends ScreenInfoContainer<MenuMachineRadiolysis> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_radiolysis.png");

    private static final int POWER_X = 8, POWER_Y = 17, POWER_W = 16, POWER_H = 34, POWER_U = 240;
    private static final int IN_X = 61, IN_Y = 17, IN_W = 8, IN_H = 52;
    private static final int OUT_X = 87, OUT_W = 12, OUT_H = 16;
    private static final int OUT1_Y = 17, OUT2_Y = 53;
    private static final int INFO_X = -16, INFO_Y = 16, INFO_STEP = 18, INFO_SIZE = 16;
    private static final String PELLET_KEY = "desc.gui.rtg.pelletPower";

    public ScreenMachineRadiolysis(
            MenuMachineRadiolysis menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 166);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 88;
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

        FluidTankNTM[] tanks = menu.blockEntity().tanks;

        long power = menu.getPower();
        int p = (int) (power * POWER_H / BlockEntityMachineRadiolysis.MAX_POWER);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    POWER_U,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, IN_X, IN_Y, IN_W, IN_H, tanks[0]);
        drawFluidBar(graphics, OUT_X, OUT1_Y, OUT_W, OUT_H, tanks[1]);
        drawFluidBar(graphics, OUT_X, OUT2_Y, OUT_W, OUT_H, tanks[2]);

        drawInfoPanel(graphics, INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE, 10);
        drawInfoPanel(graphics, INFO_X, INFO_Y + INFO_STEP, INFO_SIZE, INFO_SIZE, 2);
        drawInfoPanel(graphics, INFO_X, INFO_Y + INFO_STEP * 2, INFO_SIZE, INFO_SIZE, 3);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, IN_X, IN_Y, IN_W, IN_H, tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, OUT_X, OUT1_Y, OUT_W, OUT_H, tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, OUT_X, OUT2_Y, OUT_W, OUT_H, tanks[2]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                power,
                BlockEntityMachineRadiolysis.MAX_POWER);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_SIZE,
                lineArray("desc.gui.radiolysis.desc"));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y + INFO_STEP,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_STEP + INFO_SIZE,
                List.of(
                        Component.literal(
                                I18nUtil.resolveKey("desc.gui.rtg.heat", menu.getHeat()))));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y + INFO_STEP * 2,
                INFO_SIZE,
                INFO_SIZE,
                INFO_X + 8,
                INFO_Y + INFO_STEP * 2 + INFO_SIZE,
                pelletLines(PELLET_KEY, BlockEntityMachineRadiolysis.POWER_PER_HEAT));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
