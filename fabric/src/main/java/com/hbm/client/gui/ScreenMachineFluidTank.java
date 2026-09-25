// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineFluidTank;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFluidTank;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineFluidTank extends ScreenInfoContainer<MenuMachineFluidTank> {

    private static final Identifier TEXTURE = Library.id("textures/gui/storage/gui_tank.png");

    private static final String[] MODE_KEYS = {
        "desc.shared.tankMode.inputOnly",
        "desc.shared.tankMode.buffer",
        "desc.shared.tankMode.outputOnly",
        "desc.shared.disabled"
    };

    private static final int BAR_X = 71, BAR_Y = 17, BAR_W = 34, BAR_H = 52;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    private static final int BTN_HIT_Y = 35;

    public ScreenMachineFluidTank(
            MenuMachineFluidTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

        BlockEntityMachineFluidTank be = tank();
        drawFluidBar(graphics, BAR_X, BAR_Y, BAR_W, BAR_H, be.tank);
        int mode = Mth.positiveModulo(be.mode, BlockEntityMachineFluidTank.MODES);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                BTN_X,
                BTN_Y,
                176,
                mode * BTN_H,
                BTN_W,
                BTN_H,
                256,
                256);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, BAR_X, BAR_Y, BAR_W, BAR_H, be.tank);

        if (checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BTN_X,
                    BTN_Y,
                    BTN_W,
                    BTN_H,
                    List.of(
                            Component.translatable(
                                    "desc.shared.mode", Component.translatable(MODE_KEYS[mode]))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, BTN_X, BTN_HIT_Y, BTN_W, BTN_H)) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("mode", true);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineFluidTank tank() {
        return menu.blockEntity();
    }
}
