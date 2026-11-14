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

import com.hbm.blockentity.machine.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm.inventory.container.PlasmaForgeMenu;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FusionPlasmaForgeRecipeLayout;
import com.hbm.util.NumberFormatter;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PlasmaForgeScreen extends InfoContainerScreen<PlasmaForgeMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/reactors/gui_fusion_plasmaforge.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 62;
    private static final int TANK_X = 80, TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int BTN_X = 7, BTN_Y = 80, BTN_W = 18, BTN_H = 18;
    private static final int PLASMA_STAT_X = 25, PLASMA_STAT_Y = 115, STAT_W = 18, STAT_H = 18;

    private final List<ItemStack> isotopes =
            FusionPlasmaForgeBlockEntity.boosters().stream()
                    .flatMap(booster -> booster.ingredient().items())
                    .map(ItemStack::new)
                    .toList();

    public PlasmaForgeScreen(PlasmaForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 244);
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

        FusionPlasmaForgeBlockEntity be = menu.blockEntity();
        PlasmaForgeRecipe recipe = be.module.getRecipe() instanceof PlasmaForgeRecipe f ? f : null;

        if (be.maxPower > 0) {
            int p = (int) (be.power * POWER_H / be.maxPower);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        176,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        if (be.module.progress > 0) {
            int j = (int) Math.ceil(70 * be.module.progress);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 81, 176.0F, 62.0F, j, 16, 256, 256);
        }

        if (be.didProcess) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 76, 195.0F, 0.0F, 3, 6, 256, 256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 76, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 76, 192.0F, 0.0F, 3, 6, 256, 256);
            if (be.power >= recipe.power) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        56,
                        76,
                        192.0F,
                        0.0F,
                        3,
                        6,
                        256,
                        256);
            }
        }

        double inputGauge =
                recipe == null
                        ? 0
                        : Math.min((double) be.plasmaEnergySync / (double) recipe.ignitionTemp, 1.5)
                                / 1.5D;
        double boosterGauge = be.maxBooster <= 0 ? 0 : (double) be.booster / (double) be.maxBooster;

        SmoothGaugeElement.draw(graphics, 34, 124, inputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 70, 124, boosterGauge, 5, 2, 1, 0xA00000, 0x000000);

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                BTN_X + 1,
                BTN_Y + 1);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length && i < 12; i++) {
                Slot slot = menu.slots.get(FusionPlasmaForgeBlockEntity.SLOT_INPUT_START + i);
                if (slot.hasItem()) continue;
                CountIngredient ingredient = recipe.inputItem[i];
                graphics.item(ingredient.extractForCyclingDisplay(20), slot.x, slot.y);
            }
            for (int i = 0; i < recipe.inputItem.length && i < 12; i++) {
                Slot slot = menu.slots.get(FusionPlasmaForgeBlockEntity.SLOT_INPUT_START + i);
                if (slot.hasItem()) continue;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        slot.x,
                        slot.y,
                        (float) slot.x,
                        (float) slot.y,
                        16,
                        16,
                        256,
                        256,
                        ARGB.white(0.5F));
            }
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.inputTank);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                be.maxPower);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.inputTank);

        String plasma =
                recipe != null
                        ? ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + NumberFormatter.formatCompact(be.plasmaEnergySync)
                                + "TU / "
                                + NumberFormatter.formatCompact(recipe.ignitionTemp)
                                + "TU"
                        : "0TU / 0TU";
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PLASMA_STAT_X,
                PLASMA_STAT_Y,
                STAT_W,
                STAT_H,
                List.of(Component.literal(plasma)));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Slot booster = menu.slots.get(FusionPlasmaForgeBlockEntity.SLOT_BOOSTER);
        if (booster.hasItem()
                || !menu.getCarried().isEmpty()
                || !isHovering(booster.x, booster.y, 16, 16, mouseX, mouseY)) {
            return;
        }
        GUIElements.drawCyclingStackText(
                graphics,
                font,
                Component.translatable("desc.gui.plasmaForge.boosterIsotope"),
                isotopes,
                mouseX,
                mouseY,
                width,
                height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            FusionPlasmaForgeBlockEntity be = menu.blockEntity();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    BlueprintsItem.grabPool(
                            be.getItem(FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT));
            GUIScreenRecipeSelector.openSelector(
                    PlasmaForgeRecipes.INSTANCE,
                    be.getBlockPos(),
                    current,
                    0,
                    pool,
                    this,
                    (pos, index, selection) ->
                            PacketWire.sendToServer(
                                    FusionPlasmaForgeRecipeLayout.encode(
                                            menu.containerId, selection)));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        PlasmaForgeRecipe recipe =
                menu.blockEntity().module.getRecipe() instanceof PlasmaForgeRecipe f ? f : null;
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
}
