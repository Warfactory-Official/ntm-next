// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKOutgasser;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKOutgasser extends ScreenInfoContainer<MenuRBMKOutgasser> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/rbmk/gui_rbmk_outgasser.png");

    public ScreenRBMKOutgasser(MenuRBMKOutgasser menu, Inventory playerInventory, Component title) {
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

        BlockEntityRBMKOutgasser be = be();
        if (be != null) {
            int prog = be.duration > 0 ? (int) (be.progress * 13 / be.duration) : 0;
            if (prog > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 50, 176, 0, prog, 6, 256, 256);

            int gas = be.gas.getMaxFill() > 0 ? be.gas.getFill() * 42 / be.gas.getMaxFill() : 0;
            if (gas > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        115,
                        66 - gas,
                        188,
                        42 - gas,
                        10,
                        gas,
                        256,
                        256);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 112, 21, 16, 48, be.gas);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityRBMKOutgasser be() {
        return menu.blockEntity();
    }
}
