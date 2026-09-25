// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class MobData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("mob_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group MASKMAN = DataGroups.group(GROUPS, "maskman");
    public static final DataGroups.Param<Boolean> ENABLE_MASKMAN = MASKMAN.bool("enabled", true);
    public static final DataGroups.Param<Integer> MASKMAN_DELAY = MASKMAN.integer("delay", 20 * 60);
    public static final DataGroups.Param<Integer> MASKMAN_MIN_RAD = MASKMAN.integer("min_rad", 50);
    public static final DataGroups.Param<Boolean> MASKMAN_UNDERGROUND =
            MASKMAN.bool("underground", true);

    private static final DataGroups.Group RAIDS = DataGroups.group(GROUPS, "raids");
    public static final DataGroups.Param<Boolean> ENABLE_RAIDS = RAIDS.bool("enabled", false);
    public static final DataGroups.Param<Integer> RAID_DELAY =
            RAIDS.positive("delay", 30 * 60 * 60);
    public static final DataGroups.Param<Integer> RAID_CHANCE = RAIDS.positive("chance", 3);
    public static final DataGroups.Param<Integer> RAID_AMOUNT = RAIDS.integer("amount", 15);
    public static final DataGroups.Param<Integer> RAID_ATTACK_DELAY =
            RAIDS.integer("attack_delay", 40);
    public static final DataGroups.Param<Integer> RAID_ATTACK_REACH =
            RAIDS.integer("attack_reach", 2);
    public static final DataGroups.Param<Integer> RAID_ATTACK_DISTANCE =
            RAIDS.integer("attack_distance", 32);
    public static final DataGroups.Param<Integer> RAID_DRONES = RAIDS.integer("drones", 5);

    private static final DataGroups.Group ELEMENTALS = DataGroups.group(GROUPS, "elementals");
    public static final DataGroups.Param<Boolean> ENABLE_ELEMENTALS =
            ELEMENTALS.bool("enabled", true);
    public static final DataGroups.Param<Integer> ELEMENTAL_DELAY =
            ELEMENTALS.positive("delay", 30 * 60 * 60);
    public static final DataGroups.Param<Integer> ELEMENTAL_CHANCE =
            ELEMENTALS.positive("chance", 2);
    public static final DataGroups.Param<Integer> ELEMENTAL_AMOUNT =
            ELEMENTALS.integer("amount", 10);
    public static final DataGroups.Param<Integer> ELEMENTAL_DISTANCE =
            ELEMENTALS.integer("distance", 32);

    private static final DataGroups.Group GEAR = DataGroups.group(GROUPS, "gear");
    public static final DataGroups.Param<Boolean> ENABLE_MOB_GEAR = GEAR.bool("enabled", true);
    public static final DataGroups.Param<Boolean> ENABLE_MOB_WEAPONS = GEAR.bool("weapons", true);

    public static final DataGroups.Param<Double> WEAPON_SOOT_REDUCTION =
            GEAR.bounded("weapon_soot_reduction", 0D, -Double.MAX_VALUE, Double.MAX_VALUE);

    private static final DataGroups.Group GLYPHIDS = DataGroups.group(GROUPS, "glyphids");
    public static final DataGroups.Param<Boolean> ENABLE_HIVES = GLYPHIDS.bool("enabled", true);
    public static final DataGroups.Param<Integer> HIVE_SPAWN = GLYPHIDS.positive("hive_spawn", 256);
    public static final DataGroups.Param<Double> SCOUT_THRESHOLD =
            GLYPHIDS.real("scout_threshold", 1D);
    public static final DataGroups.Param<Double> SPAWN_MAX = GLYPHIDS.real("spawn_max", 50D);
    public static final DataGroups.Param<Double> TARGETING_THRESHOLD =
            GLYPHIDS.real("targeting_threshold", 1D);
    public static final DataGroups.Param<Integer> SCOUT_SWARM_SPAWN_CHANCE =
            GLYPHIDS.positive("scout_swarm_spawn_chance", 3);
    public static final DataGroups.Param<Integer> LARGE_HIVE_CHANCE =
            GLYPHIDS.positive("large_hive_chance", 5);
    public static final DataGroups.Param<Integer> BASE_SWARM_SIZE =
            GLYPHIDS.integer("base_swarm_size", 5);
    public static final DataGroups.Param<Double> SWARM_SCALING_MULT =
            GLYPHIDS.real("swarm_scaling_mult", 1.2D);
    public static final DataGroups.Param<Integer> SOOT_STEP = GLYPHIDS.positive("soot_step", 50);
    public static final DataGroups.Param<Integer> SWARM_COOLDOWN =
            GLYPHIDS.integer("swarm_cooldown", 120);
    public static final DataGroups.Param<Integer> GLYPHID_CHANCE_BASE =
            GLYPHIDS.signed("glyphid_chance_base", 50);
    public static final DataGroups.Param<Integer> GLYPHID_CHANCE_MOD =
            GLYPHIDS.signed("glyphid_chance_mod", -45);
    public static final DataGroups.Param<Integer> GLYPHID_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("glyphid_chance_min_soot", 0);
    public static final DataGroups.Param<Integer> BRAWLER_CHANCE_BASE =
            GLYPHIDS.signed("brawler_chance_base", 10);
    public static final DataGroups.Param<Integer> BRAWLER_CHANCE_MOD =
            GLYPHIDS.signed("brawler_chance_mod", 30);
    public static final DataGroups.Param<Integer> BRAWLER_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("brawler_chance_min_soot", 1);
    public static final DataGroups.Param<Integer> BOMBARDIER_CHANCE_BASE =
            GLYPHIDS.signed("bombardier_chance_base", 20);
    public static final DataGroups.Param<Integer> BOMBARDIER_CHANCE_MOD =
            GLYPHIDS.signed("bombardier_chance_mod", -15);
    public static final DataGroups.Param<Integer> BOMBARDIER_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("bombardier_chance_min_soot", 1);
    public static final DataGroups.Param<Integer> BLASTER_CHANCE_BASE =
            GLYPHIDS.signed("blaster_chance_base", -5);
    public static final DataGroups.Param<Integer> BLASTER_CHANCE_MOD =
            GLYPHIDS.signed("blaster_chance_mod", 40);
    public static final DataGroups.Param<Integer> BLASTER_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("blaster_chance_min_soot", 5);
    public static final DataGroups.Param<Integer> DIGGER_CHANCE_BASE =
            GLYPHIDS.signed("digger_chance_base", -15);
    public static final DataGroups.Param<Integer> DIGGER_CHANCE_MOD =
            GLYPHIDS.signed("digger_chance_mod", 25);
    public static final DataGroups.Param<Integer> DIGGER_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("digger_chance_min_soot", 5);
    public static final DataGroups.Param<Integer> BEHEMOTH_CHANCE_BASE =
            GLYPHIDS.signed("behemoth_chance_base", -30);
    public static final DataGroups.Param<Integer> BEHEMOTH_CHANCE_MOD =
            GLYPHIDS.signed("behemoth_chance_mod", 45);
    public static final DataGroups.Param<Integer> BEHEMOTH_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("behemoth_chance_min_soot", 10);
    public static final DataGroups.Param<Integer> BRENDA_CHANCE_BASE =
            GLYPHIDS.signed("brenda_chance_base", -50);
    public static final DataGroups.Param<Integer> BRENDA_CHANCE_MOD =
            GLYPHIDS.signed("brenda_chance_mod", 60);
    public static final DataGroups.Param<Integer> BRENDA_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("brenda_chance_min_soot", 20);
    public static final DataGroups.Param<Integer> JOHNSON_CHANCE_BASE =
            GLYPHIDS.signed("johnson_chance_base", -50);
    public static final DataGroups.Param<Integer> JOHNSON_CHANCE_MOD =
            GLYPHIDS.signed("johnson_chance_mod", 60);
    public static final DataGroups.Param<Integer> JOHNSON_CHANCE_MIN_SOOT =
            GLYPHIDS.integer("johnson_chance_min_soot", 50);
    public static final DataGroups.Param<Boolean> RAMPANT_MODE =
            GLYPHIDS.bool("rampant_mode", false);
    public static final DataGroups.Param<Boolean> RAMPANT_NATURAL_SCOUT_SPAWN =
            GLYPHIDS.bool("rampant_natural_scout_spawn", false);
    public static final DataGroups.Param<Double> RAMPANT_SCOUT_SPAWN_THRESH =
            GLYPHIDS.real("rampant_scout_spawn_thresh", 13D);
    public static final DataGroups.Param<Integer> RAMPANT_SCOUT_SPAWN_CHANCE =
            GLYPHIDS.positive("rampant_scout_spawn_chance", 1400);
    public static final DataGroups.Param<Boolean> RAMPANT_EXTENDED_TARGETTING =
            GLYPHIDS.bool("rampant_extended_targetting", false);
    public static final DataGroups.Param<Boolean> RAMPANT_DIG = GLYPHIDS.bool("rampant_dig", false);
    public static final DataGroups.Param<Boolean> RAMPANT_GLYPHID_GUIDANCE =
            GLYPHIDS.bool("rampant_glyphid_guidance", false);
    public static final DataGroups.Param<Double> RAMPANT_SMOKE_STACK_OVERRIDE =
            GLYPHIDS.real("rampant_smoke_stack_override", 0.4D);

    public static double scoutThreshold() {
        return RAMPANT_MODE.get() ? 0.1D : SCOUT_THRESHOLD.get();
    }

    public static int scoutSwarmSpawnChance() {
        return RAMPANT_MODE.get() ? 1 : SCOUT_SWARM_SPAWN_CHANCE.get();
    }

    public static boolean rampantNaturalScoutSpawn() {
        return RAMPANT_MODE.get() || RAMPANT_NATURAL_SCOUT_SPAWN.get();
    }

    public static boolean rampantExtendedTargetting() {
        return RAMPANT_MODE.get() || RAMPANT_EXTENDED_TARGETTING.get();
    }

    public static boolean rampantDig() {
        return RAMPANT_MODE.get() || RAMPANT_DIG.get();
    }

    public static boolean rampantGlyphidGuidance() {
        return RAMPANT_MODE.get() || RAMPANT_GLYPHID_GUIDANCE.get();
    }

    public static int swarmCooldown() {
        return SWARM_COOLDOWN.get() * 20;
    }

    public static int[] glyphidChance() {
        return new int[] {
            GLYPHID_CHANCE_BASE.get(), GLYPHID_CHANCE_MOD.get(), GLYPHID_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] brawlerChance() {
        return new int[] {
            BRAWLER_CHANCE_BASE.get(), BRAWLER_CHANCE_MOD.get(), BRAWLER_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] bombardierChance() {
        int minSoot = BOMBARDIER_CHANCE_MIN_SOOT.get();
        if (RAMPANT_MODE.get() && minSoot == 1) minSoot = 0;
        return new int[] {BOMBARDIER_CHANCE_BASE.get(), BOMBARDIER_CHANCE_MOD.get(), minSoot};
    }

    public static int[] blasterChance() {
        return new int[] {
            BLASTER_CHANCE_BASE.get(), BLASTER_CHANCE_MOD.get(), BLASTER_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] diggerChance() {
        return new int[] {
            DIGGER_CHANCE_BASE.get(), DIGGER_CHANCE_MOD.get(), DIGGER_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] behemothChance() {
        return new int[] {
            BEHEMOTH_CHANCE_BASE.get(), BEHEMOTH_CHANCE_MOD.get(), BEHEMOTH_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] brendaChance() {
        return new int[] {
            BRENDA_CHANCE_BASE.get(), BRENDA_CHANCE_MOD.get(), BRENDA_CHANCE_MIN_SOOT.get()
        };
    }

    public static int[] johnsonChance() {
        return new int[] {
            JOHNSON_CHANCE_BASE.get(), JOHNSON_CHANCE_MOD.get(), JOHNSON_CHANCE_MIN_SOOT.get()
        };
    }

    private MobData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
