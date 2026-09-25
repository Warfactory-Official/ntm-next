// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
            @Nullable String header,
            List<ItemStack> stacks,
            int x,
            int y,
            int screenWidth,
            int screenHeight) {
        if (stacks.isEmpty()) return;
        List<ItemStack> list = new ArrayList<>(stacks);

        ItemStack selected = list.get(0);
        ItemStack highlight = null;
        if (list.size() > 1) {
            int cycle = (int) ((System.currentTimeMillis() % (1000L * list.size())) / 1000L);
            selected = list.get(cycle).copy();
            highlight = selected;
            list.set(cycle, selected);
        }

        List<Object[]> lines = new ArrayList<>();
        if (header != null) lines.add(new Object[] {header});
        if (list.size() < 10) {
            lines.add(list.toArray());
        } else if (list.size() < 24) {
            lines.add(list.subList(0, list.size() / 2).toArray());
            lines.add(list.subList(list.size() / 2, list.size()).toArray());
        } else {
            int bound0 = (int) Math.ceil(list.size() / 3D);
            int bound1 = (int) Math.ceil(list.size() / 3D * 2D);
            lines.add(list.subList(0, bound0).toArray());
            lines.add(list.subList(bound0, bound1).toArray());
            lines.add(list.subList(bound1, list.size()).toArray());
        }
        lines.add(new Object[] {selected.getHoverName().getString()});

        drawStackText(graphics, font, lines, x, y, screenWidth, screenHeight, highlight);
    }

    public static void drawStackText(
            GuiGraphicsExtractor graphics,
            Font font,
            List<Object[]> lines,
            int x,
            int y,
            int screenWidth,
            int screenHeight,
            @Nullable ItemStack highlight) {
        if (lines.isEmpty()) return;

        int height = 0;
        int longest = 0;

        for (Object[] line : lines) {
            int lineWidth = 0;
            boolean hasStack = false;
            for (Object cell : line) {
                if (cell instanceof String s) {
                    lineWidth += font.width(s);
                } else {
                    lineWidth += 18;
                    hasStack = true;
                }
            }
            height += hasStack ? 18 : 10;
            longest = Math.max(longest, lineWidth);
        }

        int minX = x + 12;
        int minY = y - 12;
        if (minX + longest > screenWidth) minX -= 28 + longest;
        if (minY + height + 6 > screenHeight) minY = screenHeight - height - 6;

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

        for (int index = 0; index < lines.size(); index++) {
            Object[] line = lines.get(index);
            int indent = 0;
            boolean hasStack = false;
            for (Object cell : line) if (!(cell instanceof String)) hasStack = true;

            for (Object cell : line) {
                if (cell instanceof String s) {
                    graphics.text(font, s, minX + indent, minY + (hasStack ? 4 : 0), -1, true);
                    indent += font.width(s) + 2;
                } else {
                    ItemStack stack = (ItemStack) cell;
                    if (stack == highlight) {
                        graphics.fill(
                                minX + indent - 1,
                                minY - 1,
                                minX + indent + 17,
                                minY + 17,
                                0xFFFF0000);
                        graphics.fill(
                                minX + indent, minY, minX + indent + 16, minY + 16, 0xFFB0B0B0);
                    }
                    graphics.item(stack, minX + indent, minY);
                    graphics.itemDecorations(font, stack, minX + indent, minY);
                    indent += 18;
                }
            }

            if (index == 0) minY += 2;
            minY += hasStack ? 18 : 10;
        }
    }
}
