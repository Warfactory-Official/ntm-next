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

import com.hbm.blockentity.machine.AssemblyFactoryBlockEntity;
import com.hbm.inventory.container.AssemblyFactoryMenu;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.AssemblyFactoryRecipeLayout;
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

public class AssemblyFactoryScreen extends InfoContainerScreen<AssemblyFactoryMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_assembly_factory.png");

    private static final int BTN_W = 18, BTN_H = 18;

    public AssemblyFactoryScreen(AssemblyFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 240);
        inventoryLabelX = 33;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private static int inputSlotIndex(int module, int i) {
        return module * AssemblyFactoryBlockEntity.SLOTS_PER_MODULE
                + AssemblyFactoryBlockEntity.INPUT_OFFSET
                + i;
    }

    private static int inputSlotX(int module, int i) {
        int col = i % 6;
        return 7 + (module % 2) * 109 + col * 16;
    }

    private static int inputSlotY(int module, int i) {
        int row = i / 6;
        return 20 + (module / 2) * 56 + row * 16;
    }

    @Override
    protected int titleCenterX() {
        return 113;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        AssemblyFactoryBlockEntity be = menu.blockEntity();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 256, 140, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 25, 140, 25.0F, 140.0F, 231, 100, 256, 256);

        long maxPower = Math.max(1L, be.maxPower);
        int p = (int) (be.power * 92L / maxPower);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    234,
                    110 - p,
                    0.0F,
                    232.0F - p,
                    16,
                    p,
                    256,
                    256);
        }

        for (int g = 0; g < AssemblyFactoryBlockEntity.MODULES; g++) {
            GenericRecipe recipe = be.module[g].getRecipe();
            int colX = (g % 2) * 109, rowY = (g / 2) * 56;

            if (be.module[g].progress > 0) {
                int j = (int) Math.ceil(37 * be.module[g].progress);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        45 + colX,
                        63 + rowY,
                        0.0F,
                        240.0F,
                        j,
                        6,
                        256,
                        256);
            }

            if (be.didProcess[g]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        45 + colX,
                        55 + rowY,
                        4.0F,
                        236.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        45 + colX,
                        55 + rowY,
                        0.0F,
                        236.0F,
                        4,
                        4,
                        256,
                        256);
            }
            if (be.didProcess[g]) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        53 + colX,
                        55 + rowY,
                        4.0F,
                        236.0F,
                        4,
                        4,
                        256,
                        256);
            } else if (recipe != null && be.power >= recipe.power && be.canCool()) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        53 + colX,
                        55 + rowY,
                        0.0F,
                        236.0F,
                        4,
                        4,
                        256,
                        256);
            }

            graphics.item(
                    recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                    7 + colX,
                    54 + rowY);

            if (recipe != null && recipe.inputItem != null) {
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(inputSlotIndex(g, i)).isEmpty()) {
                        graphics.item(
                                recipe.inputItem[i].extractForCyclingDisplay(20),
                                inputSlotX(g, i),
                                inputSlotY(g, i));
                    }
                }
                for (int i = 0; i < recipe.inputItem.length; i++) {
                    if (be.getItem(inputSlotIndex(g, i)).isEmpty()) {
                        int x = inputSlotX(g, i), y = inputSlotY(g, i);
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

        for (int j = 0; j < AssemblyFactoryBlockEntity.MODULES; j++) {
            int colX = (j % 2) * 109, rowY = (j / 2) * 56;
            drawFluidBar(graphics, 105 + colX, 20 + rowY, 5, 32, be.inputTanks[j]);
            drawFluidBar(graphics, 105 + colX, 54 + rowY, 5, 16, be.outputTanks[j]);
        }
        drawFluidBar(graphics, 232, 149, 7, 52, be.water());
        drawFluidBar(graphics, 241, 149, 7, 52, be.lps());

        drawElectricityInfo(graphics, mouseX, mouseY, 234, 18, 16, 92, be.power, be.maxPower);
        for (int j = 0; j < AssemblyFactoryBlockEntity.MODULES; j++) {
            int colX = (j % 2) * 109, rowY = (j / 2) * 56;
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, 105 + colX, 20 + rowY, 5, 32, be.inputTanks[j]);
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, 105 + colX, 54 + rowY, 5, 16, be.outputTanks[j]);
        }
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 232, 149, 7, 52, be.water());
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 241, 149, 7, 52, be.lps());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        AssemblyFactoryBlockEntity be = menu.blockEntity();

        for (int i = 0; i < AssemblyFactoryBlockEntity.MODULES; i++) {
            int x = 6 + (i % 2) * 109, y = 53 + (i / 2) * 56;
            if (!checkClick(mouseX, mouseY, x, y, BTN_W, BTN_H)) continue;
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
            AssemblyFactoryBlockEntity be = menu.blockEntity();
            for (int i = 0; i < AssemblyFactoryBlockEntity.MODULES; i++) {
                int x = 6 + (i % 2) * 109, y = 53 + (i / 2) * 56;
                if (checkClick((int) event.x(), (int) event.y(), x, y, BTN_W, BTN_H)) {
                    String current =
                            be.module[i].getRecipeName().isEmpty()
                                    ? null
                                    : be.module[i].getRecipeName();
                    String pool =
                            BlueprintsItem.grabPool(
                                    be.getItem(
                                            i * AssemblyFactoryBlockEntity.SLOTS_PER_MODULE
                                                    + AssemblyFactoryBlockEntity.TEMPLATE_OFFSET));
                    GUIScreenRecipeSelector.openSelector(
                            AssemblyMachineRecipes.INSTANCE,
                            be.getBlockPos(),
                            current,
                            i,
                            pool,
                            this,
                            (pos, index, selection) ->
                                    PacketWire.sendToServer(
                                            AssemblyFactoryRecipeLayout.encode(
                                                    menu.containerId, index, selection)));
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
