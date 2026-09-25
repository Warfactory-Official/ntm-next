// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineTurbineGas;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineTurbineGas;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineTurbineGas extends ScreenInfoContainer<MenuMachineTurbineGas> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/generators/gui_turbinegas.png");

    private int numberToDisplay = 0;
    private int digitNumber = 0;
    private int exponent = 0;

    private boolean sliderGrabbed = false;

    public ScreenMachineTurbineGas(
            MenuMachineTurbineGas menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 223);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private static List<Component> splitLang(String key) {
        String resolved = Component.translatable(key).getString();
        List<Component> lines = new ArrayList<>();
        for (String part : resolved.split("\n")) lines.add(Component.literal(part));
        return lines;
    }

    @Override
    protected int titleCenterX() {
        return 88;
    }

    @Override
    protected int titleColor() {
        return 0xFF404040;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityMachineTurbineGas be = turbine();

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

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                74,
                86,
                194,
                be.autoMode ? 11 : 24,
                29,
                13,
                256,
                256);

        switch (be.state) {
            case 0 ->
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            80,
                            32,
                            178,
                            38,
                            16,
                            16,
                            256,
                            256);
            case -1 -> {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 80, 32, 194, 38, 16, 16, 256, 256);
                displayStartup(graphics, be);
            }
            case 1 -> {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 80, 32, 210, 38, 16, 16, 256, 256);
                drawPowerMeterDisplay(
                        graphics, SharedConstants.TICKS_PER_SECOND * be.instantPowerOutput);
            }
            default -> {}
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                36,
                97 - be.powerSliderPos,
                178,
                0,
                16,
                6,
                256,
                256);

        int power = (int) (be.power * 142 / BlockEntityMachineTurbineGas.maxPower);
        if (power > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 26, 109, 0, 223, power, 16, 256, 256);

        drawRPMGauge(graphics, be.rpm);
        drawThermometer(graphics, be.temp);

        drawFluidBar(graphics, 8, 17, 16, 48, be.tanks[0]);
        drawFluidBar(graphics, 8, 71, 16, 32, be.tanks[1]);
        drawFluidBar(graphics, 147, 62, 16, 36, be.tanks[2]);
        drawFluidBar(graphics, 147, 22, 16, 36, be.tanks[3]);

        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 16, 16, 48, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 8, 70, 16, 32, be.tanks[1]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 147, 61, 16, 36, be.tanks[2]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 147, 21, 16, 36, be.tanks[3]);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                26,
                108,
                142,
                16,
                be.power,
                BlockEntityMachineTurbineGas.maxPower);

        if (be.state == 1) {
            double consumption =
                    BlockEntityMachineTurbineGas.fuelMaxConsumption(be.tanks[0].getTankType());
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    36,
                    36,
                    16,
                    66,
                    List.of(
                            Component.literal(
                                    "Fuel consumption: "
                                            + SharedConstants.TICKS_PER_SECOND
                                                    * (consumption * 0.05D
                                                            + consumption * be.throttle / 100)
                                            + " mb/s")));
        } else {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    36,
                    36,
                    16,
                    66,
                    List.of(Component.translatable("desc.gui.machineTurbineGas.generatorOffline")));
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                133,
                23,
                8,
                72,
                List.of(
                        Component.translatable(
                                "desc.gui.machineTurbineGas.temperature",
                                Math.max(20, be.temp) + "°C")));

        drawInfoPanel(graphics, -16, 34, 16, 16, 3);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                34,
                16,
                16,
                -8,
                60,
                splitLang("desc.gui.turbinegas.automode"));

        drawInfoPanel(graphics, -16, 50, 16, 16, 2);
        List<Component> fuels = new ArrayList<>();
        fuels.add(Component.translatable("desc.gui.turbinegas.fuels"));
        for (Fluid type : NTMFluids.displayOrder()) {
            FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
            if (fuel != null && fuel.getGrade() == FuelGrade.GAS) {
                fuels.add(Component.literal("  " + NTMFluidProperties.clientName(type)));
            }
        }
        drawCustomInfoStat(graphics, mouseX, mouseY, -16, 50, 16, 16, -8, 60, fuels);

        if (be.tanks[0].getFill() < 5000 || be.tanks[1].getFill() < 1000) {
            drawInfoPanel(graphics, -16, 66, 16, 16, 7);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    -16,
                    66,
                    16,
                    16,
                    -8,
                    60,
                    splitLang("desc.gui.turbinegas.warning"));
        }
        if (be.tanks[0].getFill() == 0 || be.tanks[1].getFill() == 0) {
            drawInfoPanel(graphics, -16, 66, 16, 16, 6);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void displayStartup(GuiGraphicsExtractor graphics, BlockEntityMachineTurbineGas be) {
        if (numberToDisplay < 8_888_888 && be.counter < 60) {
            digitNumber++;
            if (digitNumber == 9) {
                digitNumber = 1;
                exponent++;
            }
            numberToDisplay += (int) Math.pow(10, exponent);
        }
        if (be.counter > 50) numberToDisplay = 0;
        drawPowerMeterDisplay(graphics, numberToDisplay);
    }

    private void drawPowerMeterDisplay(GuiGraphicsExtractor graphics, int number) {
        int firstDigitX = 65, firstDigitY = 62;
        int n = Math.max(0, number);
        int[] digit = new int[7];

        for (int i = 6; i >= 0; i--) {
            digit[i] = n % 10;
            n /= 10;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    firstDigitX + i * 7,
                    9 + firstDigitY,
                    194 + digit[i] * 5,
                    0,
                    5,
                    11,
                    256,
                    256);
        }

        int uselessZeros = 0;
        for (int i = 0; i < 6; i++) {
            if (digit[i] == 0) uselessZeros++;
            else break;
        }
        for (int i = 0; i < uselessZeros; i++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    firstDigitX + i * 7,
                    9 + firstDigitY,
                    244,
                    0,
                    5,
                    11,
                    256,
                    256);
        }
    }

    private void drawRPMGauge(GuiGraphicsExtractor graphics, int rpm) {
        GuiTexturedPie.draw(graphics, TEXTURE, 64, 16, 176, 64, 48, 48, rpm / 100D);
    }

    private void drawThermometer(GuiGraphicsExtractor graphics, int temp) {
        int maxTemp = 800;
        int filled = 64 * Math.max(0, Math.min(maxTemp, temp)) / maxTemp;
        if (filled <= 0) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                136,
                28 + (64 - filled),
                176,
                64 - filled,
                2,
                filled,
                256,
                256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            BlockEntityMachineTurbineGas be = turbine();
            int mx = (int) event.x(), my = (int) event.y();

            int handleY = topPos + 97 - be.powerSliderPos;
            sliderGrabbed = my >= handleY && my < handleY + 6;

            double dx = mx - (leftPos + 88), dy = my - (topPos + 40);
            if (dx * dx + dy * dy <= 64 && (be.counter == 0 || be.counter == 579)) {
                sendControl("state", be.state - 1);
                playClick();
                return true;
            }

            if (be.state == 1
                    && mx > leftPos + 74
                    && mx <= leftPos + 74 + 29
                    && my >= topPos + 86
                    && my < topPos + 86 + 13) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("autoMode", !be.autoMode);
                send(data);
                playClick();
                return true;
            }

            if (be.state == 1 && mx > leftPos + 36 && mx <= leftPos + 52 && sliderGrabbed) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("autoMode", false);
                send(data);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        BlockEntityMachineTurbineGas be = turbine();
        int mx = (int) event.x(), my = (int) event.y();

        if (sliderGrabbed
                && be.state == 1
                && !be.autoMode
                && mx > leftPos + 36
                && mx <= leftPos + 52
                && my > topPos + 37
                && my <= topPos + 103) {
            int slidPos = topPos + 100 - my;
            slidPos = Math.max(0, Math.min(60, slidPos));
            sendControl("slidPos", slidPos);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sliderGrabbed = false;
        return super.mouseReleased(event);
    }

    private void sendControl(String key, int value) {
        CompoundTag data = new CompoundTag();
        data.putInt(key, value);
        send(data);
    }

    private void send(CompoundTag data) {
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
    }

    private BlockEntityMachineTurbineGas turbine() {
        return menu.blockEntity();
    }
}
