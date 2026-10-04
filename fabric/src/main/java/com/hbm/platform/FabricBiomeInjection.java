// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.world.BiomeTarget;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FabricBiomeInjection {
    public static @SafeVarargs void addFeaturesToBiome(
            BiomeTarget biomes,
            GenerationStep.Decoration step,
            ResourceKey<PlacedFeature>... placedFeatures) {
        BiomeModifications.create(placedFeatures[0].identifier())
                .add(
                        ModificationPhase.ADDITIONS,
                        select(biomes),
                        context -> {
                            for (ResourceKey<PlacedFeature> feature : placedFeatures) {
                                context.getGenerationSettings().addFeature(step, feature);
                            }
                        });
    }

    public static void addSpawn(
            BiomeTarget biomes,
            TagKey<Biome> excluded,
            Supplier<? extends EntityType<?>> type,
            int weight,
            int min,
            int max) {
        EntityType<?> entity = type.get();
        BiomeModifications.addSpawn(
                select(biomes).and(BiomeSelectors.tag(excluded).negate()),
                entity.getCategory(),
                entity,
                weight,
                min,
                max);
    }

    public static Predicate<BiomeSelectionContext> select(BiomeTarget biomes) {
        Predicate<BiomeSelectionContext> selector = BiomeSelectors.includeByKey(biomes.biomes());
        for (TagKey<Biome> tag : biomes.tags()) selector = selector.or(BiomeSelectors.tag(tag));
        return selector;
    }
}
