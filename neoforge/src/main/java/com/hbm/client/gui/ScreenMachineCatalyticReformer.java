// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCatalyticReformer;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineCatalyticReformer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineCatalyticReformer
        extends ScreenInfoContainer<MenuMachineCatalyticReformer> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_catalytic_reformer.png");

    private static final int POWER_X = 17, POWER_Y = 18, POWER_W = 16, POWER_H = 52;

    private static final int POWER_FILL_MAX = 54;
    private static final int TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int TANK0_X = 35, TANK1_X = 107, TANK2_X = 125, TANK3_X = 143;
    private static final int CATALYST_X = 71, CATALYST_Y = 36, CATALYST_W = 16, CATALYST_H = 16;

    public ScreenMachineCatalyticReformer(
            MenuMachineCatalyticReformer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.titleLabelY = 5;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        long power = menu.getPower();
        long max = BlockEntityMachineCatalyticReformer.MAX_POWER;
        if (power > 0 && max > 0) {
            int filled = (int) Math.min(POWER_FILL_MAX, power * POWER_FILL_MAX / max);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        176,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
        }

        BlockEntityMachineCatalyticReformer be = reformer();
        drawFluidBar(graphics, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawFluidBar(graphics, TANK2_X, TANK_Y, TANK_W, TANK_H, be.tanks[2]);
        drawFluidBar(graphics, TANK3_X, TANK_Y, TANK_W, TANK_H, be.tanks[3]);

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, power, max);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK2_X, TANK_Y, TANK_W, TANK_H, be.tanks[2]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK3_X, TANK_Y, TANK_W, TANK_H, be.tanks[3]);

        if (menu.getCarried().isEmpty()
                && be.getItem(BlockEntityMachineCatalyticReformer.SLOT_CATALYST).isEmpty()
                && isHovering(CATALYST_X, CATALYST_Y, CATALYST_W, CATALYST_H, mouseX, mouseY)) {
            ItemStack converter = new ItemStack(ModItems.CATALYTIC_CONVERTER);
            graphics.setComponentTooltipForNextFrame(
                    this.font, List.of(converter.getHoverName()), mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineCatalyticReformer reformer() {
        return menu.blockEntity();
    }
}
