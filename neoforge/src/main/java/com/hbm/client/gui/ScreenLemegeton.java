// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuLemegeton;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ScreenLemegeton extends ScreenInfoContainer<MenuLemegeton> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_lemegeton.png");
    private static final FontDescription FONT =
            new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));

    public ScreenLemegeton(MenuLemegeton menu, Inventory inventory, Component title) {
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
        if (menu.hasResult())
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 22, 0, 166, 162, 42, 256, 256);
        graphics.text(
                font,
                Component.translatable("desc.gui.lemegeton.title")
                        .withStyle(style -> style.withFont(FONT)),
                28,
                6,
                0xFF404040,
                false);
        graphics.text(
                font,
                Component.translatable("desc.gui.book.standardInventory")
                        .withStyle(style -> style.withFont(FONT)),
                8,
                72,
                0xFF404040,
                false);
    }
}
