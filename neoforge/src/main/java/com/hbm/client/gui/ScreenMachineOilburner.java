// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineOilburner;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityHeaterOilburner;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenMachineOilburner extends ScreenInfoContainer<MenuMachineOilburner> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_oilburner.png");

    private static final int TANK_X = 44, TANK_Y = 17, TANK_W = 16, TANK_H = 52;
    private static final int HEAT_X = 116, HEAT_Y = 17, HEAT_W = 16, HEAT_H = 52;
    private static final int FLAME_X = 79, FLAME_Y = 34, FLAME_W = 18, FLAME_H = 18;
    private static final int ON_X = 70, ON_Y = 54, ON_W = 35, ON_H = 14;
    private static final int TOGGLE_X = 80, TOGGLE_Y = 54, TOGGLE_W = 16, TOGGLE_H = 14;

    public ScreenMachineOilburner(MenuMachineOilburner menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 203);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityHeaterOilburner be = burner();

        int i = be.heatEnergy * HEAT_H / BlockEntityHeaterOilburner.MAX_HEAT_ENERGY;
        if (i > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    HEAT_X,
                    HEAT_Y + (HEAT_H - i),
                    194,
                    HEAT_H - i,
                    HEAT_W,
                    i,
                    256,
                    256);
        }

        Fluid type = be.tank.getTankType();
        FT_Flammable trait = NTMFluidProperties.getTrait(type, FT_Flammable.class);

        if (be.isOn) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    ON_X,
                    ON_Y,
                    210,
                    0,
                    ON_W,
                    ON_H,
                    256,
                    256);
            if (trait != null && be.tank.getFill() > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        FLAME_X,
                        FLAME_Y,
                        176,
                        0,
                        FLAME_W,
                        FLAME_H,
                        256,
                        256);
            }
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        if (checkClick(mouseX, mouseY, HEAT_X, HEAT_Y, HEAT_W, HEAT_H)) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    HEAT_X,
                    HEAT_Y,
                    HEAT_W,
                    HEAT_H,
                    List.of(
                            Component.literal(
                                    String.format(Locale.US, "%,d", be.heatEnergy)
                                            + " / "
                                            + String.format(
                                                    Locale.US,
                                                    "%,d",
                                                    BlockEntityHeaterOilburner.MAX_HEAT_ENERGY)
                                            + " TU")));
        }

        if (trait != null && checkClick(mouseX, mouseY, FLAME_X, FLAME_Y, FLAME_W, FLAME_H)) {
            int heat = (int) (trait.getHeatEnergy() * be.setting / 1000L);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    FLAME_X,
                    FLAME_Y,
                    FLAME_W,
                    FLAME_H,
                    List.of(
                            Component.literal(be.setting + " mB/t"),
                            Component.literal(heat + " TU/t")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick(
                        (int) event.x(), (int) event.y(), TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) {
            BlockPos pos = menu.blockEntity().getBlockPos();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            Services.NETWORK.sendToServer(new NbtControlPayload(pos, data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityHeaterOilburner burner() {
        return menu.blockEntity();
    }
}
