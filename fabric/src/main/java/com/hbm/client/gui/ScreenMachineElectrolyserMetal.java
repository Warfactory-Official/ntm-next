// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.container.MenuMachineElectrolyserMetal;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineElectrolyser;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineElectrolyserMetal
        extends ScreenInfoContainer<MenuMachineElectrolyserMetal> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_electrolyser_metal.png");

    public ScreenMachineElectrolyserMetal(
            MenuMachineElectrolyserMetal menu, Inventory inventory, Component title) {
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

        if (be.leftStack != null) {
            int p = be.leftStack.amount * 42 / BlockEntityMachineElectrolyser.MAX_MATERIAL;
            if (p > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        58,
                        60 - p,
                        210,
                        131 - p,
                        34,
                        p,
                        256,
                        256,
                        ARGB.opaque(be.leftStack.material.moltenColor));
        }
        if (be.rightStack != null) {
            int p = be.rightStack.amount * 42 / BlockEntityMachineElectrolyser.MAX_MATERIAL;
            if (p > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        96,
                        60 - p,
                        210,
                        131 - p,
                        34,
                        p,
                        256,
                        256,
                        ARGB.opaque(be.rightStack.material.moltenColor));
        }

        int pw = (int) (be.power * 89 / BlockEntityMachineElectrolyser.MAX_POWER);
        if (pw > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    186,
                    107 - pw,
                    210,
                    89 - pw,
                    16,
                    pw,
                    256,
                    256);

        if (be.power >= be.usageOre) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 190, 4, 226, 25, 9, 12, 256, 256);
        }

        int time = be.processOreTime > 0 ? be.processOreTime : 1;
        int o = be.progressOre * 26 / time;
        if (o > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 7, 71 - o, 226, 25 - o, 22, o, 256, 256);

        drawFluidBar(graphics, 36, 18, 16, 52, be.tanks[3]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 36, 18, 16, 52, be.tanks[3]);

        materialStat(graphics, mouseX, mouseY, 58, be.leftStack);
        materialStat(graphics, mouseX, mouseY, 96, be.rightStack);

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

    private void materialStat(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            Mats.MaterialStack stack) {
        if (stack != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    x,
                    18,
                    34,
                    42,
                    List.of(
                            Component.literal(
                                            stack.material.getLocalizedName()
                                                    + ": "
                                                    + Mats.formatAmount(
                                                            stack.amount,
                                                            ModifierKeys.leftShiftHeld()))
                                    .withStyle(ChatFormatting.YELLOW)));
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    x,
                    18,
                    34,
                    42,
                    List.of(
                            Component.translatable("desc.shared.empty")
                                    .withStyle(ChatFormatting.RED)));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 8, 82, 54, 12)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgf", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
