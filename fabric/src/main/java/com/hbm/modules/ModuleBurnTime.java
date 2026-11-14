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

package com.hbm.modules;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.FuelHandler;
import com.hbm.items.ModItems;
import com.hbm.tags.HbmItemTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.hbm.inventory.material.MaterialTagRegistry.LIGNITE;

public class ModuleBurnTime {

    private static final int MOD_LOG = 0;
    private static final int MOD_WOOD = 1;
    private static final int MOD_COAL = 2;
    private static final int MOD_LIGNITE = 3;
    private static final int MOD_COKE = 4;
    private static final int MOD_SOLID = 5;
    private static final int MOD_ROCKET = 6;
    private static final int MOD_BALEFIRE = 7;
    private static final int COUNT = 8;

    private static final String[] NAMES = {
        "Logs", "Wood", "Coal", "Lignite", "Coke", "Solid Fuel", "Rocket Fuel", "Balefire"
    };

    private final double[] modTime = new double[COUNT];
    private final double[] modHeat = new double[COUNT];
    private List<String> description;

    public ModuleBurnTime() {
        for (int i = 0; i < COUNT; i++) {
            modTime[i] = 1.0D;
            modHeat[i] = 1.0D;
        }
    }

    private static void addIf(List<String> list, String name, double mod) {
        if (mod != 1.0D) list.add(ChatFormatting.YELLOW + "- " + name + ": " + percent(mod));
    }

    private static String percent(double mod) {
        mod -= 1D;
        String num = ((int) (mod * 100)) + "%";
        return mod < 0 ? ChatFormatting.RED + num : ChatFormatting.GREEN + "+" + num;
    }

    public int getBurnTime(Level level, ItemStack stack) {
        return getBurnTime(level, stack, 1.0D);
    }

    public int getBurnTime(Level level, ItemStack stack, double def) {
        int base = FuelHandler.getBurnTime(level, stack);
        if (base <= 0) return 0;
        return (int) (base * getMod(stack, def));
    }

    public int getBurnHeat(int base, ItemStack stack) {
        if (base <= 0) return 0;
        return (int) (base * getMod(stack, modHeat, 1.0D));
    }

    public double getHeatMod(ItemStack stack) {
        return getMod(stack, modHeat, 1.0D);
    }

    private double getMod(ItemStack stack, double def) {
        return getMod(stack, modTime, def);
    }

    private double getMod(ItemStack stack, double[] mod, double def) {
        if (stack.isEmpty()) return def;
        Item item = stack.getItem();
        if (item == ModItems.SOLID_FUEL.get()
                || item == ModItems.SOLID_FUEL_PRESTO.get()
                || item == ModItems.SOLID_FUEL_PRESTO_TRIPLET.get()) return mod[MOD_SOLID];
        if (item == ModItems.SOLID_FUEL_BF.get()
                || item == ModItems.SOLID_FUEL_PRESTO_BF.get()
                || item == ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get()) return mod[MOD_BALEFIRE];
        if (item == ModItems.ROCKET_FUEL.get()) return mod[MOD_ROCKET];

        if (ModItems.COKE.typeOf(item) != null
                || item == ModBlocks.BLOCK_COKE_COAL.get().asItem()
                || item == ModBlocks.BLOCK_COKE_LIGNITE.get().asItem()
                || item == ModBlocks.BLOCK_COKE_PETROLEUM.get().asItem()) return mod[MOD_COKE];
        var briquette = ModItems.BRIQUETTE.typeOf(item);
        if (briquette != null)
            return switch (briquette) {
                case COAL -> mod[MOD_COAL];
                case LIGNITE -> mod[MOD_LIGNITE];
                case WOOD -> mod[MOD_WOOD];
            };
        if (stack.is(HbmItemTags.COAL_FUELS)) return mod[MOD_COAL];
        if (stack.is(LIGNITE.gem()) || stack.is(LIGNITE.dust()) || stack.is(LIGNITE.ore()))
            return mod[MOD_LIGNITE];
        if (stack.is(ItemTags.LOGS)) return mod[MOD_LOG];
        if (stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.WOODEN_TOOL_MATERIALS)) return mod[MOD_WOOD];
        return def;
    }

    public List<String> getDesc() {
        if (description == null) {
            var lines = new ArrayList<String>();
            appendDescription(lines, modTime, ChatFormatting.GOLD + "Burn time bonuses:");
            appendDescription(lines, modHeat, ChatFormatting.RED + "Burn heat bonuses:");
            description = List.copyOf(lines);
        }
        return description;
    }

    private static void appendDescription(List<String> lines, double[] modifiers, String heading) {
        int start = lines.size();
        lines.add(heading);
        for (int category = 0; category < COUNT; category++) {
            addIf(lines, NAMES[category], modifiers[category]);
        }
        if (lines.size() == start + 1) lines.remove(start);
    }

    private ModuleBurnTime setMod(double[] modifiers, int category, double value) {
        modifiers[category] = value;
        description = null;
        return this;
    }

    public ModuleBurnTime setLogTimeMod(double mod) {
        return setMod(modTime, MOD_LOG, mod);
    }

    public ModuleBurnTime setWoodTimeMod(double mod) {
        return setMod(modTime, MOD_WOOD, mod);
    }

    public ModuleBurnTime setCoalTimeMod(double mod) {
        return setMod(modTime, MOD_COAL, mod);
    }

    public ModuleBurnTime setLigniteTimeMod(double mod) {
        return setMod(modTime, MOD_LIGNITE, mod);
    }

    public ModuleBurnTime setCokeTimeMod(double mod) {
        return setMod(modTime, MOD_COKE, mod);
    }

    public ModuleBurnTime setSolidTimeMod(double mod) {
        return setMod(modTime, MOD_SOLID, mod);
    }

    public ModuleBurnTime setRocketTimeMod(double mod) {
        return setMod(modTime, MOD_ROCKET, mod);
    }

    public ModuleBurnTime setBalefireTimeMod(double mod) {
        return setMod(modTime, MOD_BALEFIRE, mod);
    }

    public ModuleBurnTime setLogHeatMod(double mod) {
        return setMod(modHeat, MOD_LOG, mod);
    }

    public ModuleBurnTime setWoodHeatMod(double mod) {
        return setMod(modHeat, MOD_WOOD, mod);
    }

    public ModuleBurnTime setCoalHeatMod(double mod) {
        return setMod(modHeat, MOD_COAL, mod);
    }

    public ModuleBurnTime setLigniteHeatMod(double mod) {
        return setMod(modHeat, MOD_LIGNITE, mod);
    }

    public ModuleBurnTime setCokeHeatMod(double mod) {
        return setMod(modHeat, MOD_COKE, mod);
    }

    public ModuleBurnTime setSolidHeatMod(double mod) {
        return setMod(modHeat, MOD_SOLID, mod);
    }

    public ModuleBurnTime setRocketHeatMod(double mod) {
        return setMod(modHeat, MOD_ROCKET, mod);
    }

    public ModuleBurnTime setBalefireHeatMod(double mod) {
        return setMod(modHeat, MOD_BALEFIRE, mod);
    }
}
