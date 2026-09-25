// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world;

import com.hbm.lib.Library;
import com.hbm.platform.NeoForgeBiomeModifierService.Request;
import com.hbm.platform.NeoForgeBiomeModifierService.SpawnRequest;
import com.hbm.platform.NeoForgeBiomeModifierService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;

public final class HbmBiomeModifiers {

    private HbmBiomeModifiers() {}

    public static void bootstrap(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);

        for (Request req : NeoForgeBiomeModifierService.requests()) {
            List<Holder<PlacedFeature>> features = new ArrayList<>(req.features().size());
            for (ResourceKey<PlacedFeature> key : req.features())
                features.add(placed.getOrThrow(key));
            ctx.register(
                    key("add/" + req.features().getFirst().identifier().getPath()),
                    new BiomeModifiers.AddFeaturesBiomeModifier(
                            holders(biomes, req.biomes()), HolderSet.direct(features), req.step()));
        }

        for (SpawnRequest req : NeoForgeBiomeModifierService.spawns()) {
            EntityType<?> type = req.type().get();
            String path = "spawn/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            ctx.register(
                    key(path),
                    BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                            holders(biomes, req.biomes()),
                            new Weighted<>(
                                    new MobSpawnSettings.SpawnerData(type, req.min(), req.max()),
                                    req.weight())));
            ctx.register(
                    key(path + "_excluded"),
                    new BiomeModifiers.RemoveSpawnsBiomeModifier(
                            biomes.getOrThrow(req.excluded()),
                            HolderSet.direct(type.builtInRegistryHolder())));
        }
    }

    private static HolderSet<Biome> holders(HolderGetter<Biome> biomes, BiomeTarget target) {
        List<HolderSet<Biome>> sets = new ArrayList<>();
        for (TagKey<Biome> tag : target.tags()) sets.add(biomes.getOrThrow(tag));
        if (!target.biomes().isEmpty()) {
            sets.add(
                    HolderSet.direct(
                            target.biomes().stream()
                                    .<Holder<Biome>>map(biomes::getOrThrow)
                                    .toList()));
        }
        return sets.size() == 1 ? sets.getFirst() : new OrHolderSet<>(sets);
    }

    private static ResourceKey<BiomeModifier> key(String path) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Library.id(path));
    }
}
