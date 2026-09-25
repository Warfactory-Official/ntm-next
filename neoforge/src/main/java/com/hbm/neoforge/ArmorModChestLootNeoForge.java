// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge;

import com.hbm.handler.ArmorModChestLoot;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.event.LootTableLoadEvent;

final class ArmorModChestLootNeoForge {
    private ArmorModChestLootNeoForge() {}

    static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName().equals(ArmorModChestLoot.BONUS_CHEST.table())) {
            event.getTable().addPool(ArmorModChestLoot.BONUS_CHEST.pool().build());
            return;
        }
        ArmorModChestLoot.PoolSpec spec = ArmorModChestLoot.get(event.getName());
        if (spec == null) return;

        var pools = event.getTable().pools;
        if (pools.size() < spec.expectedPoolCount()) return;
        LootPool primary = pools.getFirst();
        if (primary.entries.size() < spec.expectedPrimaryEntryCount()) return;

        var entries = new ArrayList<LootPoolEntryContainer>(primary.entries);
        for (ArmorModChestLoot.Entry entry : spec.entries()) {
            entries.add(
                    LootItem.lootTableItem(entry.singleItem()).setWeight(entry.weight()).build());
        }
        pools.set(
                0,
                new LootPool(
                        entries,
                        primary.conditions,
                        primary.functions,
                        primary.rolls,
                        primary.bonusRolls,
                        Optional.ofNullable(primary.getName())));
    }
}
