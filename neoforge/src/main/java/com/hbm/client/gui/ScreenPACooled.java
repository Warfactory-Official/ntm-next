// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.BlockEntityMenu;
import com.hbm.tileentity.machine.albion.BlockEntityCooledBase;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

abstract class ScreenPACooled<T extends BlockEntityMenu<? extends BlockEntityCooledBase>>
        extends ScreenInfoContainer<T> {

    protected static final int COLD_ENOUGH = (int) BlockEntityCooledBase.TEMPERATURE_TARGET;

    protected ScreenPACooled(
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
            BlockEntityCooledBase be,
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
