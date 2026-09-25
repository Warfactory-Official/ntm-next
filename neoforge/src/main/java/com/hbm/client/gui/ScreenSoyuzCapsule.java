// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuSoyuzCapsule;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenSoyuzCapsule extends AbstractContainerScreen<MenuSoyuzCapsule> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_soyuz_capsule.png");

    public ScreenSoyuzCapsule(MenuSoyuzCapsule menu, Inventory playerInventory, Component title) {
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

        graphics.text(font, title, 115 - font.width(title) / 2, 6, 0xFF7DAF71, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }
}
