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

import com.hbm.blockentity.processing.SolderingBenchBlockEntity;
import com.hbm.inventory.container.SolderingStationMenu;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SolderingStationToggleLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class SolderingStationScreen extends InfoContainerScreen<SolderingStationMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_soldering_station.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int PROG_X = 72,
            PROG_Y = 28,
            PROG_U = 192,
            PROG_V = 0,
            PROG_W = 33,
            PROG_H = 14;
    private static final int TANK_X = 35, TANK_Y = 63, TANK_W = 34, TANK_H = 16;
    private static final int UPGRADE_X = 78, UPGRADE_Y = 67, UPGRADE_W = 8, UPGRADE_H = 8;
    private static final int COLLISION_X = 5, COLLISION_Y = 66, COLLISION_W = 10, COLLISION_H = 10;
    private static final int COLLISION_U = 192, COLLISION_V = 14;
    private static final int READY_X = 156,
            READY_Y = 4,
            READY_U = 176,
            READY_V = 52,
            READY_W = 9,
            READY_H = 12;

    public SolderingStationScreen(SolderingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 18;
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

        if (menu.isCollisionPrevention()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    COLLISION_X,
                    COLLISION_Y,
                    COLLISION_U,
                    COLLISION_V,
                    COLLISION_W,
                    COLLISION_H,
                    256,
                    256);
        }

        long power = menu.getPower();
        long maxPower = menu.getMaxPower();
        if (power > 0 && maxPower > 0) {
            int filled = (int) Math.min(POWER_H, power * POWER_H / maxPower);
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

        int arrow = menu.getProgressScaled(PROG_W);
        if (arrow > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROG_X,
                    PROG_Y,
                    PROG_U,
                    PROG_V,
                    arrow,
                    PROG_H,
                    256,
                    256);
        }

        if (power >= menu.getConsumption()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    READY_X,
                    READY_Y,
                    READY_U,
                    READY_V,
                    READY_W,
                    READY_H,
                    256,
                    256);
        }

        SolderingBenchBlockEntity be = solderer();
        drawFluidBarH(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawInfoPanel(graphics, UPGRADE_X, UPGRADE_Y, UPGRADE_W, UPGRADE_H, 8);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, power, maxPower);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                UPGRADE_X,
                UPGRADE_Y,
                UPGRADE_W,
                UPGRADE_H,
                UPGRADE_X,
                UPGRADE_Y,
                upgradeInfo(be, UpgradeType.SPEED, UpgradeType.POWER, UpgradeType.OVERDRIVE));

        if (checkClick(mouseX, mouseY, COLLISION_X, COLLISION_Y, COLLISION_W, COLLISION_H)) {
            boolean on = menu.isCollisionPrevention();
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    COLLISION_X,
                    COLLISION_Y,
                    COLLISION_W,
                    COLLISION_H,
                    List.of(
                            Component.translatable(
                                            "desc.gui.machineSolderingStation.recipeCollisionPrevention")
                                    .append(
                                            Component.literal(on ? "ON" : "OFF")
                                                    .withStyle(
                                                            on
                                                                    ? ChatFormatting.GREEN
                                                                    : ChatFormatting.RED)),
                            Component.translatable(
                                    "desc.gui.machineSolderingStation.preventsNoFluidRecipes"),
                            Component.translatable(
                                    "desc.gui.machineSolderingStation.whenFluidIsPresent")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        COLLISION_X,
                        COLLISION_Y,
                        COLLISION_W,
                        COLLISION_H)) {
            BlockPos pos = corePos();
            if (pos != null) {
                PacketWire.sendToServer(SolderingStationToggleLayout.encode(menu.containerId));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private SolderingBenchBlockEntity solderer() {
        return menu.blockEntity();
    }
}
