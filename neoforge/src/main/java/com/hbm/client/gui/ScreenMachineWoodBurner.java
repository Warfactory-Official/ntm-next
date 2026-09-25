// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineWoodBurner;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineWoodBurner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class ScreenMachineWoodBurner extends ScreenInfoContainer<MenuMachineWoodBurner> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/generators/gui_wood_burner_alt.png");

    private static final int POWER_X = 143, POWER_Y = 18, POWER_W = 16, POWER_H = 34;

    private static final int POWER_V_BOTTOM = 52;
    private static final int BURN_X = 17, BURN_Y = 18, BURN_W = 4, BURN_H = 52;
    private static final int TANK_X = 80, TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int ON_X = 53, ON_Y = 17, ON_W = 16, ON_H = 15;
    private static final int SWITCH_X = 46, SWITCH_Y = 37, SWITCH_W = 30, SWITCH_H = 14;

    public ScreenMachineWoodBurner(
            MenuMachineWoodBurner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
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

        BlockEntityMachineWoodBurner be = burner();

        if (be.liquidBurn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 16, 17, 176, 52, 60, 54, 256, 256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 79, 17, 176, 106, 36, 54, 256, 256);
        }

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ON_X,
                    ON_Y,
                    196,
                    0,
                    ON_W,
                    ON_H,
                    256,
                    256);
        }

        long max = be.getMaxPower();
        if (max > 0 && be.power > 0) {
            int p = (int) (be.power * POWER_H / max);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        176,
                        POWER_V_BOTTOM - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        if (!be.liquidBurn && be.maxBurnTime > 0) {
            int b = be.burnTime * BURN_H / be.maxBurnTime;
            if (b > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        BURN_X,
                        BURN_Y + (BURN_H - b),
                        192,
                        BURN_H - b,
                        BURN_W,
                        b,
                        256,
                        256);
            }
        }

        if (be.liquidBurn) {
            drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        }

        drawElectricityInfo(
                graphics, mouseX, mouseY, POWER_X, POWER_Y, POWER_W, POWER_H, be.power, max);

        if (checkClick(mouseX, mouseY, ON_X, ON_Y, ON_W, ON_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    ON_X,
                    ON_Y,
                    ON_W,
                    ON_H,
                    List.of(
                            be.isOn
                                    ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
                                    : Component.literal("OFF").withStyle(ChatFormatting.RED)));
        }

        if (!be.liquidBurn) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    16,
                    17,
                    8,
                    54,
                    List.of(
                            Component.literal(
                                    (be.burnTime / SharedConstants.TICKS_PER_SECOND) + "s")));
        }

        if (menu.getCarried().isEmpty()
                && be.getItem(BlockEntityMachineWoodBurner.SLOT_FUEL).isEmpty()
                && isHovering(26, 18, 16, 16, mouseX, mouseY)) {
            List<Component> desc = new ArrayList<>();
            for (String line : BlockEntityMachineWoodBurner.BURN_MODULE.getDesc())
                desc.add(Component.literal(line));
            if (!desc.isEmpty())
                graphics.setComponentTooltipForNextFrame(this.font, desc, mouseX, mouseY);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            if (checkClick(mx, my, ON_X, ON_Y, ON_W, ON_H)) return sendControl("toggle");
            if (checkClick(mx, my, SWITCH_X, SWITCH_Y, SWITCH_W, SWITCH_H))
                return sendControl("switch");
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean sendControl(String key) {
        BlockPos pos = corePos();
        if (pos == null) return false;
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        Services.NETWORK.sendToServer(new NbtControlPayload(pos, data));
        playClick();
        return true;
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityMachineWoodBurner burner() {
        return menu.blockEntity();
    }
}
