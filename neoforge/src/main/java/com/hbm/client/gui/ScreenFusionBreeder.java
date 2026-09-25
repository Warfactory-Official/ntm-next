// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFusionBreeder;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBreeder;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFusionBreeder extends ScreenInfoContainer<MenuFusionBreeder> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/reactors/gui_fusion_breeder.png");

    private static final int TANK_IN_X = 26,
            TANK_OUT_X = 134,
            TANK_Y = 18,
            TANK_W = 16,
            TANK_H = 52;
    private static final int FLUX_X = 79, FLUX_Y = 23, FLUX_W = 18, FLUX_H = 18;
    private static final int BAR_X = 67, BAR_Y = 46, BAR_W = 42, BAR_H = 14;

    public ScreenFusionBreeder(MenuFusionBreeder menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
        this.inventoryLabelX = 35;
        this.inventoryLabelY = this.imageHeight - 93;
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

        BlockEntityFusionBreeder be = menu.blockEntity();

        int p = (int) Math.ceil(be.progress * 42 / BlockEntityFusionBreeder.CAPACITY);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 67, 48, 176.0F, 0.0F, p, 10, 256, 256);
        }

        double gauge =
                1D - Math.pow(Math.E, -be.neutronEnergy * 10 / BlockEntityFusionBreeder.CAPACITY);
        SmoothGaugeElement.draw(graphics, 88, 32, gauge, 5, 2, 1, 0xA00000, 0x000000);

        drawFluidBar(graphics, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                FLUX_X,
                FLUX_Y,
                FLUX_W,
                FLUX_H,
                List.of(
                        Component.literal(
                                ChatFormatting.GREEN
                                        + "-> "
                                        + ChatFormatting.RESET
                                        + (int) Math.ceil(be.neutronEnergy)
                                        + " flux/t")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BAR_X,
                BAR_Y,
                BAR_W,
                BAR_H,
                List.of(
                        Component.literal(
                                BobMathUtil.format((int) Math.ceil(be.progress))
                                        + " / "
                                        + BobMathUtil.format(
                                                (int) Math.ceil(BlockEntityFusionBreeder.CAPACITY))
                                        + " flux")));

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_IN_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
