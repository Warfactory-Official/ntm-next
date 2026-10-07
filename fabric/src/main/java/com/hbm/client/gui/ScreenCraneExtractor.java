// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.blockentity.network.BlockEntityCraneExtractor;
import com.hbm.inventory.container.MenuCraneExtractor;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.NbtControlPayload;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenCraneExtractor extends ScreenInfoContainer<MenuCraneExtractor> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crane_ejector.png");

    private static final int MAX_EJECT_X = 187;
    private static final int MAX_EJECT_Y = 34;
    private static final int MAX_EJECT_SIZE = 18;

    private static final int LEVER_X = 128;
    private static final int LEVER_Y = 30;
    private static final int LEVER_W = 14;
    private static final int LEVER_H = 26;

    public ScreenCraneExtractor(MenuCraneExtractor menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 212, 185);
        this.inventoryLabelX = 26;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCraneExtractor extractor = menu.blockEntity();

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

        if (extractor.maxEject) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    MAX_EJECT_X,
                    MAX_EJECT_Y,
                    212,
                    0,
                    MAX_EJECT_SIZE,
                    MAX_EJECT_SIZE,
                    256,
                    256);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                139,
                extractor.isWhitelist ? 33 : 47,
                212,
                18,
                3,
                6,
                256,
                256);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                MAX_EJECT_X,
                MAX_EJECT_Y,
                MAX_EJECT_SIZE,
                MAX_EJECT_SIZE,
                List.of(
                        Component.translatable("desc.gui.craneExtractor.onlyTakeMaximumPossible")
                                .append(
                                        Component.literal(extractor.maxEject ? "ON" : "OFF")
                                                .withStyle(
                                                        extractor.maxEject
                                                                ? ChatFormatting.GREEN
                                                                : ChatFormatting.RED))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < BlockEntityCraneExtractor.FILTER_SLOTS
                ? patternModeLines(menu.blockEntity().matcher.mode(slot.index))
                : List.of();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();

        if (checkClick(mx, my, MAX_EJECT_X, MAX_EJECT_Y, MAX_EJECT_SIZE, MAX_EJECT_SIZE))
            send("maxEject");
        if (checkClick(mx, my, LEVER_X, LEVER_Y, LEVER_W, LEVER_H)) send("whitelist");

        return super.mouseClicked(event, doubleClick);
    }

    private void send(String key) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        PacketWire.sendToServer(new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
        playClick();
    }
}
