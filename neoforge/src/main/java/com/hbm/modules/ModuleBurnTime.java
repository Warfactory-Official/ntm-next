// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.modules;

import com.hbm.handler.FuelHandler;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import static com.hbm.inventory.OreDictManager.LIGNITE;

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
        if (level == null || stack.isEmpty()) return 0;
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
        Item item = stack.getItem();
        if (item == ModItems.SOLID_FUEL.get()
                || item == ModItems.SOLID_FUEL_PRESTO.get()
                || item == ModItems.SOLID_FUEL_PRESTO_TRIPLET.get()) return mod[MOD_SOLID];
        if (item == ModItems.SOLID_FUEL_BF.get()
                || item == ModItems.SOLID_FUEL_PRESTO_BF.get()
                || item == ModItems.SOLID_FUEL_PRESTO_TRIPLET_BF.get()) return mod[MOD_BALEFIRE];
        if (item == ModItems.ROCKET_FUEL.get()) return mod[MOD_ROCKET];

        if (ModItems.COKE.typeOf(item) != null) return mod[MOD_COKE];

        var briquette = ModItems.BRIQUETTE.typeOf(item);
        if (briquette != null)
            return switch (briquette) {
                case COAL -> mod[MOD_COAL];
                case LIGNITE -> mod[MOD_LIGNITE];
                case WOOD -> mod[MOD_WOOD];
            };

        if (stack.is(Items.COAL) || stack.is(Items.COAL_BLOCK)) return mod[MOD_COAL];
        if (stack.is(LIGNITE.gem()) || stack.is(LIGNITE.dust()) || stack.is(LIGNITE.ore()))
            return mod[MOD_LIGNITE];
        if (stack.is(ItemTags.LOGS)) return mod[MOD_LOG];
        if (stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.WOODEN_TOOL_MATERIALS)) return mod[MOD_WOOD];
        return def;
    }

    public List<String> getDesc() {
        List<String> list = new ArrayList<>();
        list.add(ChatFormatting.GOLD + "Burn time bonuses:");
        for (int i = 0; i < COUNT; i++) addIf(list, NAMES[i], modTime[i]);
        if (list.size() == 1) list.clear();

        List<String> heat = new ArrayList<>();
        heat.add(ChatFormatting.RED + "Burn heat bonuses:");
        for (int i = 0; i < COUNT; i++) addIf(heat, NAMES[i], modHeat[i]);
        if (heat.size() > 1) list.addAll(heat);
        return list;
    }

    public ModuleBurnTime setLogTimeMod(double mod) {
        this.modTime[MOD_LOG] = mod;
        return this;
    }

    public ModuleBurnTime setWoodTimeMod(double mod) {
        this.modTime[MOD_WOOD] = mod;
        return this;
    }

    public ModuleBurnTime setCoalTimeMod(double mod) {
        this.modTime[MOD_COAL] = mod;
        return this;
    }

    public ModuleBurnTime setLigniteTimeMod(double mod) {
        this.modTime[MOD_LIGNITE] = mod;
        return this;
    }

    public ModuleBurnTime setCokeTimeMod(double mod) {
        this.modTime[MOD_COKE] = mod;
        return this;
    }

    public ModuleBurnTime setSolidTimeMod(double mod) {
        this.modTime[MOD_SOLID] = mod;
        return this;
    }

    public ModuleBurnTime setRocketTimeMod(double mod) {
        this.modTime[MOD_ROCKET] = mod;
        return this;
    }

    public ModuleBurnTime setBalefireTimeMod(double mod) {
        this.modTime[MOD_BALEFIRE] = mod;
        return this;
    }

    public ModuleBurnTime setLogHeatMod(double mod) {
        this.modHeat[MOD_LOG] = mod;
        return this;
    }

    public ModuleBurnTime setWoodHeatMod(double mod) {
        this.modHeat[MOD_WOOD] = mod;
        return this;
    }

    public ModuleBurnTime setCoalHeatMod(double mod) {
        this.modHeat[MOD_COAL] = mod;
        return this;
    }

    public ModuleBurnTime setLigniteHeatMod(double mod) {
        this.modHeat[MOD_LIGNITE] = mod;
        return this;
    }

    public ModuleBurnTime setCokeHeatMod(double mod) {
        this.modHeat[MOD_COKE] = mod;
        return this;
    }

    public ModuleBurnTime setSolidHeatMod(double mod) {
        this.modHeat[MOD_SOLID] = mod;
        return this;
    }

    public ModuleBurnTime setRocketHeatMod(double mod) {
        this.modHeat[MOD_ROCKET] = mod;
        return this;
    }

    public ModuleBurnTime setBalefireHeatMod(double mod) {
        this.modHeat[MOD_BALEFIRE] = mod;
        return this;
    }
}
