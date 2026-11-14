/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blockentity.machine.CustomMachineBlockEntity;
import com.hbm.inventory.container.CustomMenu;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CustomScreen extends InfoContainerScreen<CustomMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_custom.png");

    private static final int POWER_X = 150, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 34;
    private static final int IN_X0 = 8, OUT_X0 = 78, STRIDE = 18;
    private static final int HEAT_X = 61, HEAT_Y = 53, HEAT_SIZE = 18;
    private static final int ARROW_SPAN = 90, ARROW_FIRST = 44;

    public CustomScreen(CustomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 68;
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

        CustomMachineBlockEntity be = menu.blockEntity();
        CustomMachineDefinition definition = be.definition;
        if (definition == null) {
            super.extractLabels(graphics, mouseX, mouseY);
            return;
        }

        if (definition.fluxMode()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    78,
                    54,
                    192.0F,
                    122.0F,
                    51,
                    15,
                    256,
                    256);
        }
        if (definition.maxHeat() > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    HEAT_Y,
                    236.0F,
                    0.0F,
                    HEAT_SIZE,
                    HEAT_SIZE,
                    256,
                    256);
            SmoothGaugeElement.draw(
                    graphics,
                    70,
                    62,
                    (double) be.heat / definition.maxHeat(),
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);
        }

        int arrow = be.progress * ARROW_SPAN / Math.max(be.progressLimit, 1);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                78,
                119,
                192.0F,
                0.0F,
                Math.min(arrow, ARROW_FIRST),
                16,
                256,
                256);
        if (arrow > ARROW_FIRST) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    78 + ARROW_FIRST,
                    119,
                    192.0F,
                    16.0F,
                    arrow - ARROW_FIRST,
                    16,
                    256,
                    256);
        }

        int filled = (int) (be.energy * POWER_H / Math.max(definition.maxPower(), 1L));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                POWER_X,
                POWER_Y + POWER_H - filled,
                176.0F,
                POWER_H - filled,
                POWER_W,
                filled,
                256,
                256);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (definition.itemInCount() <= index) {
                    plate(graphics, 7 + col * STRIDE, 71 + row * STRIDE, col, row);
                    plate(graphics, 7 + col * STRIDE, 107 + row * STRIDE, col, row);
                }
                if (definition.itemOutCount() <= index) {
                    plate(graphics, 77 + col * STRIDE, 71 + row * STRIDE, col, row);
                }
            }
        }
        for (int i = 0; i < CustomMachineBlockEntity.FLUID_COLUMNS; i++) {
            if (definition.fluidInCount() <= i) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        7 + i * STRIDE,
                        17,
                        192.0F + i * STRIDE,
                        32.0F,
                        18,
                        54,
                        256,
                        256);
            }
            if (definition.fluidOutCount() <= i) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        77 + i * STRIDE,
                        17,
                        192.0F + i * STRIDE,
                        32.0F,
                        18,
                        36,
                        256,
                        256);
            }
        }

        for (int i = 0; i < be.fluidInputs.length; i++) {
            drawFluidBar(graphics, IN_X0 + i * STRIDE, TANK_TOP, TANK_W, TANK_H, be.fluidInputs[i]);
        }
        for (int i = 0; i < be.fluidOutputs.length; i++) {
            drawFluidBar(
                    graphics, OUT_X0 + i * STRIDE, TANK_TOP, TANK_W, TANK_H, be.fluidOutputs[i]);
        }

        if (definition.fluxMode()) {
            graphics.text(
                    this.font,
                    Component.translatable("gui.customMachine.flux", be.flux),
                    83,
                    57,
                    0xFF08FF00,
                    false);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.energy,
                definition.maxPower());
        if (definition.maxHeat() > 0) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    HEAT_X,
                    HEAT_Y,
                    HEAT_SIZE,
                    HEAT_SIZE,
                    List.of(
                            Component.translatable(
                                    "gui.customMachine.heat",
                                    grouped(be.heat),
                                    grouped(definition.maxHeat()))));
        }
        for (int i = 0; i < be.fluidInputs.length; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_X0 + i * STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.fluidInputs[i]);
        }
        for (int i = 0; i < be.fluidOutputs.length; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_X0 + i * STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.fluidOutputs[i]);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        if (!(slot instanceof SlotPattern)) return List.of();
        String mode =
                menu.blockEntity()
                        .patterns
                        .mode(slot.getContainerSlot() - CustomMachineBlockEntity.TEMPLATE_START);
        if (mode == null) return List.of();
        return List.of(
                Component.translatable("gui.customMachine.pattern").withStyle(ChatFormatting.RED),
                ModulePatternMatcher.getLabel(mode));
    }

    private static Component grouped(int value) {
        return Component.literal(String.format(Locale.ROOT, "%,d", value));
    }

    private void plate(GuiGraphicsExtractor graphics, int x, int y, int col, int row) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                192.0F + col * STRIDE,
                86.0F + row * STRIDE,
                18,
                18,
                256,
                256);
    }
}
