// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.container.MenuMachineSILEX;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntitySILEX;
import java.awt.*;
import java.util.List;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class ScreenMachineSILEX extends ScreenInfoContainer<MenuMachineSILEX> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_silex.png");

    public ScreenMachineSILEX(MenuMachineSILEX menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.titleLabelY = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 54;
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

        BlockEntitySILEX be = silex();

        if (be.mode != EnumWavelengths.NULL) {
            float freq = 0.1F * (float) Math.pow(2, be.mode.ordinal());
            int color =
                    be.mode != EnumWavelengths.VISIBLE
                            ? be.mode.guiColor
                            : Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1F);
            drawWave(graphics, 81, 46, 16, 84, 0.5F, freq, color);
        }

        boolean peroxide = be.tank.getTankType() == NTMFluids.PEROXIDE;
        if (be.tank.getFill() > 0) {
            int v =
                    peroxide || SILEXRecipes.INSTANCE.getOutput(be.tank.getTankType()) != null
                            ? 118
                            : 109;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 41, 176, v, 54, 9, 256, 256);
        }

        int p = be.getProgressScaled(69);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 82, 176, 0, p, 43, 256, 256);

        int f = be.getFillScaled(52);
        if (f > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    26,
                    124 - f,
                    176,
                    109 - f,
                    16,
                    f,
                    256,
                    256);

        int i = be.getFluidScaled(52);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    8,
                    42,
                    176,
                    peroxide ? 43 : 50,
                    i,
                    7,
                    256,
                    256);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 42, 52, 7, be.tank);

        if (be.current != null || be.currentFluid != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    27,
                    72,
                    16,
                    52,
                    List.of(
                            Component.literal(
                                    be.currentFill + "/" + BlockEntitySILEX.maxFill + "mB"),
                            be.currentFluid != null
                                    ? NTMFluidProperties.getDisplayName(be.currentFluid)
                                    : be.current.getHoverName()));
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                10,
                92,
                10,
                10,
                List.of(Component.translatable("desc.gui.machineSILEX.voidContents")));

        if (be.mode != EnumWavelengths.NULL) {
            Component modeName = Component.translatable(be.mode.name).withStyle(be.mode.textColor);
            graphics.text(
                    this.font,
                    modeName,
                    132 - this.font.width(modeName) / 2,
                    16,
                    CommonColors.BLACK,
                    false);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawWave(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int height,
            int width,
            float resolution,
            float freq,
            int color) {
        float samples = width / resolution;
        float scale = height / 2F;
        float offset = (float) (gameTime() % (long) (4 * Math.PI / freq));
        int argb = ARGB.opaque(color);

        for (int i = 1; i < samples; i++) {
            double currentX = offset + x + i * resolution;
            double nextX = offset + x + (i + 1) * resolution;
            double currentY = y + scale * Math.sin(freq * currentX);
            double nextY = y + scale * Math.sin(freq * nextX);
            drawSegment(graphics, argb, currentX - offset, currentY, nextX - offset, nextY, 3F);
        }
    }

    private void drawSegment(
            GuiGraphicsExtractor graphics,
            int argb,
            double x1,
            double y1,
            double x2,
            double y2,
            float thickness) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float len = Mth.length(dx, dy);
        if (len <= 0) return;
        int h = Math.max(1, Math.round(thickness));
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x1, (float) y1);
        pose.rotate((float) Math.atan2(dy, dx));
        graphics.fill(0, -h / 2, Math.max(1, Math.round(len)), h - h / 2, argb);
        pose.popMatrix();
    }

    private long gameTime() {
        Minecraft mc = this.minecraft;
        return mc != null && mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 10, 92, 12, 12)) {
            BlockPos core = corePos();
            if (core != null) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("void", true);
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

    private BlockEntitySILEX silex() {
        return menu.blockEntity();
    }
}
