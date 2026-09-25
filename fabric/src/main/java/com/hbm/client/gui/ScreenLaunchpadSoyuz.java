// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuLaunchpadSoyuz;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenLaunchpadSoyuz extends ScreenInfoContainer<MenuLaunchpadSoyuz> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_launchpad_soyuz.png");

    public ScreenLaunchpadSoyuz(MenuLaunchpadSoyuz menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 244);
        titleLabelY = 4;
        inventoryLabelX = 17;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private BlockEntityLaunchpadSoyuz pad() {
        return menu.blockEntity();
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityLaunchpadSoyuz pad = pad();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);

        int power = (int) (pad.power * 52 / BlockEntityLaunchpadSoyuz.MAX_POWER);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                134,
                96 - power,
                194,
                52 - power,
                16,
                power,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                pad.cargoMode ? 79 : 97,
                52,
                pad.cargoMode ? 210 : 228,
                26,
                18,
                18,
                256,
                256);
        pip(graphics, 157, 31, pad.hasJetFuel());
        pip(graphics, 175, 31, pad.hasOxidizer());
        pip(graphics, 139, 31, pad.power >= BlockEntityLaunchpadSoyuz.CONSUMPTION);

        int orbital = pad.orbital();
        if (orbital > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    79,
                    25,
                    210 + (orbital - 1) * 18,
                    8,
                    18,
                    18,
                    256,
                    256);
        if (pad.soyuzStatus == BlockEntityLaunchpadSoyuz.SoyuzStatus.LAUNCHING)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 97, 210, 44, 18, 18, 256, 256);

        drawFluidBar(graphics, 152, 44, 16, 52, pad.fuelTank);
        drawFluidBar(graphics, 170, 44, 16, 52, pad.oxidizerTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 44, 16, 52, pad.fuelTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 170, 44, 16, 52, pad.oxidizerTank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                134,
                44,
                16,
                52,
                pad.power,
                BlockEntityLaunchpadSoyuz.MAX_POWER);

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                53,
                16,
                16,
                -8,
                69,
                lineArray("desc.gui.soyuz.desc"));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 79, 52, 18, 18, lineArray("desc.gui.soyuz.cargo"));
        drawCustomInfoStat(
                graphics, mouseX, mouseY, 97, 52, 18, 18, lineArray("desc.gui.soyuz.satellite"));
        drawInfoPanel(graphics, -16, 53, 16, 16, 2);

        super.extractLabels(graphics, mouseX, mouseY);
        status(graphics, pad);
    }

    private static void pip(GuiGraphicsExtractor graphics, int x, int y, boolean ready) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, ready ? 210 : 216, 0, 6, 8, 256, 256);
    }

    private void status(GuiGraphicsExtractor graphics, BlockEntityLaunchpadSoyuz pad) {
        if (pad.soyuzStatus == BlockEntityLaunchpadSoyuz.SoyuzStatus.LAUNCHING) {
            int countdown = pad.countdown;
            String seconds = String.valueOf(countdown / SharedConstants.TICKS_PER_SECOND);
            String centiseconds = String.valueOf(countdown % SharedConstants.TICKS_PER_SECOND * 5);
            if (seconds.length() == 1) seconds = "0" + seconds;
            if (centiseconds.length() == 1) centiseconds += "0";
            graphics.text(
                    font,
                    Component.literal(seconds + ":" + centiseconds),
                    85,
                    121,
                    0xFFFF0000,
                    false);
            return;
        }
        String key;
        int color;
        switch (pad.soyuzStatus) {
            case ABSENT -> {
                key = "desc.gui.soyuz.idle";
                color = 0xFF0000;
            }
            case LOADING -> {
                key = "desc.gui.soyuz.loading";
                color = 0xFF8000;
            }
            case FUELING -> {
                key = "desc.gui.soyuz.fueling";
                color = 0xFFFF00;
            }
            case READY -> {
                key = "desc.gui.soyuz.ready";
                color = 0x00FF00;
            }
            default -> throw new IllegalStateException();
        }
        Component label = Component.translatable(key);
        float scale = Math.min(1F, 22F / font.width(label));
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(
                font,
                label,
                (int) (97 / scale - font.width(label) / 2F),
                (int) (125 / scale - font.lineHeight / 2F),
                0xFF000000 | color,
                false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (click(event, 79, 52, "cargo", true)) return true;
        if (click(event, 97, 52, "cargo", false)) return true;
        if (pad().soyuzStatus == BlockEntityLaunchpadSoyuz.SoyuzStatus.READY
                && click(event, 88, 97, "launch", true)) return true;
        return handled;
    }

    private boolean click(MouseButtonEvent event, int x, int y, String key, boolean value) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, value);
        Services.NETWORK.sendToServer(new NbtControlPayload(pad().getBlockPos(), data));
        return true;
    }
}
