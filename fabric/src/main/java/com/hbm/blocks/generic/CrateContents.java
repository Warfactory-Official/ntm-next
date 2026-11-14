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

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.food.ConserveItem;
import com.hbm.items.machine.BatteryPackType;
import com.hbm.items.weapon.grenade.GrenadeExtraItem.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.GrenadeFillingItem.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.GrenadeFuzeItem.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.GrenadeShellItem.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeUniversalItem;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class CrateContents {
    public static final EnumAmmo[] SMALL_AMMO = {
        EnumAmmo.P9_SP, EnumAmmo.P9_FMJ, EnumAmmo.M357_SP, EnumAmmo.M357_FMJ,
        EnumAmmo.M44_SP, EnumAmmo.M44_FMJ, EnumAmmo.R556_SP, EnumAmmo.R556_FMJ,
        EnumAmmo.R762_SP, EnumAmmo.R762_FMJ, EnumAmmo.G12, EnumAmmo.G12_SLUG
    };
    public static final EnumAmmo[] ORDNANCE_AMMO = {EnumAmmo.G40_HE, EnumAmmo.ROCKET_HE};

    private CrateContents() {}

    public static List<ItemStack> canPool() {
        return Canned.POOL;
    }

    public static List<ItemStack> poolOf(Block block) {
        if (block == ModBlocks.CRATE_RED.get()) return Loot.RED;
        if (block == ModBlocks.CRATE_WEAPON.get()) return Loot.WEAPON;
        if (block == ModBlocks.CRATE_LEAD.get()) return Loot.LEAD;
        if (block == ModBlocks.CRATE_METAL.get()) return Loot.METAL;
        return Loot.NORMAL;
    }

    private static final class Canned {
        private static final List<ItemStack> POOL = create();

        private static List<ItemStack> create() {
            var pool = new ArrayList<ItemStack>();
            for (var food : ConserveItem.EnumFoodType.values()) {
                pool.add(ModItems.CANNED_CONSERVE.stack(food));
            }
            pool.add(new ItemStack(ModItems.CAN_SMART.get()));
            pool.add(new ItemStack(ModItems.CAN_CREATURE.get()));
            pool.add(new ItemStack(ModItems.CAN_REDBOMB.get()));
            pool.add(new ItemStack(ModItems.CAN_MRSUGAR.get()));
            pool.add(new ItemStack(ModItems.CAN_OVERCHARGE.get()));
            pool.add(new ItemStack(ModItems.CAN_LUNA.get()));
            pool.add(new ItemStack(ModItems.CAN_BREEN.get()));
            pool.add(new ItemStack(ModItems.CAN_BEPIS.get()));
            pool.add(new ItemStack(ModItems.PUDDING.get()));
            return List.copyOf(pool);
        }
    }

    private static final class Loot {
        private static final List<ItemStack> NORMAL = normal();
        private static final List<ItemStack> WEAPON = weapon();
        private static final List<ItemStack> LEAD = lead();
        private static final List<ItemStack> METAL = metal();
        private static final List<ItemStack> RED =
                List.of(
                        new ItemStack(ModItems.MYSTERY_SHOVEL.get()),
                        new ItemStack(ModItems.GUN_HEAVY_REVOLVER_LILMAC.get()),
                        new ItemStack(ModItems.GUN_AUTOSHOTGUN_SEXY.get()),
                        new ItemStack(ModItems.GUN_MARESLEG_BROKEN.get()),
                        ModItems.AMMO_SECRET.stack(EnumAmmoSecret.M44_EQUESTRIAN),
                        ModItems.AMMO_SECRET.stack(EnumAmmoSecret.G12_EQUESTRIAN),
                        ModItems.AMMO_SECRET.stack(EnumAmmoSecret.BMG50_EQUESTRIAN),
                        new ItemStack(ModItems.BATTERY_SPARK.get()),
                        new ItemStack(ModItems.BOTTLE_SPARKLE.get()),
                        new ItemStack(ModItems.BOTTLE_RAD.get()),
                        new ItemStack(ModItems.RING_STARMETAL.get()),
                        new ItemStack(ModItems.FLAME_PONY.get()),
                        new ItemStack(ModBlocks.NTM_DIRT.get()),
                        new ItemStack(ModBlocks.BROADCASTER_PC.get()));

        private static List<ItemStack> normal() {
            var pool = new ArrayList<ItemStack>();
            add(pool, ModItems.SYRINGE_METAL_STIMPAK.get(), 10);
            add(pool, ModItems.SYRINGE_ANTIDOTE.get(), 5);
            add(
                    pool,
                    GrenadeUniversalItem.make(
                            EnumGrenadeShell.FRAG,
                            EnumGrenadeFilling.HE,
                            EnumGrenadeFuze.S3,
                            EnumGrenadeExtra.FRAG_SLEEVE),
                    8);
            add(
                    pool,
                    GrenadeUniversalItem.make(
                            EnumGrenadeShell.STICK, EnumGrenadeFilling.HE, EnumGrenadeFuze.IMPACT),
                    6);
            add(
                    pool,
                    GrenadeUniversalItem.make(
                            EnumGrenadeShell.FRAG, EnumGrenadeFilling.INC, EnumGrenadeFuze.S7),
                    4);
            add(pool, ModItems.AMMO_CONTAINER.get(), 2);
            return List.copyOf(pool);
        }

        private static List<ItemStack> weapon() {
            var pool = new ArrayList<ItemStack>();
            add(pool, ModItems.GUN_LIGHT_REVOLVER.get(), 10);
            add(pool, ModItems.GUN_MARESLEG.get(), 7);
            add(pool, ModItems.GUN_HEAVY_REVOLVER.get(), 5);
            add(pool, ModItems.GUN_GREASEGUN.get(), 5);
            add(pool, ModItems.GUN_LIBERATOR.get(), 2);
            add(pool, ModItems.GUN_FLAREGUN.get(), 8);
            add(pool, ModItems.GUN_PANZERSCHRECK.get(), 1);
            return List.copyOf(pool);
        }

        private static List<ItemStack> lead() {
            var pool = new ArrayList<ItemStack>();
            add(pool, ModItems.ingot(Materials.URANIUM), 10);
            add(pool, ModItems.ingot(Materials.URANIUM_238), 8);
            add(pool, ModItems.ingot(Materials.PLUTONIUM), 7);
            add(pool, ModItems.ingot(Materials.PLUTONIUM_240), 6);
            add(pool, ModItems.ingot(Materials.NEPTUNIUM_237), 7);
            add(pool, ModItems.INGOT_URANIUM_FUEL.get(), 8);
            add(pool, ModItems.INGOT_PLUTONIUM_FUEL.get(), 7);
            add(pool, ModItems.INGOT_MOX_FUEL.get(), 6);
            add(pool, ModItems.nugget(Materials.URANIUM), 10);
            add(pool, ModItems.nugget(Materials.URANIUM_238), 8);
            add(pool, ModItems.nugget(Materials.PLUTONIUM), 7);
            add(pool, ModItems.nugget(Materials.PLUTONIUM_240), 6);
            add(pool, ModItems.nugget(Materials.NEPTUNIUM_237), 7);
            add(pool, ModItems.NUGGET_URANIUM_FUEL.get(), 8);
            add(pool, ModItems.NUGGET_PLUTONIUM_FUEL.get(), 7);
            add(pool, ModItems.NUGGET_MOX_FUEL.get(), 6);
            add(pool, ModItems.CELL_DEUTERIUM.get(), 8);
            add(pool, ModItems.CELL_TRITIUM.get(), 8);
            add(pool, ModItems.CELL_UF6.get(), 8);
            add(pool, ModItems.CELL_PUF6.get(), 8);
            add(pool, ModItems.PELLET_RTG.get(), 6);
            add(pool, ModItems.PELLET_RTG_WEAK.get(), 7);
            add(pool, ModItems.POWDER_YELLOWCAKE.get(), 10);
            return List.copyOf(pool);
        }

        private static List<ItemStack> metal() {
            var pool = new ArrayList<ItemStack>();
            add(pool, ModBlocks.MACHINE_PRESS.get(), 10);
            add(pool, ModBlocks.MACHINE_REACTOR_BREEDING.get(), 6);
            add(pool, ModBlocks.MACHINE_WOOD_BURNER.get(), 10);
            add(pool, ModBlocks.MACHINE_DIESEL.get(), 8);
            add(pool, ModBlocks.MACHINE_RTG.get(), 4);
            add(pool, ModBlocks.RED_PYLON.get(), 9);
            add(pool, ModItems.batteryPack(BatteryPackType.BATTERY_LEAD), 10);
            add(pool, ModBlocks.MACHINE_ELECTRIC_FURNACE.get(), 8);
            add(pool, ModBlocks.MACHINE_ASSEMBLY_MACHINE.get(), 10);
            add(pool, ModBlocks.MACHINE_FLUID_TANK.get(), 7);
            add(pool, ModItems.CENTRIFUGE_ELEMENT.get(), 6);
            add(pool, ModItems.MOTOR.get(), 8);
            add(pool, ModItems.COIL_TUNGSTEN.get(), 7);
            add(pool, ModItems.PHOTO_PANEL.get(), 3);
            add(pool, ModItems.COIL_COPPER.get(), 10);
            add(pool, ModItems.BLADE_TITANIUM.get(), 3);
            add(pool, ModItems.PISTON_SELENIUM.get(), 6);
            return List.copyOf(pool);
        }
    }

    private static void add(List<ItemStack> pool, ItemLike item, int weight) {
        add(pool, new ItemStack(item), weight);
    }

    private static void add(List<ItemStack> pool, ItemStack prototype, int weight) {
        for (int i = 0; i < weight; i++) {
            pool.add(prototype);
        }
    }
}
