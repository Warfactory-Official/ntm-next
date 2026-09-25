// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.hazard;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.transformer.IHazardTransformer;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;

public final class HazardTransformerSophisticatedBackpacks implements IHazardTransformer {
    @Override
    public boolean appliesTo(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem
                && stack.has(ModCoreDataComponents.STORAGE_UUID.get())
                && !stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT.get());
    }

    @Override
    public boolean readsLiveStorage() {
        return true;
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        UUID id = stack.get(ModCoreDataComponents.STORAGE_UUID.get());
        float radiation =
                StorageHazardSum.sum(
                        BackpackStorage.get().getOrCreateBackpackContents(id).inventory().stacks());
        if (radiation > 0F) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }
}
