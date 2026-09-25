// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMassStorage;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.storage.BlockEntityMassStorage;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMassStorage extends ScreenInfoContainer<MenuMassStorage> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_mass_storage.png");

    private static final int GAUGE_X = 97, GAUGE_Y = 105, GAUGE_W = 16, GAUGE_H = 88;
    private static final int INFO_X = 96, INFO_Y = 16, INFO_W = 18, INFO_H = 90;
    private static final int PROVIDE_X = 62, TOGGLE_X = 80, BUTTON_Y = 72, BUTTON_SIZE = 14;

    public ScreenMassStorage(MenuMassStorage menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 221);
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

        BlockEntityMassStorage be = menu.blockEntity();
        int stockpile = be.getStockpile();
        int capacity = be.getCapacity();

        int gauge = stockpile * GAUGE_H / capacity;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                GAUGE_X,
                GAUGE_Y - gauge,
                176,
                GAUGE_H - gauge,
                GAUGE_W,
                gauge,
                256,
                256);
        if (be.output) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    BUTTON_Y,
                    192,
                    0,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                INFO_W,
                INFO_H,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d / %,d", stockpile, capacity)),
                        Component.literal((stockpile * 1000L / capacity) / 10D + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PROVIDE_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(
                        Component.translatable("desc.gui.massStorage.clickProvideOne"),
                        Component.translatable("desc.gui.massStorage.shiftClickProvideStack")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                TOGGLE_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.massStorage.toggleOutput")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, PROVIDE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("provide", event.hasShiftDown());
                sendControl(data);
                return true;
            }
            if (checkClick(mx, my, TOGGLE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("toggle", false);
                sendControl(data);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendControl(CompoundTag data) {
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
        playClick();
    }
}
