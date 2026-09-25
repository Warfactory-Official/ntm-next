// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuSoyuzLauncher;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@Deprecated
public class ScreenSoyuzLauncher extends ScreenInfoContainer<MenuSoyuzLauncher> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_soyuz.png");

    public ScreenSoyuzLauncher(MenuSoyuzLauncher menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 194, 244);
        titleLabelY = 4;
        inventoryLabelX = 17;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    private BlockEntitySoyuzLauncher launcher() {
        return menu.blockEntity();
    }

    @Override
    protected int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntitySoyuzLauncher launcher = launcher();
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

        int power = (int) launcher.getPowerScaled(52);
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
        state(graphics, 97, 79, launcher.hasRocket() ? 2 : 1);
        state(graphics, 79, 79, launcher.designator());
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                97 - launcher.mode * 18,
                52,
                228 - launcher.mode * 18,
                26,
                18,
                18,
                256,
                256);
        state(graphics, 79, 25, launcher.orbital());
        state(graphics, 97, 25, launcher.satellite());
        if (launcher.starting)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 88, 97, 210, 44, 18, 18, 256, 256);

        pip(graphics, 157, 31, launcher.hasFuel());
        pip(graphics, 175, 31, launcher.hasOxy());
        pip(graphics, 139, 31, launcher.hasPower());
        drawFluidBar(graphics, 152, 44, 16, 52, launcher.fuelTank);
        drawFluidBar(graphics, 170, 44, 16, 52, launcher.oxidizerTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 152, 44, 16, 52, launcher.fuelTank);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 170, 44, 16, 52, launcher.oxidizerTank);
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                134,
                44,
                16,
                52,
                launcher.power,
                BlockEntitySoyuzLauncher.MAX_POWER);

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
        clock(graphics, launcher.countdown);
    }

    private static void state(GuiGraphicsExtractor graphics, int x, int y, int value) {
        if (value > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x,
                    y,
                    210 + (value - 1) * 18,
                    8,
                    18,
                    18,
                    256,
                    256);
    }

    private static void pip(GuiGraphicsExtractor graphics, int x, int y, boolean ready) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, ready ? 210 : 216, 0, 6, 8, 256, 256);
    }

    private void clock(GuiGraphicsExtractor graphics, int countdown) {
        String seconds = String.valueOf(countdown / SharedConstants.TICKS_PER_SECOND);
        String centiseconds = String.valueOf(countdown % SharedConstants.TICKS_PER_SECOND * 5);
        if (seconds.length() == 1) seconds = "0" + seconds;
        if (centiseconds.length() == 1) centiseconds += "0";
        graphics.text(
                font, Component.literal(seconds + ":" + centiseconds), 85, 121, 0xFFFF0000, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (click(event, 97, 52, "mode", BlockEntitySoyuzLauncher.MODE_SATELLITE)) return true;
        if (click(event, 79, 52, "mode", BlockEntitySoyuzLauncher.MODE_CARGO)) return true;
        if (click(event, 88, 97, "start", 0)) return true;
        return handled;
    }

    private boolean click(MouseButtonEvent event, int x, int y, String key, int value) {
        if (!checkClick((int) event.x(), (int) event.y(), x, y, 18, 18)) return false;
        playClick();
        CompoundTag data = new CompoundTag();
        data.putInt(key, value);
        Services.NETWORK.sendToServer(new NbtControlPayload(launcher().getBlockPos(), data));
        return true;
    }
}
