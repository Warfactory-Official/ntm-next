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

package com.hbm.handler;

import com.hbm.blockentity.PlushieBlockEntity;
import com.hbm.blockentity.SnowglobeBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.PlushieBlock.Plushie;
import com.hbm.blocks.generic.PlushieBlock;
import com.hbm.blocks.generic.SnowglobeBlock.SnowglobeType;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.food.ConserveItem.EnumFoodType;
import com.hbm.items.machine.BlueprintFolderItem;
import com.hbm.items.special.KitCustomItem;
import com.hbm.items.special.KitNBTItem;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.registration.Registrar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import static com.hbm.handler.BobmazonRequirement.ASSEMBLY;
import static com.hbm.handler.BobmazonRequirement.BLAST_FURNACE;
import static com.hbm.handler.BobmazonRequirement.CHEMICAL_PLANT;
import static com.hbm.handler.BobmazonRequirement.DESH;
import static com.hbm.handler.BobmazonRequirement.HIDDEN;
import static com.hbm.handler.BobmazonRequirement.TECHNETIUM;

public final class BobmazonOffers {

    private static @Nullable List<BobmazonOffer> standard;
    private static @Nullable List<BobmazonOffer> special;
    private static final List<Insert> SPECIAL_ADDONS = new ArrayList<>();

    private record Insert(Predicate<ItemStack> after, Supplier<BobmazonOffer> offer) {}

    private BobmazonOffers() {}

    public static synchronized List<BobmazonOffer> standard() {
        if (standard == null) standard = buildStandard();
        return standard;
    }

    public static synchronized List<BobmazonOffer> special() {
        if (special == null) special = buildSpecial();
        return special;
    }

    public static synchronized void addonSpecial(
            Predicate<ItemStack> after, Supplier<BobmazonOffer> offer) {
        Registrar.assertOpen("BobmazonOffers.addonSpecial");
        SPECIAL_ADDONS.add(new Insert(after, offer));
    }

    public static synchronized void invalidate() {
        standard = null;
        special = null;
    }

    public static @Nullable List<BobmazonOffer> forStack(ItemStack stack) {
        if (stack.is(ModItems.BOBMAZON.get())) return standard();
        if (stack.is(ModItems.BOBMAZON_HIDDEN.get())) return special();
        return null;
    }

    private static ItemStack ingot(Material material, int count) {
        return new ItemStack(ModItems.ingot(material), count);
    }

    private static ItemStack barrel(Fluid fluid, int count) {
        ItemStack stack = new ItemStack(ModItems.FLUID_BARREL, count);
        ModItems.FLUID_BARREL.get().setContent(stack, new FluidStackNTM(fluid, 16000));
        return stack;
    }

    private static List<BobmazonOffer> buildStandard() {
        List<BobmazonOffer> out = new ArrayList<>();

        out.add(new BobmazonOffer(new ItemStack(Items.TORCH, 64), BobmazonRequirement.FREE, 2));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModItems.DEFINITELY_FOOD, 16), BobmazonRequirement.FREE, 4));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NITRA, 4), CHEMICAL_PLANT, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_1), ASSEMBLY, 16));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModItems.GEIGER_COUNTER), BobmazonRequirement.FREE, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MATCHSTICK, 16), BLAST_FURNACE, 2));
        out.add(
                new BobmazonOffer(
                        ModItems.BLUEPRINT_FOLDER.stack(BlueprintFolderItem.Kind.ALT),
                        ASSEMBLY,
                        64));
        out.add(
                new BobmazonOffer(
                        ModItems.BLUEPRINT_FOLDER.stack(BlueprintFolderItem.Kind.DISCOVER),
                        DESH,
                        256));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.VENDING_MACHINE), CHEMICAL_PLANT, 64));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.VENDING_MACHINE_SNACKS), CHEMICAL_PLANT, 64));
        out.add(new BobmazonOffer(new ItemStack(Items.JUNGLE_SAPLING), BLAST_FURNACE, 12, 9));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.PLANT_FLOWER_FOXGLOVE), BLAST_FURNACE, 16, 5));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.PLANT_FLOWER_TOBACCO), BLAST_FURNACE, 16, 9));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.PLANT_FLOWER_NIGHTSHADE), BLAST_FURNACE, 16, 3));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.PLANT_FLOWER_WEED), BLAST_FURNACE, 4, 10));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.PLANT_FLOWER_CD0), TECHNETIUM, 64, 8));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_MACHINE, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_MACHINE_STRIPE, 16),
                        CHEMICAL_PLANT,
                        4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_INDIGO, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_PURPLE, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_PINK, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_HAZARD, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_SAND, 16), CHEMICAL_PLANT, 4));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.CONCRETE_EXT_BRONZE, 16), CHEMICAL_PLANT, 4));
        for (SnowglobeType globe : SnowglobeType.values()) {
            out.add(new BobmazonOffer(SnowglobeBlockEntity.stackOf(globe), CHEMICAL_PLANT, 128));
        }
        List<Plushie> plushies = PlushieBlock.TYPES.values();
        for (int ordinal = 1; ordinal < plushies.size(); ordinal++) {
            out.add(
                    new BobmazonOffer(
                            PlushieBlockEntity.stackOf(plushies.get(ordinal)),
                            DESH,
                            16,
                            ordinal < 3 ? 10 : 0));
        }

        return out;
    }

    private static List<BobmazonOffer> buildSpecial() {
        List<BobmazonOffer> out = new ArrayList<>();

        out.add(new BobmazonOffer(new ItemStack(Items.IRON_INGOT, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.STEEL, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(new ItemStack(Items.COPPER_INGOT, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.MINGRADE, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.TITANIUM, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.TUNGSTEN, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.COBALT, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.WORKERS_ALLOY, 64), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.TANTALUM, 64), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(ingot(Materials.BISMUTH, 16), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(ingot(Materials.SCHRABIDIUM, 16), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_EUPHEMIUM, 8), BLAST_FURNACE, 16));
        out.add(new BobmazonOffer(ingot(Materials.DINEUTRONIUM, 1), BLAST_FURNACE, 16));
        out.add(new BobmazonOffer(ingot(Materials.STARMETAL, 16), BLAST_FURNACE, 8));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_SEMTEX, 16), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.URANIUM_235, 16), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(ingot(Materials.PLUTONIUM_239, 16), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(new ItemStack(ModItems.AMMO_CONTAINER, 16), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_STARTER_KIT), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_ADVANCED_KIT), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.NUKE_COMMERCIALLY_KIT), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BOY_KIT), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.PROTOTYPE_KIT), BLAST_FURNACE, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MISSILE_KIT), BLAST_FURNACE, 5));
        out.add(new BobmazonOffer(new ItemStack(ModItems.JETPACK_VECTOR), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.JETPACK_TANK), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_1), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(new ItemStack(ModItems.GUN_KIT_2), BLAST_FURNACE, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE), BLAST_FURNACE, 3));
        out.add(
                new BobmazonOffer(
                        new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE), BLAST_FURNACE, 3));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_LAUNCHER, 40), BLAST_FURNACE, 7));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.STRUCT_SCAFFOLD, 11), BLAST_FURNACE, 7));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_10), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_15), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(new ItemStack(ModItems.LOOT_MISC), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CRATE_CAN), BLAST_FURNACE, 1));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.CRATE_AMMO), BLAST_FURNACE, 2));
        out.add(new BobmazonOffer(ModItems.CRUCIBLE.get().depleted(), BLAST_FURNACE, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_CHOPPER), BLAST_FURNACE, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_WORM), BLAST_FURNACE, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SPAWN_UFO), BLAST_FURNACE, 10));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SAT_LASER), HIDDEN, 8));
        out.add(new BobmazonOffer(new ItemStack(ModItems.SAT_GERALD), HIDDEN, 32));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BILLET_YHARONITE.get(), 4), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_CHAINSTEEL), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.INGOT_ELECTRONIUM), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.BOOK_OF), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MESE_PICKAXE), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.MYSTERY_SHOVEL), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModBlocks.NTM_DIRT), HIDDEN, 16));
        out.add(new BobmazonOffer(new ItemStack(ModItems.EUPHEMIUM_KIT), HIDDEN, 64));

        out.add(
                new BobmazonOffer(
                        KitCustomItem.create(
                                "Fusion Man",
                                "For the nuclear physicist on the go",
                                0xff00ff,
                                0x800080,
                                new ItemStack(ModBlocks.FUSION_KLYSTRON),
                                new ItemStack(ModBlocks.FUSION_TORUS),
                                new ItemStack(ModBlocks.FUSION_MHDT),
                                new ItemStack(ModBlocks.MACHINE_INTAKE, 3),
                                new ItemStack(ModItems.BATTERY_SPARK),
                                new ItemStack(ModBlocks.MACHINE_CHEMICAL_FACTORY, 4),
                                new ItemStack(ModBlocks.MACHINE_FLUID_TANK, 8),
                                new ItemStack(ModBlocks.RED_WIRE_COATED, 64),
                                new ItemStack(ModBlocks.CABLE, 64),
                                barrel(NTMFluids.DEUTERIUM, 64),
                                barrel(NTMFluids.TRITIUM, 64),
                                barrel(NTMFluids.PERFLUOROMETHYL, 64),
                                new ItemStack(ModBlocks.RED_PYLON_LARGE, 8),
                                new ItemStack(ModBlocks.SUBSTATION, 4),
                                new ItemStack(ModBlocks.RED_CONNECTOR, 64),
                                new ItemStack(ModItems.WIRING_RED_COPPER),
                                new ItemStack(ModBlocks.MACHINE_CHUNGUS, 3),
                                new ItemStack(ModItems.TEMPLATE_FOLDER),
                                new ItemStack(Items.PAPER, 64),
                                new ItemStack(Items.INK_SAC, 64)),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        KitCustomItem.create(
                                "Maid's Cleaning Utensils",
                                "For the hard to reach spots",
                                0x00ff00,
                                0x008000,
                                new ItemStack(ModItems.GUN_M2),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                ammo(EnumAmmo.BMG50_DU),
                                new ItemStack(ModItems.GUN_AUTOSHOTGUN),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_MAGNUM),
                                ammo(EnumAmmo.G12_EXPLOSIVE),
                                ammo(EnumAmmo.G12_EXPLOSIVE)),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        named(
                                KitNBTItem.create(
                                        named(new ItemStack(ModItems.ROD_OF_DISCORD), "Cock Joke"),
                                        named(
                                                ModItems.CANNED_CONSERVE.stack(
                                                        EnumFoodType.SLIME, 64),
                                                "Class A Horse Semen"),
                                        named(
                                                new ItemStack(ModItems.WEAPON_PIPE_LEAD),
                                                "Get Nutted, Dumbass"),
                                        new ItemStack(ModItems.GEM_ALEXANDRITE)),
                                "The Nut Bucket"),
                        HIDDEN,
                        64));

        out.add(
                new BobmazonOffer(
                        named(
                                KitNBTItem.create(
                                        new ItemStack(ModItems.RPA_HELMET),
                                        new ItemStack(ModItems.RPA_PLATE),
                                        new ItemStack(ModItems.RPA_LEGS),
                                        new ItemStack(ModItems.RPA_BOOTS),
                                        new ItemStack(ModItems.GUN_MINIGUN_LACUNAE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE),
                                        ammo(EnumAmmo.CAPACITOR_OVERCHARGE)),
                                "Frenchie's Reward"),
                        HIDDEN,
                        32));

        Set<BobmazonOffer> added = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Insert insert : SPECIAL_ADDONS) {
            int at = -1;
            for (int i = 0; i < out.size() && at < 0; i++) {
                if (!added.contains(out.get(i)) && insert.after().test(out.get(i).offer())) at = i;
            }
            if (at < 0)
                throw new IllegalStateException("no special offer to list an addon offer after");
            while (at + 1 < out.size() && added.contains(out.get(at + 1))) at++;
            BobmazonOffer offer = insert.offer().get();
            out.add(at + 1, offer);
            added.add(offer);
        }
        return out;
    }

    private static ItemStack ammo(EnumAmmo type) {
        return ModItems.AMMO_STANDARD.stack(type, 64);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
}
