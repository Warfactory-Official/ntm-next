// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineDiesel;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineDiesel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineDiesel extends ScreenInfoContainer<MenuMachineDiesel> {

    private static final Identifier TEXTURE = Library.id("textures/gui/generators/gui_diesel.png");

    private static final int POWER_X = 141, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int INFO_X = -8, INFO_Y = 36, ERROR_Y = 68, ICON_W = 16, ICON_H = 16;

    public ScreenMachineDiesel(MenuMachineDiesel menu, Inventory inventory, Component title) {
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

        BlockEntityMachineDiesel be = diesel();

        long cap = be.powerCap;
        if (cap > 0 && be.power > 0) {
            int p = (int) (be.power * POWER_H / cap);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        176,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        if (be.isOn)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 79, 61, 192, 16, 35, 14, 256, 256);
        if (be.wasOn)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 89, 42, 192, 0, 16, 16, 256, 256);

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, be.power, cap);

        drawInfoPanel(graphics, INFO_X, INFO_Y, ICON_W, ICON_H, 2);
        if (!be.hasAcceptableFuel()) {
            drawInfoPanel(graphics, INFO_X, ERROR_Y, ICON_W, ICON_H, 6);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                INFO_X,
                INFO_Y,
                ICON_W,
                ICON_H,
                List.of(
                        Component.translatable("desc.gui.machineDiesel.fuelConsumptionRate"),
                        Component.literal("  1 mB/t"),
                        Component.literal("  20 mB/s"),
                        Component.translatable(
                                "desc.gui.machineDiesel.consumptionRateIsConstant")));

        if (!be.hasAcceptableFuel()) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    INFO_X,
                    ERROR_Y,
                    ICON_W,
                    ICON_H,
                    List.of(
                            Component.translatable("desc.gui.machineDiesel.errorTheCurrentlySet"),
                            Component.translatable("desc.gui.machineDiesel.isNotSupportedBy")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (x >= 89 && x < 105 && y > 61 && y <= 75) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("turnOn", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineDiesel diesel() {
        return menu.blockEntity();
    }
}
