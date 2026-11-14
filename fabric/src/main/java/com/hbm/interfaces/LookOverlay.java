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

package com.hbm.interfaces;

import com.hbm.util.NumberFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

public final class LookOverlay {

    public static final int TITLE_RGB = 0xFFFF00;
    public static final int TITLE_SHADOW_RGB = 0x404000;
    public static final int TITLE_OFFSET = -10;
    public static final int ROW_RGB = 0xFFFFFF;

    public record Row(Component text, int rgb) {}

    public record Heading(Component text, int rgb, int shadowRgb, int offset) {}

    private final List<Row> rows = new ArrayList<>();
    private final List<Heading> headings = new ArrayList<>(1);
    private final List<Row> rowView = Collections.unmodifiableList(rows);
    private final List<Heading> headingView = Collections.unmodifiableList(headings);
    private @Nullable BlockPos hit;
    private @Nullable Component title;
    private int titleRgb = TITLE_RGB;
    private int titleShadowRgb = TITLE_SHADOW_RGB;
    private int titleOffset = TITLE_OFFSET;
    private boolean soloTitle;
    private boolean shadowedRows = true;

    public void reset() {
        rows.clear();
        headings.clear();
        hit = null;
        title = null;
        titleRgb = TITLE_RGB;
        titleShadowRgb = TITLE_SHADOW_RGB;
        titleOffset = TITLE_OFFSET;
        soloTitle = false;
        shadowedRows = true;
    }

    public LookOverlay hit(BlockPos pos) {
        hit = pos;
        return this;
    }

    public @Nullable BlockPos hit() {
        return hit;
    }

    public LookOverlay title(Component text) {
        return title(text, TITLE_RGB, TITLE_SHADOW_RGB);
    }

    public LookOverlay title(Component text, int rgb, int shadowRgb) {
        title = text;
        titleRgb = rgb;
        titleShadowRgb = shadowRgb;
        return this;
    }

    public LookOverlay soloTitle(Component text, int rgb, int shadowRgb) {
        soloTitle = true;
        return title(text, rgb, shadowRgb);
    }

    public LookOverlay soloTitle(Component text, int rgb, int shadowRgb, int offset) {
        titleOffset = offset;
        return soloTitle(text, rgb, shadowRgb);
    }

    public LookOverlay heading(Component text, int rgb, int shadowRgb, int offset) {
        headings.add(new Heading(text, rgb, shadowRgb, offset));
        return this;
    }

    public LookOverlay row(Component text) {
        return row(text, ROW_RGB);
    }

    public LookOverlay row(Component text, int rgb) {
        rows.add(new Row(text, rgb));
        return this;
    }

    public LookOverlay plainRows() {
        shadowedRows = false;
        return this;
    }

    public LookOverlay charge(long power, long max) {
        double fill = (double) power / (double) max;
        int hundredths = (int) Math.floor(fill * 10000);
        int red = (int) (255 - 255 * fill);
        int green = (int) (255 * fill);
        row(
                Component.translatable(
                        "overlay.hbm.charge",
                        NumberFormatter.formatCompact(power),
                        NumberFormatter.formatCompact(max)));
        return row(
                Component.translatable("unit.percent", hundredths / 100.0), red << 16 | green << 8);
    }

    public static MutableComponent inflow(Component rest) {
        return marked("-> ", ChatFormatting.GREEN, rest);
    }

    public static MutableComponent outflow(Component rest) {
        return marked("<- ", ChatFormatting.RED, rest);
    }

    public static MutableComponent flow(boolean in, Component rest) {
        return in ? inflow(rest) : outflow(rest);
    }

    public static MutableComponent marked(String mark, ChatFormatting color, Component rest) {
        return Component.empty().append(Component.literal(mark).withStyle(color)).append(rest);
    }

    public @Nullable Component title() {
        return title;
    }

    public int titleRgb() {
        return titleRgb;
    }

    public int titleShadowRgb() {
        return titleShadowRgb;
    }

    public int titleOffset() {
        return titleOffset;
    }

    public List<Heading> headings() {
        return headingView;
    }

    public List<Row> rows() {
        return rowView;
    }

    public int rowCount() {
        return rows.size();
    }

    public Component rowText(int index) {
        return rows.get(index).text();
    }

    public int rowRgb(int index) {
        return rows.get(index).rgb();
    }

    public boolean shadowedRows() {
        return shadowedRows;
    }

    public boolean isEmpty() {
        return !soloTitle && rows.isEmpty() && headings.isEmpty();
    }
}
