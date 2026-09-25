// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuBombMulti;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityBombMulti.Payload;
import com.hbm.tileentity.bomb.BlockEntityBombMulti;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenBombMulti extends AbstractContainerScreen<MenuBombMulti> {

    private static final Identifier TEXTURE = Library.id("textures/gui/weapon/bomb_generic.png");

    private static final int MISMATCH_ROW = 7;

    public ScreenBombMulti(MenuBombMulti menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

        Payload first = Payload.of(menu.getSlot(BlockEntityBombMulti.PAYLOAD_SLOTS[0]).getItem());
        Payload second = Payload.of(menu.getSlot(BlockEntityBombMulti.PAYLOAD_SLOTS[1]).getItem());

        int row = first == second ? first.ordinal() - 1 : MISMATCH_ROW;
        if (row >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    124,
                    34,
                    176.0F,
                    row * 18.0F,
                    18,
                    18,
                    256,
                    256);
        }

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);
    }
}
