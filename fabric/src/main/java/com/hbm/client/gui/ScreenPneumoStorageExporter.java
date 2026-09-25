// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPneumoStorageExporter;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoStorageExporter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenPneumoStorageExporter extends ScreenInfoContainer<MenuPneumoStorageExporter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_pneumatic_exporter.png");

    public ScreenPneumoStorageExporter(
            MenuPneumoStorageExporter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPneumoStorageExporter exporter = menu.blockEntity();

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

        if (exporter.rorConfiguredMode) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    142,
                    52,
                    imageWidth,
                    18,
                    18,
                    18,
                    256,
                    256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 14, 14, 77, 14, 58, 58, 256, 256);
        }
        if (!exporter.continuousRequest) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    142,
                    16,
                    imageWidth,
                    0,
                    18,
                    18,
                    256,
                    256);
        }
        if (exporter.requestMode == BlockEntityPneumoStorageExporter.MODE_FULL_STACK) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    142,
                    34,
                    imageWidth + 18,
                    0,
                    18,
                    18,
                    256,
                    256);
        }
        if (exporter.requestMode == BlockEntityPneumoStorageExporter.MODE_FULL_REQUEST) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    142,
                    34,
                    imageWidth + 18,
                    18,
                    18,
                    18,
                    256,
                    256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                142,
                16,
                18,
                18,
                List.of(
                        Component.translatable(
                                "desc.gui.pneumoStorageExporter.requestMode",
                                Component.translatable(
                                                exporter.continuousRequest
                                                        ? "desc.gui.pneumoStorageExporter.continuous"
                                                        : "desc.gui.pneumoStorageExporter.byRequest")
                                        .withStyle(ChatFormatting.YELLOW))));

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                142,
                34,
                18,
                18,
                List.of(
                        Component.translatable(
                                "desc.gui.pneumoStorageExporter.requestType",
                                Component.translatable(
                                                exporter.requestMode
                                                                == BlockEntityPneumoStorageExporter
                                                                        .MODE_AS_MUCH_AS_POSSIBLE
                                                        ? "desc.gui.pneumoStorageExporter.asMuchAsPossible"
                                                        : exporter.requestMode
                                                                        == BlockEntityPneumoStorageExporter
                                                                                .MODE_FULL_STACK
                                                                ? "desc.gui.pneumoStorageExporter.onlyFullStacks"
                                                                : "desc.gui.pneumoStorageExporter.onlyFullRequests")
                                        .withStyle(ChatFormatting.YELLOW))));

        if (exporter.rorConfiguredMode) {
            List<Component> label = new ArrayList<>();
            label.add(
                    Component.translatable(
                            "desc.gui.pneumoStorageExporter.filterType",
                            Component.translatable("desc.gui.pneumoStorageExporter.rorConfigured")
                                    .withStyle(ChatFormatting.YELLOW)));
            for (int i = 0; i < 9; i++) {
                ItemStack filter = exporter.rorFilters[i];
                label.add(
                        Component.translatable("desc.shared.slot", i + 1)
                                .append(CommonComponents.SPACE)
                                .append(
                                        filter.isEmpty()
                                                ? Component.translatable(
                                                        "desc.gui.pneumoStorageExporter.none")
                                                : Component.literal(
                                                        BuiltInRegistries.ITEM.getKey(
                                                                        filter.getItem())
                                                                + " x"
                                                                + filter.getCount())));
            }
            drawCustomInfoStat(graphics, mouseX, mouseY, 142, 52, 18, 18, label);
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    142,
                    52,
                    18,
                    18,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pneumoStorageExporter.filterType",
                                    Component.translatable(
                                                    "desc.gui.pneumoStorageExporter.manuallyConfigured")
                                            .withStyle(ChatFormatting.YELLOW))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        int mx = (int) x, my = (int) y;
        clickSendFlag(mx, my, 142, 16, 18, 18, "continuous");
        clickSendFlag(mx, my, 142, 34, 18, 18, "request");
        clickSendFlag(mx, my, 142, 52, 18, 18, "ror");
        return super.mouseClicked(event, doubleClick);
    }

    private void clickSendFlag(
            int mouseX, int mouseY, int left, int top, int sizeX, int sizeY, String key) {
        if (!checkClick(mouseX, mouseY, left, top, sizeX, sizeY)) return;
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
        playClick();
    }
}
