// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineElectrolyserFluid;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineElectrolyserFluid
        extends ScreenInfoContainer<MenuMachineElectrolyserFluid> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_electrolyser_fluid.png");

    public ScreenMachineElectrolyserFluid(
            MenuMachineElectrolyserFluid menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 210, 204);
        this.titleLabelY = 7;
    }

    private BlockEntityMachineElectrolyser be() {
        return menu.blockEntity();
    }

    @Override
    protected int titleCenterX() {
        return imageWidth / 2 - 16;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityMachineElectrolyser be = be();
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

        int p = (int) (be.power * 89 / BlockEntityMachineElectrolyser.MAX_POWER);
        if (p > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    186,
                    107 - p,
                    210,
                    89 - p,
                    16,
                    p,
                    256,
                    256);

        if (be.power >= be.usageFluid) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 190, 4, 226, 40, 9, 12, 256, 256);
        }

        int time = be.processFluidTime > 0 ? be.processFluidTime : 1;
        int e = be.progressFluid * 41 / time;
        if (e > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 26, 226, 0, 12, e, 256, 256);

        drawFluidBar(graphics, 42, 18, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 96, 18, 16, 52, be.tanks[1]);
        drawFluidBar(graphics, 116, 18, 16, 52, be.tanks[2]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 42, 18, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 96, 18, 16, 52, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 116, 18, 16, 52, be.tanks[2]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                186,
                18,
                16,
                89,
                be.power,
                BlockEntityMachineElectrolyser.MAX_POWER);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 8, 82, 54, 12)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgm", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
