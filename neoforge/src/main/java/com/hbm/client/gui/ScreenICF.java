// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuICF;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityICF;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenICF extends ScreenInfoContainer<MenuICF> {

    private static final Identifier TEXTURE = Library.id("textures/gui/reactors/gui_icf.png");

    public ScreenICF(MenuICF menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 248, 222);
        this.titleLabelY = 6;
        this.inventoryLabelX = 44;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, imageWidth, 114, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 36, 122, 36.0F, 122.0F, 176, 108, 256, 256);

        BlockEntityICF icf = menu.blockEntity();

        if (icf.maxLaser > 0) {
            int p = (int) (icf.laser * 70 / icf.maxLaser);
            if (p > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        8,
                        88 - p,
                        212,
                        192 - p,
                        16,
                        p,
                        256,
                        256);
        }

        SmoothGaugeElement.draw(
                graphics,
                196,
                98,
                (double) icf.heat / BlockEntityICF.maxHeat,
                5,
                2,
                1,
                0xFFFF00AF,
                0xFFFF00AF);

        drawFluidBar(graphics, 44, 18, 16, 70, icf.tanks[0]);
        drawFluidBar(graphics, 188, 18, 16, 70, icf.tanks[1]);
        drawFluidBar(graphics, 224, 18, 16, 70, icf.tanks[2]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 44, 18, 16, 70, icf.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 188, 18, 16, 70, icf.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 224, 18, 16, 70, icf.tanks[2]);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                8,
                18,
                16,
                70,
                List.of(
                        Component.literal(
                                icf.maxLaser <= 0
                                        ? "OFFLINE"
                                        : BobMathUtil.getShortNumber(icf.laser)
                                                + "TU/t - "
                                                + (icf.laser * 1000 / icf.maxLaser) / 10D
                                                + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                187,
                89,
                18,
                18,
                List.of(
                        Component.literal(
                                BobMathUtil.getShortNumber(icf.heat)
                                        + " / "
                                        + BobMathUtil.getShortNumber(BlockEntityICF.maxHeat)
                                        + "TU")));
        super.extractLabels(graphics, mouseX, mouseY);
    }
}
