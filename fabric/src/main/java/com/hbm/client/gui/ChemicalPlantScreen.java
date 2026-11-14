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

import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.inventory.container.ChemicalPlantMenu;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ChemicalPlantRecipeLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ChemicalPlantScreen extends InfoContainerScreen<ChemicalPlantMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_chemplant.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 61;
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 34;
    private static final int IN_X0 = 8, OUT_X0 = 80, TANK_STRIDE = 18;
    private static final int ITEM_IN_Y = 99;
    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;

    public ChemicalPlantScreen(ChemicalPlantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
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
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        long power = menu.getPower();
        long max = chemplant().maxPower;
        if (power > 0 && max > 0) {
            int filled = (int) Math.min(POWER_H, power * POWER_H / max);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        176,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
        }

        ChemicalPlantBlockEntity be = chemplant();
        GenericRecipe recipe = be.module.getRecipe();

        if (be.module.progress > 0) {
            int arrowW = (int) Math.ceil(70.0 * be.module.progress);
            float arrowV = be.module.restrictedMode ? 77.0F : 61.0F;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    62,
                    126,
                    176.0F,
                    arrowV,
                    arrowW,
                    16,
                    256,
                    256);
        }

        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 121, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 121, 192.0F, 0.0F, 3, 6, 256, 256);
        }
        if (be.isProgressing) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 121, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null && be.power >= recipe.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 121, 192.0F, 0.0F, 3, 6, 256, 256);
        }

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                8,
                126);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(ChemicalPlantBlockEntity.SLOT_ITEM_IN_START + i).isEmpty()) {
                    graphics.item(
                            recipe.inputItem[i].extractForCyclingDisplay(20),
                            IN_X0 + i * TANK_STRIDE,
                            ITEM_IN_Y);
                }
            }

            for (int i = 0; i < recipe.inputItem.length; i++) {
                if (be.getItem(ChemicalPlantBlockEntity.SLOT_ITEM_IN_START + i).isEmpty()) {
                    int x = IN_X0 + i * TANK_STRIDE, y = ITEM_IN_Y;
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            x,
                            y,
                            (float) x,
                            (float) y,
                            16,
                            16,
                            256,
                            256,
                            ARGB.white(0.5F));
                }
            }
        }

        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            drawFluidBar(
                    graphics, IN_X0 + i * TANK_STRIDE, TANK_TOP, TANK_W, TANK_H, be.inputTanks[i]);
            drawFluidBar(
                    graphics,
                    OUT_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.outputTanks[i]);
        }

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, power, max);

        for (int i = 0; i < ChemicalPlantBlockEntity.TANK_COUNT; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.inputTanks[i]);
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_X0 + i * TANK_STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.outputTanks[i]);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            ChemicalPlantBlockEntity be = chemplant();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    BlueprintsItem.grabPool(be.getItem(ChemicalPlantBlockEntity.SLOT_SCHEMATIC));
            GUIScreenRecipeSelector.openSelector(
                    ChemicalPlantRecipes.INSTANCE,
                    be.getBlockPos(),
                    current,
                    0,
                    pool,
                    this,
                    (pos, index, selection) ->
                            PacketWire.sendToServer(
                                    ChemicalPlantRecipeLayout.encode(menu.containerId, selection)));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        GenericRecipe recipe = chemplant().module.getRecipe();
        if (recipe != null) {
            GUIElements.drawHoveringTextRecipe(
                    graphics, font, recipe.print(), mouseX, mouseY, width, height);
        } else {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX,
                    mouseY);
        }
    }

    private ChemicalPlantBlockEntity chemplant() {
        return menu.blockEntity();
    }
}
