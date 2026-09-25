// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

public class NumberDisplay {

    private static final int VERTICAL_LENGTH = 5;
    private static final int HORIZONTAL_LENGTH = 4;
    private static final int THICKNESS = 1;

    private static final int[] SEGMENTS = {
        0b1011111, 0b0000101, 0b1110110, 0b1110101, 0b0101101, 0b1111001, 0b1111011, 0b1000101,
        0b1111111, 0b1111101,
    };

    private final int x;
    private final int y;
    private final int color;

    private int digitLength = 3;
    private int padding = 3;
    private boolean blinks;
    private boolean pads;

    public NumberDisplay(int x, int y, int color) {
        this.x = x;
        this.y = y;
        this.color = ARGB.opaque(color);
    }

    public NumberDisplay setDigitLength(int digits) {
        this.digitLength = digits;
        return this;
    }

    public NumberDisplay setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public NumberDisplay setBlinks(boolean blinks) {
        this.blinks = blinks;
        return this;
    }

    public NumberDisplay setPads(boolean pads) {
        this.pads = pads;
        return this;
    }

    private static boolean lit(int digit, int segment) {
        if (digit < 0 || digit > 9) return false;
        return (SEGMENTS[digit] & (1 << (6 - segment))) != 0;
    }

    public void draw(GuiGraphicsExtractor graphics, int value, boolean blinkOn) {
        if (blinks && !blinkOn) return;

        String text = String.valueOf(value);
        if (text.length() > digitLength) {
            for (int i = 0; i < digitLength; i++) drawOverflow(graphics, i);
            return;
        }

        int gap = digitLength - text.length();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int offset = cell(i + gap);
            if (c == '-') horizontal(graphics, offset, 1);
            else drawDigit(graphics, offset, c - '0');
        }

        if (pads) {
            for (int i = 0; i < gap; i++) drawDigit(graphics, cell(i), 0);
        }
    }

    private int cell(int index) {
        return (padding + HORIZONTAL_LENGTH + 2 * THICKNESS) * index;
    }

    private void drawDigit(GuiGraphicsExtractor graphics, int offset, int digit) {
        for (int row = 0; row < 3; row++) {
            if (lit(digit, row)) horizontal(graphics, offset, row);
        }
        if (lit(digit, 3)) vertical(graphics, offset, 0, 0);
        if (lit(digit, 4)) vertical(graphics, offset, 1, 0);
        if (lit(digit, 5)) vertical(graphics, offset, 0, 1);
        if (lit(digit, 6)) vertical(graphics, offset, 1, 1);
    }

    private void drawOverflow(GuiGraphicsExtractor graphics, int index) {
        int offset = cell(index);
        horizontal(graphics, offset, 0);
        horizontal(graphics, offset, 1);
        horizontal(graphics, offset, 2);
        vertical(graphics, offset, 0, 0);
        vertical(graphics, offset, 0, 1);
    }

    private void horizontal(GuiGraphicsExtractor graphics, int offset, int row) {
        int py = row * (VERTICAL_LENGTH + THICKNESS);
        fill(graphics, x + offset + THICKNESS, y + py, HORIZONTAL_LENGTH, THICKNESS);
    }

    private void vertical(GuiGraphicsExtractor graphics, int offset, int col, int row) {
        int px = col * (HORIZONTAL_LENGTH + THICKNESS);
        int py = row * (VERTICAL_LENGTH + THICKNESS);
        fill(graphics, x + offset + px, y + py + THICKNESS, THICKNESS, VERTICAL_LENGTH);
    }

    private void fill(GuiGraphicsExtractor graphics, int px, int py, int w, int h) {
        graphics.fill(px, py, px + w, py + h, color);
    }
}
