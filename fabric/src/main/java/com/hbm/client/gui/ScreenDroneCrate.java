// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuDroneCrate;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityDroneCrate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenDroneCrate extends ScreenInfoContainer<MenuDroneCrate> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crate_drone.png");

    private static final int TANK_X = 125, TANK_Y = 17, TANK_W = 16, TANK_H = 34;
    private static final int BUTTON = 18;
    private static final int TYPE_X = 151, TYPE_Y = 16;
    private static final int MODE_X = 151, MODE_Y = 52;

    public ScreenDroneCrate(MenuDroneCrate menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityDroneCrate crate = menu.blockEntity();

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
                TYPE_X,
                TYPE_Y,
                194,
                crate.itemType ? 0 : BUTTON,
                BUTTON,
                BUTTON,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                MODE_X,
                MODE_Y,
                176,
                crate.sendingMode ? BUTTON : 0,
                BUTTON,
                BUTTON,
                256,
                256);

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, crate.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, crate.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, TYPE_X, TYPE_Y, BUTTON, BUTTON)) return toggle("type");
            if (checkClick(mx, my, MODE_X, MODE_Y, BUTTON, BUTTON)) return toggle("mode");
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean toggle(String key) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
        playClick();
        return true;
    }
}
