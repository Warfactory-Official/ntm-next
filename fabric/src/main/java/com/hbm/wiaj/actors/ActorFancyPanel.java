// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actors;

import com.hbm.lib.Library;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class ActorFancyPanel implements ISpecialActor {
    private static final Identifier GUI = Library.id("textures/gui/gui_utility.png");
    private static final int STACK_HEIGHT = 18;

    private final List<Object[]> lines = new ArrayList<>();
    private final int x;
    private final int y;
    private boolean consistentHeight;
    private int lineDist = 2;
    private Orientation orientation = Orientation.CENTER;
    private int colorBrighter = 0xFFCCCCCC;
    private int colorFrame = 0xFFA0A0A0;
    private int colorDarker = 0xFF7D7D7D;
    private int colorBg = 0xFF302E36;

    public ActorFancyPanel(int x, int y, Object[][] raw, int autowrap) {
        this.x = x;
        this.y = y;
        Font font = Minecraft.getInstance().font;
        for (Object[] line : raw) {
            if (autowrap > 0 && line.length == 1 && line[0] instanceof Component text) {
                for (FormattedCharSequence fragment : font.split(text, autowrap))
                    lines.add(new Object[] {fragment});
            } else {
                lines.add(line);
            }
        }
    }

    public ActorFancyPanel enforceConsistentHeight() {
        consistentHeight = true;
        return this;
    }

    public ActorFancyPanel setLineDist(int distance) {
        lineDist = distance;
        return this;
    }

    public ActorFancyPanel setOrientation(Orientation orientation) {
        this.orientation = orientation;
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

    private static int elementHeight(Font font, Object element) {
        if (element instanceof Component
                || element instanceof String
                || element instanceof FormattedCharSequence) return font.lineHeight;
        if (element instanceof ItemStack) return STACK_HEIGHT;
        if (element instanceof Object[] scaled)
            return (int) Math.ceil(STACK_HEIGHT * (double) scaled[1]);
        return 0;
    }

    private static int elementWidth(Font font, Object element) {
        if (element instanceof Component text) return font.width(text);
        if (element instanceof String text) return font.width(text);
        if (element instanceof FormattedCharSequence text) return font.width(text);
        if (element instanceof ItemStack) return STACK_HEIGHT;
        if (element instanceof Object[] scaled)
            return (int) Math.ceil(STACK_HEIGHT * (double) scaled[1]);
        return 0;
    }

    @Override
    public void drawForegroundComponent(
            GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp) {
        Font font = Minecraft.getInstance().font;
        int tallest = 0;
        for (Object[] line : lines)
            for (Object element : line) tallest = Math.max(tallest, elementHeight(font, element));

        int height = 0;
        int width = 0;
        for (Object[] line : lines) {
            if (height > 0) height += lineDist;
            int lineHeight = font.lineHeight;
            int lineWidth = 0;
            for (Object element : line) {
                if (lineWidth > 0) lineWidth += 2;
                lineWidth += elementWidth(font, element);
                if (!consistentHeight)
                    lineHeight = Math.max(lineHeight, elementHeight(font, element));
            }
            height += consistentHeight ? Math.max(lineHeight, tallest) : lineHeight;
            width = Math.max(width, lineWidth);
        }

        int px = w / 2 + x;
        int py = h / 2 + y;
        switch (orientation) {
            case TOP -> {
                px -= width / 2;
                py += 15;
            }
            case BOTTOM -> {
                px -= width / 2;
                py -= height + 15;
            }
            case LEFT -> {
                px += 15;
                py -= height / 2;
            }
            case RIGHT -> {
                px -= width + 15;
                py -= height / 2;
            }
            case CENTER -> {
                px -= width / 2;
                py -= height / 2;
            }
        }

        graphics.fill(px - 5, py - 5, px + width + 5, py + height + 5, colorFrame);
        graphics.fill(px - 5, py - 5, px - 4, py + height + 4, colorBrighter);
        graphics.fill(px - 5, py - 5, px + width + 4, py - 4, colorBrighter);
        graphics.fill(px + width + 2, py - 2, px + width + 3, py + height + 3, colorBrighter);
        graphics.fill(px - 2, py + height + 2, px + width + 3, py + height + 3, colorBrighter);
        graphics.fill(px - 3, py - 3, px - 2, py + height + 2, colorDarker);
        graphics.fill(px - 3, py - 3, px + width + 2, py - 2, colorDarker);
        graphics.fill(px + width + 4, py - 4, px + width + 5, py + height + 5, colorDarker);
        graphics.fill(px - 4, py + height + 4, px + width + 5, py + height + 5, colorDarker);
        graphics.fill(px - 2, py - 2, px + width + 2, py + height + 2, colorBg);

        switch (orientation) {
            case TOP -> arrow(graphics, px + width / 2 - 7, py - 15, 40, 14, 14, 10);
            case BOTTOM -> arrow(graphics, px + width / 2 - 7, py + height + 5, 54, 14, 14, 10);
            case LEFT -> arrow(graphics, px - 15, py + height / 2 - 7, 40, 0, 10, 14);
            case RIGHT -> arrow(graphics, px + width + 5, py + height / 2 - 7, 50, 0, 10, 14);
            case CENTER -> {}
        }

        int offsetY = 0;
        for (Object[] line : lines) {
            if (offsetY > 0) offsetY += lineDist;
            int lineHeight = 0;
            for (Object element : line)
                lineHeight = Math.max(lineHeight, elementHeight(font, element));
            int indent = 0;
            for (Object element : line) {
                if (indent > 0) indent += 2;
                drawElement(graphics, font, px + indent, py + offsetY + lineHeight / 2, element);
                indent += elementWidth(font, element);
            }
            offsetY += lineHeight;
        }
    }

    private void arrow(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI,
                x,
                y,
                u,
                v,
                width,
                height,
                256,
                256,
                colorBrighter);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI,
                x,
                y,
                u + 28,
                v,
                width,
                height,
                256,
                256,
                colorFrame);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI,
                x,
                y,
                u + 56,
                v,
                width,
                height,
                256,
                256,
                colorDarker);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI,
                x,
                y,
                u + 84,
                v,
                width,
                height,
                256,
                256,
                colorBg);
    }

    private static void drawElement(
            GuiGraphicsExtractor graphics, Font font, int x, int y, Object element) {
        if (element instanceof Component text)
            graphics.text(font, text, x, y - font.lineHeight / 2, 0xFFFFFFFF, false);
        else if (element instanceof String text)
            graphics.text(font, text, x, y - font.lineHeight / 2, 0xFFFFFFFF, false);
        else if (element instanceof FormattedCharSequence text)
            graphics.text(font, text, x, y - font.lineHeight / 2, 0xFFFFFFFF, false);
        else if (element instanceof ItemStack stack) {
            graphics.item(stack, x, y - 8);
            graphics.itemDecorations(font, stack, x, y - 8);
        }
    }

    @Override
    public void drawBackgroundComponent(JarRenderContext context, int ticks, float interp) {}

    @Override
    public void updateActor(JarScene scene) {}

    @Override
    public void setActorData(CompoundTag data) {}

    @Override
    public void setDataPoint(String tag, Object value) {}

    public enum Orientation {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        CENTER
    }
}
