// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachinePyroOven;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePyroOven;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachinePyroOven extends ScreenInfoContainer<MenuMachinePyroOven> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_pyrooven.png");

    public ScreenMachinePyroOven(MenuMachinePyroOven menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
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

        BlockEntityMachinePyroOven be = menu.blockEntity();

        int i = (int) (be.power * 52 / BlockEntityMachinePyroOven.MAX_POWER);
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    152,
                    70 - i,
                    176,
                    64 - i,
                    16,
                    i,
                    256,
                    256);

        int p = (int) (be.progress * 27);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 57, 47, 176, 0, p, 12, 256, 256);

        drawFluidBar(graphics, 8, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[1]);
        drawInfoPanel(graphics, leftPos + 108, topPos + 76, 8, 8, 8);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[1]);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                152,
                18,
                16,
                52,
                be.power,
                BlockEntityMachinePyroOven.MAX_POWER);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                108,
                76,
                8,
                8,
                108,
                76,
                upgradeInfo(be, UpgradeType.SPEED, UpgradeType.POWER));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
