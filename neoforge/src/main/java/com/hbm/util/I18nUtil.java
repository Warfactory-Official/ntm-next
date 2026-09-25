// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class I18nUtil {

    private static final String[] EMPTY = new String[0];

    private I18nUtil() {}

    public static String resolveKey(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public static String[] resolveKeyArray(String key, Object... args) {
        return resolveKey(key, args).split("\\R");
    }

    public static String[] loreLines(String key, Object... args) {
        return Language.getInstance().has(key) ? resolveKeyArray(key, args) : EMPTY;
    }
}
