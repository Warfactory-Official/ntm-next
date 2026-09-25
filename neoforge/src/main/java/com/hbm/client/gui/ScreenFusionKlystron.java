// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFusionKlystron;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFusionKlystron extends ScreenInfoContainer<MenuFusionKlystron> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/reactors/gui_fusion_klystron.png");

    private static final int POWER_X = 8, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int STAT_W = 18, STAT_H = 18, STAT_Y = 71;
    private static final int OUT_X = 43, AIR_X = 76, HE_X = 115;
    private static final int LED_Y = 71, LED_POWER_X = 160, LED_AIR_X = 170, LED_ACTION_X = 180;

    private EditBox field;

    public ScreenFusionKlystron(MenuFusionKlystron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 200);
        this.inventoryLabelX = 35;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    protected int titleCenterX() {
        return 115;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(font, leftPos + 84, topPos + 22, 102, 12, Component.empty());
        field.setTextColor(0xFF00FF00);
        field.setTextColorUneditable(0xFF00FF00);
        field.setBordered(false);
        field.setValue(Long.toString(menu.blockEntity().outputTarget));
        addRenderableWidget(field);
    }

    @Override
    protected void fieldKeyTaken(EditBox box) {
        sendTarget();
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean typing = field.isFocused();
        boolean handled = super.charTyped(event);
        if (typing && handled) sendTarget();
        return handled;
    }

    private void sendTarget() {
        String text = field.getValue();
        if (text.startsWith("0")) field.setValue(text.substring(1));
        if (field.getValue().isEmpty()) field.setValue("0");
        text = field.getValue();
        if (!text.chars().allMatch(Character::isDigit)) return;

        long amount;
        try {
            amount = Long.parseLong(text);
        } catch (NumberFormatException e) {
            amount = 0L;
        }
        CompoundTag data = new CompoundTag();
        data.putLong("amount", amount);
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
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
        BlockEntityFusionKlystron be = menu.blockEntity();

        long maxPower = be.getMaxPower();
        if (maxPower > 0) {
            int p = (int) (be.power * POWER_H / maxPower);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        194,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        double outputGauge =
                be.outputTarget <= 0 ? 0 : (double) be.output / (double) be.outputTarget;
        double airGauge = (double) be.compair.getFill() / (double) be.compair.getMaxFill();
        double powerGauge = BlockEntityFusionTorus.getSpeedScaled(be.maxPower, be.power);

        led(graphics, LED_POWER_X, powerGauge >= 0.5 && be.output > 0, powerGauge > 0);
        led(graphics, LED_AIR_X, airGauge >= 0.5 && be.output > 0, airGauge > 0);
        led(graphics, LED_ACTION_X, be.output >= be.outputTarget && be.output > 0, be.output > 0);

        SmoothGaugeElement.draw(graphics, 52, 80, outputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 88, 80, airGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 124, 80, powerGauge, 5, 2, 1, 0xA00000, 0x000000);

        String result = "= " + BobMathUtil.getShortNumber(be.outputTarget) + "KyU";
        if (be.outputTarget == BlockEntityFusionKlystron.MAX_OUTPUT) result += " (max)";
        graphics.text(
                font, Component.literal(result), 183 - font.width(result), 40, 0xFF00FF00, false);

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, be.power, maxPower);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                OUT_X,
                STAT_Y,
                STAT_W,
                STAT_H,
                List.of(
                        Component.literal(
                                ChatFormatting.RED
                                        + "<- "
                                        + ChatFormatting.RESET
                                        + BobMathUtil.getShortNumber(be.output)
                                        + "KyU / "
                                        + BobMathUtil.getShortNumber(be.outputTarget)
                                        + "KyU")));
        drawFluidGaugeInfo(graphics, mouseX, mouseY, AIR_X, STAT_Y, STAT_W, STAT_H, be.compair);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                HE_X,
                STAT_Y,
                STAT_W,
                STAT_H,
                List.of(
                        Component.literal(
                                ChatFormatting.GREEN
                                        + "-> "
                                        + ChatFormatting.RESET
                                        + BobMathUtil.getShortNumber(be.output)
                                        + "HE / "
                                        + BobMathUtil.getShortNumber(be.outputTarget)
                                        + "HE")));
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void led(GuiGraphicsExtractor graphics, int x, boolean lit, boolean on) {
        if (lit)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, LED_Y, 210, 8, 8, 8, 256, 256);
        else if (on)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, LED_Y, 210, 0, 8, 8, 256, 256);
    }
}
