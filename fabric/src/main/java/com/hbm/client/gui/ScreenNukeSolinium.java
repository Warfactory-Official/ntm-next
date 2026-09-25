// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeSolinium;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class ScreenNukeSolinium extends ScreenInfoContainer<MenuNukeSolinium> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/soliniumschematic.png");

    private static final int[][] PARTS = {
        {24, 84, 0, 222, 22, 14},
        {46, 84, 22, 222, 18, 14},
        {76, 84, 52, 222, 18, 14},
        {94, 84, 70, 222, 22, 14},
        {64, 84, 40, 222, 12, 28},
        {24, 98, 0, 236, 22, 14},
        {46, 98, 22, 236, 18, 14},
        {76, 98, 52, 236, 18, 14},
        {94, 98, 70, 236, 22, 14}
    };

    public ScreenNukeSolinium(MenuNukeSolinium menu, Inventory playerInventory, Component title) {
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

        for (int slot = 0; slot < PARTS.length; slot++) {
            Item item =
                    switch (slot) {
                        case 0, 3, 5, 8 -> ModItems.SOLINIUM_IGNITER.get();
                        case 4 -> ModItems.SOLINIUM_CORE.get();
                        default -> ModItems.SOLINIUM_PROPELLANT.get();
                    };
            if (!menu.part(slot).is(item)) continue;
            int[] r = PARTS[slot];
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    r[0],
                    r[1],
                    r[2],
                    r[3],
                    r[4],
                    r[5],
                    256,
                    256);
        }

        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 134, 90, 176.0F, 0.0F, 16, 16, 256, 256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
