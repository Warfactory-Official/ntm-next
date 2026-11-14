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
        var words = text.split(" ");
        var line = new StringBuilder(words[0]);
        int lineWidth = font.width(words[0]);
        for (int w = 1; w < words.length; w++) {
            var spaced = " " + words[w];
            lineWidth += font.width(spaced);
            if (lineWidth <= width) {
                line.append(spaced);
                continue;
            }
            lines.add(line.toString());
            line.setLength(0);
            line.append(words[w]);
            lineWidth = font.width(words[w]);
        }
        lines.add(line.toString());
        return lines;
    }
}
