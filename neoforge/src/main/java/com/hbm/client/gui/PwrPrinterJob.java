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
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public final class PwrPrinterJob {

    public final PwrPrintData data;
    public final Path directory;
    private int completed;
    private boolean pending;
    private boolean cancelled;
    private IOException failure;

    public PwrPrinterJob(PwrPrintData data) {
        this.data = data;
        try {
            Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("printer");
            Files.createDirectories(root);
            String name = Util.getFilenameFormattedDateTime();
            Path directory;
            int suffix = 0;
            while (true) {
                directory = root.resolve(name + (suffix == 0 ? "" : "_" + suffix));
                try {
                    Files.createDirectory(directory);
                    break;
                } catch (FileAlreadyExistsException exists) {
                    suffix++;
                }
            }
            this.directory = directory;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public int completed() {
        return completed;
    }

    public IOException failure() {
        return failure;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean beginCapture(int slice) {
        if (cancelled || pending || slice != completed) return false;
        pending = true;
        return true;
    }

    public void write(int slice, NativeImage image) {
        Util.ioPool()
                .execute(
                        () -> {
                            try (image) {
                                image.writeToFile(directory.resolve("slice_" + slice + ".png"));
                                Minecraft.getInstance()
                                        .execute(
                                                () -> {
                                                    completed++;
                                                    pending = false;
                                                });
                            } catch (IOException error) {
                                Minecraft.getInstance().execute(() -> failure = error);
                            }
                        });
    }
}
