// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCraneInserter;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCraneInserter extends ScreenInfoContainer<MenuCraneInserter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crane_inserter.png");

    private static final int TOGGLE_X = 151;
    private static final int TOGGLE_Y = 34;
    private static final int TOGGLE_SIZE = 18;

    public ScreenCraneInserter(MenuCraneInserter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 18;
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

        boolean destroyer = menu.blockEntity().destroyer;

        if (destroyer) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    176,
                    0,
                    TOGGLE_SIZE,
                    TOGGLE_SIZE,
                    256,
                    256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                TOGGLE_X,
                TOGGLE_Y,
                TOGGLE_SIZE,
                TOGGLE_SIZE,
                List.of(
                        Component.translatable("desc.gui.craneInserter.destroyOverflow")
                                .append(
                                        Component.literal(destroyer ? "ON" : "OFF")
                                                .withStyle(
                                                        destroyer
                                                                ? ChatFormatting.GREEN
                                                                : ChatFormatting.RED))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick(
                (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("destroyer", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
        }

        return super.mouseClicked(event, doubleClick);
    }
}
