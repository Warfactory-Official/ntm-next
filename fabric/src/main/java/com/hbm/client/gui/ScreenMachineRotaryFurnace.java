// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.ModifierKeys;
import com.hbm.client.render.HbmRenderPipelines;
import com.hbm.inventory.container.MenuMachineRotaryFurnace;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityMachineRotaryFurnace;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRotaryFurnace extends ScreenInfoContainer<MenuMachineRotaryFurnace> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_rotary_furnace.png");

    public ScreenMachineRotaryFurnace(
            MenuMachineRotaryFurnace menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return (this.imageWidth - 54) / 2;
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

        BlockEntityMachineRotaryFurnace be = furnace();

        int p = (int) Math.ceil(be.progress * 33);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 63, 30, 176, 0, p, 10, 256, 256);

        if (be.maxBurnTime > 0 && be.burnTime > 0) {
            int b = be.burnTime * 14 / be.maxBurnTime;
            if (b > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        26,
                        69 - b,
                        176,
                        24 - b,
                        14,
                        b,
                        256,
                        256);
        }

        if (be.output != null) {
            int amount = be.output.amount * 52 / BlockEntityMachineRotaryFurnace.MAX_OUTPUT;
            if (amount > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        98,
                        70 - amount,
                        176,
                        76 - amount,
                        16,
                        amount,
                        256,
                        256,
                        ARGB.opaque(be.output.material.moltenColor));
                graphics.blit(
                        HbmRenderPipelines.GUI_TEXTURED_ADDITIVE,
                        TEXTURE,
                        98,
                        70 - amount,
                        176,
                        76 - amount,
                        16,
                        amount,
                        256,
                        256,
                        0x4CFFFFFF);
            }
        }

        drawFluidBarH(graphics, 8, 36, 52, 16, be.tanks[0]);
        drawFluidBar(graphics, 134, 18, 16, 52, be.tanks[1]);
        drawFluidBar(graphics, 152, 18, 16, 52, be.tanks[2]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 36, 52, 16, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 134, 18, 16, 52, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 18, 16, 52, be.tanks[2]);

        if (be.output == null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    98,
                    18,
                    16,
                    52,
                    List.of(
                            Component.translatable("desc.shared.empty")
                                    .withStyle(ChatFormatting.RED)));
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    98,
                    18,
                    16,
                    52,
                    List.of(
                            Component.literal(
                                            be.output.material.getLocalizedName()
                                                    + ": "
                                                    + Mats.formatAmount(
                                                            be.output.amount,
                                                            ModifierKeys.leftShiftHeld()))
                                    .withStyle(ChatFormatting.YELLOW)));
        }

        if (menu.getCarried().isEmpty()
                && be.getItem(4).isEmpty()
                && isHovering(44, 54, 16, 16, mouseX, mouseY)) {
            List<Component> desc = new ArrayList<>();
            for (String line : BlockEntityMachineRotaryFurnace.BURN_MODULE.getDesc())
                desc.add(Component.literal(line));
            if (!desc.isEmpty())
                graphics.setComponentTooltipForNextFrame(this.font, desc, mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineRotaryFurnace furnace() {
        return menu.blockEntity();
    }
}
