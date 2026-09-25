// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuTapeDrive;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenTapeDrive extends ScreenInfoContainer<MenuTapeDrive> {
    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_tape_drive.png");

    public ScreenTapeDrive(MenuTapeDrive menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFF;
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
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
