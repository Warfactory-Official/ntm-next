// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeFleija;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class ScreenNukeFleija extends ScreenInfoContainer<MenuNukeFleija> {

    private static final Identifier TEXTURE = Library.id("textures/gui/weapon/fleijaschematic.png");

    private static final int[][] PARTS = {
        {7, 88, 176, 0, 30, 20},
        {139, 88, 206, 0, 30, 20},
        {57, 77, 176, 62, 18, 14},
        {57, 91, 176, 76, 18, 14},
        {57, 105, 176, 90, 18, 14},
        {85, 77, 176, 20, 18, 15},
        {103, 77, 194, 20, 18, 15},
        {85, 92, 176, 35, 18, 12},
        {103, 92, 194, 35, 18, 12},
        {85, 104, 176, 47, 18, 15},
        {103, 104, 194, 47, 18, 15}
    };

    public ScreenNukeFleija(MenuNukeFleija menu, Inventory playerInventory, Component title) {
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
                    slot < 2
                            ? ModItems.FLEIJA_IGNITER.get()
                            : slot < 5
                                    ? ModItems.FLEIJA_PROPELLANT.get()
                                    : ModItems.FLEIJA_CORE.get();
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

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
