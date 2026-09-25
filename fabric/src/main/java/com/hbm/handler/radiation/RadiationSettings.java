// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.lib.Library;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;

public record RadiationSettings(
        Optional<Double> diffusivity,
        Optional<Double> halfLifeSeconds,
        Optional<Double> fogRad,
        Optional<Double> fogChance,
        Optional<Boolean> worldRadEffects,
        Optional<Double> ambientRad,
        Optional<Boolean> diffusivityTransport) {

    public static final ResourceKey<Registry<RadiationSettings>> REGISTRY =
            ResourceKey.createRegistryKey(Library.id("radiation_settings"));

    public static final RadiationSettings DEFAULT =
            new RadiationSettings(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());

    public static final Codec<RadiationSettings> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.doubleRange(1.0e-6D, Double.MAX_VALUE)
                                                    .optionalFieldOf("diffusivity")
                                                    .forGetter(RadiationSettings::diffusivity),
                                            Codec.doubleRange(1.0e-6D, Double.MAX_VALUE)
                                                    .optionalFieldOf("half_life_seconds")
                                                    .forGetter(RadiationSettings::halfLifeSeconds),
                                            Codec.doubleRange(0.0D, Double.MAX_VALUE)
                                                    .optionalFieldOf("fog_rad")
                                                    .forGetter(RadiationSettings::fogRad),
                                            Codec.doubleRange(1.0D, Double.MAX_VALUE)
                                                    .optionalFieldOf("fog_chance")
                                                    .forGetter(RadiationSettings::fogChance),
                                            Codec.BOOL
                                                    .optionalFieldOf("world_rad_effects")
                                                    .forGetter(RadiationSettings::worldRadEffects),
                                            Codec.doubleRange(0.0D, Double.MAX_VALUE)
                                                    .optionalFieldOf("ambient_rad")
                                                    .forGetter(RadiationSettings::ambientRad),
                                            Codec.BOOL
                                                    .optionalFieldOf("diffusivity_transport")
                                                    .forGetter(
                                                            RadiationSettings
                                                                    ::diffusivityTransport))
                                    .apply(instance, RadiationSettings::new));

    public record Resolved(
            double diffusionDt,
            double uuE,
            double retentionDt,
            long fogProbU64,
            double fogRad,
            boolean worldRadEffects,
            double minBound,
            double ambientRad) {}

    public Resolved resolve() {
        return resolve(RadiationSystemNT.dT);
    }

    public Resolved resolve(double dT) {
        double diffusionDt = diffusivityOrDefault() * dT;
        double chance = fogChanceOrDefault();
        return new Resolved(
                diffusionDt,
                Math.exp(-(diffusionDt / 128.0d)),
                Math.exp(Math.log(0.5) * (dT / halfLifeSecondsOrDefault())),
                (chance > 0.0D && Double.isFinite(chance))
                        ? RadiationSystemNT.probU64(dT / chance)
                        : 0L,
                fogRadOrDefault(),
                worldRadEffectsOrDefault(),
                0.0D - ambientRadOrDefault(),
                ambientRadOrDefault());
    }

    public static RadiationSettings forLevel(ServerLevel level) {
        return level.registryAccess()
                .lookupOrThrow(REGISTRY)
                .getOptional(ResourceKey.create(REGISTRY, level.dimension().identifier()))
                .orElse(DEFAULT);
    }

    public double diffusivityOrDefault() {
        return diffusivity.orElse(RadiationData.RAD_DIFFUSIVITY.get());
    }

    public double halfLifeSecondsOrDefault() {
        return halfLifeSeconds.orElse(RadiationData.RAD_HALF_LIFE_SECONDS.get());
    }

    public double fogRadOrDefault() {
        return fogRad.orElse(RadiationData.FOG_RAD.get());
    }

    public double fogChanceOrDefault() {
        return fogChance.orElse(RadiationData.FOG_CHANCE.get());
    }

    public boolean worldRadEffectsOrDefault() {
        return worldRadEffects.orElse(RadiationData.WORLD_RAD_EFFECTS.get());
    }

    public double ambientRadOrDefault() {
        return ambientRad.orElse(0.0D);
    }
}
