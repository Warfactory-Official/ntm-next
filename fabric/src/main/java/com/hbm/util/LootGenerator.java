// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.blocks.generic.BlockLoot.TileEntityLoot.Entry;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.itempool.LoreBooks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LootGenerator {

    public static final String LOOT_BOOKLET = "LOOT_BOOKLET";
    public static final String LOOT_CAPNUKE = "LOOT_CAPNUKE";
    public static final String LOOT_MEDICINE = "LOOT_MEDICINE";
    public static final String LOOT_CAPSTASH = "LOOT_CAPSTASH";
    public static final String LOOT_MAKESHIFT_GUN = "LOOT_MAKESHIFT_GUN";
    public static final String LOOT_NUKE_STORAGE = "LOOT_NUKE_STORAGE";
    public static final String LOOT_BONES = "LOOT_BONES";
    public static final String LOOT_GLYPHID_HIVE = "LOOT_GLYPHID_HIVE";
    public static final String LOOT_METEOR = "LOOT_METEOR";
    public static final String LOOT_FLAREGUN = "LOOT_FLAREGUN";
    public static final String LOOT_SHIT = "LOOT_SHIT";
    public static final String LOOT_MECHANICAL = "LOOT_MECHANICAL";
    public static final String LOOT_GEAR = "LOOT_GEAR";
    public static final String LOOT_SUPPLIES = "LOOT_SUPPLIES";

    public static List<Entry> roll(String name, RandomSource rand, long worldSeed) {
        Pile pile = new Pile(rand);
        switch (name) {
            case LOOT_BOOKLET:
                lootBooklet(pile);
                break;
            case LOOT_CAPNUKE:
                lootCapNuke(pile);
                break;
            case LOOT_MEDICINE:
                lootMedicine(pile);
                break;
            case LOOT_CAPSTASH:
                lootCapStash(pile);
                break;
            case LOOT_MAKESHIFT_GUN:
                lootMakeshiftGun(pile);
                break;
            case LOOT_NUKE_STORAGE:
                lootNukeStorage(pile);
                break;
            case LOOT_BONES:
                lootBones(pile);
                break;
            case LOOT_GLYPHID_HIVE:
                lootGlyphidHive(pile);
                break;
            case LOOT_METEOR:
                lootBookMeteor(pile, worldSeed);
                break;
            case LOOT_FLAREGUN:
                lootFlareGun(pile);
                break;
            case LOOT_SHIT:
                lootShit(pile);
                break;
            case LOOT_MECHANICAL:
                lootMechanical(pile);
                break;
            case LOOT_GEAR:
                lootGear(pile);
                break;
            case LOOT_SUPPLIES:
                lootSupplies(pile);
                break;
            default:
                lootBones(pile);
                break;
        }
        return pile.items;
    }

    public static String[] getLootNames() {
        return new String[] {
            LOOT_BOOKLET,
            LOOT_CAPNUKE,
            LOOT_MEDICINE,
            LOOT_CAPSTASH,
            LOOT_MAKESHIFT_GUN,
            LOOT_NUKE_STORAGE,
            LOOT_BONES,
            LOOT_GLYPHID_HIVE,
            LOOT_METEOR,
            LOOT_FLAREGUN,
            LOOT_SHIT,
            LOOT_MECHANICAL,
            LOOT_GEAR,
            LOOT_SUPPLIES,
        };
    }

    private LootGenerator() {}

    private record Pile(RandomSource rand, List<Entry> items) {

        Pile(RandomSource rand) {
            this(rand, new ArrayList<>());
        }

        void add(ItemStack stack, double x, double y, double z) {
            if (!stack.isEmpty()) items.add(new Entry(stack, x, y, z));
        }

        void deviated(ItemStack stack, double x, double y, double z) {
            add(stack, x + rand.nextGaussian() * 0.02, y, z + rand.nextGaussian() * 0.02);
        }
    }

    private static void lootBooklet(Pile pile) {
        pile.add(LoreBooks.BEACON.stack(), 0, 0, 0);
    }

    private static void lootCapNuke(Pile pile) {
        RandomSource rand = pile.rand();
        if (rand.nextInt(5) == 0) {
            pile.add(ammo(EnumAmmo.NUKE_STANDARD), -0.25, 0, -0.125);
        } else {
            pile.add(ammo(EnumAmmo.ROCKET_HEAT), -0.25, 0, -0.25);
        }

        for (int i = 0; i < 4; i++) {
            pile.deviated(new ItemStack(ModItems.CAP_NUKA.get(), 2), 0.125, i * 0.03125, 0.25);
        }
        for (int i = 0; i < 2; i++) {
            pile.deviated(
                    new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get(), 1),
                    -0.25,
                    i * 0.03125,
                    0.25);
        }
        for (int i = 0; i < 6; i++) {
            pile.deviated(new ItemStack(ModItems.CAP_NUKA.get(), 2), 0.125, i * 0.03125, -0.25);
        }
    }

    private static ItemStack ammo(EnumAmmo variant) {
        return ModItems.AMMO_STANDARD.stack(variant);
    }

    private static void lootMedicine(Pile pile) {
        RandomSource rand = pile.rand();
        for (int i = 0; i < 4; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MED_SYRINGE, rand),
                    0.125,
                    i * 0.03125,
                    0.25);
        }
        pile.deviated(ItemPool.getStack(ItemPools.POOL_PILE_MED_PILLS, rand), -0.25, 0, -0.125);
    }

    private static void lootCapStash(Pile pile) {
        RandomSource rand = pile.rand();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int count = rand.nextInt(5) + 3;
                for (int k = 0; k < count; k++) {
                    pile.deviated(
                            ItemPool.getStack(ItemPools.POOL_PILE_CAPS, rand),
                            i * 0.3125,
                            k * 0.03125,
                            j * 0.3125);
                }
            }
        }
    }

    private static void lootMakeshiftGun(Pile pile) {
        RandomSource rand = pile.rand();
        boolean r = rand.nextBoolean();
        if (r) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_GUN, rand), 0.125, 0.025, 0.25);
        }
        if (!r || rand.nextBoolean()) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_WRENCH, rand),
                    -0.25,
                    0,
                    -0.28125);
        }

        int count = rand.nextInt(2) + 1;
        for (int i = 0; i < count; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_PLATES, rand),
                    -0.25,
                    i * 0.03125,
                    0.3125);
        }

        count = rand.nextInt(2) + 2;
        for (int i = 0; i < count; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_WIRE, rand),
                    0.25,
                    i * 0.03125,
                    0.1875);
        }
    }

    private static void lootNukeStorage(Pile pile) {
        RandomSource rand = pile.rand();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (rand.nextBoolean()) {
                    pile.add(
                            ItemPool.getStack(ItemPools.POOL_PILE_NUKE_STORAGE, rand),
                            -0.375 + i * 0.25,
                            0,
                            -0.375 + j * 0.25);
                }
            }
        }
    }

    private static void lootBones(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(3) + 3;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_BONES, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }

    private static void lootGlyphidHive(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(3) + 3;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_HIVE, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }

    private static void lootBookMeteor(Pile pile, long worldSeed) {
        RandomSource rand = pile.rand();
        Item mku = LoreBooks.mkuItem(rand);
        ItemStack book = LoreBooks.mkuBook(worldSeed, mku);
        pile.deviated(new ItemStack(mku), 0, 0, 0.25);
        pile.deviated(book, 0, 0, -0.25);
    }

    public static List<Entry> lootBookLore(ItemStack book, RandomSource rand) {
        Pile pile = new Pile(rand);
        pile.deviated(book, 0, 0, -0.25);

        int count = rand.nextInt(3) + 2;
        for (int k = 0; k < count; k++) {
            pile.deviated(new ItemStack(Items.BOOK), -0.25, k * 0.03125, 0.25);
        }
        count = rand.nextInt(2) + 1;
        for (int k = 0; k < count; k++) {
            pile.deviated(new ItemStack(Items.PAPER), 0.25, k * 0.03125, 0.125);
        }
        return pile.items;
    }

    private static void lootFlareGun(Pile pile) {
        RandomSource rand = pile.rand();
        pile.deviated(new ItemStack(ModItems.GUN_FLAREGUN.get()), 0, 0, -0.25);

        int count = rand.nextInt(3) + 2;
        for (int k = 0; k < count; k++) {
            pile.deviated(ammo(EnumAmmo.G26_FLARE), -0.25, k * 0.03125, 0.25);
        }

        count = rand.nextInt(1) + 1;
        for (int k = 0; k < count; k++) {
            pile.deviated(
                    ammo(
                            rand.nextBoolean()
                                    ? EnumAmmo.G26_FLARE_SUPPLY
                                    : EnumAmmo.G26_FLARE_WEAPON),
                    0.25,
                    k * 0.03125,
                    0.125);
        }
    }

    private static void lootShit(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(3) + 3;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_OF_GARBAGE, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }

    private static void lootMechanical(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(6) + 1;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_MECHANICAL, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }

    private static void lootGear(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(6) + 1;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_GEAR, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }

    private static void lootSupplies(Pile pile) {
        RandomSource rand = pile.rand();
        int limit = rand.nextInt(3) + 4;
        for (int i = 0; i < limit; i++) {
            pile.deviated(
                    ItemPool.getStack(ItemPools.POOL_PILE_SUPPLIES, rand),
                    rand.nextDouble() - 0.5,
                    i * 0.03125,
                    rand.nextDouble() - 0.5);
        }
    }
}
