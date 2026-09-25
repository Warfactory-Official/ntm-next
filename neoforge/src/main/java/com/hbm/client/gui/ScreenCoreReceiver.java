// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCoreReceiver;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityCoreReceiver;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCoreReceiver extends ScreenInfoContainer<MenuCoreReceiver> {

    private static final Identifier TEXTURE = Library.id("textures/gui/dfc/gui_receiver.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17, TANK_X = 8;
    private static final int TEXT_COLOR = 0xFFFF7F7F;

    public ScreenCoreReceiver(MenuCoreReceiver menu, Inventory inventory, Component title) {
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
        BlockEntityCoreReceiver be = menu.blockEntity();

        drawFluidBar(graphics, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        graphics.text(
                font,
                Component.translatable("desc.gui.coreReceiver.input"),
                40,
                25,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.literal(BobMathUtil.getShortNumber(be.joules) + "Spk"),
                50,
                35,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.translatable("desc.gui.coreReceiver.output"),
                40,
                45,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.literal(
                        BobMathUtil.getShortNumber(be.joules * BlockEntityCoreReceiver.HE_PER_SPK)
                                + "HE"),
                50,
                55,
                TEXT_COLOR,
                false);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
