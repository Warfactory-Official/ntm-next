// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.machine.PwrPrintData;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenPwrPrinter extends Screen {

    private final PwrPrinterJob job;

    public ScreenPwrPrinter(PwrPrintData data) {
        super(Component.empty());
        job = new PwrPrinterJob(data);
    }

    public static void open(PwrPrintData data) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            minecraft.gui.setScreen(new ScreenPwrPrinter(data));
        } catch (UncheckedIOException failure) {
            minecraft.player.sendSystemMessage(
                    Component.translatable("screenshot.failure", failure.getMessage()));
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFFFF00FF);
        if (job.completed() < job.data.sizeY()) {
            graphics.guiRenderState.addPicturesInPictureState(
                    new PwrSliceRenderState(job, job.completed(), 0, 0, width, height));
        }
    }

    @Override
    public void tick() {
        if (job.failure() != null) {
            minecraft.player.sendSystemMessage(
                    Component.translatable("screenshot.failure", job.failure().getMessage()));
            onClose();
        } else if (job.completed() == job.data.sizeY()) {
            minecraft.player.sendSystemMessage(
                    Component.translatable("desc.gui.pwrPrinter.saved", job.directory.toString()));
            onClose();
        }
    }

    @Override
    public void removed() {
        job.cancel();
    }
}
