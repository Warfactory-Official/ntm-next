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

import com.hbm.blockentity.machine.CombustionEngineBlockEntity;
import com.hbm.inventory.container.CombustionEngineMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.items.PistonsItem.EnumPistonType;
import com.hbm.items.PistonsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CombustionEngineSettingLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class CombustionEngineScreen extends InfoContainerScreen<CombustionEngineMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/generators/gui_combustion.png");

    private static final int POWER_X = 143, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 16, TANK_H = 52;

    private int setting;
    private boolean mouseLocked = false;

    public CombustionEngineScreen(CombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.setting = menu.blockEntity().setting;
    }

    @Override
    protected int titleColor() {
        return 0xFF404040;
    }

    @Override
    protected boolean drawTitle() {
        return false;
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

        CombustionEngineBlockEntity be = engine();

        if (mouseLocked) {
            int dragged = Mth.clamp((mouseX - leftPos - 81) * 30 / 32, 0, 30);
            if (dragged != this.setting) {
                this.setting = dragged;
                sendSetting(dragged);
            }
        }

        ItemStack pistonStack = be.getItem(CombustionEngineBlockEntity.SLOT_PISTON);
        boolean hasPiston = pistonStack.getItem() instanceof PistonsItem;
        if (hasPiston) {
            int variant = ((PistonsItem) pistonStack.getItem()).type.ordinal();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    80,
                    51,
                    176,
                    52 + variant * 12,
                    25,
                    12,
                    256,
                    256);
        }
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                79 + (setting * 32 / 30),
                38,
                192,
                15,
                4,
                8,
                256,
                256);
        if (be.isOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 79, 13, 192, 0, 35, 15, 256, 256);
        }

        if (be.power > 0) {
            int p = (int) (be.power * POWER_H / CombustionEngineBlockEntity.maxPower);
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

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        if (!mouseLocked) {
            drawElectricityInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    POWER_X,
                    POWER_Y,
                    POWER_W,
                    POWER_H,
                    be.power,
                    CombustionEngineBlockEntity.maxPower);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        }

        if (mouseLocked || checkClick(mouseX, mouseY, 80, 38, 34, 8)) {
            graphics.setComponentTooltipForNextFrame(
                    this.font,
                    List.of(Component.literal(((setting * 2) / 10D) + " mB/t")),
                    mouseX,
                    mouseY);
        }

        if (hasPiston) {
            Fluid type = be.tank.getTankType();
            CombustibleTrait trait = NTMFluidProperties.getTrait(type, CombustibleTrait.class);
            double he = 0;
            if (trait != null) {
                EnumPistonType piston = ((PistonsItem) pistonStack.getItem()).type;
                he =
                        setting
                                * 0.2
                                * trait.energyPerBucket()
                                / 1_000D
                                * piston.eff[trait.grade().ordinal()];
            }
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    79,
                    50,
                    35,
                    14,
                    List.of(
                            Component.literal(
                                            String.format(Locale.ROOT, "%,d", (long) he) + " HE/t")
                                    .withStyle(ChatFormatting.YELLOW),
                            Component.literal(
                                            String.format(
                                                            Locale.ROOT,
                                                            "%,d",
                                                            (long)
                                                                    (he
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND))
                                                    + " HE/s")
                                    .withStyle(ChatFormatting.YELLOW)));
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                79,
                13,
                35,
                15,
                List.of(Component.translatable("desc.gui.machineCombustionEngine.ignition")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, 89, 13, 16, 14)) {
                sendPower();
                playClick();
                return true;
            }
            if (checkClick(mx, my, 79, 38, 36, 8)) {
                mouseLocked = true;
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseLocked) {
            mouseLocked = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void sendSetting(int value) {
        PacketWire.sendToServer(
                CombustionEngineSettingLayout.encode(menu.containerId, false, true, value));
    }

    private void sendPower() {
        PacketWire.sendToServer(
                CombustionEngineSettingLayout.encode(menu.containerId, true, false, 0));
    }

    private CombustionEngineBlockEntity engine() {
        return menu.blockEntity();
    }
}
