// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRebarPlacer;
import com.hbm.items.tool.ItemRebarPlacer;
import com.hbm.lib.Library;
import com.hbm.util.GameTime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ScreenRebarPlacer extends ScreenInfoContainer<MenuRebarPlacer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_rebar.png");

    public ScreenRebarPlacer(MenuRebarPlacer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 182);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
        if (!ItemRebarPlacer.isValidConcrete(menu.pattern().getItem())) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + 87,
                    topPos + 17,
                    176.0F,
                    0.0F,
                    56,
                    56,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Slot pattern = menu.pattern();
        if (!isHovering(pattern.x, pattern.y, 16, 16, mouseX, mouseY) || pattern.hasItem()) return;

        List<ItemStack> list = new ArrayList<>();
        for (Block block : ItemRebarPlacer.acceptableConcrete()) list.add(new ItemStack(block));
        int cycle = (int) (GameTime.now() % (1000L * list.size()) / 1000L);
        ItemStack selected = list.get(cycle);

        int bound0 = (int) Math.ceil(list.size() / 3D);
        int bound1 = (int) Math.ceil(list.size() / 3D * 2D);
        List<Object[]> lines = new ArrayList<>();
        lines.add(list.subList(0, bound0).toArray());
        lines.add(list.subList(bound0, bound1).toArray());
        lines.add(list.subList(bound1, list.size()).toArray());
        lines.add(new Object[] {selected.getHoverName().getString()});

        GUIElements.drawStackText(
                graphics, this.font, lines, mouseX, mouseY, this.width, this.height, selected);
    }
}
