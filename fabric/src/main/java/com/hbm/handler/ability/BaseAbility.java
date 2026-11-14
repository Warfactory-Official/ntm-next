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

package com.hbm.handler.ability;

import com.hbm.data.DataGroups;
import org.jspecify.annotations.Nullable;

public abstract sealed class BaseAbility
        permits ToolAreaAbility, ToolHarvestAbility, WeaponAbility {

    private static final String[] ONE_LEVEL = {""};

    private final String translationKey;
    private final int sortOrder;
    private final String[] levelText;
    private final DataGroups.@Nullable Param<Boolean> gate;

    BaseAbility(
            String translationKey,
            int sortOrder,
            String[] levelText,
            DataGroups.@Nullable Param<Boolean> gate) {
        this.translationKey = translationKey;
        this.sortOrder = sortOrder;
        this.levelText = levelText;
        this.gate = gate;
    }

    public final String translationKey() {
        return translationKey;
    }

    public final int sortOrder() {
        return sortOrder;
    }

    public final int levels() {
        return levelText.length;
    }

    public final String extension(int level) {
        return levelText[level];
    }

    public final boolean allowed() {
        return gate == null || gate.get();
    }

    abstract int index();

    static String[] oneLevel() {
        return ONE_LEVEL;
    }

    static String[] levelText(int[] values) {
        var text = new String[values.length];
        for (int i = 0; i < values.length; i++) text[i] = " (" + values[i] + ")";
        return text;
    }

    static String[] levelText(float[] values) {
        var text = new String[values.length];
        for (int i = 0; i < values.length; i++) text[i] = " (" + values[i] + ")";
        return text;
    }

    static String[] ratioText(int[] values) {
        var text = new String[values.length];
        for (int i = 0; i < values.length; i++) text[i] = " (1:" + values[i] + ")";
        return text;
    }
}
