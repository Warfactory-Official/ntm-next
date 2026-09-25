// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.container.MenuArmorTable;
import com.hbm.lib.Library;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenArmorTable extends ScreenInfoContainer<MenuArmorTable> {
    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_armor_modifier.png");
    private static final String[] EMPTY_SLOT_KEYS = {
        "armorMod.type.helmet", "armorMod.type.chestplate", "armorMod.type.leggings",
                "armorMod.type.boots",
        "armorMod.type.servo", "armorMod.type.cladding", "armorMod.type.insert",
                "armorMod.type.special",
        "armorMod.type.battery", "armorMod.insertHere"
    };

    public ScreenArmorTable(MenuArmorTable menu, Inventory inventory, Component title) {
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
            if (System.currentTimeMillis() % 1000L < 500L) {
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
