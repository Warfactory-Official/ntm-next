// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
