// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineExposureChamber;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineExposureChamber;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineExposureChamber extends ScreenInfoContainer<MenuMachineExposureChamber> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_exposure_chamber.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 34;
    private static final int PARTICLE_X = 26, PARTICLE_Y = 36, PARTICLE_W = 9, PARTICLE_H = 16;
    private static final int PROGRESS_X = 36, PROGRESS_Y = 39, PROGRESS_W = 42, PROGRESS_H = 10;
    private static final int LAMP_X = 156, LAMP_Y = 4, LAMP_W = 9, LAMP_H = 12;

    public ScreenMachineExposureChamber(
            MenuMachineExposureChamber menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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

        BlockEntityMachineExposureChamber be = menu.blockEntity();

        int p = be.progress * PROGRESS_W / (be.processTime + 1);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PROGRESS_X,
                    PROGRESS_Y,
                    192,
                    0,
                    p,
                    PROGRESS_H,
                    256,
                    256);
        }

        int c = be.savedParticles * PARTICLE_H / BlockEntityMachineExposureChamber.MAX_PARTICLES;
        if (c > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    PARTICLE_X,
                    PARTICLE_Y + PARTICLE_H - c,
                    192,
                    26 - c,
                    PARTICLE_W,
                    c,
                    256,
                    256);
        }

        int e = (int) (be.power * POWER_H / BlockEntityMachineExposureChamber.MAX_POWER);
        if (e > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y + POWER_H - e,
                    176,
                    POWER_H - e,
                    POWER_W,
                    e,
                    256,
                    256);
        }

        if (be.consumption <= be.power) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LAMP_X,
                    LAMP_Y,
                    176,
                    34,
                    LAMP_W,
                    LAMP_H,
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
                BlockEntityMachineExposureChamber.MAX_POWER);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PARTICLE_X,
                PARTICLE_Y,
                PARTICLE_W,
                PARTICLE_H,
                List.of(
                        Component.literal(
                                be.savedParticles
                                        + " / "
                                        + BlockEntityMachineExposureChamber.MAX_PARTICLES)));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
