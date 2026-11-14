/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.container.ArmorTableMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ArmorTableScreen extends InfoContainerScreen<ArmorTableMenu> {
    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_armor_modifier.png");
    private static final String[] EMPTY_SLOT_KEYS = {
        "armorMod.type.helmet", "armorMod.type.chestplate", "armorMod.type.leggings",
                "armorMod.type.boots",
        "armorMod.type.servo", "armorMod.type.cladding", "armorMod.type.insert",
                "armorMod.type.special",
        "armorMod.type.battery", "armorMod.insertHere"
    };

    public ArmorTableScreen(ArmorTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 198, 222);
        inventoryLabelX = 30;
        inventoryLabelY = 128;
        titleLabelY = 6;
    }

    @Override
    protected int titleCenterX() {
        return 110;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 22, 0, 0, 0, 176, 222, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 31, 176, 96, 22, 100, 256, 256);

        if (menu.armorStack().isEmpty()) {
            if (minecraft.gui.hud.getGuiTicks() % 20 < 10) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 63, 60, 176, 52, 22, 22, 256, 256);
            }
        } else if (ArmorModHandler.isArmor(menu.armorStack())) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 63, 60, 176, 74, 22, 22, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 63, 60, 176, 52, 22, 22, 256, 256);
        }

        for (int i = 0; i < ArmorModHandler.MOD_SLOTS; i++)
            drawIndicator(graphics, i, menu.slots.get(i));
        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i <= ArmorModHandler.MOD_SLOTS; i++) {
                if (hoveredSlot == menu.slots.get(i) && !menu.slots.get(i).hasItem()) {
                    ChatFormatting color =
                            i < ArmorModHandler.MOD_SLOTS
                                    ? ChatFormatting.LIGHT_PURPLE
                                    : ChatFormatting.YELLOW;
                    graphics.setComponentTooltipForNextFrame(
                            font,
                            List.of(Component.translatable(EMPTY_SLOT_KEYS[i]).withStyle(color)),
                            mouseX,
                            mouseY);
                    break;
                }
            }
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawIndicator(GuiGraphicsExtractor graphics, int index, Slot slot) {
        if (!slot.hasItem()) return;
        boolean applicable = ArmorModHandler.isApplicable(menu.armorStack(), slot.getItem());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                slot.x - 1,
                slot.y - 1,
                176,
                applicable ? 34 : 16,
                18,
                18,
                256,
                256);
    }
}
