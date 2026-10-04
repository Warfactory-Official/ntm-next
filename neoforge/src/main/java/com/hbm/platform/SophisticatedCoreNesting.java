// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

public final class SophisticatedCoreNesting {
    private SophisticatedCoreNesting() {}

    public static boolean hasStorage(ItemStack stack) {
        return stack.has(ModCoreDataComponents.STORAGE_UUID.get());
    }
}
