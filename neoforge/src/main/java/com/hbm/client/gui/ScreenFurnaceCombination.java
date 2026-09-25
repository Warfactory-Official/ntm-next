// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFurnaceCombination;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityFurnaceCombination;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFurnaceCombination extends ScreenInfoContainer<MenuFurnaceCombination> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_furnace_combination.png");

    public ScreenFurnaceCombination(
            MenuFurnaceCombination menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
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

        BlockEntityFurnaceCombination be = menu.blockEntity();

        int p = be.progress * 38 / BlockEntityFurnaceCombination.PROCESS_TIME;
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 37, 176, 0, p, 5, 256, 256);
        int h = be.heat * 37 / BlockEntityFurnaceCombination.MAX_HEAT;
        if (h > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 45, 46, 176, 5, h, 5, 256, 256);

        drawFluidBar(graphics, 118, 18, 16, 52, be.tank);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 118, 18, 16, 52, be.tank);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                44,
                36,
                39,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.progress)
                                        + " / "
                                        + String.format(
                                                Locale.US,
                                                "%,d",
                                                BlockEntityFurnaceCombination.PROCESS_TIME)
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                44,
                45,
                39,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.US,
                                                "%,d",
                                                BlockEntityFurnaceCombination.MAX_HEAT)
                                        + "TU")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
