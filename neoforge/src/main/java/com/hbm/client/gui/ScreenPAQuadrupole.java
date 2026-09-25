// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPAQuadrupole;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.albion.BlockEntityPAQuadrupole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPAQuadrupole extends ScreenPACooled<MenuPAQuadrupole> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/particleaccelerator/gui_quadrupole.png");

    public ScreenPAQuadrupole(MenuPAQuadrupole menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 9;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPAQuadrupole be = menu.blockEntity();

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
        drawPowerBar(graphics, TEXTURE, 26, be.power, be.getMaxPower());

        if (be.power >= BlockEntityPAQuadrupole.usage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 65, 64, 176, 8, 8, 8, 256, 256);
        }
        if (Math.ceil(be.temperature) <= COLD_ENOUGH) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 75, 64, 176, 8, 8, 8, 256, 256);
        }
        ItemPACoil.EnumCoilType coil =
                ItemPACoil.typeOf(be.getItem(BlockEntityPAQuadrupole.SLOT_COIL));
        if (coil != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 85, 64, 176, 8, 8, 8, 256, 256);
            int u =
                    coil == ItemPACoil.EnumCoilType.GOLD || coil == ItemPACoil.EnumCoilType.BSCCO
                            ? 200
                            : 228;
            int v =
                    coil == ItemPACoil.EnumCoilType.GOLD || coil == ItemPACoil.EnumCoilType.NIOBIUM
                            ? 0
                            : 28;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 65, 30, u, v, 28, 28, 256, 256);
        }

        drawCoolant(graphics, mouseX, mouseY, be, 116);
        drawElectricityInfo(graphics, mouseX, mouseY, 26, 18, 16, 52, be.power, be.getMaxPower());

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
