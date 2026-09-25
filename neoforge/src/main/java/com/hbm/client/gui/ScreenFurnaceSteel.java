// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFurnaceSteel;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityFurnaceSteel;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFurnaceSteel extends ScreenInfoContainer<MenuFurnaceSteel> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_furnace_steel.png");

    public ScreenFurnaceSteel(MenuFurnaceSteel menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
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

        BlockEntityFurnaceSteel be = menu.blockEntity();

        int h = be.heat * 48 / BlockEntityFurnaceSteel.MAX_HEAT;
        if (h > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    67 - h,
                    176,
                    76 - h,
                    7,
                    h,
                    256,
                    256);

        for (int i = 0; i < 3; i++) {
            int p = be.progress[i] * 69 / BlockEntityFurnaceSteel.PROCESS_TIME;
            if (p > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        54,
                        18 + 18 * i,
                        176,
                        18,
                        p,
                        5,
                        256,
                        256);
            int b = be.bonus[i] * 69 / 100;
            if (b > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        54,
                        27 + 18 * i,
                        176,
                        23,
                        b,
                        5,
                        256,
                        256);
            if (be.wasOn)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        16,
                        16 + 18 * i,
                        176,
                        0,
                        18,
                        18,
                        256,
                        256);
        }

        for (int i = 0; i < 3; i++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    53,
                    17 + 18 * i,
                    70,
                    7,
                    List.of(
                            Component.literal(
                                    String.format(Locale.US, "%,d", be.progress[i])
                                            + " / "
                                            + String.format(
                                                    Locale.US,
                                                    "%,d",
                                                    BlockEntityFurnaceSteel.PROCESS_TIME)
                                            + "TU")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    53,
                    26 + 18 * i,
                    70,
                    7,
                    List.of(
                            Component.translatable(
                                    "desc.gui.furnaceSteel.bonus", be.bonus[i] + "%")));
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                151,
                18,
                9,
                50,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.US, "%,d", BlockEntityFurnaceSteel.MAX_HEAT)
                                        + "TU")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
