// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineMicrowave;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMicrowave;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineMicrowave extends ScreenInfoContainer<MenuMachineMicrowave> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_microwave.png");

    private static final int BTN_X = 43, BTN_UP_Y = 25, BTN_DOWN_Y = 43, BTN_W = 18, BTN_H = 18;

    public ScreenMachineMicrowave(MenuMachineMicrowave menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
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

        BlockEntityMicrowave be = menu.blockEntity();

        int i = (int) (be.power * 34L / BlockEntityMicrowave.MAX_POWER);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 51 - i, 176, 34 - i, 16, i, 256, 256);

        int j = Math.min(be.time * 23 / BlockEntityMicrowave.MAX_TIME, 22);
        if (j > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 104, 34, 192, 0, j, 16, 256, 256);

        int k = be.speed * 34 / BlockEntityMicrowave.MAX_SPEED;
        if (k > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 60 - k, 214, 34 - k, 4, k, 256, 256);

        drawElectricityInfo(
                graphics, mouseX, mouseY, 8, 17, 16, 34, be.power, BlockEntityMicrowave.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            int button = -1;
            if (checkClick(mx, my, BTN_X, BTN_UP_Y, BTN_W, BTN_H)) button = 0;
            else if (checkClick(mx, my, BTN_X, BTN_DOWN_Y, BTN_W, BTN_H)) button = 1;
            if (button >= 0) {
                CompoundTag data = new CompoundTag();
                data.putInt("button", button);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
