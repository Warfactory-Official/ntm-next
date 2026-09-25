// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPneumoStorageClutter;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageClutter;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPneumoStorageClutter extends ScreenInfoContainer<MenuPneumoStorageClutter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_pneumatic_clutter.png");

    public ScreenPneumoStorageClutter(
            MenuPneumoStorageClutter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 235);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 176 / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPneumoStorageClutter storage = menu.blockEntity();

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
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                174 + 4 * (storage.compair.getPressure() - 1),
                36,
                200,
                0,
                4,
                8,
                256,
                256);
        SmoothGaugeElement.draw(
                graphics,
                184,
                25,
                (double) storage.compair.getFill() / storage.compair.getMaxFill(),
                5,
                2,
                1,
                0xFFCA6C43,
                0xFFAB4223);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                174,
                36,
                20,
                8,
                List.of(
                        Component.translatable(
                                "desc.shared.compressor", storage.compair.getPressure()),
                        Component.translatable(
                                "desc.shared.maxRange",
                                BlockEntityPneumoTube.getRangeFromPressure(
                                        storage.compair.getPressure()))));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (checkClick((int) x, (int) y, 174, 36, 20, 8)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("pressure", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
        }
        return super.mouseClicked(event, doubleClick);
    }
}
