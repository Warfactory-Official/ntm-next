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

import com.hbm.blockentity.machine.thermal.CombinationFurnaceBlockEntity;
import com.hbm.inventory.container.FurnaceCombinationMenu;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FurnaceCombinationScreen extends InfoContainerScreen<FurnaceCombinationMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/processing/gui_furnace_combination.png");

    public FurnaceCombinationScreen(
            FurnaceCombinationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        CombinationFurnaceBlockEntity be = menu.blockEntity();

        int p = be.progress * 38 / CombinationFurnaceBlockEntity.PROCESS_TIME;
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 37, 176, 0, p, 5, 256, 256);
        int h = be.heat * 37 / CombinationFurnaceBlockEntity.MAX_HEAT;
        if (h > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 46, 176, 5, h, 5, 256, 256);

        drawFluidBar(graphics, 118, 18, 16, 52, be.tank);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 118, 18, 16, 52, be.tank);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                44,
                36,
                39,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.ROOT, "%,d", be.progress)
                                        + " / "
                                        + String.format(
                                                Locale.ROOT,
                                                "%,d",
                                                CombinationFurnaceBlockEntity.PROCESS_TIME)
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                44,
                45,
                39,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.ROOT, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.ROOT,
                                                "%,d",
                                                CombinationFurnaceBlockEntity.MAX_HEAT)
                                        + "TU")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
