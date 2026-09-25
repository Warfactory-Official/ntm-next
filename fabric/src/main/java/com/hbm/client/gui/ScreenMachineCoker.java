// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCoker;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityMachineCoker;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineCoker extends ScreenInfoContainer<MenuMachineCoker> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_coker.png");

    public ScreenMachineCoker(MenuMachineCoker menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleColor() {
        return 0xFFC7C1A3;
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

        BlockEntityMachineCoker be = menu.blockEntity();

        int p = be.progress * 53 / BlockEntityMachineCoker.PROCESS_TIME;
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 61, 46, 176, 0, p, 5, 256, 256);

        int h = be.heat * 52 / BlockEntityMachineCoker.MAX_HEAT;
        if (h > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 61, 55, 176, 5, h, 5, 256, 256);

        drawFluidBar(graphics, 35, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 125, 18, 16, 52, be.tanks[1]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 35, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 125, 18, 16, 52, be.tanks[1]);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                60,
                45,
                54,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.progress)
                                        + " / "
                                        + String.format(
                                                Locale.US,
                                                "%,d",
                                                BlockEntityMachineCoker.PROCESS_TIME)
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                60,
                54,
                54,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.heat)
                                        + " / "
                                        + String.format(
                                                Locale.US, "%,d", BlockEntityMachineCoker.MAX_HEAT)
                                        + "TU")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
