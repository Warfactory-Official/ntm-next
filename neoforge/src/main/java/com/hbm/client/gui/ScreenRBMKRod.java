// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKRod;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenRBMKRod extends ScreenInfoContainer<MenuRBMKRod> {

    private static final Identifier TEXTURE = Library.id("textures/gui/rbmk/gui_rbmk_element.png");
    private static final int PANEL_X = -16,
            AUTOLOADER_PANEL_Y = 20,
            MANUAL_PANEL_Y = 36,
            PANEL_SIZE = 16;

    public ScreenRBMKRod(MenuRBMKRod menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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

        ItemStack fuel = menu.slots.get(0).getItem();
        if (fuel.getItem() instanceof ItemRBMKRod) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 34, 21, 176, 0, 18, 67, 256, 256);
            double depletion = 1D - ItemRBMKRod.getEnrichment(fuel);
            int d = (int) (depletion * 67);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 34, 21, 194, 0, 18, d, 256, 256);

            double xenon = ItemRBMKRod.getPoisonLevel(fuel);
            int x = (int) (xenon * 58);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    126,
                    82 - x,
                    212,
                    58 - x,
                    14,
                    x,
                    256,
                    256);
        }

        if (!BlockEntityRBMKRod.coldEnoughForAutoloader(fuel)) {
            drawInfoPanel(graphics, PANEL_X, AUTOLOADER_PANEL_Y, PANEL_SIZE, PANEL_SIZE, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    AUTOLOADER_PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    PANEL_X + 8,
                    AUTOLOADER_PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable("desc.gui.rbmkRod.fuelSkinTemperature1000"),
                            Component.translatable("desc.gui.rbmkRod.autoloadersCanNoLonger")));
        }
        if (!BlockEntityRBMKRod.coldEnoughForManual(fuel)) {
            drawInfoPanel(graphics, PANEL_X, MANUAL_PANEL_Y, PANEL_SIZE, PANEL_SIZE, 7);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    PANEL_X,
                    MANUAL_PANEL_Y,
                    PANEL_SIZE,
                    PANEL_SIZE,
                    PANEL_X + 8,
                    MANUAL_PANEL_Y + PANEL_SIZE,
                    List.of(
                            Component.translatable("desc.gui.rbmkRod.fuelSkinTemperature200"),
                            Component.translatable("desc.gui.rbmkRod.fuelCanNoLonger")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
