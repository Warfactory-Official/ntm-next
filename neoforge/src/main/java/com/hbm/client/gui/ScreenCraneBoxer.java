// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCraneBoxer;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCraneBoxer extends ScreenInfoContainer<MenuCraneBoxer> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crane_boxer.png");

    private static final int TOGGLE_X = 151;
    private static final int TOGGLE_Y = 34;
    private static final int TOGGLE_SIZE = 18;

    public ScreenCraneBoxer(MenuCraneBoxer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                TOGGLE_X,
                TOGGLE_Y,
                176,
                menu.blockEntity().mode * 18,
                TOGGLE_SIZE,
                TOGGLE_SIZE,
                256,
                256);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick(
                (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
        }

        return super.mouseClicked(event, doubleClick);
    }
}
