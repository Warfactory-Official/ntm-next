// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.lib.Library;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class HbmPlacedFeatures {

    public static final ResourceKey<PlacedFeature> ORE_URANIUM = key("ore_uranium");
    public static final ResourceKey<PlacedFeature> ORE_THORIUM = key("ore_thorium");
    public static final ResourceKey<PlacedFeature> ORE_TITANIUM = key("ore_titanium");
    public static final ResourceKey<PlacedFeature> ORE_SULFUR = key("ore_sulfur");
    public static final ResourceKey<PlacedFeature> ORE_ALUMINIUM = key("ore_aluminium");
    public static final ResourceKey<PlacedFeature> ORE_FLUORITE = key("ore_fluorite");
    public static final ResourceKey<PlacedFeature> ORE_NITER = key("ore_niter");
    public static final ResourceKey<PlacedFeature> ORE_TUNGSTEN = key("ore_tungsten");
    public static final ResourceKey<PlacedFeature> ORE_LEAD = key("ore_lead");
    public static final ResourceKey<PlacedFeature> ORE_BERYLLIUM = key("ore_beryllium");
    public static final ResourceKey<PlacedFeature> ORE_RARE = key("ore_rare");
    public static final ResourceKey<PlacedFeature> ORE_LIGNITE = key("ore_lignite");
    public static final ResourceKey<PlacedFeature> ORE_ASBESTOS = key("ore_asbestos");
    public static final ResourceKey<PlacedFeature> ORE_CINNABAR = key("ore_cinnabar");
    public static final ResourceKey<PlacedFeature> ORE_COBALT = key("ore_cobalt");
    public static final ResourceKey<PlacedFeature> ORE_COLTAN = key("ore_coltan");

    public static final ResourceKey<PlacedFeature> ORE_NETHER_URANIUM = key("ore_nether_uranium");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_TUNGSTEN = key("ore_nether_tungsten");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_SULFUR = key("ore_nether_sulfur");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_COBALT = key("ore_nether_cobalt");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_PLUTONIUM =
            key("ore_nether_plutonium");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_FIRE = key("ore_nether_fire");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_COAL = key("ore_nether_coal");
    public static final ResourceKey<PlacedFeature> ORE_END_TIKITE = key("ore_end_tikite");

    public static final ResourceKey<PlacedFeature> STRATUM_GNEISS = key("stratum_gneiss");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_IRON = key("ore_gneiss_iron");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_GOLD = key("ore_gneiss_gold");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_COPPER = key("ore_gneiss_copper");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_ASBESTOS = key("ore_gneiss_asbestos");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_LITHIUM = key("ore_gneiss_lithium");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_URANIUM = key("ore_gneiss_uranium");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_RARE = key("ore_gneiss_rare");
    public static final ResourceKey<PlacedFeature> ORE_GNEISS_GAS = key("ore_gneiss_gas");

    public static final ResourceKey<PlacedFeature> STRATUM_HEMATITE = key("stratum_hematite");
    public static final ResourceKey<PlacedFeature> STRATUM_BAUXITE = key("stratum_bauxite");
    public static final ResourceKey<PlacedFeature> STRATUM_MALACHITE = key("stratum_malachite");
    public static final ResourceKey<PlacedFeature> ORE_LIMESTONE = key("ore_limestone");

    public static final ResourceKey<PlacedFeature> CLUSTER_IRON = key("cluster_iron");
    public static final ResourceKey<PlacedFeature> CLUSTER_TITANIUM = key("cluster_titanium");
    public static final ResourceKey<PlacedFeature> CLUSTER_ALUMINIUM = key("cluster_aluminium");
    public static final ResourceKey<PlacedFeature> CLUSTER_COPPER = key("cluster_copper");
    public static final ResourceKey<PlacedFeature> ORE_ALEXANDRITE = key("ore_alexandrite");

    public static final ResourceKey<PlacedFeature> DEPOSIT_IRON = key("deposit_iron");
    public static final ResourceKey<PlacedFeature> DEPOSIT_TITANIUM = key("deposit_titanium");
    public static final ResourceKey<PlacedFeature> DEPOSIT_TUNGSTEN = key("deposit_tungsten");
    public static final ResourceKey<PlacedFeature> DEPOSIT_CINNABAR = key("deposit_cinnabar");
    public static final ResourceKey<PlacedFeature> DEPOSIT_ZIRCONIUM = key("deposit_zirconium");
    public static final ResourceKey<PlacedFeature> DEPOSIT_BORAX = key("deposit_borax");

    public static final ResourceKey<PlacedFeature> DEPOSIT_NETHER_NEODYMIUM_FLOOR =
            key("deposit_nether_neodymium_floor");
    public static final ResourceKey<PlacedFeature> DEPOSIT_NETHER_NEODYMIUM_ROOF =
            key("deposit_nether_neodymium_roof");

    public static final ResourceKey<PlacedFeature> ORE_AUSTRALIUM = key("ore_australium");
    public static final ResourceKey<PlacedFeature> DEPOSIT_COLTAN = key("deposit_coltan");
    public static final ResourceKey<PlacedFeature> GAS_FLAMMABLE = key("gas_flammable");
    public static final ResourceKey<PlacedFeature> ORE_NETHER_SMOLDERING =
            key("ore_nether_smoldering");
    public static final ResourceKey<PlacedFeature> BEDROCK_ORE_OVERWORLD =
            key("bedrock_ore_overworld");
    public static final ResourceKey<PlacedFeature> BEDROCK_ORE_NETHER = key("bedrock_ore_nether");
    public static final ResourceKey<PlacedFeature> GEYSIR_NETHER = key("geysir_nether");
    public static final ResourceKey<PlacedFeature> ORE_CAVE_SULFUR = key("ore_cave_sulfur");
    public static final ResourceKey<PlacedFeature> ORE_CAVE_ASBESTOS = key("ore_cave_asbestos");
    public static final ResourceKey<PlacedFeature> OIL_BUBBLE = key("oil_bubble");
    public static final ResourceKey<PlacedFeature> BEDROCK_OIL_DEPOSIT = key("bedrock_oil_deposit");

    public static final ResourceKey<PlacedFeature> METEORITE = key("meteorite");
    public static final ResourceKey<PlacedFeature> GEYSER = key("geyser");
    public static final ResourceKey<PlacedFeature> BIOME_CAVE = key("biome_cave");
    public static final ResourceKey<PlacedFeature> GLYPHID_HIVE = key("glyphid_hive");
    public static final ResourceKey<PlacedFeature> DUD = key("dud");
    public static final ResourceKey<PlacedFeature> LOCKED_SAFE = key("locked_safe");
    public static final ResourceKey<PlacedFeature> LANTERN_BEHEMOTH = key("lantern_behemoth");
    public static final ResourceKey<PlacedFeature> PINK_LOG = key("pink_log");
    public static final ResourceKey<PlacedFeature> LIBRARY_DUNGEON = key("library_dungeon");
    public static final ResourceKey<PlacedFeature> BARREL = key("barrel");
    public static final ResourceKey<PlacedFeature> ARCTIC_VAULT = key("arctic_vault");
    public static final ResourceKey<PlacedFeature> FOXGLOVE = key("foxglove");
    public static final ResourceKey<PlacedFeature> NIGHTSHADE = key("nightshade");
    public static final ResourceKey<PlacedFeature> TOBACCO = key("tobacco");
    public static final ResourceKey<PlacedFeature> WEED = key("weed");
    public static final ResourceKey<PlacedFeature> REEDS_RIVER = key("reeds_river");
    public static final ResourceKey<PlacedFeature> REEDS_BEACH = key("reeds_beach");
    public static final ResourceKey<PlacedFeature> JUNGLE_DUNGEON_MARKER =
            key("jungle_dungeon_marker");
    public static final ResourceKey<PlacedFeature> CAPSULE = key("capsule");
    public static final ResourceKey<PlacedFeature> KEYHOLE = key("keyhole");

    private HbmPlacedFeatures() {}

    private static ResourceKey<PlacedFeature> key(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Library.id(path));
    }
}
