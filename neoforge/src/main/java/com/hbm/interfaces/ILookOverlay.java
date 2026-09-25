// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ILookOverlay {

    void buildLookOverlay(Level level, BlockPos pos, LookInfo info);

    final class LookInfo {

        private final List<String> lines = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();
        private final List<Heading> headings = new ArrayList<>();
        private boolean shadowedLines = true;
        private @Nullable String title;
        private int titleColor = 0xFFFF00;
        private int bgColor = 0x404000;
        private int titleOffset = -10;
        private boolean titleOnly;
        private @Nullable BlockPos hitPos;

        public LookInfo hitPos(BlockPos hitPos) {
            this.hitPos = hitPos;
            return this;
        }

        public @Nullable BlockPos getHitPos() {
            return hitPos;
        }

        public LookInfo title(String title, int titleColor, int bgColor) {
            this.title = title;
            this.titleColor = titleColor;
            this.bgColor = bgColor;
            return this;
        }

        public LookInfo titleOnly(String title, int titleColor, int bgColor, int offset) {
            title(title, titleColor, bgColor);
            this.titleOffset = offset;
            titleOnly = true;
            return this;
        }

        public int getTitleOffset() {
            return titleOffset;
        }

        public LookInfo heading(String text, int color, int bgColor, int offset) {
            headings.add(new Heading(text, color, bgColor, offset));
            return this;
        }

        public List<Heading> getHeadings() {
            return headings;
        }

        public LookInfo plainLines() {
            shadowedLines = false;
            return this;
        }

        public boolean shadowedLines() {
            return shadowedLines;
        }

        public LookInfo line(String text) {
            return line(text, TextColor.WHITE.getValue());
        }

        public LookInfo line(String text, int rgb) {
            lines.add(text);
            colors.add(rgb);
            return this;
        }

        public @Nullable String getTitle() {
            return title;
        }

        public int getTitleColor() {
            return titleColor;
        }

        public int getBgColor() {
            return bgColor;
        }

        public boolean isEmpty() {
            return !titleOnly && lines.isEmpty() && headings.isEmpty();
        }

        public int size() {
            return lines.size();
        }

        public String line(int i) {
            return lines.get(i);
        }

        public int color(int i) {
            return colors.get(i);
        }
    }

    record Heading(String text, int color, int bgColor, int offset) {}
}
