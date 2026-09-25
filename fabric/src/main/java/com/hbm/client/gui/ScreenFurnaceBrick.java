// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFurnaceBrick;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityFurnaceBrick;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFurnaceBrick extends ScreenInfoContainer<MenuFurnaceBrick> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_furnace_brick.png");

    public ScreenFurnaceBrick(MenuFurnaceBrick menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        BlockEntityFurnaceBrick be = menu.blockEntity();

        if (be.burnTime > 0) {
            int b = be.burnTime * 13 / Math.max(be.maxBurnTime, 1);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    54 + 12 - b,
                    176,
                    12 - b,
                    14,
                    b + 1,
                    256,
                    256);
            int p = be.progress * 24 / BlockEntityFurnaceBrick.SMELT_TIME;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 85, 34, 176, 14, p + 1, 16, 256, 256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
