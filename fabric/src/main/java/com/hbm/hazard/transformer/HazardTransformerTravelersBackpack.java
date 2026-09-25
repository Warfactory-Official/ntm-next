// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.tiviacz.travelersbackpack.item.TravelersBackpackItem;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class HazardTransformerTravelersBackpack implements IHazardTransformer {
    private volatile DataComponentType<ItemContainerContents> storage;
    private DataComponentType<ItemContainerContents> tools;
    private DataComponentType<ItemContainerContents> upgrades;

    @Override
    public boolean appliesTo(ItemStack stack) {
        if (!(stack.getItem() instanceof TravelersBackpackItem)) return false;
        bind();
        return stack.has(storage) || stack.has(tools) || stack.has(upgrades);
    }

    @Override
    public boolean readsLiveStorage() {
        return true;
    }

    @Override
    public void transform(ItemStack stack, List<HazardEntry> entries) {
        float radiation =
                HazardTransformerRadiationContainer.sum(
                        stack.getOrDefault(storage, ItemContainerContents.EMPTY));
        radiation +=
                HazardTransformerRadiationContainer.sum(
                        stack.getOrDefault(tools, ItemContainerContents.EMPTY));
        radiation +=
                HazardTransformerRadiationContainer.sum(
                        stack.getOrDefault(upgrades, ItemContainerContents.EMPTY));
        if (radiation > 0F) entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
    }

    private void bind() {
        if (storage != null) return;
        synchronized (this) {
            if (storage != null) return;

            DataComponentType<ItemContainerContents> storageType = component("backpack_container");
            tools = component("tools_container");
            upgrades = component("upgrades");
            storage = storageType;
        }
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<ItemContainerContents> component(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("travelersbackpack", name);
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) {
            throw new IllegalStateException("Missing Traveler's Backpack component " + id);
        }
        return (DataComponentType<ItemContainerContents>)
                (DataComponentType<?>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
    }
}
