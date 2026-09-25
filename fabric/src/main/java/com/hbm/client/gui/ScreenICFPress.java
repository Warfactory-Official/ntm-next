// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuICFPress;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityICFPress;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenICFPress extends ScreenInfoContainer<MenuICFPress> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_icf_press.png");

    public ScreenICFPress(MenuICFPress menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 179);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityICFPress press = menu.blockEntity();

        int m = press.muon * 52 / BlockEntityICFPress.maxMuon;
        if (m > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 28, 70 - m, 176, 52 - m, 4, m, 256, 256);

        drawFluidBar(graphics, 44, 18, 16, 52, press.tanks[0]);
        drawFluidBar(graphics, 152, 18, 16, 52, press.tanks[1]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 44, 18, 16, 52, press.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 18, 16, 52, press.tanks[1]);

        if (!menu.getSlot(4).hasItem()) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    menu.getSlot(4).x,
                    menu.getSlot(4).y,
                    16,
                    16,
                    List.of(
                            Component.translatable("desc.gui.icfPress.itemInputTopBottom")
                                    .withStyle(ChatFormatting.YELLOW)));
        }
        if (!menu.getSlot(5).hasItem()) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    menu.getSlot(5).x,
                    menu.getSlot(5).y,
                    16,
                    16,
                    List.of(
                            Component.translatable("desc.gui.icfPress.itemInputSides")
                                    .withStyle(ChatFormatting.YELLOW)));
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
