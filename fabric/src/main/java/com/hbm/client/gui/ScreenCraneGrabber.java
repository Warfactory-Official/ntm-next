// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.blockentity.network.BlockEntityCraneGrabber;
import com.hbm.inventory.container.MenuCraneGrabber;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenCraneGrabber extends ScreenInfoContainer<MenuCraneGrabber> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crane_grabber.png");

    private static final int LEVER_X = 97;
    private static final int LEVER_Y = 30;
    private static final int LEVER_W = 14;
    private static final int LEVER_H = 26;

    public ScreenCraneGrabber(MenuCraneGrabber menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCraneGrabber grabber = menu.blockEntity();

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
                108,
                grabber.isWhitelist ? 33 : 47,
                176,
                0,
                3,
                6,
                256,
                256);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < BlockEntityCraneGrabber.FILTER_SLOTS
                ? patternModeLines(menu.blockEntity().matcher.mode(slot.index))
                : List.of();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick((int) event.x(), (int) event.y(), LEVER_X, LEVER_Y, LEVER_W, LEVER_H)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("whitelist", true);
            PacketWire.sendToServer(new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
        }

        return super.mouseClicked(event, doubleClick);
    }
}
