// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

public interface BaseAbility {

    String translationKey();

    int sortOrder();

    default int levels() {
        return 1;
    }

    default String extension(int level) {
        return "";
    }

    default boolean allowed() {
        return true;
    }
}
