// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineKeyForge;
import com.hbm.lib.Library;
import com.hbm.util.I18nUtil;
import java.util.Arrays;
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
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
                Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.keyforge.key"))
                        .<Component>map(Component::literal)
                        .toList());
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
                Arrays.stream(I18nUtil.resolveKeyArray("desc.gui.keyforge.random"))
                        .<Component>map(Component::literal)
                        .toList());
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
