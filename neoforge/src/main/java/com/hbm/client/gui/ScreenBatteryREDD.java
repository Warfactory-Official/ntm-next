// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuBatteryREDD;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import java.math.BigInteger;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenBatteryREDD extends ScreenInfoContainer<MenuBatteryREDD> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_battery_redd.png");

    private static final int LOW_X = 133, LOW_Y = 16, MODE_W = 18, MODE_H = 18;
    private static final int HIGH_X = 133, HIGH_Y = 52;
    private static final int PRIO_X = 152, PRIO_Y = 35, PRIO_W = 16, PRIO_H = 16;

    public ScreenBatteryREDD(MenuBatteryREDD menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 181);
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

        BlockEntityBatteryREDD battery = menu.blockEntity();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                LOW_X,
                LOW_Y,
                176,
                52 + menu.getRedLow() * MODE_H,
                MODE_W,
                MODE_H,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                HIGH_X,
                HIGH_Y,
                176,
                52 + menu.getRedHigh() * MODE_H,
                MODE_W,
                MODE_H,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                PRIO_X,
                PRIO_Y,
                194,
                52 + (menu.getPriorityOrdinal() - 1) * PRIO_H,
                PRIO_W,
                PRIO_H,
                256,
                256);

        String charge = String.format(Locale.US, "%,d", battery.bigPower) + " HE";
        drawHalfScaleRight(graphics, Component.literal(charge), 45);

        int sign = battery.bigDelta.compareTo(BigInteger.ZERO);
        String rate = String.format(Locale.US, "%,d", battery.bigDelta) + " HE/s";
        ChatFormatting colour =
                sign > 0
                        ? ChatFormatting.GREEN
                        : sign < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        drawHalfScaleRight(
                graphics, Component.literal(sign < 0 ? rate : "+" + rate).withStyle(colour), 65);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawHalfScaleRight(GuiGraphicsExtractor graphics, Component line, int y) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(font, line, 242 - font.width(line), y, 0xFF00FF00, false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            CompoundTag data = new CompoundTag();
            if (checkClick(mx, my, LOW_X, LOW_Y, MODE_W, MODE_H)) data.putBoolean("redLow", true);
            if (checkClick(mx, my, HIGH_X, HIGH_Y, MODE_W, MODE_H))
                data.putBoolean("redHigh", true);
            if (checkClick(mx, my, PRIO_X, PRIO_Y, PRIO_W, PRIO_H))
                data.putBoolean("priority", true);
            if (!data.isEmpty()) {
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
