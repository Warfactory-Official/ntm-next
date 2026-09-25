// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.RTTYPagerControlPayload;
import com.hbm.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class ScreenPager extends Screen {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_rtty_pager.png");
    private static final int X_SIZE = 184;
    private static final int Y_SIZE = 42;
    private static final int SAVE_X = 137;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_SIZE = 18;

    private final String startingChannel;
    private int guiLeft;
    private int guiTop;
    private EditBox channel;

    public ScreenPager(ItemStack pager) {
        super(Component.translatable("container.rttyPager"));
        startingChannel =
                pager.getItem() instanceof ItemRTTYPager ? ItemRTTYPager.channel(pager) : "";
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;

        channel =
                new EditBox(
                        font,
                        guiLeft + 27 + 4,
                        guiTop + 19 + 4,
                        90 - 8,
                        14,
                        Component.translatable("container.rttyPager"));
        channel.setTextColor(0xFF00FF00);
        channel.setTextColorUneditable(0xFF00FF00);
        channel.setBordered(false);
        channel.setMaxLength(ItemRTTYPager.MAX_CHANNEL_LENGTH);
        channel.setValue(startingChannel);
        addRenderableWidget(channel);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                X_SIZE,
                Y_SIZE,
                256,
                256);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Component title = Component.translatable("container.rttyPager");
        graphics.text(
                font,
                title,
                guiLeft + X_SIZE / 2 - font.width(title) / 2,
                guiTop + 6,
                0xFF404040,
                false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inSaveZone(event.x(), event.y())) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Services.NETWORK.sendToServer(new RTTYPagerControlPayload(channel.getValue()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean inSaveZone(double x, double y) {

        return guiLeft + SAVE_X <= x
                && guiLeft + SAVE_X + BUTTON_SIZE > x
                && guiTop + BUTTON_Y < y
                && guiTop + BUTTON_Y + BUTTON_SIZE >= y;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null
                && this.minecraft.options.keyInventory.matches(event)
                && !(getFocused() instanceof EditBox box && box.canConsumeInput())) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
