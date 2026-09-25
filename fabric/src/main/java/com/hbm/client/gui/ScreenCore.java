// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCore;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityCore;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCore extends ScreenInfoContainer<MenuCore> {

    private static final Identifier TEXTURE = Library.id("textures/gui/dfc/gui_core.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17;
    private static final int FIELD_X = 8, TANK_A_X = 26, TANK_B_X = 134, HEAT_X = 152;

    public ScreenCore(MenuCore menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCore be = menu.blockEntity();

        int field = be.getFieldScaled(GAUGE_H);
        if (field > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    FIELD_X,
                    GAUGE_Y + (GAUGE_H - field),
                    176,
                    GAUGE_H - field,
                    GAUGE_W,
                    field,
                    256,
                    256);
        }
        int heat = be.getHeatScaled(GAUGE_H);
        if (heat > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    GAUGE_Y + (GAUGE_H - heat),
                    192,
                    GAUGE_H - heat,
                    GAUGE_W,
                    heat,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_A_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, TANK_B_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tanks[1]);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                FIELD_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                List.of(Component.translatable("desc.gui.dfc.field", be.field)));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEAT_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                List.of(Component.translatable("desc.gui.dfc.heat", be.heat)));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
