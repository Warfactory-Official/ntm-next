// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class GUIElements {

    public static final int STANDARD_COLOR_BACKGROUND = -0xFEFFFF0;
    public static final int RECIPE_COLOR_LINE0 = 0xFFFF8000;
    public static final int RECIPE_COLOR_LINE1 = 0xFFFFFF00;
    public static final int STANDARD_LINE_DIST = 10;
    public static final int STANDARD_HEADER_OFFSET = 2;
    public static final int RECIPE_HEADER_OFFSET = 6;
    public static final int STACK_COLOR_BACKGROUND = 0xF0100010;
    public static final int STACK_COLOR_LINE = 0x505000FF;

    private GUIElements() {}

    public static void defer(GuiGraphicsExtractor graphics, Consumer<GuiGraphicsExtractor> draw) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ((DeferredGuiDraws) graphics)
                .hbm$defer(
                        deferred -> {
                            deferred.pose().pushMatrix();
                            deferred.pose().set(pose);
                            draw.accept(deferred);
                            deferred.pose().popMatrix();
                        });
    }

    public static void drawHoveringTextRecipe(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Component> lines,
            int x,
            int y,
            int guiWidth,
            int guiHeight) {
        drawHoveringText(
                graphics,
                font,
                lines,
                x,
                y,
                guiWidth,
                guiHeight,
                RECIPE_HEADER_OFFSET,
                STANDARD_LINE_DIST,
                STANDARD_COLOR_BACKGROUND,
                STANDARD_COLOR_BACKGROUND,
                RECIPE_COLOR_LINE0,
                RECIPE_COLOR_LINE1);
    }

    public static void drawHoveringText(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Component> lines,
            int x,
            int y,
            int guiWidth,
            int guiHeight,
            int headerOffset,
            int lineDist,
            int colBG0,
            int colBG1,
            int colLine0,
            int colLine1) {
        if (lines.isEmpty()) return;
        defer(
                graphics,
                deferred ->
                        hoveringText(
                                deferred,
                                font,
                                lines,
                                x,
                                y,
                                guiWidth,
                                guiHeight,
                                headerOffset,
                                lineDist,
                                colBG0,
                                colBG1,
                                colLine0,
                                colLine1));
    }

    private static void hoveringText(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Component> lines,
            int x,
            int y,
            int guiWidth,
            int guiHeight,
            int headerOffset,
            int lineDist,
            int colBG0,
            int colBG1,
            int colLine0,
            int colLine1) {
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));

        int boundX = x + 12;
        int boundY = y - 12;
        int height = 6 + headerOffset;

        if (lines.size() > 1) height += 2 + (lines.size() - 1) * lineDist;

        if (boundX + width + 4 > guiWidth) boundX -= 28 + width;
        if (boundY + height + 6 > guiHeight) boundY = guiHeight - height - 6;

        if (boundX < 4) boundX = 4;
        if (boundY < 4) boundY = 4;

        graphics.fillGradient(
                boundX - 3, boundY - 4, boundX + width + 3, boundY - 3, colBG0, colBG0);
        graphics.fillGradient(
                boundX - 3,
                boundY + height + 3,
                boundX + width + 3,
                boundY + height + 4,
                colBG1,
                colBG1);
        graphics.fillGradient(
                boundX - 3, boundY - 3, boundX + width + 3, boundY + height + 3, colBG0, colBG1);
        graphics.fillGradient(
                boundX - 4, boundY - 3, boundX - 3, boundY + height + 3, colBG0, colBG1);
        graphics.fillGradient(
                boundX + width + 3,
                boundY - 3,
                boundX + width + 4,
                boundY + height + 3,
                colBG0,
                colBG1);

        graphics.fillGradient(
                boundX - 3,
                boundY - 3 + 1,
                boundX - 3 + 1,
                boundY + height + 3 - 1,
                colLine0,
                colLine1);
        graphics.fillGradient(
                boundX + width + 2,
                boundY - 3 + 1,
                boundX + width + 3,
                boundY + height + 3 - 1,
                colLine0,
                colLine1);
        graphics.fillGradient(
                boundX - 3, boundY - 3, boundX + width + 3, boundY - 3 + 1, colLine0, colLine0);
        graphics.fillGradient(
                boundX - 3,
                boundY + height + 2,
                boundX + width + 3,
                boundY + height + 3,
                colLine1,
                colLine1);

        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, lines.get(i), boundX, boundY, -1, true);
            if (i == 0) boundY += headerOffset;
            boundY += lineDist;
        }
    }

    public static void drawCyclingStackText(
            GuiGraphicsExtractor graphics,
            Font font,
            @Nullable Component header,
            List<ItemStack> stacks,
            int x,
            int y,
            int screenWidth,
            int screenHeight) {
        if (stacks.isEmpty()) return;
        defer(
                graphics,
                deferred ->
                        cyclingStackText(
                                deferred, font, header, stacks, x, y, screenWidth, screenHeight));
    }

    private static void cyclingStackText(
            GuiGraphicsExtractor graphics,
            Font font,
            @Nullable Component header,
            List<ItemStack> stacks,
            int x,
            int y,
            int screenWidth,
            int screenHeight) {
        int count = stacks.size();
        int highlight =
                count > 1 ? (int) (System.currentTimeMillis() % (1000L * count) / 1000L) : -1;
        Component name = stacks.get(Math.max(highlight, 0)).getHoverName();
        int rows = count < 10 ? 1 : count < 24 ? 2 : 3;

        int longest = font.width(name);
        for (int row = 0; row < rows; row++)
            longest =
                    Math.max(
                            longest,
                            18 * (rowStart(row + 1, rows, count) - rowStart(row, rows, count)));
        int height = 18 * rows + 10;
        if (header != null) {
            longest = Math.max(longest, font.width(header));
            height += 10;
        }

        int minX = boxX(x, longest, screenWidth);
        int lineY = boxY(y, height, screenHeight);
        drawStackBox(graphics, minX, lineY, longest, height);

        if (header != null) {
            graphics.text(font, header, minX, lineY, -1, true);
            lineY += 12;
        }
        for (int row = 0; row < rows; row++) {
            int indent = minX;
            for (int i = rowStart(row, rows, count), end = rowStart(row + 1, rows, count);
                    i < end;
                    i++) {
                drawStackCell(graphics, font, stacks.get(i), indent, lineY, i == highlight);
                indent += 18;
            }
            lineY += header == null && row == 0 ? 20 : 18;
        }
        graphics.text(font, name, minX, lineY, -1, true);
    }

    private static int rowStart(int row, int rows, int count) {
        return rows == 3 ? (row * count + 2) / 3 : row * count / rows;
    }

    public static void drawStackText(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Object[]> lines,
            int x,
            int y,
            int screenWidth,
            int screenHeight) {
        if (lines.isEmpty()) return;
        defer(
                graphics,
                deferred -> stackText(deferred, font, lines, x, y, screenWidth, screenHeight));
    }

    private static void stackText(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Object[]> lines,
            int x,
            int y,
            int screenWidth,
            int screenHeight) {
        int height = 0;
        int longest = 0;

        for (Object[] line : lines) {
            int lineWidth = 0;
            boolean hasStack = false;
            for (Object cell : line) {
                if (cell instanceof Component s) {
                    lineWidth += font.width(s);
                } else {
                    lineWidth += 18;
                    hasStack = true;
                }
            }
            height += hasStack ? 18 : 10;
            longest = Math.max(longest, lineWidth);
        }

        int minX = boxX(x, longest, screenWidth);
        int minY = boxY(y, height, screenHeight);
        drawStackBox(graphics, minX, minY, longest, height);

        for (int index = 0; index < lines.size(); index++) {
            Object[] line = lines.get(index);
            int indent = 0;
            boolean hasStack = false;
            for (Object cell : line)
                if (!(cell instanceof Component)) {
                    hasStack = true;
                    break;
                }

            for (Object cell : line) {
                if (cell instanceof Component s) {
                    graphics.text(font, s, minX + indent, minY + (hasStack ? 4 : 0), -1, true);
                    indent += font.width(s) + 2;
                } else {
                    drawStackCell(graphics, font, (ItemStack) cell, minX + indent, minY, false);
                    indent += 18;
                }
            }

            if (index == 0) minY += 2;
            minY += hasStack ? 18 : 10;
        }
    }

    private static int boxX(int x, int longest, int screenWidth) {
        int minX = x + 12;
        return minX + longest > screenWidth ? minX - 28 - longest : minX;
    }

    private static int boxY(int y, int height, int screenHeight) {
        int minY = y - 12;
        return minY + height + 6 > screenHeight ? screenHeight - height - 6 : minY;
    }

    private static void drawStackBox(
            GuiGraphicsExtractor graphics, int minX, int minY, int longest, int height) {
        int colBG = STACK_COLOR_BACKGROUND;
        int colLine0 = STACK_COLOR_LINE;
        int colLine1 = (colLine0 & 0xFEFEFE) >> 1 | colLine0 & 0xFF000000;

        graphics.fillGradient(minX - 3, minY - 4, minX + longest + 3, minY - 3, colBG, colBG);
        graphics.fillGradient(
                minX - 3, minY + height + 3, minX + longest + 3, minY + height + 4, colBG, colBG);
        graphics.fillGradient(
                minX - 3, minY - 3, minX + longest + 3, minY + height + 3, colBG, colBG);
        graphics.fillGradient(minX - 4, minY - 3, minX - 3, minY + height + 3, colBG, colBG);
        graphics.fillGradient(
                minX + longest + 3, minY - 3, minX + longest + 4, minY + height + 3, colBG, colBG);

        graphics.fillGradient(minX - 3, minY - 2, minX - 2, minY + height + 2, colLine0, colLine1);
        graphics.fillGradient(
                minX + longest + 2,
                minY - 2,
                minX + longest + 3,
                minY + height + 2,
                colLine0,
                colLine1);
        graphics.fillGradient(minX - 3, minY - 3, minX + longest + 3, minY - 2, colLine0, colLine0);
        graphics.fillGradient(
                minX - 3,
                minY + height + 2,
                minX + longest + 3,
                minY + height + 3,
                colLine1,
                colLine1);
    }

    private static void drawStackCell(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            int x,
            int y,
            boolean highlight) {
        if (highlight) {
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFF0000);
            graphics.fill(x, y, x + 16, y + 16, 0xFFB0B0B0);
        }
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }
}
