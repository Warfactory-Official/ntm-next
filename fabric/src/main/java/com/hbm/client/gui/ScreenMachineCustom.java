// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCustom;
import com.hbm.inventory.machine.CustomMachineDefinition;
import com.hbm.inventory.slot.SlotPattern;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.tileentity.machine.BlockEntityCustomMachine;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenMachineCustom extends ScreenInfoContainer<MenuMachineCustom> {

    private static final Identifier TEXTURE = Library.id("textures/gui/processing/gui_custom.png");

    private static final int POWER_X = 150, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int TANK_TOP = 18, TANK_W = 16, TANK_H = 34;
    private static final int IN_X0 = 8, OUT_X0 = 78, STRIDE = 18;
    private static final int HEAT_X = 61, HEAT_Y = 53, HEAT_SIZE = 18;
    private static final int ARROW_SPAN = 90, ARROW_FIRST = 44;

    public ScreenMachineCustom(MenuMachineCustom menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 256);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 68;
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

        BlockEntityCustomMachine be = menu.blockEntity();
        CustomMachineDefinition definition = be.definition;
        if (definition == null) {
            super.extractLabels(graphics, mouseX, mouseY);
            return;
        }

        if (definition.fluxMode()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    78,
                    54,
                    192.0F,
                    122.0F,
                    51,
                    15,
                    256,
                    256);
        }
        if (definition.maxHeat() > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    HEAT_Y,
                    236.0F,
                    0.0F,
                    HEAT_SIZE,
                    HEAT_SIZE,
                    256,
                    256);
            SmoothGaugeElement.draw(
                    graphics,
                    70,
                    62,
                    (double) be.heat / definition.maxHeat(),
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);
        }

        int arrow = be.progress * ARROW_SPAN / Math.max(be.maxProgress, 1);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                78,
                119,
                192.0F,
                0.0F,
                Math.min(arrow, ARROW_FIRST),
                16,
                256,
                256);
        if (arrow > ARROW_FIRST) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    78 + ARROW_FIRST,
                    119,
                    192.0F,
                    16.0F,
                    arrow - ARROW_FIRST,
                    16,
                    256,
                    256);
        }

        int filled = (int) (be.power * POWER_H / Math.max(definition.maxPower(), 1L));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                POWER_X,
                POWER_Y + POWER_H - filled,
                176.0F,
                POWER_H - filled,
                POWER_W,
                filled,
                256,
                256);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (definition.itemInCount() <= index) {
                    plate(graphics, 7 + col * STRIDE, 71 + row * STRIDE, col, row);
                    plate(graphics, 7 + col * STRIDE, 107 + row * STRIDE, col, row);
                }
                if (definition.itemOutCount() <= index) {
                    plate(graphics, 77 + col * STRIDE, 71 + row * STRIDE, col, row);
                }
            }
        }
        for (int i = 0; i < BlockEntityCustomMachine.MAX_FLUID_IN; i++) {
            if (definition.fluidInCount() <= i) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        7 + i * STRIDE,
                        17,
                        192.0F + i * STRIDE,
                        32.0F,
                        18,
                        54,
                        256,
                        256);
            }
            if (definition.fluidOutCount() <= i) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        77 + i * STRIDE,
                        17,
                        192.0F + i * STRIDE,
                        32.0F,
                        18,
                        36,
                        256,
                        256);
            }
        }

        for (int i = 0; i < be.inputTanks.length; i++) {
            drawFluidBar(graphics, IN_X0 + i * STRIDE, TANK_TOP, TANK_W, TANK_H, be.inputTanks[i]);
        }
        for (int i = 0; i < be.outputTanks.length; i++) {
            drawFluidBar(
                    graphics, OUT_X0 + i * STRIDE, TANK_TOP, TANK_W, TANK_H, be.outputTanks[i]);
        }

        if (definition.fluxMode()) {
            graphics.text(
                    this.font,
                    Component.translatable("gui.customMachine.flux", be.flux),
                    83,
                    57,
                    0xFF08FF00,
                    false);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                definition.maxPower());
        if (definition.maxHeat() > 0) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    HEAT_X,
                    HEAT_Y,
                    HEAT_SIZE,
                    HEAT_SIZE,
                    List.of(
                            Component.translatable(
                                    "gui.customMachine.heat",
                                    grouped(be.heat),
                                    grouped(definition.maxHeat()))));
        }
        for (int i = 0; i < be.inputTanks.length; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    IN_X0 + i * STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.inputTanks[i]);
        }
        for (int i = 0; i < be.outputTanks.length; i++) {
            drawFluidGaugeInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    OUT_X0 + i * STRIDE,
                    TANK_TOP,
                    TANK_W,
                    TANK_H,
                    be.outputTanks[i]);
        }

        if (this.menu.getCarried().isEmpty()) {
            for (Slot slot : this.menu.slots) {
                if (!(slot instanceof SlotPattern)
                        || !isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) continue;
                int index = slot.getContainerSlot() - BlockEntityCustomMachine.SLOT_TEMPLATE_START;
                String mode = be.matcher.mode(index);
                if (mode == null) continue;
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                Component.translatable("gui.customMachine.pattern")
                                        .withStyle(ChatFormatting.RED),
                                ModulePatternMatcher.getLabel(mode)),
                        mouseX,
                        mouseY - 30);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private static Component grouped(int value) {
        return Component.literal(String.format(Locale.US, "%,d", value));
    }

    private void plate(GuiGraphicsExtractor graphics, int x, int y, int col, int row) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                192.0F + col * STRIDE,
                86.0F + row * STRIDE,
                18,
                18,
                256,
                256);
    }
}
