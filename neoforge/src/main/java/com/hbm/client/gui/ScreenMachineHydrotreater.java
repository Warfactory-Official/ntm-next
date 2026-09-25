// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineHydrotreater;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineHydrotreater;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineHydrotreater extends ScreenInfoContainer<MenuMachineHydrotreater> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_hydrotreater.png");

    private static final int POWER_X = 17, TANK_TOP = 18, BAR_W = 16, BAR_H = 52;

    private static final int POWER_FILL_MAX = 54;
    private static final int TANK0_X = 35, TANK1_X = 53, TANK2_X = 125, TANK3_X = 143;
    private static final int CATALYST_X = 89, CATALYST_Y = 36;

    public ScreenMachineHydrotreater(
            MenuMachineHydrotreater menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 238);
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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
        long max = BlockEntityMachineHydrotreater.MAX_POWER;
        if (power > 0 && max > 0) {
            int filled = (int) Math.min(POWER_FILL_MAX, power * POWER_FILL_MAX / max);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        TANK_TOP + (BAR_H - filled),
                        176,
                        BAR_H - filled,
                        BAR_W,
                        filled,
                        256,
                        256);
            }
        }

        BlockEntityMachineHydrotreater be = hydrotreater();
        drawFluidBar(graphics, TANK0_X, TANK_TOP, BAR_W, BAR_H, be.tanks[0]);
        drawFluidBar(graphics, TANK1_X, TANK_TOP, BAR_W, BAR_H, be.tanks[1]);
        drawFluidBar(graphics, TANK2_X, TANK_TOP, BAR_W, BAR_H, be.tanks[2]);
        drawFluidBar(graphics, TANK3_X, TANK_TOP, BAR_W, BAR_H, be.tanks[3]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                TANK_TOP,
                BAR_W,
                BAR_H,
                menu.getPower(),
                BlockEntityMachineHydrotreater.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK_TOP, BAR_W, BAR_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK_TOP, BAR_W, BAR_H, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK2_X, TANK_TOP, BAR_W, BAR_H, be.tanks[2]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK3_X, TANK_TOP, BAR_W, BAR_H, be.tanks[3]);

        if (menu.getCarried().isEmpty()
                && isHovering(CATALYST_X, CATALYST_Y, 16, 16, mouseX, mouseY)
                && !menu.slots.get(BlockEntityMachineHydrotreater.SLOT_CATALYST).hasItem()) {
            graphics.setTooltipForNextFrame(
                    this.font, new ItemStack(ModItems.CATALYTIC_CONVERTER), mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineHydrotreater hydrotreater() {
        return menu.blockEntity();
    }
}
