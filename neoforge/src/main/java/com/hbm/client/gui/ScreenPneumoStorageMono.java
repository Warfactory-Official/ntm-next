// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPneumoStorageMono;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageMono;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPneumoStorageMono extends ScreenInfoContainer<MenuPneumoStorageMono> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_pneumatic_mono.png");

    public ScreenPneumoStorageMono(
            MenuPneumoStorageMono menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 181);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 176 / 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPneumoStorageMono storage = menu.blockEntity();

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

        for (int i = 0; i < BlockEntityPneumoStorageMono.SLOT_COUNT; i++) {
            if (storage.getItem(i).isEmpty()) continue;
            int bar = storage.amounts[i] * 124 / BlockEntityPneumoStorageMono.CAPACITY;
            if (bar > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        44,
                        17 + i * 18,
                        0,
                        181,
                        bar,
                        16,
                        256,
                        256);
            }
        }

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

        for (int k = 0; k < BlockEntityPneumoStorageMono.SLOT_COUNT; k++) {
            if (storage.getItem(k).isEmpty()) continue;
            int amount = storage.amounts[k];
            String percent =
                    " ("
                            + (((int) (amount * 1000D / BlockEntityPneumoStorageMono.CAPACITY))
                                    / 10D)
                            + "%)";
            graphics.text(
                    font,
                    Component.literal(String.format(Locale.US, "%,d", amount) + percent),
                    50,
                    22 + k * 18,
                    0xFF000000,
                    false);
        }

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
