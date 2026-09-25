// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.container.MenuMachineStrandCaster;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineStrandCaster;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineStrandCaster extends ScreenInfoContainer<MenuMachineStrandCaster> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_strand_caster.png");

    public ScreenMachineStrandCaster(
            MenuMachineStrandCaster menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 214);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
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

        BlockEntityMachineStrandCaster be = caster();

        if (be.amount != 0 && be.type != null) {
            int targetHeight = Math.min(be.amount * 79 / be.getCapacity(), 92);
            if (targetHeight > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        17,
                        93 - targetHeight,
                        176,
                        89 - targetHeight,
                        34,
                        targetHeight,
                        256,
                        256,
                        ARGB.opaque(be.type.moltenColor));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        17,
                        93 - targetHeight,
                        176,
                        89 - targetHeight,
                        34,
                        targetHeight,
                        256,
                        256,
                        0x4CFFFFFF);
            }
        }

        drawFluidBar(graphics, 82, 14, 16, 24, be.water);
        drawFluidBar(graphics, 82, 65, 16, 24, be.steam);

        drawStackInfo(graphics, mouseX, mouseY, 16, 17);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 82, 14, 16, 24, be.water);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 82, 65, 16, 24, be.steam);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawStackInfo(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y) {
        BlockEntityMachineStrandCaster be = caster();
        List<Component> list;

        if (be.type == null) {
            list =
                    List.of(
                            Component.translatable("desc.shared.empty")
                                    .withStyle(ChatFormatting.RED));
        } else {
            list =
                    List.of(
                            Component.literal(
                                            be.type.getLocalizedName()
                                                    + ": "
                                                    + Mats.formatAmount(
                                                            be.amount,
                                                            ModifierKeys.leftShiftHeld()))
                                    .withStyle(ChatFormatting.YELLOW));
        }

        drawCustomInfoStat(graphics, mouseX, mouseY, x, y, 36, 81, list);
    }

    private BlockEntityMachineStrandCaster caster() {
        return menu.blockEntity();
    }
}
