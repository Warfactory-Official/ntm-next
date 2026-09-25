// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.itempool;

import java.util.Locale;

public final class ItemPools {

    private ItemPools() {}

    public static String slug(String pool) {
        String name = pool.startsWith("POOL_") ? pool.substring("POOL_".length()) : pool;
        return name.toLowerCase(Locale.ROOT);
    }

    public static final String POOL_SUPPLIES = "POOL_SUPPLIES";
    public static final String POOL_WEAPONS = "POOL_WEAPONS";
    public static final String POOL_AMMO = "POOL_AMMO";

    public static final String POOL_MACHINE_PARTS = "POOL_MACHINE_PARTS";
    public static final String POOL_NUKE_FUEL = "POOL_NUKE_FUEL";
    public static final String POOL_SILO = "POOL_SILO";
    public static final String POOL_OFFICE_TRASH = "POOL_OFFICE_TRASH";
    public static final String POOL_FILING_CABINET = "POOL_FILING_CABINET";
    public static final String POOL_SOLID_FUEL = "POOL_SOLID_FUEL";
    public static final String POOL_VAULT_LAB = "POOL_VAULT_LAB";
    public static final String POOL_VAULT_LOCKERS = "POOL_VAULT_LOCKERS";
    public static final String POOL_METEOR_SAFE = "POOL_METEOR_SAFE";
    public static final String POOL_OIL_RIG = "POOL_OIL_RIG";
    public static final String POOL_RTG = "POOL_RTG";
    public static final String POOL_REPAIR_MATERIALS = "POOL_REPAIR_MATERIALS";

    public static final String POOL_GENERIC = "POOL_GENERIC";
    public static final String POOL_ANTENNA = "POOL_ANTENNA";
    public static final String POOL_EXPENSIVE = "POOL_EXPENSIVE";
    public static final String POOL_NUKE_TRASH = "POOL_NUKE_TRASH";
    public static final String POOL_NUKE_MISC = "POOL_NUKE_MISC";
    public static final String POOL_VERTIBIRD = "POOL_VERTIBIRD";
    public static final String POOL_SPACESHIP = "POOL_SPACESHIP";

    public static final String POOL_LAUNCH_KEY = "POOL_LAUNCH_KEY";

    public static final String POOL_PILE_HIVE = "POOL_PILE_HIVE";
    public static final String POOL_PILE_BONES = "POOL_PILE_BONES";
    public static final String POOL_PILE_CAPS = "POOL_PILE_CAPS";
    public static final String POOL_PILE_MED_SYRINGE = "POOL_PILE_MED_SYRINGE";
    public static final String POOL_PILE_MED_PILLS = "POOL_PILE_MED_PILLS";
    public static final String POOL_PILE_MAKESHIFT_GUN = "POOL_PILE_MAKESHIFT_GUN";
    public static final String POOL_PILE_MAKESHIFT_WRENCH = "POOL_PILE_MAKESHIFT_WRENCH";
    public static final String POOL_PILE_MAKESHIFT_PLATES = "POOL_PILE_MAKESHIFT_PLATES";
    public static final String POOL_PILE_MAKESHIFT_WIRE = "POOL_PILE_MAKESHIFT_WIRE";
    public static final String POOL_PILE_NUKE_STORAGE = "POOL_PILE_NUKE_STORAGE";
    public static final String POOL_PILE_OF_GARBAGE = "POOL_PILE_OF_GARBAGE";
    public static final String POOL_PILE_MECHANICAL = "POOL_PILE_MECHANICAL";
    public static final String POOL_PILE_GEAR = "POOL_PILE_GEAR";
    public static final String POOL_PILE_SUPPLIES = "POOL_PILE_SUPPLIES";

    public static final String POOL_RED_PEDESTAL = "POOL_RED_PEDESTAL";
    public static final String POOL_BLACK_SLAB = "POOL_BLACK_SLAB";
    public static final String POOL_BLACK_PART = "POOL_BLACK_PART";

    public static final String POOL_VAULT_RUSTY = "POOL_VAULT_RUSTY";
    public static final String POOL_VAULT_STANDARD = "POOL_VAULT_STANDARD";
    public static final String POOL_VAULT_REINFORCED = "POOL_VAULT_REINFORCED";
    public static final String POOL_VAULT_UNBREAKABLE = "POOL_VAULT_UNBREAKABLE";
    public static final String POOL_METEORITE_TREASURE = "POOL_METEORITE_TREASURE";
    public static final String POOL_BLUEPRINTS = "POOL_BLUEPRINTS";

    public static final String POOL_SAT_MINER = "POOL_SAT_MINER";
    public static final String POOL_SAT_LUNAR = "POOL_SAT_LUNAR";

    public static final String POOL_SODA = "POOL_SODA";
    public static final String POOL_SNACKS = "POOL_SNACKS";
}
