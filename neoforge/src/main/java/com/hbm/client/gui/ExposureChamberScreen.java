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

import com.hbm.blockentity.machine.ExposureChamberBlockEntity;
import com.hbm.inventory.container.ExposureChamberMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ExposureChamberScreen extends InfoContainerScreen<ExposureChamberMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_exposure_chamber.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 34;
    private static final int PARTICLE_X = 26, PARTICLE_Y = 36, PARTICLE_W = 9, PARTICLE_H = 16;
    private static final int PROGRESS_X = 36, PROGRESS_Y = 39, PROGRESS_W = 42, PROGRESS_H = 10;
    private static final int LAMP_X = 156, LAMP_Y = 4, LAMP_W = 9, LAMP_H = 12;

    public ExposureChamberScreen(ExposureChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        ExposureChamberBlockEntity be = menu.blockEntity();

        int p = be.work * PROGRESS_W / (be.duration + 1);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_X,
                    PROGRESS_Y,
                    192,
                    0,
                    p,
                    PROGRESS_H,
                    256,
                    256);
        }

        int c = be.charges * PARTICLE_H / ExposureChamberBlockEntity.CHARGES_PER_CAPSULE;
        if (c > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PARTICLE_X,
                    PARTICLE_Y + PARTICLE_H - c,
                    192,
                    26 - c,
                    PARTICLE_W,
                    c,
                    256,
                    256);
        }

        int e = (int) (be.energy * POWER_H / ExposureChamberBlockEntity.ENERGY_CAPACITY);
        if (e > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + POWER_H - e,
                    176,
                    POWER_H - e,
                    POWER_W,
                    e,
                    256,
                    256);
        }

        if (be.tickCost <= be.energy) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LAMP_X,
                    LAMP_Y,
                    176,
                    34,
                    LAMP_W,
                    LAMP_H,
                    256,
                    256);
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
                ExposureChamberBlockEntity.ENERGY_CAPACITY);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PARTICLE_X,
                PARTICLE_Y,
                PARTICLE_W,
                PARTICLE_H,
                List.of(
                        Component.literal(
                                be.charges
                                        + " / "
                                        + ExposureChamberBlockEntity.CHARGES_PER_CAPSULE)));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
