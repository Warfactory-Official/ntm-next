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

import com.hbm.blockentity.ordnance.launch.LaunchTableBlockEntity;
import com.hbm.inventory.container.LaunchTableMenu;
import com.hbm.items.weapon.CustomMissileItem;
import com.hbm.items.weapon.CustomMissilePartItem.PartSize;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.LaunchTablePadSizeLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class LaunchTableScreen extends InfoContainerScreen<LaunchTableMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/gui_launch_table.png");

    private static final int POWER_X = 134, POWER_Y = 113, POWER_W = 34, POWER_H = 6;
    private static final int SOLID_X = 152, SOLID_BOTTOM = 88, SOLID_H = 52, SOLID_W = 16;
    private static final int FUEL_X = 116, OX_X = 134, TANK_Y = 36, TANK_W = 16, TANK_H = 34;
    private static final int PIP_Y = 23, PIP_W = 6, PIP_H = 8;
    private static final int LIQUID_PIP_X = 121, OX_PIP_X = 139, SOLID_PIP_X = 157;
    private static final int GREEN_U = 176, RED_U = 182;
    private static final int CHECK_U = 176, CHECK_V = 26, CHECK_SIZE = 18;
    private static final int MISSILE_CHECK_X = 25, MISSILE_CHECK_Y = 35;
    private static final int DESIGNATOR_CHECK_Y = 71;
    private static final int BUTTON_Y = 98, BUTTON_SIZE = 18, BUTTON_V = 8;
    private static final int BUTTON_10_X = 7, BUTTON_15_X = 25, BUTTON_20_X = 43;
    private static final int PANEL_X = -16, PANEL_Y = 36, PANEL_SIZE = 16;

    private final CustomMissilePreview preview = new CustomMissilePreview();

    public LaunchTableScreen(LaunchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private LaunchTableBlockEntity table() {
        return menu.blockEntity();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        LaunchTableBlockEntity table = table();
        ItemStack missile = menu.getSlot(LaunchTableBlockEntity.MISSILE_SLOT).getItem();
        ItemStack designator = menu.getSlot(LaunchTableBlockEntity.DESIGNATOR_SLOT).getItem();

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

        int power = (int) (table.storedEnergy * POWER_W / LaunchTableBlockEntity.ENERGY_CAPACITY);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y,
                    176,
                    96,
                    power,
                    POWER_H,
                    256,
                    256);
        }

        int solid = table.solidPropellant * SOLID_H / LaunchTableBlockEntity.SOLID_CAPACITY;
        if (solid > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    SOLID_X,
                    SOLID_BOTTOM - solid,
                    176,
                    96 - solid,
                    SOLID_W,
                    solid,
                    256,
                    256);
        }

        if (table.acceptsAssembly(missile)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    MISSILE_CHECK_X,
                    MISSILE_CHECK_Y,
                    CHECK_U,
                    CHECK_V,
                    CHECK_SIZE,
                    CHECK_SIZE,
                    256,
                    256);
        }
        if (LaunchTableBlockEntity.designatorReady(designator)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    MISSILE_CHECK_X,
                    DESIGNATOR_CHECK_Y,
                    CHECK_U,
                    CHECK_V,
                    CHECK_SIZE,
                    CHECK_SIZE,
                    256,
                    256);
        }

        pip(graphics, LIQUID_PIP_X, table.propellantStatus(missile));
        pip(graphics, OX_PIP_X, table.oxidizerStatus(missile));
        pip(graphics, SOLID_PIP_X, table.solidStatus(missile));

        if (table.selectedSize == PartSize.SIZE_10) pressed(graphics, BUTTON_10_X, 176);
        else if (table.selectedSize == PartSize.SIZE_15) pressed(graphics, BUTTON_15_X, 194);
        else if (table.selectedSize == PartSize.SIZE_20) pressed(graphics, BUTTON_20_X, 212);

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 2);
        drawInfoPanel(graphics, PANEL_X, PANEL_Y + PANEL_SIZE, PANEL_SIZE, PANEL_SIZE, 11);

        drawFluidBar(graphics, FUEL_X, TANK_Y, TANK_W, TANK_H, table.propellantTank);
        drawFluidBar(graphics, OX_X, TANK_Y, TANK_W, TANK_H, table.oxidizerTank);

        if (table.acceptsAssembly(missile))
            preview.draw(this, graphics, CustomMissileItem.getStruct(missile));

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, FUEL_X, TANK_Y, TANK_W, TANK_H, table.propellantTank);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, OX_X, TANK_Y, TANK_W, TANK_H, table.oxidizerTank);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                SOLID_X,
                SOLID_BOTTOM - SOLID_H,
                SOLID_W,
                SOLID_H,
                List.of(
                        Component.translatable(
                                "desc.gui.launchTable.solidFuel", table.solidPropellant + "l")));
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                table.storedEnergy,
                LaunchTableBlockEntity.ENERGY_CAPACITY);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_10_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size101015")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_15_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size151520")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_20_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size20")));

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_X + 8,
                PANEL_Y + PANEL_SIZE,
                List.of(
                        Component.translatable("desc.gui.launchTable.acceptsCustomMissiles"),
                        Component.translatable("desc.gui.launchTable.ofAllSizesAs"),
                        Component.translatable("desc.gui.launchTable.correctSizeSettingIs")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y + PANEL_SIZE,
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_X + 8,
                PANEL_Y + PANEL_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.detonatorCanOnlyTrigger")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void pressed(GuiGraphicsExtractor graphics, int x, int u) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                BUTTON_Y,
                u,
                BUTTON_V,
                BUTTON_SIZE,
                BUTTON_SIZE,
                256,
                256);
    }

    private void pip(GuiGraphicsExtractor graphics, int x, int state) {
        if (state < 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                PIP_Y,
                state == 1 ? GREEN_U : RED_U,
                0,
                PIP_W,
                PIP_H,
                256,
                256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (press(event, BUTTON_10_X, PartSize.SIZE_10)) return true;
        if (press(event, BUTTON_15_X, PartSize.SIZE_15)) return true;
        if (press(event, BUTTON_20_X, PartSize.SIZE_20)) return true;
        return handled;
    }

    private boolean press(MouseButtonEvent event, int buttonX, PartSize size) {
        if (!checkClick(
                (int) event.x(), (int) event.y(), buttonX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
            return false;
        }
        playClick();
        PacketWire.sendToServer(LaunchTablePadSizeLayout.encode(menu.containerId, size.ordinal()));
        return true;
    }
}
