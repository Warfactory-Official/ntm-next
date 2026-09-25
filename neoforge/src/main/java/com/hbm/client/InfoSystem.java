// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.config.HudConfig;
import com.hbm.platform.Services;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class InfoSystem {

    private static final Map<Integer, InfoEntry> inbox = new HashMap<>();
    private static final Map<Integer, InfoEntry> messages = new HashMap<>();
    private static int nextId = 1_000;

    private InfoSystem() {}

    public static void clientTick() {
        messages.putAll(inbox);
        inbox.clear();

        long now = System.currentTimeMillis();
        messages.entrySet()
                .removeIf(entry -> entry.getValue().start + entry.getValue().millis < now);
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (messages.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        List<InfoEntry> entries = new ArrayList<>(messages.values());
        Collections.sort(entries);

        int longest = 0;
        for (InfoEntry entry : entries) longest = Math.max(longest, font.width(entry.text));

        int mode = HudConfig.infoPosition;
        int x =
                mode == 0
                        ? 15
                        : mode == 1
                                ? minecraft.getWindow().getGuiScaledWidth() - longest - 15
                                : mode == 2
                                        ? minecraft.getWindow().getGuiScaledWidth() / 2 + 7
                                        : minecraft.getWindow().getGuiScaledWidth() / 2
                                                - longest
                                                - 6;
        int y = mode == 0 || mode == 1 ? 15 : minecraft.getWindow().getGuiScaledHeight() / 2 + 7;
        x += HudConfig.infoOffsetHorizontal;
        y += HudConfig.infoOffsetVertical;

        graphics.fill(x - 5, y - 5, x + 5 + longest, y + messages.size() * 10 + 2, 0x80404040);

        long now = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); i++) {
            InfoEntry entry = entries.get(i);
            int elapsed = (int) (now - entry.start);
            int alpha = Math.max(Math.min(510 * (entry.millis - elapsed) / entry.millis, 255), 5);
            graphics.text(font, entry.text, x, y + i * 10, ARGB.color(alpha, entry.color), false);
        }
    }

    public static void push(InfoEntry entry) {
        push(entry, nextId++);
    }

    public static void push(InfoEntry entry, int id) {
        inbox.put(id, entry);
    }

    public static final class InfoEntry implements Comparable<InfoEntry> {
        private final Component text;
        private final long start;
        private final int millis;
        private int color = 0xFFFFFF;

        public InfoEntry(String text) {
            this(Component.literal(text), 3_000);
        }

        public InfoEntry(String text, int millis) {
            this(Component.literal(text), millis);
        }

        public InfoEntry(Component text, int millis) {
            this.text = text;
            this.millis = millis;
            this.start = System.currentTimeMillis();
        }

        public InfoEntry withColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public int compareTo(InfoEntry other) {
            return Integer.compare(millis, other.millis);
        }
    }
}
