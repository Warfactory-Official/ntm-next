// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.ModDataComponents;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class HazardTransformerRadiationNBT implements IHazardTransformer {

    @Override
    public boolean appliesTo(ItemStack stack) {
        Float rad = stack.get(ModDataComponents.HAZ_RADIATION.get());
        return rad != null && rad > 0F;
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        entries.add(
                new HazardEntry(
                        HazardRegistry.RADIATION,
                        stack.get(ModDataComponents.HAZ_RADIATION.get())));
    }
}
