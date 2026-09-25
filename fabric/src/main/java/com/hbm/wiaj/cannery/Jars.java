// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.EnumPlantType;
import com.hbm.items.ModItems;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public final class Jars {
    private static volatile @Nullable Map<Item, CanneryBase> byItem;

    private Jars() {}

    public static void initJars() {
        Map<Item, CanneryBase> canneries = new IdentityHashMap<>();
        register(canneries, ModBlocks.HEATER_FIREBOX.get(), new CanneryFirebox());
        CanneryStirling stirling = new CanneryStirling();
        register(canneries, ModBlocks.MACHINE_STIRLING.get(), stirling);
        register(canneries, ModBlocks.MACHINE_STIRLING_STEEL.get(), stirling);
        register(canneries, ModBlocks.MACHINE_GASCENT.get(), new CanneryCentrifuge());
        register(canneries, ModBlocks.MACHINE_FENSU.get(), new CanneryFEnSU());
        CannerySILEX silex = new CannerySILEX();
        register(canneries, ModBlocks.MACHINE_FEL.get(), silex);
        register(canneries, ModBlocks.MACHINE_SILEX.get(), silex);
        register(canneries, ModBlocks.FOUNDRY_CHANNEL.get(), new CanneryFoundryChannel());
        register(canneries, ModBlocks.MACHINE_CRUCIBLE.get(), new CanneryCrucible());
        CanneryWillow willow = new CanneryWillow();
        register(canneries, ModItems.PLANT_ITEM.get(EnumPlantType.MUSTARDWILLOW), willow);
        register(canneries, ModBlocks.PLANT_FLOWER_CD0.get(), willow);
        if (canneries.size() != 11)
            throw new IllegalStateException("Cannery item registration incomplete");
        byItem = canneries;
    }

    private static void register(
            Map<Item, CanneryBase> canneries, ItemLike trigger, CanneryBase cannery) {
        CanneryBase old = canneries.put(trigger.asItem(), cannery);
        if (old != null)
            throw new IllegalStateException("Duplicate cannery item: " + trigger.asItem());
    }

    public static @Nullable CanneryBase find(ItemStack stack) {
        Map<Item, CanneryBase> canneries = byItem;
        if (canneries == null)
            throw new IllegalStateException("Cannery catalog used before registration");
        return canneries.get(stack.getItem());
    }
}
