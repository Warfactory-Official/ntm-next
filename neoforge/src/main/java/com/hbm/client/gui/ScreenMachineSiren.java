// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineSiren;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineSiren extends ScreenInfoContainer<MenuMachineSiren> {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_siren.png");

    public ScreenMachineSiren(MenuMachineSiren menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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

        TrackType track = menu.blockEntity().getCurrentType();
        if (track != TrackType.NULL) {
            int colour = ARGB.opaque(track.getColor());
            graphics.text(font, Component.literal(track.getTrackTitle()), 46, 28, colour, false);
            graphics.text(
                    font,
                    Component.translatable("desc.shared.type", track.getType().name()),
                    46,
                    40,
                    colour,
                    false);
            graphics.text(
                    font,
                    Component.translatable("desc.gui.machineSiren.volume", track.getVolume()),
                    46,
                    52,
                    colour,
                    false);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
