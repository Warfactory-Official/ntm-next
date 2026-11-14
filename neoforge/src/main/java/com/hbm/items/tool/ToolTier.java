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

package com.hbm.items.tool;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

public record ToolTier(
        TagKey<Block> incorrectBlocksForDrops, int durability, float speed, int enchantmentValue) {

    public static final ToolTier SCHRABIDIUM = diamond(10_000, 50.0F, 200);
    public static final ToolTier STEEL = diamond(750, 8.0F, 10);
    public static final ToolTier TITANIUM = diamond(1_000, 9.0F, 15);
    public static final ToolTier CMB = diamond(8_500, 40.0F, 100);
    public static final ToolTier ELEC = diamond(0, 30.0F, 2);
    public static final ToolTier CHAINSAW = diamond(0, 50.0F, 0);
    public static final ToolTier ALLOY = diamond(2_000, 15.0F, 5);
    public static final ToolTier DESH = iron(0, 7.5F, 10);
    public static final ToolTier COBALT = diamond(750, 9.0F, 60);
    public static final ToolTier COBALT_DECORATED = diamond(2_500, 15.0F, 75);
    public static final ToolTier STARMETAL = diamond(3_000, 20.0F, 100);
    public static final ToolTier BISMUTH = beyond(0, 50.0F, 200);
    public static final ToolTier VOLCANIC = beyond(0, 50.0F, 200);
    public static final ToolTier CHLOROPHYTE = beyond(0, 75.0F, 200);
    public static final ToolTier MESE = beyond(0, 100.0F, 200);
    public static final ToolTier DWARVEN = iron(0, 4.0F, 10);
    public static final ToolTier METEORITE = beyond(0, 50.0F, 200);
    public static final ToolTier MESE_GAVEL = beyond(0, 50.0F, 200);
    public static final ToolTier SCHRABIDIUM_HAMMER = diamond(0, 50.0F, 200);
    public static final ToolTier SHIMMER =
            new ToolTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 0, 25.0F, 200);
    public static final ToolTier PIPE_LEAD =
            new ToolTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 250, 1.5F, 25);
    public static final ToolTier BOTTLE_OPENER =
            new ToolTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 250, 1.5F, 200);
    public static final ToolTier VANILLA_WOOD =
            new ToolTier(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 15);
    public static final ToolTier VANILLA_STONE =
            new ToolTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 5);
    public static final ToolTier VANILLA_DIAMOND = diamond(1_561, 8.0F, 10);

    public ToolMaterial vanilla(TagKey<Item> repairItems) {
        return new ToolMaterial(
                incorrectBlocksForDrops, durability, speed, 0.0F, enchantmentValue, repairItems);
    }

    private static ToolTier iron(int durability, float speed, int enchantmentValue) {
        return new ToolTier(BlockTags.INCORRECT_FOR_IRON_TOOL, durability, speed, enchantmentValue);
    }

    private static ToolTier diamond(int durability, float speed, int enchantmentValue) {
        return new ToolTier(
                BlockTags.INCORRECT_FOR_DIAMOND_TOOL, durability, speed, enchantmentValue);
    }

    private static ToolTier beyond(int durability, float speed, int enchantmentValue) {
        return new ToolTier(
                BlockTags.INCORRECT_FOR_NETHERITE_TOOL, durability, speed, enchantmentValue);
    }
}
