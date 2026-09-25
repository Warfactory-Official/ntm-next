// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.NuclearTech;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

public final class ScreenWikiRender extends Screen {

    private static final int BACKGROUND = 0xFFFF00FF;
    private final List<ItemStack> preview;
    private final String prefix;
    private final String directory;
    private final int scale;
    private final Function<ItemStack, String> stackName;
    private int index;
    private boolean rendered;
    private boolean capturePending;

    public ScreenWikiRender(List<ItemStack> stacks, String prefix, String directory, int scale) {
        this(stacks, prefix, directory, scale, ScreenWikiRender::defaultName);
    }

    public ScreenWikiRender(
            List<ItemStack> stacks,
            String prefix,
            String directory,
            int scale,
            Function<ItemStack, String> stackName) {
        super(Component.empty());
        this.preview = List.copyOf(stacks);
        this.prefix = prefix;
        this.directory = directory;
        this.scale = scale;
        this.stackName = stackName;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        if (index >= preview.size()) return;

        ItemStack stack = preview.get(index);
        if (!stack.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(scale, height - 17F * scale);
            graphics.pose().scale(scale, scale);
            graphics.item(stack, 0, 0);
            graphics.itemDecorations(font, stack, 0, 0);
            graphics.pose().popMatrix();
        }
        rendered = true;
    }

    @Override
    public void tick() {
        if (index >= preview.size()) {
            minecraft.gui.setScreen(null);
            return;
        }
        if (!rendered || capturePending) return;

        ItemStack stack = preview.get(index);
        String displayName = stackName.apply(stack);
        rendered = false;
        if (displayName == null) {
            index++;
            return;
        }
        String name = displayName.replaceAll("§.", "").replaceAll("[^\\w ().-]+", "");
        if (name.endsWith(".name")) {
            index++;
            return;
        }

        capturePending = true;
        Minecraft client = minecraft;
        int pixelScale = client.getWindow().getGuiScale();
        Path output =
                client.gameDirectory.toPath().resolve(directory).resolve(prefix + name + ".png");

        try {
            Screenshot.takeScreenshot(
                    client.gameRenderer.mainRenderTarget(),
                    image ->
                            client.execute(
                                    () -> {
                                        try (image) {
                                            save(image, output, scale * pixelScale);
                                        } catch (Exception ex) {
                                            reportFailure(client, output, ex);
                                        } finally {
                                            index++;
                                            capturePending = false;
                                        }
                                    }));
        } catch (RuntimeException ex) {
            reportFailure(client, output, ex);
            index++;
            capturePending = false;
        }
    }

    private static String defaultName(ItemStack stack) {
        Component name = stack.getHoverName();
        if (name.getContents() instanceof TranslatableContents translated
                && !Language.getInstance().has(translated.getKey())) return null;
        return name.getString();
    }

    private static void reportFailure(Minecraft client, Path output, Exception ex) {
        NuclearTech.LOGGER.warn("Failed to save NTM screenshot {}", output, ex);
        if (client.player != null) {
            client.player.sendSystemMessage(
                    Component.translatable("screenshot.failure", ex.getMessage()));
        }
    }

    private static void save(NativeImage source, Path output, int margin) throws IOException {
        int side = 16 * margin;
        int left = margin;
        int top = source.getHeight() - margin - side;
        if (top < 0 || left + side > source.getWidth()) {
            throw new IOException("Window too small for a " + side + "px wiki render capture");
        }

        try (NativeImage cropped = new NativeImage(side, side, false)) {
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < side; x++) {
                    int color = source.getPixel(left + x, top + y);
                    cropped.setPixel(x, y, color == BACKGROUND ? 0 : color);
                }
            }
            Files.createDirectories(output.getParent());
            cropped.writeToFile(output);
        }
    }

    public int completed() {
        return index;
    }

    public int total() {
        return preview.size();
    }
}
