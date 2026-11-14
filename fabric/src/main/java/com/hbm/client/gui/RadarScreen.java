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

import com.hbm.api.entity.RadarEntry;
import com.hbm.blockentity.machine.RadarBlockEntity;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.RadarMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadarDispatchEntityLayout;
import com.hbm.packet.layout.RadarDispatchPositionLayout;
import com.hbm.packet.layout.RadarOpenSlotsLayout;
import com.hbm.packet.layout.RadarSettingsLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

public class RadarScreen extends InfoContainerScreen<RadarMenu> {

    public static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_radar_nt.png");

    private static final int BUTTON_X = -10;
    private static final String[] BUTTON_COMMAND = {
        "missiles", "shells", "players", "smart", "red", "map", "gui1", "clear"
    };
    private static final int[] BUTTON_Y = {88, 98, 108, 118, 128, 138, 158, 178};
    private static final String[] BUTTON_TOOLTIP = {
        "radar.detectMissiles", "radar.detectShells", "radar.detectPlayers", "radar.smartMode",
        "radar.redMode", "radar.showMap", "radar.toggleGui", "radar.clearMap"
    };
    private static final int LAMP_COUNT = 6;

    private static final double SCOPE_SPAN = 200D - 8D;
    private static final double SCOPE_REVERSE_SPAN = 192D;

    private int lastMouseX;
    private int lastMouseY;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 216, 234);
    }

    private RadarBlockEntity be() {
        return menu.blockEntity();
    }

    private static double blipOffset(int delta, int range) {
        return delta / ((double) range * 2 + 1) * SCOPE_SPAN;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        RadarBlockEntity be = be();
        drawBackground(graphics, be);
        drawTooltips(graphics, be, mouseX, mouseY);
    }

    private void drawBackground(GuiGraphicsExtractor graphics, RadarBlockEntity be) {
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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, -14, 84, 224.0F, 0.0F, 14, 66, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, -14, 154, 224.0F, 66.0F, 14, 36, 256, 256);

        if (be.power > 0) {
            int i = (int) (be.power * 200 / be.getMaxPower());
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 221, 0.0F, 234.0F, i, 16, 256, 256);
        }

        RandomSource random = be.getLevel().getRandom();
        boolean[] lamps = {
            be.scanMissiles, be.scanShells, be.scanPlayers, be.smartMode, be.redMode, be.showMap
        };
        for (int i = 0; i < LAMP_COUNT; i++) {
            if (lamps[i] == (be.jammed && random.nextBoolean())) continue;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BUTTON_X,
                    BUTTON_Y[i],
                    238.0F,
                    4.0F + i * 10,
                    8,
                    8,
                    256,
                    256);
        }

        if (be.power < MachineData.RADAR_CONSUMPTION.get()) return;

        if (be.jammed) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            8 + i * 40,
                            17 + j * 40,
                            216.0F,
                            118.0F + random.nextInt(81),
                            40,
                            40,
                            256,
                            256);
                }
            }
            return;
        }

        if (be.showMap) {
            RadarScopeElements.map(graphics, be.map, RadarBlockEntity.MAP_SIDE);
        }

        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        RadarScopeElements.sweep(
                graphics, be.prevRotation + (be.rotation - be.prevRotation) * partial);

        int range = be.getRange();
        for (RadarEntry m : be.entries) {
            double x = blipOffset(m.posX - be.getBlockPos().getX(), range) - 4D;
            double z = blipOffset(m.posZ - be.getBlockPos().getZ(), range) - 4D;
            graphics.pose().pushMatrix();
            graphics.pose()
                    .translate(
                            (float) (RadarScopeElements.SCOPE_X + x),
                            (float) (RadarScopeElements.SCOPE_Y + z));
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    0,
                    0,
                    216.0F,
                    8.0F * m.blipLevel,
                    8,
                    8,
                    256,
                    256);
            graphics.pose().popMatrix();
        }
    }

    private void drawTooltips(
            GuiGraphicsExtractor graphics, RadarBlockEntity be, int mouseX, int mouseY) {
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 221, 200, 7, be.power, be.getMaxPower());

        for (int i = 0; i < BUTTON_Y.length; i++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BUTTON_X,
                    BUTTON_Y[i],
                    8,
                    8,
                    lineArray(BUTTON_TOOLTIP[i]));
        }

        int range = be.getRange();
        for (RadarEntry m : be.entries) {
            int x =
                    leftPos
                            + (int) blipOffset(m.posX - be.getBlockPos().getX(), range)
                            + RadarScopeElements.SCOPE_X;
            int z =
                    topPos
                            + (int) blipOffset(m.posZ - be.getBlockPos().getZ(), range)
                            + RadarScopeElements.SCOPE_Y;

            if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(
                                Component.translatable(m.getRadarName),
                                Component.literal(m.posX + " / " + m.posZ),
                                Component.translatable("desc.gui.machineRadar.alt", m.posY)),
                        x,
                        z);
                return;
            }
        }

        if (checkClick(mouseX, mouseY, 8, 17, 200, 200)) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.literal(targetX(be) + " / " + targetZ(be))),
                    mouseX,
                    mouseY);
        }
    }

    private int targetX(RadarBlockEntity be) {
        return (int)
                ((lastMouseX - leftPos - RadarScopeElements.SCOPE_X)
                                * ((double) be.getRange() * 2 + 1)
                                / SCOPE_REVERSE_SPAN
                        + be.getBlockPos().getX());
    }

    private int targetZ(RadarBlockEntity be) {
        return (int)
                ((lastMouseY - topPos - RadarScopeElements.SCOPE_Y)
                                * ((double) be.getRange() * 2 + 1)
                                / SCOPE_REVERSE_SPAN
                        + be.getBlockPos().getZ());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < BUTTON_Y.length; i++) {
                if (!checkClick((int) event.x(), (int) event.y(), BUTTON_X, BUTTON_Y[i], 8, 8))
                    continue;
                playClick();
                switch (BUTTON_COMMAND[i]) {
                    case "missiles" -> sendSettings(true, false, false, false, false, false, false);
                    case "shells" -> sendSettings(false, true, false, false, false, false, false);
                    case "players" -> sendSettings(false, false, true, false, false, false, false);
                    case "smart" -> sendSettings(false, false, false, true, false, false, false);
                    case "red" -> sendSettings(false, false, false, false, true, false, false);
                    case "map" -> sendSettings(false, false, false, false, false, true, false);
                    case "gui1" ->
                            PacketWire.sendToServer(RadarOpenSlotsLayout.encode(menu.containerId));
                    case "clear" -> sendSettings(false, false, false, false, false, false, true);
                    default -> throw new IllegalArgumentException(BUTTON_COMMAND[i]);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int slot = event.key() - 49;
        if (slot < 0
                || slot >= RadarBlockEntity.SLOT_LINK_COUNT
                || !checkClick(lastMouseX, lastMouseY, 8, 17, 200, 200)) {
            return super.keyPressed(event);
        }

        RadarBlockEntity be = be();
        int range = be.getRange();
        for (RadarEntry m : be.entries) {
            int x =
                    leftPos
                            + (int) blipOffset(m.posX - be.getBlockPos().getX(), range)
                            + RadarScopeElements.SCOPE_X;
            int z =
                    topPos
                            + (int) blipOffset(m.posZ - be.getBlockPos().getZ(), range)
                            + RadarScopeElements.SCOPE_Y;

            if (lastMouseX + 5 > x
                    && lastMouseX - 4 <= x
                    && lastMouseY + 5 > z
                    && lastMouseY - 4 <= z) {
                PacketWire.sendToServer(
                        RadarDispatchEntityLayout.encode(menu.containerId, slot, m.entityID));
                return true;
            }
        }

        PacketWire.sendToServer(
                RadarDispatchPositionLayout.encode(
                        menu.containerId, slot, targetX(be), targetZ(be)));
        return true;
    }

    private void sendSettings(
            boolean missiles,
            boolean shells,
            boolean players,
            boolean smart,
            boolean red,
            boolean map,
            boolean clear) {
        PacketWire.sendToServer(
                RadarSettingsLayout.encode(
                        menu.containerId, missiles, shells, players, smart, red, map, clear));
    }
}
