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

import com.hbm.blockentity.processing.ChemicalFactoryBlockEntity;
import com.hbm.inventory.container.ChemicalFactoryMenu;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ChemicalFactoryRecipeLayout;
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

public class ChemicalFactoryScreen extends InfoContainerScreen<ChemicalFactoryMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_chemical_factory.png");

    private static final int BTN_X = 74, BTN_W = 18, BTN_H = 18;

    public ChemicalFactoryScreen(ChemicalFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 248, 216);
        inventoryLabelX = 26;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 106;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ChemicalFactoryBlockEntity be = menu.blockEntity();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 248, 116, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 18, 116, 18.0F, 116.0F, 230, 100, 256, 256);

        long maxPower = Math.max(1L, be.maxPower);
        int p = (int) (be.power * 68L / maxPower);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    224,
                    86 - p,
                    0.0F,
                    184.0F - p,
                    16,
                    p,
                    256,
                    256);
        }

        for (int g = 0; g < ChemicalFactoryBlockEntity.MODULES; g++) {
            GenericRecipe recipe = be.module[g].getRecipe();

            if (be.module[g].progress > 0) {
                int j = (int) Math.ceil(22 * be.module[g].progress);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        29 + g * 22,
                        0.0F,
                        216.0F,
                        j,
                        6,
                        256,
                        256);
            }

            if (be.isProcessing(g)) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        21 + g * 22,
                        4.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        113,
                        21 + g * 22,
                        0.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            }
            if (be.isProcessing(g)) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        121,
                        21 + g * 22,
                        4.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null && be.power >= recipe.power && be.canCool()) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        121,
                        21 + g * 22,
                        0.0F,
                        222.0F,
                        4,
                        4,
                        256,
                        256);
            }

            graphics.item(
                    recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                    75,
                    20 + g * 22);

            if (recipe != null && recipe.inputItem != null) {
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(5 + g * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE + i)
                            .isEmpty()) {
                        graphics.item(
                                recipe.inputItem[i].extractForCyclingDisplay(20),
                                10 + i * 16,
                                20 + g * 22);
                    }
                }
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(5 + g * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE + i)
                            .isEmpty()) {
                        int x = 10 + i * 16, y = 20 + g * 22;
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
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < ChemicalFactoryBlockEntity.MODULES; j++) {
                drawFluidBar(graphics, 60 + i * 5, 20 + j * 22, 3, 16, be.inputTanks[i + j * 3]);
                drawFluidBar(graphics, 189 + i * 5, 20 + j * 22, 3, 16, be.outputTanks[i + j * 3]);
            }
        }
        drawFluidBar(graphics, 224, 125, 7, 52, be.water());
        drawFluidBar(graphics, 233, 125, 7, 52, be.lps());

        drawElectricityInfo(graphics, mouseX, mouseY, 224, 18, 16, 68, be.power, be.maxPower);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < ChemicalFactoryBlockEntity.MODULES; j++) {
                drawFluidGaugeInfo(
                        graphics,
                        mouseX,
                        mouseY,
                        60 + i * 5,
                        20 + j * 22,
                        3,
                        16,
                        be.inputTanks[i + j * 3]);
                drawFluidGaugeInfo(
                        graphics,
                        mouseX,
                        mouseY,
                        189 + i * 5,
                        20 + j * 22,
                        3,
                        16,
                        be.outputTanks[i + j * 3]);
            }
        }
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 224, 125, 7, 52, be.water());
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 233, 125, 7, 52, be.lps());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        ChemicalFactoryBlockEntity be = menu.blockEntity();

        for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
            if (!checkClick(mouseX, mouseY, BTN_X, 19 + i * 22, BTN_W, BTN_H)) continue;
            GenericRecipe recipe = be.module[i].getRecipe();
            if (recipe != null) {
                GUIElements.drawHoveringTextRecipe(
                        graphics, font, recipe.print(), mouseX, mouseY, width, height);
            } else {
                graphics.setTooltipForNextFrame(
                        font,
                        Component.translatable("gui.recipe.setRecipe")
                                .withStyle(ChatFormatting.YELLOW),
                        mouseX,
                        mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            ChemicalFactoryBlockEntity be = menu.blockEntity();
            for (int i = 0; i < ChemicalFactoryBlockEntity.MODULES; i++) {
                if (checkClick(
                        (int) event.x(), (int) event.y(), BTN_X, 19 + i * 22, BTN_W, BTN_H)) {
                    String current =
                            be.module[i].getRecipeName().isEmpty()
                                    ? null
                                    : be.module[i].getRecipeName();
                    String pool =
                            BlueprintsItem.grabPool(
                                    be.getItem(
                                            4 + i * ChemicalFactoryBlockEntity.SLOTS_PER_MODULE));
                    GUIScreenRecipeSelector.openSelector(
                            ChemicalPlantRecipes.INSTANCE,
                            be.getBlockPos(),
                            current,
                            i,
                            pool,
                            this,
                            (pos, index, selection) ->
                                    PacketWire.sendToServer(
                                            ChemicalFactoryRecipeLayout.encode(
                                                    menu.containerId, index, selection)));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
