// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import com.hbm.util.BobMathUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface ILookOverlay {

    void buildLookOverlay(Level level, BlockPos pos, LookInfo info);

    final class LookInfo {

        private final List<Component> lines = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();
        private final List<Heading> headings = new ArrayList<>();
        private boolean shadowedLines = true;
        private @Nullable Component title;
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
            return title(Component.literal(title), titleColor, bgColor);
        }

        public LookInfo title(Component title, int titleColor, int bgColor) {
            this.title = title;
            this.titleColor = titleColor;
            this.bgColor = bgColor;
            return this;
        }

        public LookInfo titleOnly(String title, int titleColor, int bgColor, int offset) {
            return titleOnly(Component.literal(title), titleColor, bgColor, offset);
        }

        public LookInfo titleOnly(Component title, int titleColor, int bgColor, int offset) {
            titleOffset = offset;
            return titleOnly(title, titleColor, bgColor);
        }

        public LookInfo titleOnly(Component title, int titleColor, int bgColor) {
            title(title, titleColor, bgColor);
            titleOnly = true;
            return this;
        }

        public int getTitleOffset() {
            return titleOffset;
        }

        public LookInfo heading(String text, int color, int bgColor, int offset) {
            return heading(Component.literal(text), color, bgColor, offset);
        }

        public LookInfo heading(Component text, int color, int bgColor, int offset) {
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
            return line(Component.literal(text), rgb);
        }

        public LookInfo line(Component text) {
            return line(text, TextColor.WHITE.getValue());
        }

        public LookInfo line(Component text, int rgb) {
            lines.add(text);
            colors.add(rgb);
            return this;
        }

        public LookInfo charge(long power, long maxPower) {
            line(
                    Component.translatable(
                            "overlay.hbm.charge",
                            BobMathUtil.getShortNumber(power),
                            BobMathUtil.getShortNumber(maxPower)));
            double percent = (double) power / (double) maxPower;
            int charge = (int) Math.floor(percent * 10_000D);
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
            return line((charge / 100D) + "%", color);
        }

        public @Nullable Component getTitle() {
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

        public Component line(int i) {
            return lines.get(i);
        }

        public int color(int i) {
            return colors.get(i);
        }
    }

    record Heading(Component text, int color, int bgColor, int offset) {}
}
