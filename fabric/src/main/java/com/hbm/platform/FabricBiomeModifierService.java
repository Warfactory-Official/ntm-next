// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.platform.services.IBiomeModifierService;
import com.hbm.world.BiomeTarget;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FabricBiomeModifierService implements IBiomeModifierService {

    @SafeVarargs
    @Override
    public final void addFeaturesToBiome(
            BiomeTarget biomes,
            GenerationStep.Decoration step,
            ResourceKey<PlacedFeature>... placedFeatures) {
        for (ResourceKey<PlacedFeature> feature : placedFeatures) {
            BiomeModifications.addFeature(select(biomes), step, feature);
        }
    }

    @Override
    public void addSpawn(
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

    private static Predicate<BiomeSelectionContext> select(BiomeTarget biomes) {
        Predicate<BiomeSelectionContext> selector = BiomeSelectors.includeByKey(biomes.biomes());
        for (TagKey<Biome> tag : biomes.tags()) selector = selector.or(BiomeSelectors.tag(tag));
        return selector;
    }
}
