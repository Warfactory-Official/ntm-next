// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuForceField;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityForceField;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenForceField extends ScreenInfoContainer<MenuForceField> {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_field.png");

    private static final int BAR_H = 52;

    private static final int BAR_BOTTOM = 69;
    private static final int POWER_X = 8, HEALTH_X = 62, BAR_W = 16;
    private static final int TOGGLE_X = 142, TOGGLE_Y = 34, TOGGLE_W = 18, TOGGLE_H = 18;

    public ScreenForceField(MenuForceField menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

        BlockEntityForceField field = field();

        int p = menu.getPowerScaled(BAR_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    BAR_BOTTOM - p,
                    176,
                    BAR_H - p,
                    BAR_W,
                    p,
                    256,
                    256);
        }

        int h = field.getHealthScaled(BAR_H);
        if (h > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEALTH_X,
                    BAR_BOTTOM - h,
                    192,
                    BAR_H - h,
                    BAR_W,
                    h,
                    256,
                    256);
        }

        if (field.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    176,
                    52,
                    TOGGLE_W,
                    TOGGLE_H,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                BAR_BOTTOM - BAR_H,
                BAR_W,
                BAR_H,
                menu.getPower(),
                MachineData.FORCE_FIELD_MAX_POWER.get());
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HEALTH_X,
                BAR_BOTTOM - BAR_H,
                BAR_W,
                BAR_H,
                List.of(Component.literal(field.health + " / " + field.maxHealth + "HP")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(field().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityForceField field() {
        return menu.blockEntity();
    }
}
