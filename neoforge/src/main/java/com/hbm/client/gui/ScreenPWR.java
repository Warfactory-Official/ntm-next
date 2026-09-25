// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPWR;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ScreenPWR extends ScreenInfoContainer<MenuPWR> {

    private static final Identifier TEXTURE = Library.id("textures/gui/reactors/gui_pwr.png");

    private EditBox rodField;

    public ScreenPWR(MenuPWR menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 188);
    }

    private static String fmt(long v) {
        return String.format(Locale.US, "%,d", v);
    }

    @Override
    protected void init() {
        super.init();
        rodField = new EditBox(this.font, leftPos + 57, topPos + 63, 30, 8, Component.empty());
        rodField.setMaxLength(3);
        rodField.setBordered(false);
        rodField.setTextColor(CommonColors.GREEN);
        BlockEntityMachinePWRController be = be();
        rodField.setValue(String.valueOf(be != null ? (int) (100 - be.rodTarget) : 100));
        addRenderableWidget(rodField);
    }

    @Override
    protected boolean drawTitle() {
        return false;
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
        BlockEntityMachinePWRController be = be();
        if (be != null) {
            if (be.hullHeat > BlockEntityMachinePWRController.coreHeatCapacityBase * 0.8
                    || be.coreHeat > be.coreHeatCapacity * 0.8) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 147, 0, 176, 14, 26, 26, 256, 256);
            }
            if (be.processTime > 0) {
                int p = (int) (be.progress * 33 / be.processTime);
                if (p > 0)
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            54,
                            33,
                            176,
                            0,
                            Math.min(p, 33),
                            14,
                            256,
                            256);
            }
            int c = (int) (be.rodLevel * 52 / 100);
            if (c > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 54, 176, 40, c, 2, 256, 256);

            SmoothGaugeElement.draw(
                    graphics,
                    124,
                    40,
                    (double) be.coreHeat / be.coreHeatCapacity,
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);
            SmoothGaugeElement.draw(
                    graphics,
                    160,
                    40,
                    (double) be.hullHeat / BlockEntityMachinePWRController.coreHeatCapacityBase,
                    5,
                    2,
                    1,
                    0x7F0000,
                    0x000000);

            if (be.typeLoaded >= 0
                    && be.typeLoaded < ModItems.PWR_FUELS.size()
                    && be.amountLoaded > 0) {
                int x = 89;
                int y = 5;
                ItemStack fuel = new ItemStack(ModItems.PWR_FUELS.get(be.typeLoaded).get());
                String label = ChatFormatting.YELLOW + "" + be.amountLoaded + "/" + be.rodCount;
                graphics.item(fuel, x, y);
                graphics.pose().pushMatrix();
                graphics.pose().scale(0.5F, 0.5F);
                graphics.itemDecorations(
                        this.font, fuel, (x + this.font.width(label) / 4) * 2, (y + 15) * 2, label);
                graphics.pose().popMatrix();
            }

            graphics.pose().pushMatrix();
            graphics.pose().scale(1F / 1.25F, 1F / 1.25F);
            String fluxStr = String.format(Locale.US, "%,.1f", be.flux);

            graphics.text(
                    this.font,
                    fluxStr,
                    (int) (165 * 1.25F - this.font.width(fluxStr)),
                    (int) (64 * 1.25F),
                    0xFF00FF00,
                    false);
            graphics.pose().popMatrix();

            drawFluidBar(graphics, 8, 5, 16, 52, be.tanks[0]);
            drawFluidBar(graphics, 26, 5, 16, 52, be.tanks[1]);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 5, 16, 52, be.tanks[0]);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 26, 5, 16, 52, be.tanks[1]);

            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    115,
                    31,
                    18,
                    18,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.core",
                                    fmt(be.coreHeat),
                                    fmt(be.coreHeatCapacity))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    151,
                    31,
                    18,
                    18,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.hull",
                                    fmt(be.hullHeat),
                                    fmt(BlockEntityMachinePWRController.coreHeatCapacityBase))));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    52,
                    31,
                    36,
                    18,
                    List.of(
                            Component.literal(
                                    (be.processTime > 0
                                                    ? (int) (be.progress * 100 / be.processTime)
                                                    : 0)
                                            + "%")));
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    52,
                    53,
                    54,
                    4,
                    List.of(
                            Component.translatable(
                                    "desc.gui.pwr.controlRodWithdrawal",
                                    100 - Math.round(be.rodLevel)),
                            Component.translatable(
                                    "desc.gui.pwr.flux",
                                    String.format(Locale.US, "%,.1f", be.flux))));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 88, 58, 18, 18)) {
            sendRodLevel();
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendRodLevel() {
        BlockPos core = corePos();
        if (core == null || rodField == null) return;
        int withdrawal;
        try {
            withdrawal = Math.clamp(Integer.parseInt(rodField.getValue().trim()), 0, 100);
        } catch (NumberFormatException e) {
            return;
        }
        rodField.setValue(String.valueOf(withdrawal));
        CompoundTag data = new CompoundTag();
        data.putInt("control", 100 - withdrawal);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityMachinePWRController be() {
        return menu.blockEntity();
    }
}
