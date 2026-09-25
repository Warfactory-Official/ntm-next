// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRadioTorchCounter;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public final class ScreenRadioTorchCounter extends ScreenInfoContainer<MenuRadioTorchCounter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_rtty_counter.png");
    private static final int POLLING_X = 193;
    private static final int POLLING_Y = 8;
    private static final int SAVE_Y = 30;
    private static final int BUTTON_SIZE = 18;

    private final EditBox[] channels = new EditBox[3];

    public ScreenRadioTorchCounter(
            MenuRadioTorchCounter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 218, 238);
        inventoryLabelX = 16;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private static void click() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < channels.length; i++) {
            EditBox field =
                    new EditBox(
                            font, leftPos + 29, topPos + 21 + 44 * i, 86, 14, Component.empty());
            field.setBordered(false);
            field.setTextColor(0xFF00FF00);
            field.setTextColorUneditable(0xFF00FF00);
            field.setMaxLength(10);
            field.setValue(menu.blockEntity().channels[i]);
            channels[i] = field;
            addRenderableWidget(field);
        }
    }

    @Override
    protected int titleCenterX() {
        return 92;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0F,
                0F,
                imageWidth,
                imageHeight,
                256,
                256);
        if (menu.blockEntity().polling) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + POLLING_X,
                    topPos + POLLING_Y,
                    218F,
                    0F,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (inButton(mouseX, mouseY, POLLING_Y)) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.literal(menu.blockEntity().polling ? "Polling" : "State Change"),
                    mouseX,
                    mouseY);
        } else if (inButton(mouseX, mouseY, SAVE_Y)) {
            graphics.setTooltipForNextFrame(
                    font, Component.translatable("desc.shared.saveSettings"), mouseX, mouseY);
        }

        if (!menu.getCarried().isEmpty()) return;
        for (int i = 0; i < channels.length; i++) {
            if (mouseX < leftPos + 138
                    || mouseX >= leftPos + 154
                    || mouseY < topPos + 18 + 44 * i
                    || mouseY >= topPos + 34 + 44 * i) continue;
            String mode = menu.blockEntity().matcher.mode(i);
            if (mode != null) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(
                                Component.translatable("desc.shared.rightClickToChange")
                                        .withStyle(ChatFormatting.RED),
                                ModulePatternMatcher.getLabel(mode)),
                        mouseX,
                        mouseY - 30);
            }
            break;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inButton(event.x(), event.y(), POLLING_Y)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", true);
            send(data);
            click();
            return true;
        }
        if (inButton(event.x(), event.y(), SAVE_Y)) {
            CompoundTag data = new CompoundTag();
            for (int i = 0; i < channels.length; i++)
                data.putString("c" + i, channels[i].getValue());
            send(data);
            click();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean inButton(double mouseX, double mouseY, int y) {
        return leftPos + POLLING_X <= mouseX
                && leftPos + POLLING_X + BUTTON_SIZE > mouseX
                && topPos + y < mouseY
                && topPos + y + BUTTON_SIZE >= mouseY;
    }

    private void send(CompoundTag data) {
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
    }
}
