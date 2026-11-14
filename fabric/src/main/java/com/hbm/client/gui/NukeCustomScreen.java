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

import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.explosion.assembly.ChargeYield;
import com.hbm.inventory.container.NukeCustomMenu;
import com.hbm.util.RegistryUtil;
import com.hbm.util.TooltipStyle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class NukeCustomScreen extends InfoContainerScreen<NukeCustomMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/gun_bomb_schematic.png");

    private static final int PIP_Y = 89, PIP_SIZE = 18;
    private static final int[] PIP_X = {16, 34, 52, 70, 88, 106, 142};
    private static final int[] PIP_V = {0, 18, 36, 54, 72, 90, 108};

    public NukeCustomScreen(NukeCustomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
    }

    public static Component stageHead(String key, float level, float adjusted) {
        return Component.translatable(key + ".head", level, adjusted)
                .withStyle(ChatFormatting.YELLOW);
    }

    private static List<Component> lines(String key) {
        List<Component> out = new ArrayList<>();
        for (Component s : TooltipStyle.lines(Component.translatable(key))) out.add(s);
        return out;
    }

    private static int litStage(ChargeYield bomb) {
        if (bomb.antiMass() > 0) return 6;
        if (bomb.schrabidium() > 0) return 5;
        if (bomb.antimatter() > 0) return 3;
        if (bomb.thermonuclear() > 0) return 2;
        if (bomb.nuclear() > 0) return 1;
        if (bomb.conventional() > 0) return 0;
        return -1;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ChargeYield bomb = menu.blockEntity().snapshot();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        int lit = litStage(bomb);
        if (lit >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + PIP_X[lit],
                    topPos + PIP_Y,
                    176.0F,
                    PIP_V[lit],
                    PIP_SIZE,
                    PIP_SIZE,
                    256,
                    256);
        }
        if (bomb.fallout() > 0
                && bomb.nuclear() > 0
                && bomb.antimatter() == 0
                && bomb.schrabidium() == 0
                && bomb.antiMass() == 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + PIP_X[4],
                    topPos + PIP_Y,
                    176.0F,
                    PIP_V[4],
                    PIP_SIZE,
                    PIP_SIZE,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ChargeYield bomb = menu.blockEntity().snapshot();

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);

        stage(
                graphics,
                mouseX,
                mouseY,
                0,
                "desc.gui.nukeCustom.tnt",
                bomb.conventional(),
                Math.min(bomb.conventional(), ExplosionNukeCustom.MAX_TNT));
        stage(
                graphics,
                mouseX,
                mouseY,
                1,
                "desc.gui.nukeCustom.nuke",
                bomb.nuclear(),
                bomb.adjustedNuclear());
        stage(
                graphics,
                mouseX,
                mouseY,
                2,
                "desc.gui.nukeCustom.hydro",
                bomb.thermonuclear(),
                bomb.adjustedThermonuclear());
        stage(
                graphics,
                mouseX,
                mouseY,
                3,
                "desc.gui.nukeCustom.amat",
                bomb.antimatter(),
                bomb.adjustedAntimatter());
        stage(
                graphics,
                mouseX,
                mouseY,
                4,
                "desc.gui.nukeCustom.dirty",
                bomb.fallout(),
                Math.min(bomb.fallout(), ExplosionNukeCustom.MAX_DIRTY));
        stage(
                graphics,
                mouseX,
                mouseY,
                5,
                "desc.gui.nukeCustom.schrab",
                bomb.schrabidium(),
                bomb.adjustedSchrabidium());

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PIP_X[6],
                PIP_Y,
                PIP_SIZE,
                PIP_SIZE,
                lines("desc.gui.nukeCustom.euph"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void stage(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int pip,
            String key,
            float level,
            float adjusted) {
        List<Component> text = new ArrayList<>();
        text.add(stageHead(key, level, adjusted));
        text.addAll(lines(key));
        drawCustomInfoStat(graphics, mouseX, mouseY, PIP_X[pip], PIP_Y, PIP_SIZE, PIP_SIZE, text);
    }
}
