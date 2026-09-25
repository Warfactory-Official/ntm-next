// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuDroneRequester;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.tileentity.network.BlockEntityDroneRequester;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenDroneRequester extends ScreenInfoContainer<MenuDroneRequester> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_drone_requester.png");
    private static final int TOOLTIP_LIFT = 30;

    public ScreenDroneRequester(MenuDroneRequester menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
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

        BlockEntityDroneRequester requester = menu.blockEntity();
        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < BlockEntityDroneRequester.FILTER_COUNT; i++) {
                Slot slot = menu.getSlot(i);
                String mode = requester.matcher.mode(i);
                if (mode != null && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    graphics.setComponentTooltipForNextFrame(
                            font,
                            List.of(
                                    Component.translatable("desc.shared.rightClickToChange")
                                            .withStyle(ChatFormatting.RED),
                                    ModulePatternMatcher.getLabel(mode)),
                            mouseX,
                            mouseY - TOOLTIP_LIFT);
                }
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
