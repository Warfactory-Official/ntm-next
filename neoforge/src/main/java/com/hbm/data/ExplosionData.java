// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class ExplosionData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("explosion_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group EXPLOSIONS = DataGroups.group(GROUPS, "explosions");
    public static final DataGroups.Param<Integer> GADGET_RADIUS =
            EXPLOSIONS.positive("gadget_radius", 150);
    public static final DataGroups.Param<Integer> BOY_RADIUS =
            EXPLOSIONS.positive("boy_radius", 120);
    public static final DataGroups.Param<Integer> MAN_RADIUS =
            EXPLOSIONS.positive("man_radius", 175);
    public static final DataGroups.Param<Integer> MIKE_RADIUS =
            EXPLOSIONS.positive("mike_radius", 250);
    public static final DataGroups.Param<Integer> TSAR_RADIUS =
            EXPLOSIONS.positive("tsar_radius", 500);
    public static final DataGroups.Param<Integer> PROTOTYPE_RADIUS =
            EXPLOSIONS.positive("prototype_radius", 150);
    public static final DataGroups.Param<Integer> FLEIJA_RADIUS =
            EXPLOSIONS.positive("fleija_radius", 50);
    public static final DataGroups.Param<Integer> MISSILE_RADIUS =
            EXPLOSIONS.positive("missile_radius", 100);
    public static final DataGroups.Param<Integer> SOLINIUM_RADIUS =
            EXPLOSIONS.positive("solinium_radius", 150);
    public static final DataGroups.Param<Integer> N2_RADIUS = EXPLOSIONS.positive("n2_radius", 200);

    public static final DataGroups.Param<Integer> FATMAN_RADIUS =
            EXPLOSIONS.positive("fatman_radius", 35);

    public static final DataGroups.Param<Integer> A_SCHRAB_RADIUS =
            EXPLOSIONS.positive("a_schrab_radius", 20);
    public static final DataGroups.Param<Integer> FALLOUT_RANGE =
            EXPLOSIONS.integer("fallout_range", 100);
    public static final DataGroups.Param<Boolean> CRATER_BIOMES =
            EXPLOSIONS.bool("crater_biomes", true);
    public static final DataGroups.Param<Boolean> DISABLE_NUCLEAR =
            EXPLOSIONS.bool("disable_nuclear", false);
    public static final DataGroups.Param<Double> CRATER_BIOME_RAD =
            EXPLOSIONS.real("crater_biome_rad", 5D);
    public static final DataGroups.Param<Double> CRATER_BIOME_INNER_RAD =
            EXPLOSIONS.real("crater_biome_inner_rad", 25D);
    public static final DataGroups.Param<Double> CRATER_BIOME_OUTER_RAD =
            EXPLOSIONS.real("crater_biome_outer_rad", 0.5D);
    public static final DataGroups.Param<Double> CRATER_BIOME_WATER_MULT =
            EXPLOSIONS.real("crater_biome_water_mult", 5D);
    public static final DataGroups.Param<Integer> SCHRAB_ORE_RATE =
            EXPLOSIONS.positive("schrab_ore_rate", 20);

    private static final DataGroups.Group MINES = DataGroups.group(GROUPS, "mines");
    public static final DataGroups.Param<Double> MINE_AP_DAMAGE = MINES.real("ap_damage", 10D);
    public static final DataGroups.Param<Double> MINE_HE_DAMAGE = MINES.real("he_damage", 35D);
    public static final DataGroups.Param<Double> MINE_SHRAP_DAMAGE =
            MINES.real("shrap_damage", 7.5D);
    public static final DataGroups.Param<Double> MINE_NUKE_DAMAGE = MINES.real("nuke_damage", 100D);
    public static final DataGroups.Param<Double> MINE_NAVAL_DAMAGE =
            MINES.real("naval_damage", 60D);

    private ExplosionData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
