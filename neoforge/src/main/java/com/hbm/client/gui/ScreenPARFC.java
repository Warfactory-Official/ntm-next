// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPARFC;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.BlockEntityPARFC;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPARFC extends ScreenPACooled<MenuPARFC> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/particleaccelerator/gui_rfc.png");

    public ScreenPARFC(MenuPARFC menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPARFC be = menu.blockEntity();

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
        drawPowerBar(graphics, TEXTURE, 53, be.power, be.getMaxPower());

        drawCoolant(graphics, mouseX, mouseY, be, 89);
        drawElectricityInfo(graphics, mouseX, mouseY, 53, 18, 16, 52, be.power, be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
