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

import com.hbm.blockentity.machine.SuperComputerBlockEntity;
import com.hbm.inventory.container.SuperComputerMenu;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SuperComputerRecipeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SuperComputerScreen extends InfoContainerScreen<SuperComputerMenu> {
    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_supercomputer.png");
    private static final int BUTTON_X = 7, BUTTON_Y = 80, BUTTON_W = 18, BUTTON_H = 18;

    public SuperComputerScreen(SuperComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 211);
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
        SuperComputerBlockEntity computer = menu.blockEntity();
        long power = menu.getPower();
        if (power > 0 && computer.maxPower > 0) {
            int filled = (int) Math.min(61, power * 61 / computer.maxPower);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    79 - filled,
                    176,
                    61 - filled,
                    16,
                    filled,
                    256,
                    256);
        }
        if (computer.module.progress > 0) {
            int width = (int) Math.ceil(70 * computer.module.progress);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 81, 176, 61, width, 16, 256, 256);
        }

        GenericRecipe recipe = computer.module.getRecipe();
        if (computer.didProcess || recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    51,
                    76,
                    computer.didProcess ? 195 : 192,
                    0,
                    3,
                    6,
                    256,
                    256);
        }
        if (computer.didProcess || recipe != null && computer.power >= recipe.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    56,
                    76,
                    computer.didProcess ? 195 : 192,
                    0,
                    3,
                    6,
                    256,
                    256);
        }

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER), 8, 81);
        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (computer.getItem(SuperComputerBlockEntity.SLOT_INPUT_START + i).isEmpty()) {
                    graphics.item(recipe.inputItem[i].extractForCyclingDisplay(20), 8 + i * 18, 27);
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            8 + i * 18,
                            27,
                            8 + i * 18,
                            27,
                            16,
                            16,
                            256,
                            256,
                            ARGB.white(0.5F));
                }
            }
        }

        drawFluidBarH(graphics, 8, 54, 52, 16, computer.inputTanks[0]);
        drawFluidBarH(graphics, 80, 54, 52, 16, computer.outputTanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 54, 52, 16, computer.inputTanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 80, 54, 52, 16, computer.outputTanks[0]);
        drawElectricityInfo(graphics, mouseX, mouseY, 152, 18, 16, 61, power, computer.maxPower);
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(), (int) event.y(), BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
            SuperComputerBlockEntity computer = menu.blockEntity();
            String current =
                    computer.module.getRecipeName().isEmpty()
                            ? null
                            : computer.module.getRecipeName();
            String pool =
                    BlueprintsItem.grabPool(
                            computer.getItem(SuperComputerBlockEntity.SLOT_SCHEMATIC));
            GUIScreenRecipeSelector.openSelector(
                    SuperComputerRecipes.INSTANCE,
                    computer.getBlockPos(),
                    current,
                    0,
                    pool,
                    this,
                    (pos, index, selection) ->
                            PacketWire.sendToServer(
                                    SuperComputerRecipeLayout.encode(menu.containerId, selection)));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BUTTON_X, BUTTON_Y, BUTTON_W, BUTTON_H)) return;
        GenericRecipe recipe = menu.blockEntity().module.getRecipe();
        if (recipe == null) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX,
                    mouseY);
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.addAll(recipe.print());
        GUIElements.drawHoveringTextRecipe(graphics, font, lines, mouseX, mouseY, width, height);
    }
}
