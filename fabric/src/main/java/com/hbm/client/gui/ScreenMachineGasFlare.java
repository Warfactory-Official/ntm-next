// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineGasFlare;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.oil.BlockEntityMachineGasFlare;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineGasFlare extends ScreenInfoContainer<MenuMachineGasFlare> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/generators/gui_flare_stack.png");

    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int POWER_X = 143, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int VALVE_X = 79, VALVE_Y = 15, VALVE_W = 35, VALVE_H = 10;
    private static final int DIAL_X = 79, DIAL_Y = 49, DIAL_W = 35, DIAL_H = 14;
    private static final int VALVE_HIT_Y = 16, DIAL_HIT_Y = 50;
    private static final int VALVE_BTN_X = 89, VALVE_BTN_Y = 16, VALVE_BTN_W = 16, VALVE_BTN_H = 10;
    private static final int DIAL_BTN_X = 89, DIAL_BTN_Y = 50, DIAL_BTN_W = 16, DIAL_BTN_H = 14;

    public ScreenMachineGasFlare(MenuMachineGasFlare menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityMachineGasFlare be = flare();

        int power = (int) (be.power * POWER_H / BlockEntityMachineGasFlare.maxPower);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - power),
                    176,
                    94 - power,
                    POWER_W,
                    power,
                    256,
                    256);
        }

        if (be.isOn)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    VALVE_X,
                    VALVE_Y,
                    176,
                    0,
                    VALVE_W,
                    VALVE_H,
                    256,
                    256);
        if (be.doesBurn)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    DIAL_X,
                    DIAL_Y,
                    176,
                    10,
                    DIAL_W,
                    DIAL_H,
                    256,
                    256);

        if (be.isOn
                && be.doesBurn
                && be.tank.getFill() > 0
                && NTMFluidProperties.hasTrait(be.tank.getTankType(), FT_Flammable.class)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 29, 176, 24, 18, 18, 256, 256);
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                BlockEntityMachineGasFlare.maxPower);

        if (checkClick(mouseX, mouseY, VALVE_X, VALVE_HIT_Y, VALVE_W, VALVE_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    VALVE_X,
                    VALVE_HIT_Y,
                    VALVE_W,
                    VALVE_H,
                    List.of(Component.translatable("flare.valve")));
        }
        if (checkClick(mouseX, mouseY, DIAL_X, DIAL_HIT_Y, DIAL_W, DIAL_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    DIAL_X,
                    DIAL_HIT_Y,
                    DIAL_W,
                    DIAL_H,
                    List.of(Component.translatable("flare.ignition")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            String key = null;
            if (checkClick(mx, my, VALVE_BTN_X, VALVE_BTN_Y, VALVE_BTN_W, VALVE_BTN_H))
                key = "valve";
            else if (checkClick(mx, my, DIAL_BTN_X, DIAL_BTN_Y, DIAL_BTN_W, DIAL_BTN_H))
                key = "dial";

            if (key != null) {
                CompoundTag data = new CompoundTag();
                data.putBoolean(key, true);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineGasFlare flare() {
        return menu.blockEntity();
    }
}
