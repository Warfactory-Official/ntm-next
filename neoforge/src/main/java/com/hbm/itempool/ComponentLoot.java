/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.itempool;

import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public record ComponentLoot(String pool, int lo, int hi, boolean configScaled) {

    private static final List<ComponentLoot> LISTED = new ArrayList<>();
    public static final List<ComponentLoot> ALL = Collections.unmodifiableList(LISTED);

    public static final ComponentLoot ANTENNA_5 = of(ItemPoolIds.POOL_ANTENNA, 5);
    public static final ComponentLoot ANTENNA_10 = of(ItemPoolIds.POOL_ANTENNA, 10);
    public static final ComponentLoot EXPENSIVE_2 = of(ItemPoolIds.POOL_EXPENSIVE, 2);
    public static final ComponentLoot EXPENSIVE_7 = of(ItemPoolIds.POOL_EXPENSIVE, 7);
    public static final ComponentLoot EXPENSIVE_8 = of(ItemPoolIds.POOL_EXPENSIVE, 8);
    public static final ComponentLoot FILING_CABINET_4 = of(ItemPoolIds.POOL_FILING_CABINET, 4);
    public static final ComponentLoot FILING_CABINET_5 = of(ItemPoolIds.POOL_FILING_CABINET, 5);
    public static final ComponentLoot LAUNCH_KEY_1 = of(ItemPoolIds.POOL_LAUNCH_KEY, 1);
    public static final ComponentLoot GENERIC_8 = of(ItemPoolIds.POOL_GENERIC, 8);
    public static final ComponentLoot GENERIC_8_9 =
            register(new ComponentLoot(ItemPoolIds.POOL_GENERIC, 8, 9, true));
    public static final ComponentLoot GENERIC_8_9_UNSCALED =
            register(new ComponentLoot(ItemPoolIds.POOL_GENERIC, 8, 9, false));
    public static final ComponentLoot MACHINE_PARTS_4 = of(ItemPoolIds.POOL_MACHINE_PARTS, 4);
    public static final ComponentLoot MACHINE_PARTS_6 = of(ItemPoolIds.POOL_MACHINE_PARTS, 6);
    public static final ComponentLoot MACHINE_PARTS_10 = of(ItemPoolIds.POOL_MACHINE_PARTS, 10);
    public static final ComponentLoot MACHINE_PARTS_10_UNSCALED =
            unscaled(ItemPoolIds.POOL_MACHINE_PARTS, 10);
    public static final ComponentLoot ANTENNA_10_UNSCALED = unscaled(ItemPoolIds.POOL_ANTENNA, 10);
    public static final ComponentLoot ANTENNA_8_UNSCALED = unscaled(ItemPoolIds.POOL_ANTENNA, 8);
    public static final ComponentLoot NUKE_FUEL_5 = of(ItemPoolIds.POOL_NUKE_FUEL, 5);
    public static final ComponentLoot NUKE_FUEL_8 = of(ItemPoolIds.POOL_NUKE_FUEL, 8);
    public static final ComponentLoot NUKE_FUEL_10 = of(ItemPoolIds.POOL_NUKE_FUEL, 10);
    public static final ComponentLoot NUKE_TRASH_5 = of(ItemPoolIds.POOL_NUKE_TRASH, 5);
    public static final ComponentLoot NUKE_TRASH_9 = of(ItemPoolIds.POOL_NUKE_TRASH, 9);
    public static final ComponentLoot OFFICE_TRASH_4 = of(ItemPoolIds.POOL_OFFICE_TRASH, 4);
    public static final ComponentLoot OFFICE_TRASH_8 = of(ItemPoolIds.POOL_OFFICE_TRASH, 8);
    public static final ComponentLoot OFFICE_TRASH_10 = of(ItemPoolIds.POOL_OFFICE_TRASH, 10);
    public static final ComponentLoot SILO_6 = of(ItemPoolIds.POOL_SILO, 6);
    public static final ComponentLoot SOLID_FUEL_5 = of(ItemPoolIds.POOL_SOLID_FUEL, 5);
    public static final ComponentLoot SOLID_FUEL_6 = of(ItemPoolIds.POOL_SOLID_FUEL, 6);
    public static final ComponentLoot VAULT_LAB_6 = of(ItemPoolIds.POOL_VAULT_LAB, 6);
    public static final ComponentLoot VAULT_LAB_8 = of(ItemPoolIds.POOL_VAULT_LAB, 8);
    public static final ComponentLoot VAULT_LOCKERS_3 = of(ItemPoolIds.POOL_VAULT_LOCKERS, 3);
    public static final ComponentLoot VAULT_LOCKERS_4 = of(ItemPoolIds.POOL_VAULT_LOCKERS, 4);
    public static final ComponentLoot VAULT_LOCKERS_5 = of(ItemPoolIds.POOL_VAULT_LOCKERS, 5);
    public static final ComponentLoot VAULT_LOCKERS_6 = of(ItemPoolIds.POOL_VAULT_LOCKERS, 6);
    public static final ComponentLoot VAULT_LOCKERS_8 = of(ItemPoolIds.POOL_VAULT_LOCKERS, 8);
    public static final ComponentLoot VAULT_RUSTY_3 = of(ItemPoolIds.POOL_VAULT_RUSTY, 3);
    public static final ComponentLoot VAULT_RUSTY_3_6_UNSCALED =
            register(new ComponentLoot(ItemPoolIds.POOL_VAULT_RUSTY, 3, 6, false));
    public static final ComponentLoot VAULT_STANDARD_2_4_UNSCALED =
            register(new ComponentLoot(ItemPoolIds.POOL_VAULT_STANDARD, 2, 4, false));
    public static final ComponentLoot VAULT_REINFORCED_1_3_UNSCALED =
            register(new ComponentLoot(ItemPoolIds.POOL_VAULT_REINFORCED, 1, 3, false));
    public static final ComponentLoot VAULT_UNBREAKABLE_1_2_UNSCALED =
            register(new ComponentLoot(ItemPoolIds.POOL_VAULT_UNBREAKABLE, 1, 2, false));
    public static final ComponentLoot VERTIBIRD_5 = of(ItemPoolIds.POOL_VERTIBIRD, 5);

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
                RegistryUtil.id(
                        "chests/component/"
                                + (configScaled ? "" : "unscaled/")
                                + ItemPoolIds.getLootTablePath(pool)
                                + "_"
                                + lo
                                + "_"
                                + hi));
    }
}
