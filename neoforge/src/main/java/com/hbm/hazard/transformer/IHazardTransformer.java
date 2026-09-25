// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface IHazardTransformer {

    boolean appliesTo(ItemStack stack);

    void transform(ItemStack stack, List<HazardEntry> entries);

    default boolean readsLiveStorage() {
        return false;
    }
}
