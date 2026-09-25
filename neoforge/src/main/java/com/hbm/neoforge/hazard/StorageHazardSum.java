// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import java.util.List;
import net.minecraft.world.item.ItemStack;

final class StorageHazardSum {
    private StorageHazardSum() {}

    static float sum(List<ItemStack> contents) {
        float radiation = 0F;
        for (ItemStack held : contents) {
            if (!held.isEmpty()) {
                radiation +=
                        (float) HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION)
                                * held.getCount();
            }
        }
        return radiation;
    }
}
