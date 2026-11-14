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

package com.hbm.wiaj.actors;

import com.hbm.util.RegistryUtil;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class ActorFancyPanel implements JarActor {
    private static final Identifier GUI = RegistryUtil.id("textures/gui/gui_utility.png");
    private static final int STACK_HEIGHT = 18;
    private static final int GAP = 2;
    private static final int ARROW_SHEET_STRIDE = 28;

    private record Row(Object[] cells, int[] indent, int width, int height) {}

    private record Extent(int width, int height) {}

    private final int x;
    private final int y;
    private final Row[] rows;
    private final int textHeight;
    private final int width;
    private final int tallest;
    private boolean consistentHeight;
    private int lineDist = 2;
    private Orientation orientation = Orientation.CENTER;
    private int colorBrighter = 0xFFCCCCCC;
    private int colorFrame = 0xFFA0A0A0;
    private int colorDarker = 0xFF7D7D7D;
    private int colorBg = 0xFF302E36;

    private final int[] rowTop;
    private int height;
    private int cornerX;
    private int cornerY;

    public ActorFancyPanel(int x, int y, Object[][] raw, int autowrap) {
        this.x = x;
        this.y = y;
        Font font = Minecraft.getInstance().font;
        textHeight = font.lineHeight;
        var built = new ArrayList<Row>();
        for (Object[] line : raw) {
            if (autowrap > 0 && line.length == 1 && line[0] instanceof Component text)
                for (var fragment : font.split(text, autowrap))
                    built.add(measure(font, new Object[] {fragment}));
            else built.add(measure(font, line));
        }
        rows = built.toArray(Row[]::new);
        int wide = 0, tall = 0;
        for (Row row : rows) {
            wide = Math.max(wide, row.width);
            tall = Math.max(tall, row.height);
        }
        width = wide;
        tallest = tall;
        rowTop = new int[rows.length];
        relayout();
    }

    public ActorFancyPanel enforceConsistentHeight() {
        consistentHeight = true;
        relayout();
        return this;
    }

    public ActorFancyPanel setLineDist(int distance) {
        lineDist = distance;
        relayout();
        return this;
    }

    public ActorFancyPanel setOrientation(Orientation orientation) {
        this.orientation = orientation;
        relayout();
        return this;
    }

    public ActorFancyPanel setColors(int brighter, int frame, int darker, int background) {
        colorBrighter = brighter;
        colorFrame = frame;
        colorDarker = darker;
        colorBg = background;
        return this;
    }

    public ActorFancyPanel setColors(int[] colors) {
        return setColors(colors[0], colors[1], colors[2], colors[3]);
    }

    private static Row measure(Font font, Object[] cells) {
        var indent = new int[cells.length];
        int run = 0, tall = 0;
        for (int i = 0; i < cells.length; i++) {
            if (run > 0) run += GAP;
            indent[i] = run;
            var extent = extent(font, cells[i]);
            run += extent.width;
            tall = Math.max(tall, extent.height);
        }
        return new Row(cells, indent, run, tall);
    }

    private static Extent extent(Font font, Object cell) {
        return switch (cell) {
            case Component text -> new Extent(font.width(text), font.lineHeight);
            case String text -> new Extent(font.width(text), font.lineHeight);
            case FormattedCharSequence text -> new Extent(font.width(text), font.lineHeight);
            case ItemStack _ -> new Extent(STACK_HEIGHT, STACK_HEIGHT);
            case Object[] scaled -> {
                int side = (int) Math.ceil(STACK_HEIGHT * (double) scaled[1]);
                yield new Extent(side, side);
            }
            default -> new Extent(0, 0);
        };
    }

    private void relayout() {
        int box = 0, top = 0;
        for (int i = 0; i < rows.length; i++) {
            if (box > 0) box += lineDist;
            box += Math.max(textHeight, consistentHeight ? tallest : rows[i].height);
            if (top > 0) top += lineDist;
            rowTop[i] = top;
            top += rows[i].height;
        }
        height = box;
        cornerX =
                switch (orientation) {
                    case LEFT -> 15;
                    case RIGHT -> -(width + 15);
                    default -> -(width / 2);
                };
        cornerY =
                switch (orientation) {
                    case TOP -> 15;
                    case BOTTOM -> -(height + 15);
                    default -> -(height / 2);
                };
    }

    @Override
    public void drawOverlay(GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp) {
        int px = w / 2 + x + cornerX;
        int py = h / 2 + y + cornerY;

        graphics.fill(px - 5, py - 5, px + width + 5, py + height + 5, colorFrame);
        bevel(graphics, px, py, 5, colorBrighter, colorDarker);
        bevel(graphics, px, py, 3, colorDarker, colorBrighter);
        graphics.fill(px - 2, py - 2, px + width + 2, py + height + 2, colorBg);

        switch (orientation) {
            case TOP -> arrow(graphics, px + width / 2 - 7, py - 15, 40, 14, 14, 10);
            case BOTTOM -> arrow(graphics, px + width / 2 - 7, py + height + 5, 54, 14, 14, 10);
            case LEFT -> arrow(graphics, px - 15, py + height / 2 - 7, 40, 0, 10, 14);
            case RIGHT -> arrow(graphics, px + width + 5, py + height / 2 - 7, 50, 0, 10, 14);
            case CENTER -> {}
        }

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < rows.length; i++) {
            Row row = rows[i];
            int mid = py + rowTop[i] + row.height / 2;
            for (int c = 0; c < row.cells.length; c++)
                draw(graphics, font, px + row.indent[c], mid, row.cells[c]);
        }
    }

    private void bevel(
            GuiGraphicsExtractor graphics, int px, int py, int inset, int lit, int shade) {
        int left = px - inset, top = py - inset;
        int right = px + width + inset, bottom = py + height + inset;
        graphics.fill(left, top, left + 1, bottom - 1, lit);
        graphics.fill(left, top, right - 1, top + 1, lit);
        graphics.fill(right - 1, top + 1, right, bottom, shade);
        graphics.fill(left + 1, bottom - 1, right, bottom, shade);
    }

    private void arrow(
            GuiGraphicsExtractor graphics, int ax, int ay, int u, int v, int aw, int ah) {
        int[] tints = {colorBrighter, colorFrame, colorDarker, colorBg};
        for (int layer = 0; layer < tints.length; layer++) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    GUI,
                    ax,
                    ay,
                    u + ARROW_SHEET_STRIDE * layer,
                    v,
                    aw,
                    ah,
                    256,
                    256,
                    tints[layer]);
        }
    }

    private static void draw(
            GuiGraphicsExtractor graphics, Font font, int cx, int mid, Object cell) {
        int textTop = mid - font.lineHeight / 2;
        switch (cell) {
            case Component text -> graphics.text(font, text, cx, textTop, 0xFFFFFFFF, false);
            case String text -> graphics.text(font, text, cx, textTop, 0xFFFFFFFF, false);
            case FormattedCharSequence text ->
                    graphics.text(font, text, cx, textTop, 0xFFFFFFFF, false);
            case ItemStack stack -> {
                graphics.item(stack, cx, mid - 8);
                graphics.itemDecorations(font, stack, cx, mid - 8);
            }
            default -> {}
        }
    }

    @Override
    public void drawInJar(JarRenderContext context, int ticks, float interp) {}

    @Override
    public void tick(JarScene scene) {}

    @Override
    public void replaceData(CompoundTag data) {}

    @Override
    public void putData(String key, Tag value) {}

    public enum Orientation {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        CENTER
    }
}
