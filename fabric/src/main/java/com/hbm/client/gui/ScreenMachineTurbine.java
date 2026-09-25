// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineTurbine;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineTurbine;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineTurbine extends ScreenInfoContainer<MenuMachineTurbine> {

    private static final Identifier TEXTURE = Library.id("textures/gui/generators/gui_turbine.png");

    private static final int TANK0_X = 62, TANK0_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int TANK1_X = 134, TANK1_Y = 17;
    private static final int POWER_X = 123, POWER_Y = 35, POWER_W = 7, POWER_H = 34;
    private static final int FLUID_ICON_X = 99,
            FLUID_ICON_Y = 18,
            FLUID_ICON_W = 14,
            FLUID_ICON_H = 14;
    private static final int ERROR_X = -16, ERROR_Y = 36 + 32, ICON_W = 16, ICON_H = 16;

    public ScreenMachineTurbine(MenuMachineTurbine menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static int fluidIconV(Fluid type) {
        if (type == NTMFluids.STEAM) return 0;
        if (type == NTMFluids.HOTSTEAM) return 14;
        if (type == NTMFluids.SUPERHOTSTEAM) return 28;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 42;
        return -1;
    }

    @Override
    protected int titleCenterX() {
        return 88;
    }

    @Override
    protected int titleColor() {
        return 0xFF404040;
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

        BlockEntityMachineTurbine be = turbine();

        Fluid type0 = be.tank0.getTankType();
        int iconV = fluidIconV(type0);
        if (iconV >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    FLUID_ICON_X,
                    FLUID_ICON_Y,
                    183,
                    iconV,
                    FLUID_ICON_W,
                    FLUID_ICON_H,
                    256,
                    256);
        }

        int p = (int) be.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    176,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK0_X, TANK0_Y, TANK_W, TANK_H, be.tank0);
        drawFluidBar(graphics, TANK1_X, TANK1_Y, TANK_W, TANK_H, be.tank1);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK0_Y, TANK_W, TANK_H, be.tank0);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK1_Y, TANK_W, TANK_H, be.tank1);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                be.getMaxPower());

        if (be.tank1.getTankType() == NTMFluids.NONE) {
            drawInfoPanel(graphics, ERROR_X, ERROR_Y, ICON_W, ICON_H, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    ERROR_X,
                    ERROR_Y,
                    ICON_W,
                    ICON_H,
                    List.of(Component.translatable("desc.shared.errorInvalidFluid")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineTurbine turbine() {
        return menu.blockEntity();
    }
}
