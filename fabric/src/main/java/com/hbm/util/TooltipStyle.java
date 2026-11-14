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

package com.hbm.util;

import com.hbm.registration.Reg;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;

public final class TooltipStyle {

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private TooltipStyle() {}

    public static List<Component> lines(Component text) {
        String value = text.getString();
        if (value.isEmpty()) return List.of();
        if (!LINE_BREAK.matcher(value).find()) return List.of(text);
        List<Component> lines = new ArrayList<>();
        text.visit(
                (style, part) -> {
                    if (lines.isEmpty()) lines.add(Component.empty());
                    String[] parts = LINE_BREAK.split(part, -1);
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) lines.add(Component.empty());
                        if (!parts[i].isEmpty())
                            lines.getLast()
                                    .getSiblings()
                                    .add(Component.literal(parts[i]).withStyle(style));
                    }
                    return Optional.empty();
                },
                Style.EMPTY);
        while (!lines.isEmpty() && lines.getLast().getString().isEmpty()) lines.removeLast();
        return lines;
    }

    public static boolean isNtm(Item item) {
        return Reg.isNtmNamespace(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static Component defaultColor(Component line, ChatFormatting color) {
        return line.getStyle().getColor() != null
                ? line
                : line.copy().withStyle(style -> style.withColor(color));
    }

    public static Consumer<Component> defaultGray(Consumer<Component> adder) {
        return line -> adder.accept(defaultColor(line, ChatFormatting.GRAY));
    }

    public static Component holdDetails() {
        return Component.translatable(
                "desc.tooltip.hold",
                KeyHints.key("key.hbm.tooltipDetails", "key.keyboard.left.shift")
                        .copy()
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
