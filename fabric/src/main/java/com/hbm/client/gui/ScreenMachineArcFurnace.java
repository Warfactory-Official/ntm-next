// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.container.MenuMachineArcFurnace;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineArcFurnace;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineArcFurnace extends ScreenInfoContainer<MenuMachineArcFurnace> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_arc_furnace.png");

    private static final int TOGGLE_X = 151, TOGGLE_Y = 17;
    private static final int PROGRESS_ICON_X = 7, PROGRESS_ICON_Y = 17;
    private static final int POWER_X = 8, POWER_Y = 36, POWER_W = 7, POWER_H = 70;
    private static final int PROGRESS_BAR_X = 17;
    private static final int STACK_X = 152, STACK_Y = 106, STACK_INFO_Y = 36;

    public ScreenMachineArcFurnace(
            MenuMachineArcFurnace menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityMachineArcFurnace be = furnace();

        if (be.liquidMode)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    TOGGLE_X,
                    TOGGLE_Y,
                    190,
                    18,
                    18,
                    18,
                    256,
                    256);
        if (be.isProgressing)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_ICON_X,
                    PROGRESS_ICON_Y,
                    190,
                    0,
                    18,
                    18,
                    256,
                    256);

        int p = (int) (be.getPower() * POWER_H / be.getMaxPower());
        if (p > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + (POWER_H - p),
                    176,
                    POWER_H - p,
                    7,
                    p,
                    256,
                    256);

        int o = (int) (be.progress * POWER_H);
        if (o > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_BAR_X,
                    POWER_Y + (POWER_H - o),
                    183,
                    POWER_H - o,
                    7,
                    o,
                    256,
                    256);

        drawStack(graphics, be.liquids, BlockEntityMachineArcFurnace.MAX_LIQUID, STACK_X, STACK_Y);

        drawStackInfo(graphics, be.liquids, mouseX, mouseY, STACK_X, STACK_INFO_Y);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.getPower(),
                be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawStack(
            GuiGraphicsExtractor graphics, List<MaterialStack> stack, int capacity, int x, int y) {
        int lastHeight = 0;
        int lastQuant = 0;

        for (MaterialStack sta : stack) {
            int targetHeight = (lastQuant + sta.amount) * POWER_H / capacity;
            if (lastHeight != targetHeight) {
                int h = targetHeight - lastHeight;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y - targetHeight,
                        208,
                        POWER_H - targetHeight,
                        16,
                        h,
                        256,
                        256,
                        ARGB.opaque(sta.material.moltenColor));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        x,
                        y - targetHeight,
                        208,
                        POWER_H - targetHeight,
                        16,
                        h,
                        256,
                        256,
                        0x4CFFFFFF);
            }
            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }
    }

    private void drawStackInfo(
            GuiGraphicsExtractor graphics,
            List<MaterialStack> stack,
            int mouseX,
            int mouseY,
            int x,
            int y) {
        List<Component> list = new ArrayList<>();
        if (stack.isEmpty())
            list.add(Component.translatable("desc.shared.empty").withStyle(ChatFormatting.RED));

        boolean shift = ModifierKeys.leftShiftHeld();
        for (MaterialStack sta : stack) {
            list.add(
                    Component.literal(
                                    sta.material.getLocalizedName()
                                            + ": "
                                            + Mats.formatAmount(sta.amount, shift))
                            .withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, x, y, 16, POWER_H, list);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick((int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, 18, 18)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("liquid", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(furnace().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineArcFurnace furnace() {
        return menu.blockEntity();
    }
}
