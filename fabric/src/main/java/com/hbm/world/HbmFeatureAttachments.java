// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.data.DataGroups;
import com.hbm.data.WorldData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import static net.minecraft.world.level.levelgen.GenerationStep.Decoration.*;

public final class HbmFeatureAttachments {

    private static final Group OVERWORLD_ORES =
            new Group(BiomeTarget.tags(BiomeTags.IS_OVERWORLD), List.of(WorldData.OVERWORLD_ORES));

    private static final Group NETHER_ORES =
            new Group(BiomeTarget.tags(BiomeTags.IS_NETHER), List.of(WorldData.NETHER_ORES));

    private static final Group END_ORES =
            new Group(BiomeTarget.tags(BiomeTags.IS_END), List.of(WorldData.END_ORES));

    private static final Group OVERWORLD_FEATURES =
            new Group(BiomeTarget.tags(BiomeTags.IS_OVERWORLD), List.of());

    private static final Group NETHER_FEATURES =
            new Group(BiomeTarget.tags(BiomeTags.IS_NETHER), List.of());
    public static final List<Attachment> ALL =
            List.of(
                    att(HbmPlacedFeatures.ORE_URANIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_THORIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_TITANIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_SULFUR, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_ALUMINIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_FLUORITE, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_NITER, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_TUNGSTEN, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_LEAD, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_BERYLLIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_RARE, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_LIGNITE, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_ASBESTOS, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_CINNABAR, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_COBALT, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(
                            HbmPlacedFeatures.ORE_COLTAN,
                            UNDERGROUND_ORES,
                            OVERWORLD_ORES,
                            WorldData.COLTAN_SPAWN),
                    att(HbmPlacedFeatures.ORE_GNEISS_IRON, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_GOLD, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_COPPER, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_ASBESTOS, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_LITHIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_URANIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_RARE, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_GNEISS_GAS, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_LIMESTONE, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.CLUSTER_IRON, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.CLUSTER_TITANIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.CLUSTER_ALUMINIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.CLUSTER_COPPER, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_IRON, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_TITANIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_TUNGSTEN, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_CINNABAR, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_ZIRCONIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_BORAX, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.ORE_AUSTRALIUM, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.DEPOSIT_COLTAN, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.BEDROCK_ORE_OVERWORLD, UNDERGROUND_ORES, OVERWORLD_ORES),
                    att(HbmPlacedFeatures.GAS_FLAMMABLE, UNDERGROUND_ORES, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.ORE_ALEXANDRITE, UNDERGROUND_ORES, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.STRATUM_GNEISS, LOCAL_MODIFICATIONS, OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.STRATUM_HEMATITE,
                            LOCAL_MODIFICATIONS,
                            OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.STRATUM_BAUXITE, LOCAL_MODIFICATIONS, OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.STRATUM_MALACHITE,
                            LOCAL_MODIFICATIONS,
                            OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.ORE_CAVE_SULFUR,
                            UNDERGROUND_DECORATION,
                            OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.ORE_CAVE_ASBESTOS,
                            UNDERGROUND_DECORATION,
                            OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.OIL_BUBBLE, UNDERGROUND_ORES, OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.BEDROCK_OIL_DEPOSIT,
                            UNDERGROUND_ORES,
                            OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.KEYHOLE, UNDERGROUND_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.METEORITE, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.GLYPHID_HIVE, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.DUD, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.LOCKED_SAFE, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.LANTERN_BEHEMOTH, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.PINK_LOG, TOP_LAYER_MODIFICATION, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.LIBRARY_DUNGEON, UNDERGROUND_ORES, OVERWORLD_FEATURES),
                    att(HbmPlacedFeatures.ARCTIC_VAULT, UNDERGROUND_ORES, OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.FOXGLOVE,
                            VEGETAL_DECORATION,
                            HbmWorldgen.FOXGLOVE_FORESTS),
                    att(
                            HbmPlacedFeatures.NIGHTSHADE,
                            VEGETAL_DECORATION,
                            HbmWorldgen.NIGHTSHADE_FORESTS),
                    att(HbmPlacedFeatures.TOBACCO, VEGETAL_DECORATION, HbmWorldgen.TOBACCO_JUNGLES),
                    att(HbmPlacedFeatures.WEED, VEGETAL_DECORATION, OVERWORLD_FEATURES),
                    att(
                            HbmPlacedFeatures.REEDS_RIVER,
                            VEGETAL_DECORATION,
                            HbmWorldgen.REEDS_RIVERS),
                    att(
                            HbmPlacedFeatures.REEDS_BEACH,
                            VEGETAL_DECORATION,
                            HbmWorldgen.REEDS_BEACHES),
                    att(
                            HbmPlacedFeatures.JUNGLE_DUNGEON_MARKER,
                            TOP_LAYER_MODIFICATION,
                            HbmWorldgen.JUNGLE_DUNGEONS),
                    att(HbmPlacedFeatures.GEYSER, VEGETAL_DECORATION, HbmWorldgen.GEYSER_CHLORINE),
                    att(HbmPlacedFeatures.CAPSULE, VEGETAL_DECORATION, HbmWorldgen.CAPSULE_BEACH),
                    att(HbmPlacedFeatures.BARREL, VEGETAL_DECORATION, HbmWorldgen.DESERT_OIL),
                    att(HbmPlacedFeatures.ORE_NETHER_URANIUM, UNDERGROUND_ORES, NETHER_ORES),
                    att(HbmPlacedFeatures.ORE_NETHER_TUNGSTEN, UNDERGROUND_ORES, NETHER_ORES),
                    att(HbmPlacedFeatures.ORE_NETHER_SULFUR, UNDERGROUND_ORES, NETHER_ORES),
                    att(HbmPlacedFeatures.ORE_NETHER_COBALT, UNDERGROUND_ORES, NETHER_ORES),
                    att(
                            HbmPlacedFeatures.ORE_NETHER_PLUTONIUM,
                            UNDERGROUND_ORES,
                            NETHER_ORES,
                            WorldData.NETHER_PLUTONIUM),
                    att(HbmPlacedFeatures.ORE_NETHER_FIRE, UNDERGROUND_ORES, NETHER_ORES),
                    att(HbmPlacedFeatures.ORE_NETHER_COAL, UNDERGROUND_ORES, NETHER_ORES),
                    att(
                            HbmPlacedFeatures.DEPOSIT_NETHER_NEODYMIUM_FLOOR,
                            UNDERGROUND_ORES,
                            NETHER_ORES),
                    att(
                            HbmPlacedFeatures.DEPOSIT_NETHER_NEODYMIUM_ROOF,
                            UNDERGROUND_ORES,
                            NETHER_ORES),
                    att(HbmPlacedFeatures.BEDROCK_ORE_NETHER, UNDERGROUND_ORES, NETHER_ORES),
                    att(HbmPlacedFeatures.ORE_NETHER_SMOLDERING, UNDERGROUND_ORES, NETHER_FEATURES),
                    att(HbmPlacedFeatures.GEYSIR_NETHER, UNDERGROUND_ORES, NETHER_FEATURES),
                    att(HbmPlacedFeatures.ORE_END_TIKITE, UNDERGROUND_ORES, END_ORES));

    private HbmFeatureAttachments() {}

    public static List<DataGroups.Param<Boolean>> switches(ResourceKey<PlacedFeature> feature) {
        for (Attachment attachment : ALL) {
            if (attachment.feature().equals(feature)) return attachment.switches();
        }
        return List.of();
    }

    private static Attachment att(
            ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step, Group group) {
        return new Attachment(feature, step, group.biomes(), group.switches());
    }

    private static Attachment att(
            ResourceKey<PlacedFeature> feature,
            GenerationStep.Decoration step,
            Group group,
            DataGroups.Param<Boolean> gate) {
        List<DataGroups.Param<Boolean>> switches = new ArrayList<>(group.switches());
        switches.add(gate);
        return new Attachment(feature, step, group.biomes(), List.copyOf(switches));
    }

    private static Attachment att(
            ResourceKey<PlacedFeature> feature,
            GenerationStep.Decoration step,
            BiomeTarget biomes) {
        return new Attachment(feature, step, biomes, List.of());
    }

    private record Group(BiomeTarget biomes, List<DataGroups.Param<Boolean>> switches) {}

    public record Attachment(
            ResourceKey<PlacedFeature> feature,
            GenerationStep.Decoration step,
            BiomeTarget biomes,
            List<DataGroups.Param<Boolean>> switches) {}
}
