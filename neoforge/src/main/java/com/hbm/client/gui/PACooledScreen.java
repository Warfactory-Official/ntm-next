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

import com.hbm.blockentity.machine.albion.CooledAcceleratorBlockEntity;
import com.hbm.inventory.container.BlockEntityMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

abstract class PACooledScreen<T extends BlockEntityMenu<? extends CooledAcceleratorBlockEntity>>
        extends InfoContainerScreen<T> {
    protected static final int COLD_ENOUGH = (int) CooledAcceleratorBlockEntity.TEMPERATURE_TARGET;

    protected PACooledScreen(
            T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    protected void drawPowerBar(
            GuiGraphicsExtractor graphics, Identifier texture, int x, long power, long maxPower) {
        int filled = (int) (power * 52 / maxPower);
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    x,
                    70 - filled,
                    184,
                    52 - filled,
                    16,
                    filled,
                    256,
                    256);
        }
    }

    protected void drawCoolant(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CooledAcceleratorBlockEntity be,
            int x) {
        drawFluidBar(graphics, x, 36, 16, 52, be.coolantTanks[0]);
        drawFluidBar(graphics, x + 18, 36, 16, 52, be.coolantTanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, x, 36, 16, 52, be.coolantTanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, x + 18, 36, 16, 52, be.coolantTanks[1]);

        graphics.text(
                this.font,
                Component.literal("/" + COLD_ENOUGH + "K").withStyle(ChatFormatting.AQUA),
                x + 2,
                22,
                CommonColors.BLACK,
                false);

        int heat = (int) Math.ceil(be.temperature);
        Component label =
                Component.literal(heat + "K")
                        .withStyle(heat > COLD_ENOUGH ? ChatFormatting.RED : ChatFormatting.AQUA);
        graphics.text(
                this.font, label, x + 32 - this.font.width(label), 12, CommonColors.BLACK, false);
    }
}
