/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.blockentity.machine.HeaterOvenBlockEntity;
import com.hbm.inventory.container.HeaterOvenMenu;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class HeaterOvenScreen extends InfoContainerScreen<HeaterOvenMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_heating_oven.png");

    public HeaterOvenScreen(HeaterOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        HeaterOvenBlockEntity be = oven();

        int heat = be.heatEnergy * 69 / be.getMaxHeat();
        if (heat > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 81, 28, 176, 0, heat, 5, 256, 256);

        int burn = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (burn > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 81, 37, 176, 5, burn, 5, 256, 256);

        if (be.wasOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 25, 26, 176, 10, 18, 18, 256, 256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                80,
                27,
                71,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.ROOT, "%,d", be.heatEnergy)
                                        + " / "
                                        + String.format(Locale.ROOT, "%,d", be.getMaxHeat())
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                80,
                36,
                71,
                7,
                List.of(
                        Component.literal(be.burnHeat + "TU/t"),
                        Component.literal((be.burnTime / SharedConstants.TICKS_PER_SECOND) + "s")));

        if (this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && !this.hoveredSlot.hasItem()
                && (this.hoveredSlot == this.menu.slots.get(0)
                        || this.hoveredSlot == this.menu.slots.get(1))) {
            List<String> bonuses = be.getFuelBonuses();
            if (!bonuses.isEmpty()) {
                List<Component> lines = new ArrayList<>(bonuses.size());
                for (String s : bonuses) lines.add(Component.literal(s));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private HeaterOvenBlockEntity oven() {
        return menu.blockEntity();
    }
}
