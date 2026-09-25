// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineExcavator;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineExcavator;
import com.hbm.util.I18nUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenMachineExcavator extends ScreenInfoContainer<MenuMachineExcavator> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_mining_drill.png");

    private static final int TOGGLE_Y = 42, TOGGLE_W = 20, TOGGLE_H = 40;
    private static final int LAMP_Y = 5, LAMP_W = 10, LAMP_H = 10;
    private static final int POWER_X = 220, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int TANK_X = 202, TANK_Y = 18, TANK_W = 16, TANK_H = 52;

    private static final Toggle[] TOGGLES = {
        new Toggle(6, "drill", "excavator.drill"),
        new Toggle(30, "crusher", "excavator.crusher"),
        new Toggle(54, "walling", "excavator.walling"),
        new Toggle(78, "veinminer", "excavator.veinminer"),
        new Toggle(102, "silktouch", "excavator.silktouch"),
    };

    public ScreenMachineExcavator(MenuMachineExcavator menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 242, 204);
        this.inventoryLabelX = 8 + 33;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected boolean drawTitle() {
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 242, 96, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, 33, 104, 33.0F, 104.0F, 176, 100, 256, 256);

        BlockEntityMachineExcavator be = excavator();
        boolean blink = System.currentTimeMillis() % 1000L < 500L;

        int p = menu.getPowerScaled(POWER_H);
        if (p > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    70 - p,
                    229.0F,
                    156.0F - p,
                    16,
                    p,
                    256,
                    256);
        }

        if (menu.getPower() > be.getPowerConsumption()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 224, 4, 239.0F, 156.0F, 9, 12, 256, 256);
        }

        if (be.getInstalledDrill() == null && blink) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    171,
                    74,
                    209.0F,
                    154.0F,
                    18,
                    18,
                    256,
                    256);
        }

        boolean powered =
                be.getInstalledDrill() != null && menu.getPower() >= be.getPowerConsumption();
        drawToggle(graphics, TOGGLES[0], be.enableDrill, powered, blink);
        drawToggle(graphics, TOGGLES[1], be.enableCrusher, true, blink);
        drawToggle(graphics, TOGGLES[2], be.enableWalling, true, blink);
        drawToggle(graphics, TOGGLES[3], be.enableVeinMiner, be.canVeinMine(), blink);
        drawToggle(graphics, TOGGLES[4], be.enableSilkTouch, be.canSilkTouch(), blink);

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        for (Toggle toggle : TOGGLES) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    toggle.x,
                    TOGGLE_Y,
                    TOGGLE_W,
                    TOGGLE_H,
                    List.of(Component.literal(I18nUtil.resolveKey(toggle.key))));
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                menu.getPower(),
                BlockEntityMachineExcavator.MAX_POWER);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.tank);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawToggle(
            GuiGraphicsExtractor graphics,
            Toggle toggle,
            boolean on,
            boolean ready,
            boolean blink) {
        if (!on) return;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                toggle.x,
                TOGGLE_Y,
                209.0F,
                114.0F,
                TOGGLE_W,
                TOGGLE_H,
                256,
                256);
        if (ready) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    toggle.x + 5,
                    LAMP_Y,
                    209.0F,
                    104.0F,
                    LAMP_W,
                    LAMP_H,
                    256,
                    256);
        } else if (blink) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    toggle.x + 5,
                    LAMP_Y,
                    219.0F,
                    104.0F,
                    LAMP_W,
                    LAMP_H,
                    256,
                    256);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        for (Toggle toggle : TOGGLES) {
            if (!checkClick(x, y, toggle.x, TOGGLE_Y, TOGGLE_W, TOGGLE_H)) continue;

            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.LEVER_LARGE.get(), 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean(toggle.packetKey, true);
            Services.NETWORK.sendToServer(new NbtControlPayload(excavator().getBlockPos(), data));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private BlockEntityMachineExcavator excavator() {
        return menu.blockEntity();
    }

    private record Toggle(int x, String packetKey, String key) {}
}
