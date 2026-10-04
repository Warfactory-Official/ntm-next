// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.lib.Library;
import com.hbm.util.DataCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public record BiomeRadiation(HolderSet<Biome> targets, double radPerSecond, double wetMultiplier) {

    public static final ResourceKey<Registry<BiomeRadiation>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("biome_radiation"));

    public static final Codec<BiomeRadiation> CODEC =
            DataCodecs.strict(
                            RecordCodecBuilder.<BiomeRadiation>mapCodec(
                                    instance ->
                                            instance.group(
                                                            RegistryCodecs.homogeneousList(
                                                                            Registries.BIOME)
                                                                    .fieldOf("targets")
                                                                    .forGetter(
                                                                            BiomeRadiation
                                                                                    ::targets),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .fieldOf("rad_per_second")
                                                                    .forGetter(
                                                                            BiomeRadiation
                                                                                    ::radPerSecond),
                                                            Codec.doubleRange(0D, Double.MAX_VALUE)
                                                                    .optionalFieldOf(
                                                                            "wet_multiplier", 1D)
                                                                    .forGetter(
                                                                            BiomeRadiation
                                                                                    ::wetMultiplier))
                                                    .apply(instance, BiomeRadiation::new)))
                    .codec()
                    .validate(
                            rule ->
                                    Double.isFinite(rule.radPerSecond * rule.wetMultiplier)
                                            ? DataResult.success(rule)
                                            : DataResult.error(
                                                    () ->
                                                            "biome radiation wet dose must be finite"));

    private static Reference2DoubleOpenHashMap<Biome> dryDose =
            new Reference2DoubleOpenHashMap<>(0);
    private static Reference2DoubleOpenHashMap<Biome> wetDose =
            new Reference2DoubleOpenHashMap<>(0);

    public static void applyDataPack(RegistryAccess registries) {
        Reference2DoubleOpenHashMap<Biome> dry = new Reference2DoubleOpenHashMap<>();
        Reference2DoubleOpenHashMap<Biome> wet = new Reference2DoubleOpenHashMap<>();
        Map<Biome, Identifier> claims = new HashMap<>();
        for (var holder : registries.lookupOrThrow(REGISTRY).listElements().toList()) {
            BiomeRadiation rule = holder.value();
            Identifier id = holder.key().identifier();
            for (Holder<Biome> target : rule.targets) {
                Biome biome = target.value();
                Identifier prior = claims.putIfAbsent(biome, id);
                if (prior != null) {
                    throw new IllegalStateException(
                            "Duplicate biome radiation target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + id);
                }
                dry.put(biome, rule.radPerSecond);
                wet.put(biome, rule.radPerSecond * rule.wetMultiplier);
            }
        }
        dryDose = dry;
        wetDose = wet;
    }

    public static double dose(Holder<Biome> biome, boolean wet) {
        return (wet ? wetDose : dryDose).getDouble(biome.value());
    }

    public static void clear() {
        dryDose = new Reference2DoubleOpenHashMap<>(0);
        wetDose = new Reference2DoubleOpenHashMap<>(0);
    }
}
