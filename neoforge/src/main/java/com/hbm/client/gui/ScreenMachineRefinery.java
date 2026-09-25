// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRefinery;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipe;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityMachineRefinery;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineRefinery extends ScreenInfoContainer<MenuMachineRefinery> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_refinery.png");
    private static final int TEX_W = 256, TEX_H = 256;
    private static final int POWER_X = 158, POWER_H = 88;
    private static final int IN_X = 12, IN_Y = 18, IN_W = 16, IN_H = 70;
    private static final int IN_INFO_X = 12, IN_INFO_Y = 17, IN_INFO_W = 16, IN_INFO_H = 70;
    private static final int OUT_TOP = 36, OUT_INFO_TOP = 35, OUT_W = 16, OUT_H = 52;
    private static final int[] OUT_X = {64, 82, 100, 118};

    private static final int[][] PIPES = {
        {30, 30, 0, 248, 43, 4},
        {30, 26, 0, 240, 61, 8},
        {30, 22, 61, 240, 79, 12},
        {30, 18, 140, 240, 97, 16},
    };

    public ScreenMachineRefinery(MenuMachineRefinery menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 182, 240);
        this.inventoryLabelX = 11;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static int fluidColor(Fluid type) {
        if (type == null) return CommonColors.WHITE;
        NTMFluidProperty prop = NTMFluidProperties.get(type);
        return prop != null ? prop.colorARGB() : CommonColors.WHITE;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 18;
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
                TEX_W,
                TEX_H);

        BlockEntityMachineRefinery be = refinery();
        long power = menu.getPower();
        int filled =
                (int) Math.min(POWER_H, power * POWER_H / BlockEntityMachineRefinery.MAX_POWER);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    106 - filled,
                    182,
                    88 - filled,
                    16,
                    filled,
                    TEX_W,
                    TEX_H);
        }

        FluidTankNTM inputTank = be.tanks[0];
        if (inputTank.getFill() != 0) {
            drawFluidBar(graphics, IN_X, IN_Y, IN_W, IN_H, inputTank);
        }

        RefineryRecipe recipe = RefineryRecipes.INSTANCE.getRefinery(inputTank.getTankType());
        for (int i = 0; i < PIPES.length; i++) {
            int[] p = PIPES[i];
            int tint =
                    recipe == null ? CommonColors.WHITE : fluidColor(recipe.outputFluid[i].type());
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    p[0],
                    p[1],
                    p[2],
                    p[3],
                    p[4],
                    p[5],
                    TEX_W,
                    TEX_H,
                    tint);
        }

        for (int i = 0; i < 4; i++)
            drawFluidBar(graphics, OUT_X[i], OUT_TOP, OUT_W, OUT_H, be.tanks[i + 1]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                18,
                16,
                POWER_H,
                power,
                BlockEntityMachineRefinery.MAX_POWER);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, IN_INFO_X, IN_INFO_Y, IN_INFO_W, IN_INFO_H, inputTank);
        for (int i = 0; i < 4; i++)
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_X[i],
                    OUT_INFO_TOP,
                    OUT_W,
                    OUT_H,
                    be.tanks[i + 1]);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineRefinery refinery() {
        return menu.blockEntity();
    }
}
