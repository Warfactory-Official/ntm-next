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

import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.inventory.container.RBMKOutgasserMenu;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RBMKOutgasserScreen extends InfoContainerScreen<RBMKOutgasserMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_outgasser.png");

    public RBMKOutgasserScreen(RBMKOutgasserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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

        RBMKIrradiationColumnBlockEntity be = be();
        if (be != null) {
            int prog = be.threshold > 0 ? (int) (be.progress * 13 / be.threshold) : 0;
            if (prog > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 50, 176, 0, prog, 6, 256, 256);

            int gas = be.gas.getMaxFill() > 0 ? be.gas.getFill() * 42 / be.gas.getMaxFill() : 0;
            if (gas > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        115,
                        66 - gas,
                        188,
                        42 - gas,
                        10,
                        gas,
                        256,
                        256);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 112, 21, 16, 48, be.gas);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private RBMKIrradiationColumnBlockEntity be() {
        return menu.blockEntity();
    }
}
