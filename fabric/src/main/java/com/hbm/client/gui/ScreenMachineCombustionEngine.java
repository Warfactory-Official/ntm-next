// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineCombustionEngine;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.items.ItemPistons.EnumPistonType;
import com.hbm.items.ItemPistons;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineCombustionEngine;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineCombustionEngine
        extends ScreenInfoContainer<MenuMachineCombustionEngine> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/generators/gui_combustion.png");

    private static final int POWER_X = 143, POWER_Y = 17, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 35, TANK_Y = 17, TANK_W = 16, TANK_H = 52;

    private int setting;
    private boolean mouseLocked = false;

    public ScreenMachineCombustionEngine(
            MenuMachineCombustionEngine menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.setting = menu.blockEntity().setting;
    }

    @Override
    protected int titleColor() {
        return 0xFF404040;
    }

    @Override
    protected boolean drawTitle() {
        return false;
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

        BlockEntityMachineCombustionEngine be = engine();

        if (mouseLocked) {
            int dragged = Mth.clamp((mouseX - leftPos - 81) * 30 / 32, 0, 30);
            if (dragged != this.setting) {
                this.setting = dragged;
                sendSetting(dragged);
            }
        }

        ItemStack pistonStack = be.getItem(BlockEntityMachineCombustionEngine.SLOT_PISTON);
        boolean hasPiston = pistonStack.getItem() instanceof ItemPistons;
        if (hasPiston) {
            int variant = ((ItemPistons) pistonStack.getItem()).type.ordinal();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    80,
                    51,
                    176,
                    52 + variant * 12,
                    25,
                    12,
                    256,
                    256);
        }
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                79 + (setting * 32 / 30),
                38,
                192,
                15,
                4,
                8,
                256,
                256);
        if (be.isOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 79, 13, 192, 0, 35, 15, 256, 256);
        }

        if (be.power > 0) {
            int p = (int) (be.power * POWER_H / BlockEntityMachineCombustionEngine.maxPower);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        176,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        if (!mouseLocked) {
            drawElectricityInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    POWER_X,
                    POWER_Y,
                    POWER_W,
                    POWER_H,
                    be.power,
                    BlockEntityMachineCombustionEngine.maxPower);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        }

        if (mouseLocked || checkClick(mouseX, mouseY, 80, 38, 34, 8)) {
            graphics.setComponentTooltipForNextFrame(
                    this.font,
                    List.of(Component.literal(((setting * 2) / 10D) + " mB/t")),
                    mouseX,
                    mouseY);
        }

        if (hasPiston) {
            Fluid type = be.tank.getTankType();
            FT_Combustible trait = NTMFluidProperties.getTrait(type, FT_Combustible.class);
            double he = 0;
            if (trait != null) {
                EnumPistonType piston = ((ItemPistons) pistonStack.getItem()).type;
                he =
                        setting
                                * 0.2
                                * trait.getCombustionEnergy()
                                / 1_000D
                                * piston.eff[trait.getGrade().ordinal()];
            }
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    79,
                    50,
                    35,
                    14,
                    List.of(
                            Component.literal(String.format(Locale.US, "%,d", (long) he) + " HE/t")
                                    .withStyle(ChatFormatting.YELLOW),
                            Component.literal(
                                            String.format(
                                                            Locale.US,
                                                            "%,d",
                                                            (long)
                                                                    (he
                                                                            * SharedConstants
                                                                                    .TICKS_PER_SECOND))
                                                    + " HE/s")
                                    .withStyle(ChatFormatting.YELLOW)));
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                79,
                13,
                35,
                15,
                List.of(Component.translatable("desc.gui.machineCombustionEngine.ignition")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, 89, 13, 16, 14)) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("turnOn", true);
                send(data);
                playClick();
                return true;
            }
            if (checkClick(mx, my, 79, 38, 36, 8)) {
                mouseLocked = true;
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseLocked) {
            mouseLocked = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void sendSetting(int value) {
        CompoundTag data = new CompoundTag();
        data.putInt("setting", value);
        send(data);
    }

    private void send(CompoundTag data) {
        BlockPos pos = menu.blockEntity().getBlockPos();
        Services.NETWORK.sendToServer(new NbtControlPayload(pos, data));
    }

    private BlockEntityMachineCombustionEngine engine() {
        return menu.blockEntity();
    }
}
