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

import com.hbm.blockentity.machine.fusion.FusionTorusAssemblyBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public final class ConstructionCatalog {
    private static final int SLOT_SPLIT = 256;

    private ConstructionCatalog() {}

    public record Entry(List<List<ItemStack>> pile, ItemStack assembled, ItemStack seed) {}

    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        out.add(watz());
        out.add(compactLauncher());
        out.add(launchTable());
        out.add(soyuzLauncher());
        out.add(icf());
        out.add(fusionTorus());
        return out;
    }

    private static Entry compactLauncher() {
        List<List<ItemStack>> pile = new ArrayList<>();
        slot(pile, ModBlocks.STRUCT_LAUNCHER, 8);
        return new Entry(
                pile,
                new ItemStack(ModBlocks.COMPACT_LAUNCHER),
                new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE));
    }

    private static Entry launchTable() {
        List<List<ItemStack>> pile = new ArrayList<>();
        slot(pile, ModBlocks.STRUCT_LAUNCHER, 16);
        slot(pile, ModBlocks.STRUCT_LAUNCHER, 64);
        slot(pile, ModBlocks.STRUCT_SCAFFOLD, 11);
        return new Entry(
                pile,
                new ItemStack(ModBlocks.LAUNCH_TABLE),
                new ItemStack(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE));
    }

    private static Entry soyuzLauncher() {
        List<List<ItemStack>> pile = new ArrayList<>();
        slot(pile, ModBlocks.STRUCT_LAUNCHER, 30);
        slot(pile, ModBlocks.STRUCT_LAUNCHER, 384);
        slot(pile, ModBlocks.STRUCT_SCAFFOLD, 63);
        slot(pile, ModBlocks.STRUCT_SCAFFOLD, 384);
        slot(pile, ModBlocks.CONCRETE_SMOOTH, 38);
        slot(pile, ModBlocks.CONCRETE_SMOOTH, 256);
        return new Entry(
                pile,
                new ItemStack(ModBlocks.SOYUZ_LAUNCHER),
                new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE));
    }

    private static Entry watz() {
        List<List<ItemStack>> pile = new ArrayList<>();
        slot(pile, ModBlocks.WATZ_END, 48);
        ItemLike bolt = Autogen.require(MaterialShape.BOLT, Materials.DURA_STEEL);
        slot(pile, bolt, 64);
        slot(pile, bolt, 64);
        slot(pile, bolt, 64);
        slot(pile, ModBlocks.WATZ_ELEMENT, 36);
        slot(pile, ModBlocks.WATZ_COOLER, 26);
        slot(pile, ModItems.BOLTGUN, 1);
        return new Entry(
                pile, new ItemStack(ModBlocks.WATZ), new ItemStack(ModBlocks.STRUCT_WATZ_CORE));
    }

    private static Entry icf() {
        List<List<ItemStack>> pile = new ArrayList<>();
        slot(pile, ModBlocks.ICF_COMPONENT, 50);
        slot(pile, ModBlocks.ICF_COMPONENT_STRUCTURE, 240);
        slot(pile, Autogen.require(MaterialShape.BOLT, Materials.DURA_STEEL), 960);
        slot(pile, Autogen.require(MaterialShape.CAST_PLATE, Materials.STEEL), 240);
        slot(pile, ModBlocks.ICF_COMPONENT_VESSEL, 117);
        pile.add(
                List.of(
                        new ItemStack(
                                Autogen.require(MaterialShape.CAST_PLATE, Materials.BISMUTH_BRONZE),
                                117),
                        new ItemStack(
                                Autogen.require(MaterialShape.CAST_PLATE, Materials.ARSENIC_BRONZE),
                                117)));
        slot(pile, ModItems.BLOWTORCH, 1);
        slot(pile, ModItems.BOLTGUN, 1);
        return new Entry(
                pile,
                new ItemStack(ModBlocks.MACHINE_ICF),
                new ItemStack(ModBlocks.STRUCT_ICF_CORE));
    }

    private static Entry fusionTorus() {
        Map<Block, Integer> shell = new LinkedHashMap<>();
        FusionTorusAssemblyBlockEntity.forEachRequirement(
                (block, dx, dy, dz) -> {
                    if (dx == 0 && dy == 0 && dz == 0) return;
                    shell.merge(block, 1, Integer::sum);
                });
        int walls = shell.getOrDefault(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get(), 0);

        List<List<ItemStack>> pile = new ArrayList<>();
        split(pile, ModBlocks.FUSION_COMPONENT, walls);
        split(pile, Autogen.require(MaterialShape.CAST_PLATE, Materials.STEEL), walls);
        split(
                pile,
                ModBlocks.FUSION_COMPONENT_BLANKET,
                shell.getOrDefault(ModBlocks.FUSION_COMPONENT_BLANKET.get(), 0));
        split(
                pile,
                ModBlocks.FUSION_COMPONENT_MOTOR,
                shell.getOrDefault(ModBlocks.FUSION_COMPONENT_MOTOR.get(), 0));
        slot(pile, ModItems.BLOWTORCH, 1);
        return new Entry(
                pile,
                new ItemStack(ModBlocks.FUSION_TORUS),
                new ItemStack(ModBlocks.STRUCT_TORUS_CORE));
    }

    private static void slot(List<List<ItemStack>> pile, ItemLike item, int count) {
        pile.add(List.of(new ItemStack(item, count)));
    }

    private static void split(List<List<ItemStack>> pile, ItemLike item, int count) {
        int left = count;
        while (left > 0) {
            int chunk = Math.min(left, SLOT_SPLIT);
            slot(pile, item, chunk);
            left -= chunk;
        }
    }
}
