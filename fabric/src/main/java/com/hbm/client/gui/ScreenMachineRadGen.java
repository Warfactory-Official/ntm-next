// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineRadGen;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineRadGen;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRadGen extends ScreenInfoContainer<MenuMachineRadGen> {

    private static final Identifier TEXTURE = Library.id("textures/gui/reactors/gui_radgen.png");

    private static final int BAR_X = 66, BAR_Y = 19, BAR_W = 44, BAR_H = 3, BAR_PITCH = 5;
    private static final int BAR_HOVER_X = 65, BAR_HOVER_Y = 18, BAR_HOVER_W = 46, BAR_HOVER_H = 5;
    private static final int POWER_X = 64, POWER_Y = 83, POWER_W = 48, POWER_H = 4;

    public ScreenMachineRadGen(MenuMachineRadGen menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 184);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityMachineRadGen be = menu.blockEntity();

        for (int i = 0; i < BlockEntityMachineRadGen.CHANNELS; i++) {
            if (be.maxProgress[i] <= 0) continue;

            int bar = be.progress[i] * BAR_W / be.maxProgress[i];
            if (bar > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        BAR_X,
                        BAR_Y + i * BAR_PITCH,
                        176,
                        0,
                        bar,
                        BAR_H,
                        256,
                        256);
            }
        }

        int charge = (int) (be.power * POWER_W / BlockEntityMachineRadGen.MAX_POWER);
        if (charge > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y,
                    176,
                    3,
                    charge,
                    POWER_H,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                BlockEntityMachineRadGen.MAX_POWER);

        for (int i = 0; i < BlockEntityMachineRadGen.CHANNELS; i++) {
            if (be.maxProgress[i] <= 0) continue;

            int left = be.maxProgress[i] - be.progress[i];
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BAR_HOVER_X,
                    BAR_HOVER_Y + i * BAR_PITCH,
                    BAR_HOVER_W,
                    BAR_HOVER_H,
                    List.of(
                            Component.translatable("desc.shared.slot", i + 1),
                            Component.literal(be.production[i] + "HE/t for"),
                            Component.literal(
                                    left + " ticks (" + (left * 100 / be.maxProgress[i]) + "%)")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
