// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.item;

import net.minecraft.world.item.ItemStack;

public interface IDesignatorItem {

    static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    static int unpackZ(long packed) {
        return (int) packed;
    }

    boolean isReady(ItemStack stack);

    int getTargetX(ItemStack stack);

    int getTargetZ(ItemStack stack);
}
