// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeMike;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class ScreenNukeMike extends ScreenInfoContainer<MenuNukeMike> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/ivymikeschematic.png");

    private static final int[][] LENS = {
        {24, 20, 209, 1}, {24, 43, 209, 24}, {47, 20, 232, 1}, {47, 43, 232, 24}
    };

    public ScreenNukeMike(MenuNukeMike menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 217);
        this.titleLabelY = 4;
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

        if (menu.isReady() && !menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 5, 35, 177.0F, 1.0F, 16, 16, 256, 256);
        }
        if (menu.isReady() && menu.isFilled()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 5, 35, 177.0F, 19.0F, 16, 16, 256, 256);
        }

        part(graphics, 5, ModItems.MIKE_CORE.get(), 75, 25, 176, 49, 80, 36);
        part(graphics, 6, ModItems.MIKE_DEUT.get(), 79, 30, 180, 88, 58, 26);
        part(graphics, 7, ModItems.MIKE_COOLING_UNIT.get(), 140, 30, 240, 88, 12, 26);

        for (int i = 0; i < LENS.length; i++) {
            int[] r = LENS[i];
            part(graphics, i, ModItems.EXPLOSIVE_LENSES.get(), r[0], r[1], r[2], r[3], 23, 23);
        }

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
                lineArray("desc.gui.nukeMike.desc"));

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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, w, h, 256, 256);
    }
}
