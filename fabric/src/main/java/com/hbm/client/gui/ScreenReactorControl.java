// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuReactorControl;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityReactorControl;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ScreenReactorControl extends ScreenInfoContainer<MenuReactorControl> {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_reactor_control.png");
    private static final int GREEN = 0xFF08FF00;

    private final NumberDisplay[] displays = {
        new NumberDisplay(6, 20, 0x08FF00).setDigitLength(3),
        new NumberDisplay(66, 20, 0x08FF00).setDigitLength(4),
        new NumberDisplay(126, 20, 0x08FF00).setDigitLength(3)
    };
    private final EditBox[] fields = new EditBox[4];

    public ScreenReactorControl(MenuReactorControl menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    private BlockEntityReactorControl control() {
        return menu.blockEntity();
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 4; i++) {
            EditBox field =
                    new EditBox(
                            font,
                            leftPos + 35 + 30 * (i % 2),
                            topPos + (i < 2 ? 38 : 49),
                            26,
                            7,
                            Component.empty());
            field.setTextColor(GREEN);
            field.setTextColorUneditable(-1);
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            fields[i] = addRenderableWidget(field);
        }
        BlockEntityReactorControl control = control();
        fields[0].setValue(String.valueOf((int) control.levelUpper));
        fields[1].setValue(String.valueOf((int) control.levelLower));
        fields[2].setValue(String.valueOf((int) control.heatUpper / 50));
        fields[3].setValue(String.valueOf((int) control.heatLower / 50));
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityReactorControl control = control();
        float[] curve = new float[40 * 2];
        for (int i = 0; i < 40; i++) {
            curve[i * 2] = 128 + i;
            curve[i * 2 + 1] =
                    (float)
                            (39
                                    + Mth.clamp(
                                            control.getTargetLevel(control.function, i * 1250)
                                                    / 100
                                                    * 28,
                                            0,
                                            28));
        }
        GuiLineStrip.draw(graphics, curve, GREEN);

        int[] data = control.getDisplayData();
        for (int i = 0; i < 3; i++) displays[i].draw(graphics, data[i], true);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        int x = (int) event.x(), y = (int) event.y();

        if (checkClick(x, y, 33, 59, 58, 10)) {
            playClick();
            double[] vals = new double[4];
            for (int k = 0; k < 4; k++) {
                int clamp = k < 2 ? 100 : 1000;
                int mod = k < 2 ? 1 : 50;
                String text = fields[k].getValue();
                if (!text.isEmpty() && text.chars().allMatch(c -> c >= '0' && c <= '9')) {
                    int j = (int) Mth.clamp(Double.parseDouble(text), 0, clamp);
                    fields[k].setValue(String.valueOf(j));
                    vals[k] = j * mod;
                } else {
                    fields[k].setValue("0");
                }
            }
            CompoundTag data = new CompoundTag();
            data.putDouble("levelUpper", vals[0]);
            data.putDouble("levelLower", vals[1]);
            data.putDouble("heatUpper", vals[2]);
            data.putDouble("heatLower", vals[3]);
            Services.NETWORK.sendToServer(new NbtControlPayload(control().getBlockPos(), data));
            return true;
        }

        for (int k = 0; k < 3; k++) {
            if (checkClick(x, y, 7, 37 + k * 11, 22, 10)) {
                playClick();
                CompoundTag data = new CompoundTag();
                data.putInt("function", k);
                Services.NETWORK.sendToServer(new NbtControlPayload(control().getBlockPos(), data));
                return true;
            }
        }

        return handled;
    }
}
