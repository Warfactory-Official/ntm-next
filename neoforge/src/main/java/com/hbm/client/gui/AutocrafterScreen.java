/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.blockentity.machine.TemplateCrafterBlockEntity;
import com.hbm.inventory.container.AutocrafterMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.GhostFilterStackLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AutocrafterScreen extends InfoContainerScreen<AutocrafterMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_autocrafter.png");

    private static final int POWER_X = 17, POWER_Y = 45, POWER_W = 16, POWER_H = 52;
    private static final int POWER_U = 176, POWER_BOTTOM = 97;

    public AutocrafterScreen(AutocrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 240);
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

        TemplateCrafterBlockEntity be = menu.blockEntity();
        int filled = (int) (be.getPower() * POWER_H / be.getMaxPower());
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - filled,
                    POWER_U,
                    POWER_H - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.getPower(),
                be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        TemplateCrafterBlockEntity be = menu.blockEntity();
        if (slot.index < TemplateCrafterBlockEntity.TEMPLATE_COUNT)
            return patternModeLines(be.patternMatcher().mode(slot.index));
        if (slot.index != TemplateCrafterBlockEntity.PREVIEW_SLOT || !slot.hasItem())
            return List.of();
        return List.of(
                Component.translatable("desc.shared.rightClickToChange")
                        .withStyle(ChatFormatting.RED),
                Component.literal((be.selectedRecipeIndex() + 1) + " / " + be.candidateCount())
                        .withStyle(ChatFormatting.YELLOW));
    }

    public Rect2i patternArea(int index) {
        Slot slot = menu.getSlot(index);
        return new Rect2i(leftPos + slot.x, topPos + slot.y, 16, 16);
    }

    public void sendPattern(int index, ItemStack stack) {
        PacketWire.sendToServer(GhostFilterStackLayout.encode(menu.containerId, index, stack));
    }
}
