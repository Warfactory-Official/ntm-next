// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

public class EnumUtil {

    public static <T extends Enum<T>> T grabEnumSafely(Class<T> theEnum, int index) {
        T[] values = theEnum.getEnumConstants();
        index = Math.abs(index % values.length);
        return values[index];
    }
}
