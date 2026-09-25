// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineMiningLaser;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineMiningLaser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineMiningLaser extends ScreenInfoContainer<MenuMachineMiningLaser> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_laser_miner.png");

    private static final int TOGGLE_X = 61, TOGGLE_Y = 17, TOGGLE_W = 18, TOGGLE_H = 18;
    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 88;
    private static final int TANK_X = 35, TANK_Y = 72, TANK_W = 7, TANK_H = 52;
    private static final int PANEL_X = 87, PANEL_Y = 31, PANEL_W = 8, PANEL_H = 8;

    public ScreenMachineMiningLaser(
            MenuMachineMiningLaser menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 4;
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

        BlockEntityMachineMiningLaser be = laser();

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    200,
                    0,
                    TOGGLE_W,
                    TOGGLE_H,
                    256,
                    256);
        }

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    106 - p,
                    176,
                    88 - p,
                    16,
                    p,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 8);

        String width = String.valueOf(be.getWidth());
        graphics.text(this.font, width, 43 - this.font.width(width) / 2, 26, 0xFFFFFFFF, false);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                BlockEntityMachineMiningLaser.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                141,
                55,
                lineArray("desc.gui.miningLaser.upgrades"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(laser().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineMiningLaser laser() {
        return menu.blockEntity();
    }
}
