// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.qmaw;

import com.hbm.qmaw.QMAWCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public final class QMAWLayout {
    private static final Pattern WORDS = Pattern.compile("\\s+|\\S+");

    public record Span(
            String text,
            @Nullable ItemStack icon,
            @Nullable Identifier target,
            boolean link,
            int width,
            int height) {}

    public record Line(List<Span> spans, int height) {}

    private final Font font;
    private final int width;
    private final List<Line> lines = new ArrayList<>();
    private List<Span> current = new ArrayList<>();
    private int used;
    private int height;

    private QMAWLayout(Font font, int width) {
        this.font = font;
        this.width = width;
    }

    public static List<Line> build(
            Font font, int width, String text, QMAWCatalog catalog, Identifier page) {
        QMAWLayout layout = new QMAWLayout(font, width);
        int at = 0;
        while (at < text.length()) {
            if (text.startsWith("<br>", at)) {
                layout.newLine();
                at += 4;
            } else if (text.startsWith("[[", at) && text.indexOf("]]", at + 2) >= 0) {
                int end = text.indexOf("]]", at + 2);
                String link = text.substring(at + 2, end);
                int pipe = link.indexOf('|');
                String label = pipe < 0 ? link : link.substring(0, pipe);
                String destination = pipe < 0 ? link : link.substring(pipe + 1);
                Identifier target = catalog.link(page, destination);
                ItemStack icon =
                        target == null
                                ? null
                                : catalog.page(target)
                                        .icon()
                                        .map(ItemStackTemplate::create)
                                        .orElse(null);
                layout.append(label, icon, target, true);
                at = end + 2;
            } else {
                int end = text.length();
                int link = text.indexOf("[[", at + 1), br = text.indexOf("<br>", at + 1);
                if (link >= 0) end = Math.min(end, link);
                if (br >= 0) end = Math.min(end, br);
                layout.plain(text.substring(at, end));
                at = end;
            }
        }
        if (!layout.current.isEmpty() || layout.lines.isEmpty()) layout.newLine();
        return List.copyOf(layout.lines);
    }

    private void plain(String text) {
        var matcher = WORDS.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (word.isBlank()) {
                if (!current.isEmpty() && used + font.width(" ") <= width)
                    append(" ", null, null, false);
            } else {
                append(word, null, null, false);
            }
        }
    }

    private void append(
            String text, @Nullable ItemStack icon, @Nullable Identifier target, boolean link) {
        int prefix = icon == null ? 0 : 18;
        if (!current.isEmpty() && used + prefix + font.width(text) > width) newLine();
        do {
            String part = font.plainSubstrByWidth(text, width - used - prefix);
            if (part.isEmpty() && !text.isEmpty())
                part = text.substring(0, Character.charCount(text.codePointAt(0)));
            int spanWidth = prefix + font.width(part);
            int spanHeight = Math.max(font.lineHeight, icon == null ? 0 : 16);
            current.add(new Span(part, icon, target, link, spanWidth, spanHeight));
            used += spanWidth;
            height = Math.max(height, spanHeight);
            text = text.substring(part.length());
            icon = null;
            prefix = 0;
            if (!text.isEmpty()) newLine();
        } while (!text.isEmpty());
    }

    private void newLine() {
        lines.add(new Line(List.copyOf(current), Math.max(font.lineHeight, height)));
        current = new ArrayList<>();
        used = 0;
        height = 0;
    }
}
