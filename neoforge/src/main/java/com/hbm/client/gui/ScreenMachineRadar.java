// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.entity.RadarEntry;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.MenuMachineRadar;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineRadar extends ScreenInfoContainer<MenuMachineRadar> {

    public static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_radar_nt.png");

    private static final int BUTTON_X = -10;
    private static final String[] BUTTON_COMMAND = {
        "missiles", "shells", "players", "smart", "red", "map", "gui1", "clear"
    };
    private static final int[] BUTTON_Y = {88, 98, 108, 118, 128, 138, 158, 178};
    private static final String[] BUTTON_TOOLTIP = {
        "radar.detectMissiles", "radar.detectShells", "radar.detectPlayers", "radar.smartMode",
        "radar.redMode", "radar.showMap", "radar.toggleGui", "radar.clearMap"
    };

    private static final int LAMP_COUNT = 6;

    private static final double SCOPE_SPAN = 200D - 8D;
    private static final double SCOPE_REVERSE_SPAN = 192D;

    private int lastMouseX;
    private int lastMouseY;

    public ScreenMachineRadar(MenuMachineRadar menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 216, 234);
    }

    private BlockEntityMachineRadar be() {
        return menu.blockEntity();
    }

    private static double blipOffset(int delta, int range) {
        return delta / ((double) range * 2 + 1) * SCOPE_SPAN;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        BlockEntityMachineRadar be = be();
        drawBackground(graphics, be);
        drawTooltips(graphics, be, mouseX, mouseY);
    }

    private void drawBackground(GuiGraphicsExtractor graphics, BlockEntityMachineRadar be) {
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
                RenderPipelines.GUI_TEXTURED, TEXTURE, -14, 84, 224.0F, 0.0F, 14, 66, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, -14, 154, 224.0F, 66.0F, 14, 36, 256, 256);

        if (be.power > 0) {
            int i = (int) (be.power * 200 / be.getMaxPower());
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 221, 0.0F, 234.0F, i, 16, 256, 256);
        }

        RandomSource random = be.getLevel().getRandom();
        boolean[] lamps = {
            be.scanMissiles, be.scanShells, be.scanPlayers, be.smartMode, be.redMode, be.showMap
        };
        for (int i = 0; i < LAMP_COUNT; i++) {

            if (!(lamps[i] ^ (be.jammed && random.nextBoolean()))) continue;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BUTTON_X,
                    BUTTON_Y[i],
                    238.0F,
                    4.0F + i * 10,
                    8,
                    8,
                    256,
                    256);
        }

        if (be.power < MachineData.RADAR_CONSUMPTION.get()) return;

        if (be.jammed) {

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            8 + i * 40,
                            17 + j * 40,
                            216.0F,
                            118.0F + random.nextInt(81),
                            40,
                            40,
                            256,
                            256);
                }
            }
            return;
        }

        if (be.showMap) {
            RadarScopeElements.map(graphics, be.map, BlockEntityMachineRadar.MAP_SIDE);
        }

        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        RadarScopeElements.sweep(
                graphics, be.prevRotation + (be.rotation - be.prevRotation) * partial);

        int range = be.getRange();
        for (RadarEntry m : be.entries) {
            double x = blipOffset(m.posX - be.getBlockPos().getX(), range) - 4D;
            double z = blipOffset(m.posZ - be.getBlockPos().getZ(), range) - 4D;
            graphics.pose().pushMatrix();
            graphics.pose()
                    .translate(
                            (float) (RadarScopeElements.SCOPE_X + x),
                            (float) (RadarScopeElements.SCOPE_Y + z));
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    0,
                    0,
                    216.0F,
                    8.0F * m.blipLevel,
                    8,
                    8,
                    256,
                    256);
            graphics.pose().popMatrix();
        }
    }

    private void drawTooltips(
            GuiGraphicsExtractor graphics, BlockEntityMachineRadar be, int mouseX, int mouseY) {
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 221, 200, 7, be.power, be.getMaxPower());

        for (int i = 0; i < BUTTON_Y.length; i++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    BUTTON_X,
                    BUTTON_Y[i],
                    8,
                    8,
                    lineArray(BUTTON_TOOLTIP[i]));
        }

        int range = be.getRange();
        for (RadarEntry m : be.entries) {
            int x =
                    leftPos
                            + (int) blipOffset(m.posX - be.getBlockPos().getX(), range)
                            + RadarScopeElements.SCOPE_X;
            int z =
                    topPos
                            + (int) blipOffset(m.posZ - be.getBlockPos().getZ(), range)
                            + RadarScopeElements.SCOPE_Y;

            if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                Component.literal(I18nUtil.resolveKey(m.radarName)),
                                Component.literal(m.posX + " / " + m.posZ),
                                Component.translatable("desc.gui.machineRadar.alt", m.posY)),
                        x,
                        z);
                return;
            }
        }

        if (checkClick(mouseX, mouseY, 8, 17, 200, 200)) {
            graphics.setComponentTooltipForNextFrame(
                    this.font,
                    List.of(Component.literal(targetX(be) + " / " + targetZ(be))),
                    mouseX,
                    mouseY);
        }
    }

    private int targetX(BlockEntityMachineRadar be) {
        return (int)
                ((lastMouseX - leftPos - RadarScopeElements.SCOPE_X)
                                * ((double) be.getRange() * 2 + 1)
                                / SCOPE_REVERSE_SPAN
                        + be.getBlockPos().getX());
    }

    private int targetZ(BlockEntityMachineRadar be) {
        return (int)
                ((lastMouseY - topPos - RadarScopeElements.SCOPE_Y)
                                * ((double) be.getRange() * 2 + 1)
                                / SCOPE_REVERSE_SPAN
                        + be.getBlockPos().getZ());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < BUTTON_Y.length; i++) {
                if (!checkClick((int) event.x(), (int) event.y(), BUTTON_X, BUTTON_Y[i], 8, 8))
                    continue;
                playClick();
                CompoundTag data = new CompoundTag();
                data.putBoolean(BUTTON_COMMAND[i], true);
                Services.NETWORK.sendToServer(new NbtControlPayload(be().getBlockPos(), data));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int slot = event.key() - 49;
        if (slot < 0
                || slot >= BlockEntityMachineRadar.SLOT_LINK_COUNT
                || !checkClick(lastMouseX, lastMouseY, 8, 17, 200, 200)) {
            return super.keyPressed(event);
        }

        BlockEntityMachineRadar be = be();
        int range = be.getRange();
        CompoundTag data = new CompoundTag();
        data.putInt("link", slot);

        for (RadarEntry m : be.entries) {
            int x =
                    leftPos
                            + (int) blipOffset(m.posX - be.getBlockPos().getX(), range)
                            + RadarScopeElements.SCOPE_X;
            int z =
                    topPos
                            + (int) blipOffset(m.posZ - be.getBlockPos().getZ(), range)
                            + RadarScopeElements.SCOPE_Y;

            if (lastMouseX + 5 > x
                    && lastMouseX - 4 <= x
                    && lastMouseY + 5 > z
                    && lastMouseY - 4 <= z) {
                data.putInt("launchEntity", m.entityID);
                Services.NETWORK.sendToServer(new NbtControlPayload(be.getBlockPos(), data));
                return true;
            }
        }

        data.putInt("launchPosX", targetX(be));
        data.putInt("launchPosZ", targetZ(be));
        Services.NETWORK.sendToServer(new NbtControlPayload(be.getBlockPos(), data));
        return true;
    }
}
