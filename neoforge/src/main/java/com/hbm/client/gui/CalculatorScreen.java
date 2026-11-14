/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.util.ArithmeticExpression;
import com.mojang.blaze3d.platform.InputConstants;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class CalculatorScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 50;
    private static final int BORDER = 2;
    private static final int MAX_HISTORY = 6;
    private static final Deque<Result> HISTORY = new ArrayDeque<>();

    private EditBox input;
    private int selectedHistory = -1;
    private String latestResult = "?";

    public CalculatorScreen() {
        super(Component.translatable("hbm.key.calculator"));
    }

    @Override
    protected void init() {
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        input = new EditBox(font, x + 5, y + 8, 210, 13, Component.empty());
        input.setTextColor(0xFFFFFFFF);
        input.setCanLoseFocus(false);
        input.setMaxLength(1_000);
        input.setResponder(text -> updatePreview());
        addRenderableWidget(input);
        setFocused(input);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN
                || event.key() == InputConstants.KEY_NUMPADENTER) {
            enter();
            return true;
        }
        if (event.key() == InputConstants.KEY_UP) {
            selectedHistory = Math.max(selectedHistory - 1, -1);
            updatePreview();
            return true;
        }
        if (event.key() == InputConstants.KEY_DOWN) {
            selectedHistory = Math.min(selectedHistory + 1, HISTORY.size() - 1);
            updatePreview();
            return true;
        }
        boolean handled = super.keyPressed(event);
        if (handled) {
            selectedHistory = -1;
            updatePreview();
        }
        return handled;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean handled = super.charTyped(event);
        if (handled) {
            selectedHistory = -1;
            updatePreview();
        }
        return handled;
    }

    private void enter() {
        if (selectedHistory != -1) {
            input.setValue(new ArrayList<>(HISTORY).get(selectedHistory).expression());
            selectedHistory = -1;
            updatePreview();
            return;
        }
        String expression = filteredInput();
        try {
            double result = ArithmeticExpression.evaluate(expression);
            HISTORY.addFirst(new Result(expression, result));
            if (HISTORY.size() > MAX_HISTORY) HISTORY.removeLast();
            String decimal = new BigDecimal(result, MathContext.DECIMAL64).toPlainString();
            minecraft.keyboardHandler.setClipboard(decimal);
            input.setValue(decimal);
            input.moveCursorToEnd(false);
            input.setHighlightPos(0);
        } catch (Exception ignored) {
        }
    }

    private String filteredInput() {
        return input.getValue().replaceAll("[^\\d+\\-*/%^!.()\\sA-Za-z]+", "");
    }

    private void updatePreview() {
        String expression = filteredInput();
        if (expression.isEmpty()) {
            latestResult = "?";
            return;
        }
        try {
            latestResult = Double.toString(ArithmeticExpression.evaluate(expression));
        } catch (Exception e) {
            latestResult = e.toString();
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        int historyHeight = (font.lineHeight + 2) * MAX_HISTORY;
        int historyStart = y + 30 + font.lineHeight + 8;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT + historyHeight, 0xFF2D2D2D);
        graphics.fill(
                x + BORDER,
                y + BORDER,
                x + WIDTH - BORDER,
                y + HEIGHT - BORDER + historyHeight,
                0xFF3D3D3D);
        graphics.fill(x, historyStart - 5, x + WIDTH, historyStart - 3, 0xFF2D2D2D);
        int i = 0;
        for (Result ignored : HISTORY) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            if (i == selectedHistory)
                graphics.fill(x + 4, hy - 1, x + WIDTH - 5, hy + font.lineHeight, 0xFF111111);
            i++;
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        graphics.text(font, "=" + latestResult, x + 5, y + 30, 0xFFFFFFFF, false);
        int i = 0;
        for (Result result : HISTORY) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            graphics.text(
                    font,
                    result.expression() + " = " + result.value(),
                    x + 5,
                    hy,
                    0xFFFFFFFF,
                    false);
            i++;
        }
    }

    private record Result(String expression, double value) {}
}
