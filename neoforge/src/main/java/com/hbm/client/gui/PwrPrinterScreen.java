/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.items.machine.PwrPrintData;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PwrPrinterScreen extends Screen {

    private final PwrPrinterJob job;

    public PwrPrinterScreen(PwrPrintData data) {
        super(Component.empty());
        job = new PwrPrinterJob(data);
    }

    public static void open(PwrPrintData data) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            minecraft.gui.setScreen(new PwrPrinterScreen(data));
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
