// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineBattery;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineBattery extends ScreenInfoContainer<MenuMachineBattery> {

    private static final Identifier TEXTURE = Library.id("textures/gui/storage/gui_battery.png");

    private static final int POWER_X = 62, POWER_Y = 17, POWER_W = 52, POWER_H = 52;
    private static final int LOW_X = 133, LOW_Y = 16, MODE_W = 18, MODE_H = 18;
    private static final int HIGH_X = 133, HIGH_Y = 52;
    private static final int PRIO_X = 152, PRIO_Y = 35, PRIO_W = 16, PRIO_H = 16;

    public ScreenMachineBattery(MenuMachineBattery menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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

        long power = menu.getPower();
        long max = menu.getMaxPower();
        if (power > 0L && max > 0L) {
            int filled = (int) Math.min(POWER_H, menu.getPowerRemainingScaled(POWER_H));
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        176,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
        }

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

        long delta = menu.getDelta();
        String rate = BobMathUtil.getShortNumber(Math.abs(delta)) + "HE/s";
        ChatFormatting color =
                delta > 0
                        ? ChatFormatting.GREEN
                        : delta < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        Component rateLine = Component.literal((delta < 0 ? "-" : "+") + rate).withStyle(color);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                List.of(
                        Component.literal(
                                BobMathUtil.getShortNumber(power)
                                        + "/"
                                        + BobMathUtil.getShortNumber(max)
                                        + "HE"),
                        rateLine));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PRIO_X,
                PRIO_Y,
                PRIO_W,
                PRIO_H,
                priorityInfo(menu.getPriorityOrdinal()));

        super.extractLabels(graphics, mouseX, mouseY);
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
