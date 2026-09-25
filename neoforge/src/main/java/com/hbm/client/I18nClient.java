// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public final class I18nClient {

    private I18nClient() {}

    public static List<String> autoBreakWithParagraphs(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\$")) lines.addAll(autoBreak(font, paragraph, width));
        return lines;
    }

    public static List<String> autoBreak(Font font, String text, int width) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);
        for (int w = 1; w < words.length; w++) {
            indent += font.width(" " + words[w]);
            if (indent <= width) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
        }
        return lines;
    }
}
