// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal.natives;

public final class RadsimKeys {

    public static final int EDIT_WIRE_BYTES = 48;

    public static final int EDIT_OFFSET_KEY = 0;
    public static final int EDIT_OFFSET_ADD = 8;
    public static final int EDIT_OFFSET_SET = 16;
    public static final int EDIT_OFFSET_SET_SEQ = 24;
    public static final int EDIT_OFFSET_LOCAL = 32;
    public static final int EDIT_OFFSET_FLAGS = 34;

    public static final int EDIT_OFFSET_SATURATION = 40;

    public static final byte EDIT_FLAG_HAS_SET = 1;

    public static final byte EDIT_FLAG_HAS_SATURATION = 2;

    private RadsimKeys() {}

    public static int slotOf(int sectionY, int minSectionY) {
        return sectionY - minSectionY;
    }

    public static long sectionKey(int sectionX, int slot, int sectionZ) {
        return ((long) (sectionX & 0x3FFFFF) << 42)
                | ((long) (sectionZ & 0x3FFFFF) << 20)
                | (slot & 0xFFFFFL);
    }
}
