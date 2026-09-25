// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric;

import com.hbm.handler.ArmorModChestLoot;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.entries.LootItem;

final class ArmorModChestLootFabric {
    private ArmorModChestLootFabric() {}

    static void init() {
        LootTableEvents.MODIFY.register(
                (key, table, source, registries) -> {
                    if (key.identifier().equals(ArmorModChestLoot.BONUS_CHEST.table())) {
                        table.withPool(ArmorModChestLoot.BONUS_CHEST.pool());
                        return;
                    }
                    ArmorModChestLoot.PoolSpec spec = ArmorModChestLoot.get(key.identifier());
                    if (spec == null) return;

                    int[] layout = {0, 0};
                    table.modifyPools(
                            pool -> {
                                if (layout[0]++ == 0) layout[1] = pool.entries.build().size();
                            });
                    if (layout[0] < spec.expectedPoolCount()
                            || layout[1] < spec.expectedPrimaryEntryCount()) {
                        if (!source.isBuiltin()) return;
                        throw new IllegalStateException(
                                "Armor chest pool layout drifted: " + key.identifier());
                    }

                    int[] index = {0};
                    table.modifyPools(
                            pool -> {
                                if (index[0]++ != 0) return;
                                for (ArmorModChestLoot.Entry entry : spec.entries()) {
                                    pool.add(
                                            LootItem.lootTableItem(entry.singleItem())
                                                    .setWeight(entry.weight()));
                                }
                            });
                });
    }
}
