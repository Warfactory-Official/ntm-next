// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineFunnel;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineFunnel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineFunnel extends ScreenInfoContainer<MenuMachineFunnel> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_funnel.png");

    private static final String[] MODE_KEYS = {
        "desc.gui.funnel.mode3x3Then2x2",
        "desc.gui.funnel.mode3x3Only",
        "desc.gui.funnel.mode2x2Only"
    };

    private static final int BTN_X = 159, BTN_Y = 73, BTN_W = 10, BTN_H = 10;

    public ScreenMachineFunnel(MenuMachineFunnel menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
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

        int mode = Mth.positiveModulo(funnel().mode, MODE_KEYS.length);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                BTN_X,
                BTN_Y,
                176,
                mode * BTN_H,
                BTN_W,
                BTN_H,
                256,
                256);

        if (checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BTN_X,
                    BTN_Y,
                    BTN_W,
                    BTN_H,
                    List.of(
                            Component.translatable(
                                    "desc.shared.mode", Component.translatable(MODE_KEYS[mode]))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, BTN_X, BTN_Y, BTN_W, BTN_H)) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("toggle", true);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineFunnel funnel() {
        return menu.blockEntity();
    }
}
