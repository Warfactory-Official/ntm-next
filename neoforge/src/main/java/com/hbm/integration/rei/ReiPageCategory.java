// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.rei;

import com.hbm.integration.recipeviewer.RecipePage;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

final class ReiPageCategory implements DisplayCategory<ReiPageDisplay> {

    private static final int PADDING = 4;
    private static final int TEXT_COLOUR = 0xFF000000;
    private static final int LINE_SPACING = 2;

    private final RecipePage<?> page;
    private final CategoryIdentifier<ReiPageDisplay> id;

    ReiPageCategory(RecipePage<?> page) {
        this.page = page;
        this.id = CategoryIdentifier.of(page.id());
    }

    @Override
    public CategoryIdentifier<? extends ReiPageDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return page.title();
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(page.icon());
    }

    @Override
    public int getDisplayWidth(ReiPageDisplay display) {
        return page.width() + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return page.height() + 2 * PADDING;
    }

    @Override
    public List<Widget> setupDisplay(ReiPageDisplay display, Rectangle bounds) {
        int x = bounds.x + PADDING;
        int y = bounds.y + PADDING;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(
                Widgets.createDrawableWidget(
                        (graphics, mouseX, mouseY, delta) -> {
                            graphics.pose().pushMatrix();
                            graphics.pose().translate(x, y);
                            draw(display, graphics);
                            graphics.pose().popMatrix();
                        }));
        for (ReiPageDisplay.Slot recorded : display.slots()) {
            Slot slot =
                    Widgets.createSlot(new Point(x + recorded.x, y + recorded.y))
                            .entries(recorded.entries);
            if (!recorded.background) slot.disableBackground();
            switch (recorded.role) {
                case INPUT -> slot.markInput();
                case OUTPUT -> slot.markOutput();
                case CATALYST, DISPLAY -> {}
            }
            widgets.add(slot);
        }
        for (int[] arrow : display.arrows())
            widgets.add(Widgets.createArrow(new Point(x + arrow[0], y + arrow[1])));
        for (ReiPageDisplay.Text text : display.texts()) text(widgets, text, x, y);
        widgets.add(Widgets.createTooltip(point -> tooltip(display, point, x, y)));
        return widgets;
    }

    @SuppressWarnings("unchecked")
    private static <T> Tooltip tooltip(ReiPageDisplay display, Point point, int x, int y) {
        List<Component> lines = new ArrayList<>();
        ((RecipePage<T>) display.page())
                .tooltip((T) display.row(), point.x - x, point.y - y, lines::add);
        return lines.isEmpty() ? null : Tooltip.create(point, lines);
    }

    @SuppressWarnings("unchecked")
    private static <T> void draw(ReiPageDisplay display, GuiGraphicsExtractor graphics) {
        ((RecipePage<T>) display.page()).draw((T) display.row(), graphics);
    }

    private static void text(
            List<Widget> widgets, ReiPageDisplay.Text text, int originX, int originY) {
        Font font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight + LINE_SPACING;
        int maxLines = text.height() / lineHeight;
        if (maxLines * lineHeight + font.lineHeight <= text.height()) maxLines++;
        List<FormattedText> lines =
                new ArrayList<>(
                        font.getSplitter().splitLines(text.text(), text.width(), Style.EMPTY));
        boolean clipped = lines.size() > maxLines;
        if (clipped) {
            FormattedText ellipsis = FormattedText.of("...");
            FormattedText last =
                    font.substrByWidth(
                            lines.get(maxLines - 1), text.width() - font.width(ellipsis));
            lines = new ArrayList<>(lines.subList(0, maxLines - 1));
            lines.add(FormattedText.composite(last, ellipsis));
        }
        int x = originX + text.x();
        int y = originY + text.y();
        List<FormattedText> shown = lines;
        widgets.add(
                Widgets.createDrawableWidget(
                        (graphics, mouseX, mouseY, delta) -> {
                            int lineY = y;
                            for (FormattedText line : shown) {
                                graphics.text(
                                        font,
                                        Language.getInstance().getVisualOrder(line),
                                        x,
                                        lineY,
                                        TEXT_COLOUR,
                                        false);
                                lineY += lineHeight;
                            }
                        }));
        if (clipped) {
            Rectangle area = new Rectangle(x, y, text.width(), text.height());
            widgets.add(
                    Widgets.createTooltip(
                            point ->
                                    area.contains(point)
                                            ? Tooltip.create(point, text.text())
                                            : null));
        }
    }
}
