// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuLaunchTable;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenLaunchTable extends ScreenInfoContainer<MenuLaunchTable> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/gui_launch_table.png");

    private static final int POWER_X = 134, POWER_Y = 113, POWER_W = 34, POWER_H = 6;
    private static final int SOLID_X = 152, SOLID_BOTTOM = 88, SOLID_H = 52, SOLID_W = 16;
    private static final int FUEL_X = 116, OX_X = 134, TANK_Y = 36, TANK_W = 16, TANK_H = 34;
    private static final int PIP_Y = 23, PIP_W = 6, PIP_H = 8;
    private static final int LIQUID_PIP_X = 121, OX_PIP_X = 139, SOLID_PIP_X = 157;
    private static final int GREEN_U = 176, RED_U = 182;
    private static final int CHECK_U = 176, CHECK_V = 26, CHECK_SIZE = 18;
    private static final int MISSILE_CHECK_X = 25, MISSILE_CHECK_Y = 35;
    private static final int DESIGNATOR_CHECK_Y = 71;
    private static final int BUTTON_Y = 98, BUTTON_SIZE = 18, BUTTON_V = 8;
    private static final int BUTTON_10_X = 7, BUTTON_15_X = 25, BUTTON_20_X = 43;
    private static final int PANEL_X = -16, PANEL_Y = 36, PANEL_SIZE = 16;

    private final CustomMissilePreview preview = new CustomMissilePreview();

    public ScreenLaunchTable(MenuLaunchTable menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private BlockEntityLaunchTable table() {
        return menu.blockEntity();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityLaunchTable table = table();

        ItemStack missile = menu.getSlot(BlockEntityLaunchTable.SLOT_MISSILE).getItem();
        ItemStack designator = menu.getSlot(BlockEntityLaunchTable.SLOT_DESIGNATOR).getItem();

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

        int power = (int) (table.power * POWER_W / BlockEntityLaunchTable.MAX_POWER);
        if (power > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_Y,
                    176,
                    96,
                    power,
                    POWER_H,
                    256,
                    256);
        }

        int solid = table.solid * SOLID_H / BlockEntityLaunchTable.MAX_SOLID;
        if (solid > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    SOLID_X,
                    SOLID_BOTTOM - solid,
                    176,
                    96 - solid,
                    SOLID_W,
                    solid,
                    256,
                    256);
        }

        if (table.isMissileValid(missile)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    MISSILE_CHECK_X,
                    MISSILE_CHECK_Y,
                    CHECK_U,
                    CHECK_V,
                    CHECK_SIZE,
                    CHECK_SIZE,
                    256,
                    256);
        }
        if (BlockEntityLaunchTable.hasDesignator(designator)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    MISSILE_CHECK_X,
                    DESIGNATOR_CHECK_Y,
                    CHECK_U,
                    CHECK_V,
                    CHECK_SIZE,
                    CHECK_SIZE,
                    256,
                    256);
        }

        pip(graphics, LIQUID_PIP_X, table.liquidState(missile));
        pip(graphics, OX_PIP_X, table.oxidizerState(missile));
        pip(graphics, SOLID_PIP_X, table.solidState(missile));

        switch (table.padSize) {
            case SIZE_10 -> pressed(graphics, BUTTON_10_X, 176);
            case SIZE_15 -> pressed(graphics, BUTTON_15_X, 194);
            case SIZE_20 -> pressed(graphics, BUTTON_20_X, 212);
            default -> {}
        }

        drawInfoPanel(graphics, PANEL_X, PANEL_Y, PANEL_SIZE, PANEL_SIZE, 2);
        drawInfoPanel(graphics, PANEL_X, PANEL_Y + PANEL_SIZE, PANEL_SIZE, PANEL_SIZE, 11);

        drawFluidBar(graphics, FUEL_X, TANK_Y, TANK_W, TANK_H, table.fuelTank);
        drawFluidBar(graphics, OX_X, TANK_Y, TANK_W, TANK_H, table.oxidizerTank);

        if (table.isMissileValid(missile))
            preview.draw(this, graphics, ItemCustomMissile.getStruct(missile));

        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, FUEL_X, TANK_Y, TANK_W, TANK_H, table.fuelTank);
        drawFluidGaugeInfo(
                graphics, mouseX, mouseY, OX_X, TANK_Y, TANK_W, TANK_H, table.oxidizerTank);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                SOLID_X,
                SOLID_BOTTOM - SOLID_H,
                SOLID_W,
                SOLID_H,
                List.of(
                        Component.translatable(
                                "desc.gui.launchTable.solidFuel", table.solid + "l")));
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                table.power,
                BlockEntityLaunchTable.MAX_POWER);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_10_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size101015")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_15_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size151520")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                BUTTON_20_X,
                BUTTON_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.size20")));

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y,
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_X + 8,
                PANEL_Y + PANEL_SIZE,
                List.of(
                        Component.translatable("desc.gui.launchTable.acceptsCustomMissiles"),
                        Component.translatable("desc.gui.launchTable.ofAllSizesAs"),
                        Component.translatable("desc.gui.launchTable.correctSizeSettingIs")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_X,
                PANEL_Y + PANEL_SIZE,
                PANEL_SIZE,
                PANEL_SIZE,
                PANEL_X + 8,
                PANEL_Y + PANEL_SIZE,
                List.of(Component.translatable("desc.gui.launchTable.detonatorCanOnlyTrigger")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void pressed(GuiGraphicsExtractor graphics, int x, int u) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                BUTTON_Y,
                u,
                BUTTON_V,
                BUTTON_SIZE,
                BUTTON_SIZE,
                256,
                256);
    }

    private void pip(GuiGraphicsExtractor graphics, int x, int state) {
        if (state < 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                PIP_Y,
                state == 1 ? GREEN_U : RED_U,
                0,
                PIP_W,
                PIP_H,
                256,
                256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        boolean handled = super.mouseClicked(event, doubleClick);
        if (press(event, BUTTON_10_X, PartSize.SIZE_10)) return true;
        if (press(event, BUTTON_15_X, PartSize.SIZE_15)) return true;
        if (press(event, BUTTON_20_X, PartSize.SIZE_20)) return true;
        return handled;
    }

    private boolean press(MouseButtonEvent event, int buttonX, PartSize size) {
        if (!checkClick(
                (int) event.x(), (int) event.y(), buttonX, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE)) {
            return false;
        }
        playClick();
        CompoundTag data = new CompoundTag();
        data.putInt("padSize", size.ordinal());
        Services.NETWORK.sendToServer(new NbtControlPayload(table().getBlockPos(), data));
        return true;
    }
}
