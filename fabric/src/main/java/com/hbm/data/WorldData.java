// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.data;

import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

public final class WorldData {

    public static final ResourceKey<Registry<DataGroup>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("world_config"));

    private static final List<DataGroups.Group> GROUPS = new ArrayList<>();

    private static final DataGroups.Group METEORS = DataGroups.group(GROUPS, "meteors");
    public static final DataGroups.Param<Boolean> ENABLE_METEOR_STRIKES =
            METEORS.bool("enable_strikes", true);
    public static final DataGroups.Param<Boolean> ENABLE_METEOR_SHOWERS =
            METEORS.bool("enable_showers", true);

    public static final DataGroups.Param<Boolean> ENABLE_METEOR_TAILS =
            METEORS.bool("enable_tails", true);
    public static final DataGroups.Param<Boolean> ENABLE_SPECIAL_METEORS =
            METEORS.bool("enable_special", true);
    public static final DataGroups.Param<Integer> METEOR_STRIKE_CHANCE =
            METEORS.bounded("strike_chance", 20 * 60 * 60 * 5, 1, Integer.MAX_VALUE / 100);
    public static final DataGroups.Param<Integer> METEOR_SHOWER_CHANCE =
            METEORS.positive("shower_chance", 20 * 60 * 15);
    public static final DataGroups.Param<Integer> METEOR_SHOWER_DURATION =
            METEORS.integer("shower_duration", 20 * 60 * 30);

    private static final DataGroups.Group WORLD = DataGroups.group(GROUPS, "world");
    public static final DataGroups.Param<Boolean> ENABLE_MYCELIUM =
            WORLD.bool("enable_mycelium_spread", false);
    public static final DataGroups.Param<Boolean> ENABLE_VIRUS =
            WORLD.bool("enable_virus_spread", false);
    public static final DataGroups.Param<Boolean> ENABLE_IMPACT_ATMOSPHERE =
            WORLD.bool("enable_impact_atmosphere", true);

    private static final DataGroups.Group LOOT = DataGroups.group(GROUPS, "loot");
    public static final DataGroups.Param<Double> LOOT_AMOUNT_FACTOR =
            LOOT.bounded("amount_factor", 1D, 0D, 1000D);

    private static final DataGroups.Group WORLDGEN = DataGroups.group(GROUPS, "worldgen");
    public static final DataGroups.Param<Boolean> OVERWORLD_ORES =
            WORLDGEN.bool("overworld_ores", true);
    public static final DataGroups.Param<Boolean> NETHER_ORES = WORLDGEN.bool("nether_ores", true);
    public static final DataGroups.Param<Boolean> END_ORES = WORLDGEN.bool("end_ores", true);
    public static final DataGroups.Param<Boolean> COLTAN_SPAWN =
            WORLDGEN.bool("coltan_spawn", false);
    public static final DataGroups.Param<Boolean> NETHER_PLUTONIUM =
            WORLDGEN.bool("nether_plutonium", false);

    public static final Codec<DataGroups.Param<Boolean>> WORLDGEN_SWITCH =
            Codec.STRING.comapFlatMap(
                    key ->
                            Stream.of(
                                            OVERWORLD_ORES,
                                            NETHER_ORES,
                                            END_ORES,
                                            COLTAN_SPAWN,
                                            NETHER_PLUTONIUM)
                                    .filter(param -> param.key().equals(key))
                                    .findFirst()
                                    .map(DataResult::success)
                                    .orElseGet(
                                            () ->
                                                    DataResult.error(
                                                            () -> "no worldgen switch " + key)),
                    DataGroups.Param::key);

    private WorldData() {}

    public static List<DataGroups.Group> groups() {
        return List.copyOf(GROUPS);
    }

    public static void applyDataPack(RegistryAccess registries) {
        DataGroups.apply(registries, REGISTRY, GROUPS);
    }
}
