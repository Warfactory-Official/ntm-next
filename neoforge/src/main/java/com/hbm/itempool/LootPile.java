/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.blocks.generic.LootBlock.BlockEntityLoot.Entry;
import com.hbm.blocks.generic.LootBlock.BlockEntityLoot;
import com.hbm.blocks.generic.LootBlock;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

import static com.hbm.itempool.ItemPoolIds.*;

public enum LootPile {
    BOOKLET("LOOT_BOOKLET", LootPile::booklet),
    CAPNUKE("LOOT_CAPNUKE", LootPile::capNuke),
    MEDICINE("LOOT_MEDICINE", LootPile::medicine),
    CAPSTASH("LOOT_CAPSTASH", LootPile::capStash),
    MAKESHIFT_GUN("LOOT_MAKESHIFT_GUN", LootPile::makeshiftGun),
    NUKE_STORAGE("LOOT_NUKE_STORAGE", LootPile::nukeStorage),
    BONES("LOOT_BONES", scatter(POOL_PILE_BONES, 3, 3)),
    GLYPHID_HIVE("LOOT_GLYPHID_HIVE", scatter(POOL_PILE_HIVE, 3, 3)),
    METEOR("LOOT_METEOR", LootPile::meteor),
    FLAREGUN("LOOT_FLAREGUN", LootPile::flareGun),
    GARBAGE("LOOT_SHIT", scatter(POOL_PILE_OF_GARBAGE, 3, 3)),
    MECHANICAL("LOOT_MECHANICAL", scatter(POOL_PILE_MECHANICAL, 1, 6)),
    GEAR("LOOT_GEAR", scatter(POOL_PILE_GEAR, 1, 6)),
    SUPPLIES("LOOT_SUPPLIES", scatter(POOL_PILE_SUPPLIES, 4, 3));

    private static final double LAYER = 0.03125;
    private static final double DEVIATION = 0.02;
    private static final LootPile[] KINDS = values();
    private static final Map<String, LootPile> BY_ID = new HashMap<>();

    static {
        for (LootPile kind : KINDS) BY_ID.put(kind.id, kind);
    }

    private final String id;
    private final Layout layout;

    LootPile(String id, Layout layout) {
        this.id = id;
        this.layout = layout;
    }

    public String id() {
        return id;
    }

    public static @Nullable LootPile byId(String id) {
        return BY_ID.get(id);
    }

    public static List<Entry> roll(String id, RandomSource random, long worldSeed) {
        LootPile kind = BY_ID.get(id);
        return roll(kind == null ? KINDS.length : kind.ordinal(), random, worldSeed);
    }

    public List<Entry> roll(RandomSource random, long worldSeed) {
        return roll(ordinal(), random, worldSeed);
    }

    private static List<Entry> roll(int start, RandomSource random, long worldSeed) {
        Pile pile = new Pile(random, worldSeed);
        for (int k = start; k < KINDS.length; k++) {
            KINDS[k].layout.lay(pile);
            if (pile.produced > 0) return pile.entries;
        }
        BONES.layout.lay(pile);
        return pile.entries;
    }

    public void place(LevelAccessor level, BlockPos pos, RandomSource random, long worldSeed) {
        BlockEntityLoot target = LootBlock.placeEmpty(level, pos);
        if (target != null) target.fill(roll(random, worldSeed));
    }

    public static void placeLore(
            LevelAccessor level, BlockPos pos, ItemStack book, RandomSource random) {
        BlockEntityLoot target = LootBlock.placeEmpty(level, pos);
        if (target == null) return;
        Pile pile = new Pile(random, 0L);
        pile.deviated(book, 0, 0, -0.25);
        int books = 2 + random.nextInt(3);
        for (int k = 0; k < books; k++)
            pile.deviated(new ItemStack(Items.BOOK), -0.25, k * LAYER, 0.25);
        int papers = 1 + random.nextInt(2);
        for (int k = 0; k < papers; k++)
            pile.deviated(new ItemStack(Items.PAPER), 0.25, k * LAYER, 0.125);
        target.fill(pile.entries);
    }

    private static Layout scatter(String pool, int base, int spread) {
        return pile -> {
            RandomSource random = pile.random;
            int count = base + random.nextInt(spread);
            for (int k = 0; k < count; k++) {
                ItemStack stack = pile.draw(pool);
                double x = random.nextDouble() - 0.5;
                double z = random.nextDouble() - 0.5;
                pile.deviated(stack, x, k * LAYER, z);
            }
        };
    }

    private static void booklet(Pile pile) {
        pile.put(LoreBooks.BEACON.stack(), 0, 0, 0);
    }

    private static void capNuke(Pile pile) {
        if (pile.random.nextInt(5) == 0) pile.put(ammo(EnumAmmo.NUKE_STANDARD), -0.25, 0, -0.125);
        else pile.put(ammo(EnumAmmo.ROCKET_HEAT), -0.25, 0, -0.25);
        for (int k = 0; k < 4; k++)
            pile.deviated(new ItemStack(ModItems.CAP_NUKA.get(), 2), 0.125, k * LAYER, 0.25);
        for (int k = 0; k < 2; k++) {
            pile.deviated(
                    new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get()), -0.25, k * LAYER, 0.25);
        }
        for (int k = 0; k < 6; k++)
            pile.deviated(new ItemStack(ModItems.CAP_NUKA.get(), 2), 0.125, k * LAYER, -0.25);
    }

    private static void medicine(Pile pile) {
        for (int k = 0; k < 4; k++)
            pile.deviated(pile.draw(POOL_PILE_MED_SYRINGE), 0.125, k * LAYER, 0.25);
        pile.deviated(pile.draw(POOL_PILE_MED_PILLS), -0.25, 0, -0.125);
    }

    private static void capStash(Pile pile) {
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                int count = 3 + pile.random.nextInt(5);
                for (int k = 0; k < count; k++) {
                    pile.deviated(pile.draw(POOL_PILE_CAPS), 0.3125 * a, k * LAYER, 0.3125 * b);
                }
            }
        }
    }

    private static void makeshiftGun(Pile pile) {
        RandomSource random = pile.random;
        boolean gun = random.nextBoolean();
        if (gun) pile.deviated(pile.draw(POOL_PILE_MAKESHIFT_GUN), 0.125, 0.025, 0.25);
        if (!gun || random.nextBoolean())
            pile.deviated(pile.draw(POOL_PILE_MAKESHIFT_WRENCH), -0.25, 0, -0.28125);
        int plates = 1 + random.nextInt(2);
        for (int k = 0; k < plates; k++) {
            pile.deviated(pile.draw(POOL_PILE_MAKESHIFT_PLATES), -0.25, k * LAYER, 0.3125);
        }
        int wires = 2 + random.nextInt(2);
        for (int k = 0; k < wires; k++)
            pile.deviated(pile.draw(POOL_PILE_MAKESHIFT_WIRE), 0.25, k * LAYER, 0.1875);
    }

    private static void nukeStorage(Pile pile) {
        for (int a = 0; a < 4; a++) {
            for (int b = 0; b < 4; b++) {
                if (pile.random.nextBoolean()) {
                    pile.put(
                            pile.draw(POOL_PILE_NUKE_STORAGE),
                            -0.375 + 0.25 * a,
                            0,
                            -0.375 + 0.25 * b);
                }
            }
        }
    }

    private static void meteor(Pile pile) {
        Item ingredient = LoreBooks.mkuItem(pile.random);
        ItemStack book = LoreBooks.mkuBook(pile.worldSeed, ingredient);
        pile.deviated(new ItemStack(ingredient), 0, 0, 0.25);
        pile.deviated(book, 0, 0, -0.25);
    }

    private static void flareGun(Pile pile) {
        RandomSource random = pile.random;
        pile.deviated(new ItemStack(ModItems.GUN_FLAREGUN.get()), 0, 0, -0.25);
        int flares = 2 + random.nextInt(3);
        for (int k = 0; k < flares; k++)
            pile.deviated(ammo(EnumAmmo.G26_FLARE), -0.25, k * LAYER, 0.25);
        int signals = 1 + random.nextInt(1);
        for (int k = 0; k < signals; k++) {
            EnumAmmo flare =
                    random.nextBoolean() ? EnumAmmo.G26_FLARE_SUPPLY : EnumAmmo.G26_FLARE_WEAPON;
            pile.deviated(ammo(flare), 0.25, k * LAYER, 0.125);
        }
    }

    private static ItemStack ammo(EnumAmmo type) {
        return ModItems.AMMO_STANDARD.stack(type);
    }

    @FunctionalInterface
    private interface Layout {
        void lay(Pile pile);
    }

    private static final class Pile {
        final RandomSource random;
        final long worldSeed;
        final List<Entry> entries = new ArrayList<>();
        int produced;

        Pile(RandomSource random, long worldSeed) {
            this.random = random;
            this.worldSeed = worldSeed;
        }

        ItemStack draw(String pool) {
            return ItemPool.getStack(pool, random);
        }

        void put(ItemStack stack, double x, double y, double z) {
            produced++;
            if (!stack.isEmpty()) entries.add(new Entry(stack, x, y, z));
        }

        void deviated(ItemStack stack, double x, double y, double z) {
            double dx = DEVIATION * random.nextGaussian();
            double dz = DEVIATION * random.nextGaussian();
            put(stack, x + dx, y, z + dz);
        }
    }
}
