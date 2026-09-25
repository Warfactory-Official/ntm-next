// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCoreEmitter;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityCoreEmitter;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class ScreenCoreEmitter extends ScreenInfoContainer<MenuCoreEmitter> {

    private static final Identifier TEXTURE = Library.id("textures/gui/dfc/gui_emitter.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17;
    private static final int TANK_X = 8, POWER_X = 26;
    private static final int SET_X = 97, TOGGLE_X = 133, BUTTON_Y = 52, BUTTON_SIZE = 18;
    private static final int SLIDER_X = 53, SLIDER_Y = 45, SLIDER_W = 34;
    private static final int FIELD_BOX_X = 53, FIELD_BOX_Y = 53;

    private EditBox field;

    public ScreenCoreEmitter(MenuCoreEmitter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(font, leftPos + 57, topPos + 57, 29, 12, Component.empty());
        field.setTextColor(-1);
        field.setTextColorUneditable(-1);
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf(menu.blockEntity().watts));
        addRenderableWidget(field);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            CompoundTag data = new CompoundTag();

            if (checkClick(mx, my, SET_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                Integer watts = parseWatts();
                if (watts != null) data.putInt("watts", watts);
            }
            if (checkClick(mx, my, TOGGLE_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                data.putBoolean("toggle", true);
            }
            if (!data.isEmpty()) {
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable Integer parseWatts() {
        String text = field.getValue();
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) return null;
        int watts = Math.clamp(Integer.parseInt(text), 1, 100);
        field.setValue(String.valueOf(watts));
        return watts;
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
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (field.isFocused()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + FIELD_BOX_X,
                    topPos + FIELD_BOX_Y,
                    210,
                    4,
                    34,
                    16,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCoreEmitter be = menu.blockEntity();

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    BUTTON_Y,
                    192,
                    0,
                    BUTTON_SIZE,
                    BUTTON_SIZE,
                    256,
                    256);
        }
        int slider = be.watts * SLIDER_W / 100;
        if (slider > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    SLIDER_X,
                    SLIDER_Y,
                    210,
                    0,
                    slider,
                    4,
                    256,
                    256);
        }

        int power = (int) be.getPowerScaled(GAUGE_H);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    GAUGE_Y + (GAUGE_H - power),
                    176,
                    GAUGE_H - power,
                    GAUGE_W,
                    power,
                    256,
                    256);
        }

        drawFluidBar(graphics, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);

        graphics.text(
                font,
                Component.translatable(
                        "desc.gui.coreEmitter.output", BobMathUtil.getShortNumber(be.prev)),
                50,
                30,
                0xFFFF7F7F,
                false);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, GAUGE_Y, GAUGE_W, GAUGE_H, be.tank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                GAUGE_Y,
                GAUGE_W,
                GAUGE_H,
                be.power,
                be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
