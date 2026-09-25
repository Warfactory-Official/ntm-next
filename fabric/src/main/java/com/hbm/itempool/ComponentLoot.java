// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.itempool;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public record ComponentLoot(String pool, int lo, int hi, boolean configScaled) {

    private static final List<ComponentLoot> LISTED = new ArrayList<>();

    public static final List<ComponentLoot> ALL = Collections.unmodifiableList(LISTED);

    public static final ComponentLoot ANTENNA_5 = of(ItemPools.POOL_ANTENNA, 5);
    public static final ComponentLoot ANTENNA_8 = of(ItemPools.POOL_ANTENNA, 8);
    public static final ComponentLoot ANTENNA_10 = of(ItemPools.POOL_ANTENNA, 10);
    public static final ComponentLoot EXPENSIVE_2 = of(ItemPools.POOL_EXPENSIVE, 2);

    public static final ComponentLoot EXPENSIVE_7 = of(ItemPools.POOL_EXPENSIVE, 7);

    public static final ComponentLoot EXPENSIVE_8 = of(ItemPools.POOL_EXPENSIVE, 8);
    public static final ComponentLoot FILING_CABINET_4 = of(ItemPools.POOL_FILING_CABINET, 4);
    public static final ComponentLoot FILING_CABINET_5 = of(ItemPools.POOL_FILING_CABINET, 5);
    public static final ComponentLoot LAUNCH_KEY_1 = of(ItemPools.POOL_LAUNCH_KEY, 1);
    public static final ComponentLoot GENERIC_8 = of(ItemPools.POOL_GENERIC, 8);

    public static final ComponentLoot GENERIC_8_9 =
            register(new ComponentLoot(ItemPools.POOL_GENERIC, 8, 9, true));

    public static final ComponentLoot GENERIC_8_9_UNSCALED =
            register(new ComponentLoot(ItemPools.POOL_GENERIC, 8, 9, false));

    public static final ComponentLoot MACHINE_PARTS_4 = of(ItemPools.POOL_MACHINE_PARTS, 4);
    public static final ComponentLoot MACHINE_PARTS_6 = of(ItemPools.POOL_MACHINE_PARTS, 6);
    public static final ComponentLoot MACHINE_PARTS_10 = of(ItemPools.POOL_MACHINE_PARTS, 10);

    public static final ComponentLoot MACHINE_PARTS_10_UNSCALED =
            unscaled(ItemPools.POOL_MACHINE_PARTS, 10);

    public static final ComponentLoot ANTENNA_10_UNSCALED = unscaled(ItemPools.POOL_ANTENNA, 10);

    public static final ComponentLoot NUKE_FUEL_5 = of(ItemPools.POOL_NUKE_FUEL, 5);
    public static final ComponentLoot NUKE_FUEL_8 = of(ItemPools.POOL_NUKE_FUEL, 8);
    public static final ComponentLoot NUKE_FUEL_10 = of(ItemPools.POOL_NUKE_FUEL, 10);

    public static final ComponentLoot NUKE_TRASH_5 = of(ItemPools.POOL_NUKE_TRASH, 5);
    public static final ComponentLoot NUKE_TRASH_9 = of(ItemPools.POOL_NUKE_TRASH, 9);
    public static final ComponentLoot OFFICE_TRASH_4 = of(ItemPools.POOL_OFFICE_TRASH, 4);
    public static final ComponentLoot OFFICE_TRASH_8 = of(ItemPools.POOL_OFFICE_TRASH, 8);
    public static final ComponentLoot OFFICE_TRASH_10 = of(ItemPools.POOL_OFFICE_TRASH, 10);

    public static final ComponentLoot SILO_6 = of(ItemPools.POOL_SILO, 6);
    public static final ComponentLoot SOLID_FUEL_5 = of(ItemPools.POOL_SOLID_FUEL, 5);
    public static final ComponentLoot SOLID_FUEL_6 = of(ItemPools.POOL_SOLID_FUEL, 6);

    public static final ComponentLoot VAULT_LAB_6 = of(ItemPools.POOL_VAULT_LAB, 6);
    public static final ComponentLoot VAULT_LAB_8 = of(ItemPools.POOL_VAULT_LAB, 8);
    public static final ComponentLoot VAULT_LOCKERS_3 = of(ItemPools.POOL_VAULT_LOCKERS, 3);
    public static final ComponentLoot VAULT_LOCKERS_4 = of(ItemPools.POOL_VAULT_LOCKERS, 4);
    public static final ComponentLoot VAULT_LOCKERS_5 = of(ItemPools.POOL_VAULT_LOCKERS, 5);
    public static final ComponentLoot VAULT_LOCKERS_6 = of(ItemPools.POOL_VAULT_LOCKERS, 6);
    public static final ComponentLoot VAULT_LOCKERS_8 = of(ItemPools.POOL_VAULT_LOCKERS, 8);
    public static final ComponentLoot VAULT_RUSTY_3 = of(ItemPools.POOL_VAULT_RUSTY, 3);

    public static final ComponentLoot VAULT_RUSTY_3_6_UNSCALED =
            register(new ComponentLoot(ItemPools.POOL_VAULT_RUSTY, 3, 6, false));
    public static final ComponentLoot VAULT_STANDARD_2_4_UNSCALED =
            register(new ComponentLoot(ItemPools.POOL_VAULT_STANDARD, 2, 4, false));
    public static final ComponentLoot VAULT_REINFORCED_1_3_UNSCALED =
            register(new ComponentLoot(ItemPools.POOL_VAULT_REINFORCED, 1, 3, false));
    public static final ComponentLoot VAULT_UNBREAKABLE_1_2_UNSCALED =
            register(new ComponentLoot(ItemPools.POOL_VAULT_UNBREAKABLE, 1, 2, false));
    public static final ComponentLoot VERTIBIRD_5 = of(ItemPools.POOL_VERTIBIRD, 5);

    private static ComponentLoot of(String pool, int rolls) {
        return register(new ComponentLoot(pool, rolls, rolls, true));
    }

    private static ComponentLoot unscaled(String pool, int rolls) {
        return register(new ComponentLoot(pool, rolls, rolls, false));
    }

    private static ComponentLoot register(ComponentLoot loot) {
        if (LISTED.contains(loot)) {
            throw new IllegalArgumentException(loot + " is already a declared component roll");
        }
        LISTED.add(loot);
        return loot;
    }

    public ResourceKey<LootTable> table() {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                Library.id(
                        "chests/component/"
                                + (configScaled ? "" : "unscaled/")
                                + ItemPools.slug(pool)
                                + "_"
                                + lo
                                + "_"
                                + hi));
    }
}
