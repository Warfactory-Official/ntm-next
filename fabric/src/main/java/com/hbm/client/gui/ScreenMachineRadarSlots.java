// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRadarSlots;
import com.hbm.lib.Library;
import com.hbm.main.Polaroid;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRadarSlots extends ScreenInfoContainer<MenuMachineRadarSlots> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_radar_link.png");

    private static final Component POLAROID_TITLE =
            Component.translatable("desc.gui.machineRadarSlots.reda");

    public ScreenMachineRadarSlots(
            MenuMachineRadarSlots menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityMachineRadar be = menu.blockEntity();

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

        if (be.power > 0) {
            int i = (int) (be.power * 160 / be.getMaxPower());
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 64, 0.0F, 185.0F, i, 16, 256, 256);
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, 5, 5, 8, 8, lineArray("radar.toggleGui"));

        if (Polaroid.isBalefireDay()) {
            int tx = titleCenterX() - font.width(POLAROID_TITLE) / 2;
            graphics.text(font, POLAROID_TITLE, tx, titleLabelY, titleColor(), false);
            graphics.text(
                    font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, -12566464, false);
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 5, 5, 8, 8)) {
            playClick();
            CompoundTag data = new CompoundTag();
            data.putBoolean("gui0", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
