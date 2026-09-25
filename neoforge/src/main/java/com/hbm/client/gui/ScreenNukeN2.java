// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeN2;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenNukeN2 extends ScreenInfoContainer<MenuNukeN2> {

    private static final Identifier TEXTURE = Library.id("textures/gui/weapon/n2schematic.png");

    public ScreenNukeN2(MenuNukeN2 menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
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

        int count = 0;
        for (int slot = 0; slot < 12; slot++) {
            if (menu.part(slot).is(ModItems.N2_CHARGE.get())) count++;
        }

        if (count > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    35,
                    120 - 6 * count,
                    176.0F,
                    0.0F,
                    34,
                    6 * count,
                    256,
                    256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
