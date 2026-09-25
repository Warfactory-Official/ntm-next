// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeTsar;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class ScreenNukeTsar extends ScreenInfoContainer<MenuNukeTsar> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/tsarbombaschematic.png");
    private static final Identifier MIKE = Library.id("textures/gui/weapon/ivymikeschematic.png");

    private static final int[][] LENS = {
        {40, 36, 209, 1}, {40, 59, 209, 24}, {63, 36, 232, 1}, {63, 59, 232, 24}
    };

    public ScreenNukeTsar(MenuNukeTsar menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 233);
        this.inventoryLabelX = 48;
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

        if (menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, MIKE, 18, 50, 176.0F, 18.0F, 16, 16, 256, 256);
        } else if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, MIKE, 18, 50, 176.0F, 0.0F, 16, 16, 256, 256);
        }

        for (int i = 0; i < LENS.length; i++) {
            int[] r = LENS[i];
            part(graphics, i, ModItems.EXPLOSIVE_LENSES.get(), r[0], r[1], r[2], r[3], 23, 23);
        }
        part(graphics, 5, ModItems.TSAR_CORE.get(), 91, 41, 176, 220, 80, 36);

        drawInfoPanel(graphics, -16, 16, 16, 16, 2);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                16,
                16,
                16,
                -8,
                32,
                lineArray("desc.gui.nukeTsar.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void part(
            GuiGraphicsExtractor graphics,
            int slot,
            Item item,
            int x,
            int y,
            int u,
            int v,
            int w,
            int h) {
        if (!menu.part(slot).is(item)) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MIKE, x, y, u, v, w, h, 256, 256);
    }
}
