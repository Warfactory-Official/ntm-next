// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCoreStabilizer;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityCoreStabilizer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenCoreStabilizer extends ScreenInfoContainer<MenuCoreStabilizer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/dfc/gui_stabilizer.png");

    private static final int GAUGE_W = 16, GAUGE_H = 52, GAUGE_Y = 17, POWER_X = 35;
    private static final int SET_X = 124, BUTTON_Y = 52, BUTTON_SIZE = 18;
    private static final int SLIDER_X = 71, SLIDER_Y = 45, SLIDER_W = 34;
    private static final int FIELD_BOX_X = 71, FIELD_BOX_Y = 53;

    private EditBox field;

    public ScreenCoreStabilizer(MenuCoreStabilizer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(font, leftPos + 75, topPos + 57, 29, 12, Component.empty());
        field.setTextColor(-1);
        field.setTextColorUneditable(-1);
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf(menu.blockEntity().watts));
        addRenderableWidget(field);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        SET_X,
                        BUTTON_Y,
                        BUTTON_SIZE,
                        BUTTON_SIZE)) {
            String text = field.getValue();
            if (!text.isEmpty() && text.chars().allMatch(Character::isDigit)) {
                int watts = Math.clamp(Integer.parseInt(text), 1, 100);
                field.setValue(String.valueOf(watts));
                CompoundTag data = new CompoundTag();
                data.putInt("watts", watts);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
                    192,
                    4,
                    34,
                    16,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCoreStabilizer be = menu.blockEntity();

        int slider = be.watts * SLIDER_W / 100;
        if (slider > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    SLIDER_X,
                    SLIDER_Y,
                    192,
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
