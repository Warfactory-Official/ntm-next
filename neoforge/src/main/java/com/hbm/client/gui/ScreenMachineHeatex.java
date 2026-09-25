// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineHeatex;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityHeaterHeatex;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineHeatex extends ScreenInfoContainer<MenuMachineHeatex> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_heatex.png");

    private static final int TANK0_X = 44, TANK1_X = 116, TANK_Y = 36, TANK_W = 16, TANK_H = 52;

    private EditBox toCoolField;
    private EditBox delayField;

    public ScreenMachineHeatex(MenuMachineHeatex menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        BlockEntityHeaterHeatex be = heatex();

        toCoolField = new EditBox(this.font, leftPos + 73, topPos + 31, 30, 10, Component.empty());
        toCoolField.setBordered(false);
        toCoolField.setMaxLength(6);
        toCoolField.setTextColor(CommonColors.GREEN);
        toCoolField.setValue(String.valueOf(be.amountToCool));
        toCoolField.setResponder(text -> sendControl("toCool", text));
        addRenderableWidget(toCoolField);

        delayField = new EditBox(this.font, leftPos + 73, topPos + 49, 30, 10, Component.empty());
        delayField.setBordered(false);
        delayField.setMaxLength(6);
        delayField.setTextColor(CommonColors.GREEN);
        delayField.setValue(String.valueOf(be.tickDelay));
        delayField.setResponder(text -> sendControl("delay", text));
        addRenderableWidget(delayField);
    }

    private void sendControl(String key, String text) {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return;
        }
        BlockPos pos = menu.blockEntity().getBlockPos();
        CompoundTag data = new CompoundTag();
        data.putInt(key, value);
        Services.NETWORK.sendToServer(new NbtControlPayload(pos, data));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityHeaterHeatex be = heatex();

        drawFluidBar(graphics, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidBar(graphics, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK0_X, TANK_Y, TANK_W, TANK_H, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK1_X, TANK_Y, TANK_W, TANK_H, be.tanks[1]);

        if (checkClick(mouseX, mouseY, 70, 26, 36, 18)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    70,
                    26,
                    36,
                    18,
                    List.of(Component.translatable("desc.gui.machineHeatex.amountPerCycle")));
        }
        if (checkClick(mouseX, mouseY, 70, 44, 36, 18)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    70,
                    44,
                    36,
                    18,
                    List.of(Component.translatable("desc.gui.machineHeatex.cycleTickDelay")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityHeaterHeatex heatex() {
        return menu.blockEntity();
    }
}
