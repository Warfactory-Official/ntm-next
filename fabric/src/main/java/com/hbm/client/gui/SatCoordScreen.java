/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.SatelliteInterfaceItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.SatelliteCoordinatesLayout;
import com.hbm.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.commons.lang3.math.NumberUtils;
import org.jspecify.annotations.Nullable;

public class SatCoordScreen extends MenuScreen<HeldItemMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/satellites/gui_sat_coord.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;
    private static final int CONFIRM_X = 133;
    private static final int CONFIRM_Y = 52;
    private static final int CONFIRM_SIZE = 18;

    private final ItemStack device;
    private int left;
    private int top;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public SatCoordScreen(HeldItemMenu menu, Inventory inventory, Component title) {
        super(menu, title);
        this.device = menu.stack();
    }

    private static @Nullable Integer parse(EditBox field) {
        String text = field.getValue();
        if (!NumberUtils.isCreatable(text)) return null;
        try {
            return (int) Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        xField = field(left + 66, top + 21, "x");
        yField = field(left + 66, top + 56, "y");
        zField = field(left + 66, top + 92, "z");
    }

    private EditBox field(int x, int y, String axis) {
        EditBox box = new EditBox(font, x, y, 48, 12, Component.literal(axis));
        box.setMaxLength(7);
        box.setBordered(false);
        box.setTextColor(CommonColors.WHITE);
        addRenderableWidget(box);
        return box;
    }

    private boolean connected() {
        CustomData data = device.get(ModDataComponents.PERSISTENT_DATA.get());
        return data != null
                && data.copyTag().getBooleanOr(SatelliteInterfaceItem.KEY_CONNECTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!connected()) return false;
        setFocused(null);
        if (super.mouseClicked(event, doubleClick)) return true;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (mouseX < left + CONFIRM_X || mouseX >= left + CONFIRM_X + CONFIRM_SIZE) return false;
        if (mouseY < top + CONFIRM_Y || mouseY >= top + CONFIRM_Y + CONFIRM_SIZE) return false;

        Integer x = parse(xField);
        Integer z = parse(zField);
        Integer y = parse(yField);
        if (x == null || z == null) return false;

        if (minecraft != null) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        PacketWire.sendToServer(
                SatelliteCoordinatesLayout.encode(
                        menu.containerId, x, z, y != null, y == null ? 0 : y));
        onClose();
        return true;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left,
                top,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                256,
                256);

        if (xField.isFocused()) focus(graphics, top + 16);
        if (yField.isFocused()) focus(graphics, top + 52);
        if (zField.isFocused()) focus(graphics, top + 88);

        if (connected()) {
            pip(graphics, top + 17);
            pip(graphics, top + 25);
        }
    }

    private void focus(GuiGraphicsExtractor graphics, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 61,
                y,
                0.0F,
                126.0F,
                54,
                18,
                256,
                256);
    }

    private void pip(GuiGraphicsExtractor graphics, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, left + 120, y, 194.0F, 0.0F, 7, 7, 256, 256);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
