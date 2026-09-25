// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKAutoloader;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKAutoloader extends ScreenInfoContainer<MenuRBMKAutoloader> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_autoloader.png");
    private static final int BUTTON_SIZE = 12;
    private static final int MINUS_X = 74;
    private static final int PLUS_X = 90;
    private static final int BUTTON_Y = 36;

    public ScreenRBMKAutoloader(
            MenuRBMKAutoloader menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 182);
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

        Component percent = Component.literal(menu.blockEntity().cycle + "%");
        graphics.text(
                font, percent, imageWidth / 2 - font.width(percent) / 2, 23, 0xFF00FF00, false);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int x = (int) event.x(), y = (int) event.y();
            if (checkClick(x, y, MINUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) return send("minus");
            if (checkClick(x, y, PLUS_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) return send("plus");
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean send(String key) {
        BlockPos core = menu.blockEntity().getBlockPos();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
        playClick();
        return true;
    }
}
