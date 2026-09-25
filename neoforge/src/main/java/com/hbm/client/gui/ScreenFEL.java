// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFEL;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityFEL;
import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class ScreenFEL extends ScreenInfoContainer<MenuFEL> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_fel.png");

    public ScreenFEL(MenuFEL menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 203, 169);
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 98;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 + 90;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        BlockEntityFEL fel = fel();

        if (fel.isOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 142, 41, 203, 0, 29, 17, 256, 256);
        }

        int k = (int) fel.getPowerScaled(114);
        if (k > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    182,
                    27 + 113 - k,
                    203,
                    17 + 113 - k,
                    16,
                    k,
                    256,
                    256);
        }

        if (fel.power > BlockEntityFEL.powerReq * Math.pow(2, fel.mode.ordinal())
                && fel.isOn
                && fel.mode != EnumWavelengths.NULL
                && fel.distance > 0) {
            int color =
                    fel.mode != EnumWavelengths.VISIBLE
                            ? fel.mode.guiColor
                            : Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1F);
            int argb = ARGB.opaque(color);

            graphics.fill(113, 29, 135, 34, argb);
            graphics.fill(-leftPos, 29, 4, 34, argb);
        }

        drawElectricityInfo(
                graphics, mouseX, mouseY, 182, 27, 16, 113, fel.power, BlockEntityFEL.maxPower);

        this.titleLabelX = 90 + this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (fel.missingValidSilex && fel.isOn) {
            graphics.text(
                    this.font,
                    "ERR.",
                    55 + this.imageWidth / 2 - this.font.width(this.title) / 2,
                    9,
                    0xFFFF0000,
                    false);
        } else if (fel.isOn) {
            graphics.text(
                    this.font,
                    "LIVE",
                    54 + this.imageWidth / 2 - this.font.width(this.title) / 2,
                    9,
                    0xFF00FF00,
                    false);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private long gameTime() {
        Minecraft mc = this.minecraft;
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 142, 41, 29, 17)) {
            BlockPos core = corePos();
            if (core != null) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("toggle", true);
                Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityFEL fel() {
        return menu.blockEntity();
    }
}
