// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuReactorZirnox;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityReactorZirnox;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenReactorZirnox extends ScreenInfoContainer<MenuReactorZirnox> {

    private static final Identifier TEXTURE = Library.id("textures/gui/reactors/gui_zirnox.png");

    public ScreenReactorZirnox(MenuReactorZirnox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 203, 256);
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2;
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

        BlockEntityReactorZirnox be = be();
        if (be != null) {
            SmoothLinearGaugeElement.draw(
                    graphics,
                    162,
                    114,
                    (double) be.steam.getFill() / be.steam.getMaxFill(),
                    2,
                    5,
                    0.75,
                    14,
                    0,
                    0x7F0000,
                    0);
            SmoothLinearGaugeElement.draw(
                    graphics,
                    144,
                    114,
                    (double) be.carbonDioxide.getFill() / be.carbonDioxide.getMaxFill(),
                    2,
                    5,
                    0.75,
                    14,
                    0,
                    0x7F0000,
                    0);
            SmoothLinearGaugeElement.draw(
                    graphics,
                    180,
                    114,
                    (double) be.water.getFill() / be.water.getMaxFill(),
                    2,
                    5,
                    0.75,
                    14,
                    0,
                    0x7F0000,
                    0);
            SmoothGaugeElement.draw(graphics, 169, 42, be.heat / 100_000D, 5, 2, 1, 0x7F0000, 0);
            SmoothGaugeElement.draw(
                    graphics, 187, 42, be.pressure / 100_000D, 5, 2, 1, 0x7F0000, 0);

            if (be.isOn) {
                for (int x = 0; x < 4; x++)
                    for (int y = 0; y < 4; y++)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                7 + 36 * x,
                                15 + 36 * y,
                                238,
                                238,
                                18,
                                18,
                                256,
                                256);
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 3; y++)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                25 + 36 * x,
                                33 + 36 * y,
                                238,
                                238,
                                18,
                                18,
                                256,
                                256);
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 142, 15, 220, 238, 18, 18, 256, 256);
            }

            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    160,
                    33,
                    18,
                    17,
                    List.of(
                            Component.translatable("desc.gui.reactorZirnox.temperature"),
                            Component.literal(
                                    "   " + Math.round(be.heat * 0.00001 * 780 + 20) + "°C")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    178,
                    33,
                    18,
                    17,
                    List.of(
                            Component.translatable("desc.gui.reactorZirnox.pressure"),
                            Component.literal(
                                    "   " + Math.round(be.pressure * 0.00001 * 30) + " bar")));
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 160, 108, 18, 12, be.steam);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 142, 108, 18, 12, be.carbonDioxide);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 178, 108, 18, 12, be.water);

            drawInfoPanel(graphics, -16, 36, 16, 16, 2);
            drawInfoPanel(graphics, -16, 52, 16, 16, 3);
            if (be.water.getFill() <= 0) drawInfoPanel(graphics, -16, 68, 16, 16, 6);
            if (be.carbonDioxide.getFill() <= 4000) drawInfoPanel(graphics, -16, 84, 16, 16, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    -16,
                    36,
                    16,
                    16,
                    -8,
                    52,
                    lineArray("desc.gui.zirnox.coolant"));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    -16,
                    52,
                    16,
                    16,
                    -8,
                    68,
                    lineArray("desc.gui.zirnox.pressure"));
            if (be.water.getFill() <= 0) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        -16,
                        68,
                        16,
                        16,
                        -8,
                        84,
                        lineArray("desc.gui.zirnox.warning1"));
            }
            if (be.carbonDioxide.getFill() < 4000) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        -16,
                        84,
                        16,
                        16,
                        -8,
                        100,
                        lineArray("desc.gui.zirnox.warning2"));
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, 144, 35, 14, 14)) {
                sendControl("control");
                playClick();
                return true;
            }
            if (checkClick(mx, my, 151, 51, 36, 36)) {
                sendControl("vent");
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendControl(String key) {
        BlockPos core = corePos();
        if (core == null) return;
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityReactorZirnox be() {
        return menu.blockEntity();
    }
}
