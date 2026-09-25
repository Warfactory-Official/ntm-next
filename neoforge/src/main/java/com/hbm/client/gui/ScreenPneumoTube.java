// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.ntl.PneumaticNetwork;
import com.hbm.inventory.container.MenuPneumoTube;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.pneumatic.BlockEntityPneumoTube;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenPneumoTube extends ScreenInfoContainer<MenuPneumoTube> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_pneumatic_pipe.png");
    private static final Identifier TEXTURE_ENDPOINT =
            Library.id("textures/gui/storage/gui_pneumatic_endpoint.png");

    private static final int TOOLTIP_LIFT = 30;

    public ScreenPneumoTube(MenuPneumoTube menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private boolean endpointOnly() {
        return !menu.blockEntity().isCompressor();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPneumoTube tube = menu.blockEntity();
        boolean endpointOnly = endpointOnly();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                endpointOnly ? TEXTURE_ENDPOINT : TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (tube.whitelist) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 33, 176, 0, 3, 6, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 47, 176, 0, 3, 6, 256, 256);
        }

        if (!endpointOnly) {
            if (tube.redstone) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 52, 179, 0, 18, 18, 256, 256);
            }
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    151,
                    16,
                    197,
                    18 * tube.receiveOrder,
                    18,
                    18,
                    256,
                    256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    151,
                    52,
                    215,
                    18 * tube.sendOrder,
                    18,
                    18,
                    256,
                    256);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    6 + 4 * (tube.compair.getPressure() - 1),
                    36,
                    179,
                    18,
                    4,
                    8,
                    256,
                    256);
            SmoothGaugeElement.draw(
                    graphics,
                    16,
                    25,
                    (double) tube.compair.getFill() / tube.compair.getMaxFill(),
                    5,
                    2,
                    1,
                    0xFFCA6C43,
                    0xFFAB4223);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 7, 16, 18, 18, tube.compair);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    7,
                    52,
                    18,
                    18,
                    List.of(
                            Component.literal(
                                    (tube.redstone
                                                    ? ChatFormatting.GREEN + "ON "
                                                    : ChatFormatting.RED + "OFF ")
                                            + ChatFormatting.RESET
                                            + "with Redstone")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    6,
                    36,
                    20,
                    8,
                    List.of(
                            Component.translatable(
                                    "desc.shared.compressor", tube.compair.getPressure()),
                            Component.translatable(
                                    "desc.shared.maxRange",
                                    BlockEntityPneumoTube.getRangeFromPressure(
                                            tube.compair.getPressure()))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    16,
                    18,
                    18,
                    List.of(
                            Component.literal(ChatFormatting.YELLOW + "Receiver order:"),
                            Component.literal(
                                    tube.receiveOrder == PneumaticNetwork.RECEIVE_ROBIN
                                            ? "Round robin"
                                            : "Random")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    52,
                    18,
                    18,
                    List.of(
                            Component.literal(ChatFormatting.YELLOW + "Provider slot order:"),
                            Component.literal(
                                    tube.sendOrder == PneumaticNetwork.SEND_FIRST
                                            ? "First to last"
                                            : tube.sendOrder == PneumaticNetwork.SEND_LAST
                                                    ? "Last to first"
                                                    : "Random")));
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < BlockEntityPneumoTube.SLOT_COUNT; i++) {
                Slot slot = menu.getSlot(i);
                String mode = tube.pattern.mode(i);
                if (mode != null && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    graphics.setComponentTooltipForNextFrame(
                            font,
                            List.of(
                                    Component.translatable("desc.shared.rightClickToChange")
                                            .withStyle(ChatFormatting.RED),
                                    ModulePatternMatcher.getLabel(mode)),
                            mouseX,
                            mouseY - TOOLTIP_LIFT);
                }
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        int mx = (int) x, my = (int) y;

        if (!endpointOnly()) {
            clickSendFlag(mx, my, 7, 52, 18, 18, "redstone");
            clickSendFlag(mx, my, 6, 36, 20, 8, "pressure");
            clickSendFlag(mx, my, 151, 16, 18, 18, "receive");
            clickSendFlag(mx, my, 151, 52, 18, 18, "send");
        }

        clickSendFlag(mx, my, 128, 30, 14, 26, "whitelist");

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
