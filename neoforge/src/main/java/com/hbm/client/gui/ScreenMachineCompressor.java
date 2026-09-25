// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCompressor;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineCompressor;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineCompressor extends ScreenInfoContainer<MenuMachineCompressor> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_compressor.png");

    private static final int TANK_X0 = 17, TANK_X1 = 107, TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int BTN_X0 = 43, BTN_Y = 46, BTN_W = 8, BTN_H = 14;
    private static final int TIERS = 5;

    public ScreenMachineCompressor(
            MenuMachineCompressor menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
    }

    @Override
    protected int titleColor() {
        return ARGB.opaque(0xC7C1A3);
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

        BlockEntityMachineCompressor be = compressor();

        if (be.power >= be.powerRequirement) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 156, 4, 176, 52, 9, 12, 256, 256);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                BTN_X0 + be.tanks[0].getPressure() * 11,
                46,
                193,
                18,
                8,
                124,
                256,
                256);

        int filled = be.processTime <= 0 ? 0 : be.progress * 55 / be.processTime;
        if (filled > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 42, 26, 192, 0, filled, 17, 256, 256);

        int power = (int) (be.power * POWER_H / BlockEntityMachineCompressor.maxPower);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - power),
                    176,
                    52 - power,
                    POWER_W,
                    power,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X0, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK_X1, TANK_Y, TANK_W, TANK_H, be.tanks[1]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X0, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X1, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                BlockEntityMachineCompressor.maxPower);

        for (int j = 0; j < TIERS; j++) {
            if (checkClick(mouseX, mouseY, BTN_X0 + j * 11, BTN_Y, BTN_W, BTN_H)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        BTN_X0 + j * 11,
                        BTN_Y,
                        BTN_W,
                        BTN_H,
                        List.of(Component.literal(j + " PU -> " + (j + 1) + " PU")));
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            for (int j = 0; j < TIERS; j++) {
                if (!checkClick(mx, my, BTN_X0 + j * 11, BTN_Y, BTN_W, BTN_H)) continue;
                CompoundTag data = new CompoundTag();
                data.putInt("compression", j);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineCompressor compressor() {
        return menu.blockEntity();
    }
}
