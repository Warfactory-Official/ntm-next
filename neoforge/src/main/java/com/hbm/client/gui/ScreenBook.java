// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuBook;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ScreenBook extends ScreenInfoContainer<MenuBook> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_book.png");
    private static final int TEXT_COLOR = -12566464;

    public ScreenBook(MenuBook menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
        if (menu.hasResult()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 29, 16, 176, 0, 54, 54, 256, 256);
        }

        graphics.text(
                font,
                Component.translatable("desc.gui.book.extended4SlotCrafting"),
                28,
                6,
                TEXT_COLOR,
                false);
        graphics.text(
                font,
                Component.translatable("desc.gui.book.standardInventory"),
                8,
                imageHeight - 96 + 2,
                TEXT_COLOR,
                false);
    }
}
