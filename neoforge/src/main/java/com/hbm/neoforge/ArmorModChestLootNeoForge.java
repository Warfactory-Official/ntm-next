/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
                        primary.condition,
                        primary.modifier,
                        primary.rolls,
                        primary.bonusRolls,
                        Optional.ofNullable(primary.getName())));
    }
}
