// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class PileRodLore {
    private PileRodLore() {}

    public static void add(String key, Consumer<Component> adder) {
        var text = Component.translatable(key).withStyle(ChatFormatting.YELLOW);
        if (!RenderSystem.isOnRenderThread()) {
            adder.accept(text);
            return;
        }
        for (String line : wrap(text.getString(), 225))
            adder.accept(Component.literal(line).withStyle(ChatFormatting.YELLOW));
    }

    private static List<String> wrap(String text, int width) {
        var font = Minecraft.getInstance().font;
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        lines.add(words[0]);
        int indent = font.width(words[0]);
        for (int i = 1; i < words.length; i++) {
            int next = indent + font.width(" " + words[i]);
            if (next <= width) {
                int last = lines.size() - 1;
                lines.set(last, lines.get(last) + " " + words[i]);
                indent = next;
            } else {
                lines.add(words[i]);
                indent = font.width(words[i]);
            }
        }
        return lines;
    }
}
