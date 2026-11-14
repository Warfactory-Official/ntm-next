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

import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.inventory.container.FusionTorusMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.BlueprintsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.FusionTorusRecipeLayout;
import com.hbm.util.NumberFormatter;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FusionTorusScreen extends InfoContainerScreen<FusionTorusMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/reactors/gui_fusion_torus.png");

    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 62;
    private static final int BTN_X = 43, BTN_Y = 80, BTN_W = 18, BTN_H = 18;
    private static final int TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int FUEL_X0 = 44, FUEL_STRIDE = 18, OUT_X = 152;
    private static final int COOL_IN_X = 188, COOL_OUT_X = 206, COOL_Y = 46;
    private static final int STAT_Y = 115, STAT_W = 18, STAT_H = 18;
    private static final int IN_STAT_X = 43, OUT_STAT_X = 79, FUEL_STAT_X = 115;

    private static final int HEAT_LIMIT = 123;

    public FusionTorusScreen(FusionTorusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 244);
        inventoryLabelX = 35;
        inventoryLabelY = imageHeight - 93;
    }

    @Override
    protected int titleCenterX() {
        return 106;
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

        FusionTorusBlockEntity be = menu.blockEntity();
        FusionRecipe recipe = be.module.getRecipe() instanceof FusionRecipe f ? f : null;

        int p = (int) (be.power * POWER_H / be.getMaxPower());
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    230,
                    POWER_H - p,
                    POWER_W,
                    p,
                    256,
                    256);
        }

        if (be.module.progress > 0) {
            int j = (int) Math.ceil(70 * be.module.progress);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 98, 81, 0.0F, 244.0F, j, 6, 256, 256);
        }
        if (be.module.bonus > 0) {
            int j = (int) Math.min(Math.ceil(70 * be.module.bonus), 70);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 98, 91, 0.0F, 250.0F, j, 6, 256, 256);
        }

        int heat = (int) Math.ceil(be.temperature);

        if (recipe != null && be.power >= recipe.power) statusLed(graphics, 160);
        if (heat <= HEAT_LIMIT) statusLed(graphics, 170);
        if (be.didProcess) statusLed(graphics, 180);

        if (be.didProcess) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 87, 76, 249.0F, 0.0F, 3, 6, 256, 256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 92, 76, 249.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 87, 76, 246.0F, 0.0F, 3, 6, 256, 256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 92, 76, 246.0F, 0.0F, 3, 6, 256, 256);
        }

        double inputGauge =
                recipe == null
                        ? 0
                        : Math.min((double) be.klystronEnergy / (double) recipe.ignitionTemp, 1.5)
                                / 1.5D;
        double outputGauge =
                recipe == null
                        ? 0
                        : Math.min((double) be.plasmaEnergy / (double) recipe.outputTemp, 1);

        SmoothGaugeElement.draw(graphics, 52, 124, inputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 88, 124, outputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(
                graphics, 124, 124, be.fuelConsumption, 5, 2, 1, 0xA00000, 0x000000);

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                BTN_X + 1,
                BTN_Y + 1);

        for (int i = 0; i < 3; i++)
            drawFluidBar(graphics, FUEL_X0 + i * FUEL_STRIDE, TANK_Y, TANK_W, TANK_H, be.tanks[i]);
        drawFluidBar(graphics, OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[3]);
        drawFluidBar(graphics, COOL_IN_X, COOL_Y, TANK_W, TANK_H, be.coolantTanks[0]);
        drawFluidBar(graphics, COOL_OUT_X, COOL_Y, TANK_W, TANK_H, be.coolantTanks[1]);

        graphics.text(
                font,
                Component.literal(ChatFormatting.AQUA + "/" + HEAT_LIMIT + "K"),
                190,
                32,
                0xFF404040,
                false);
        String label =
                (heat > HEAT_LIMIT ? ChatFormatting.RED : ChatFormatting.AQUA) + "" + heat + "K";
        graphics.text(
                font, Component.literal(label), 220 - font.width(label), 22, 0xFF404040, false);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                be.getMaxPower());
        for (int i = 0; i < 3; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    FUEL_X0 + i * FUEL_STRIDE,
                    TANK_Y,
                    TANK_W,
                    TANK_H,
                    be.tanks[i]);
        }
        drawFluidGaugeInfo(graphics, mouseX, mouseY, OUT_X, TANK_Y, TANK_W, TANK_H, be.tanks[3]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, COOL_IN_X, COOL_Y, TANK_W, TANK_H, be.coolantTanks[0]);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, COOL_OUT_X, COOL_Y, TANK_W, TANK_H, be.coolantTanks[1]);

        if (recipe != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_STAT_X,
                    STAT_Y,
                    STAT_W,
                    STAT_H,
                    List.of(
                            Component.literal(
                                    ChatFormatting.GREEN
                                            + "-> "
                                            + ChatFormatting.RESET
                                            + NumberFormatter.formatCompact(be.klystronEnergy)
                                            + "KyU / "
                                            + NumberFormatter.formatCompact(recipe.ignitionTemp)
                                            + "KyU")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_STAT_X,
                    STAT_Y,
                    STAT_W,
                    STAT_H,
                    List.of(
                            Component.literal(
                                    ChatFormatting.RED
                                            + "<- "
                                            + ChatFormatting.RESET
                                            + NumberFormatter.formatCompact(be.plasmaEnergy)
                                            + "TU / "
                                            + NumberFormatter.formatCompact(recipe.outputTemp)
                                            + "TU")));

            List<Component> fuel = new ArrayList<>(recipe.inputFluid.length);
            for (FluidStackNTM stack : recipe.inputFluid) {
                int consumption = (int) Math.ceil(stack.amount() * be.fuelConsumption);
                fuel.add(
                        Component.literal(
                                ChatFormatting.GREEN
                                        + "-> "
                                        + ChatFormatting.RESET
                                        + consumption
                                        + "mB/t "
                                        + NTMFluidProperties.clientName(stack.type())));
            }
            drawCustomInfoStat(graphics, mouseX, mouseY, FUEL_STAT_X, STAT_Y, STAT_W, STAT_H, fuel);
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_STAT_X,
                    STAT_Y,
                    STAT_W,
                    STAT_H,
                    List.of(Component.literal("0KyU / 0KyU")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_STAT_X,
                    STAT_Y,
                    STAT_W,
                    STAT_H,
                    List.of(Component.literal("0TU / 0TU")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void statusLed(GuiGraphicsExtractor graphics, int x) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, STAT_Y, 246.0F, 14.0F, 8, 8, 256, 256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            FusionTorusBlockEntity be = menu.blockEntity();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    BlueprintsItem.grabPool(be.getItem(FusionTorusBlockEntity.SLOT_BLUEPRINT));
            GUIScreenRecipeSelector.openSelector(
                    FusionRecipes.INSTANCE,
                    be.getBlockPos(),
                    current,
                    0,
                    pool,
                    this,
                    (pos, index, selection) ->
                            PacketWire.sendToServer(
                                    FusionTorusRecipeLayout.encode(menu.containerId, selection)));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        FusionRecipe recipe =
                menu.blockEntity().module.getRecipe() instanceof FusionRecipe f ? f : null;
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
