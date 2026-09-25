// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKHeater;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKHeater extends ScreenInfoContainer<MenuRBMKHeater> {

    private static final Identifier TEXTURE = Library.id("textures/gui/rbmk/gui_rbmk_heater.png");

    public ScreenRBMKHeater(MenuRBMKHeater menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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

        BlockEntityRBMKHeater be = be();
        if (be != null) {

            drawFluidBar(graphics, 68, 24, 14, 58, be.feed);
            drawFluidBar(graphics, 126, 24, 14, 58, be.steam);

            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 72, 72, 176, 0, 10, 10, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 130, 72, 186, 0, 10, 10, 256, 256);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 68, 24, 16, 58, be.feed);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 126, 24, 16, 58, be.steam);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityRBMKHeater be() {
        return menu.blockEntity();
    }
}
