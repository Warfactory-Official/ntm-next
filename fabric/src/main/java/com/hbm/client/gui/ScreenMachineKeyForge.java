// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineKeyForge;
import com.hbm.lib.Library;
import com.hbm.util.TooltipStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineKeyForge extends ScreenInfoContainer<MenuMachineKeyForge> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_keyforge.png");

    public ScreenMachineKeyForge(MenuMachineKeyForge menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        inventoryLabelY = imageHeight - 96 + 2;
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
        drawInfoPanel(graphics, 12, 28, 16, 16, 2);
        drawInfoPanel(graphics, 12, 44, 16, 16, 3);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                12,
                28,
                16,
                16,
                20,
                44,
                TooltipStyle.lines(Component.translatable("desc.gui.keyforge.key")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                12,
                44,
                16,
                16,
                20,
                60,
                TooltipStyle.lines(Component.translatable("desc.gui.keyforge.random")));
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
