// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record Stratum3DConfig(
        Identifier domainX,
        Identifier domainY,
        Identifier domainZ,
        BlockState state,
        RuleTest replaceable,
        double scaleH,
        double scaleV,
        double threshold)
        implements FeatureConfiguration {

    public static final Codec<Stratum3DConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            Identifier.CODEC
                                                    .fieldOf("domain_x")
                                                    .forGetter(Stratum3DConfig::domainX),
                                            Identifier.CODEC
                                                    .fieldOf("domain_y")
                                                    .forGetter(Stratum3DConfig::domainY),
                                            Identifier.CODEC
                                                    .fieldOf("domain_z")
                                                    .forGetter(Stratum3DConfig::domainZ),
                                            BlockState.CODEC
                                                    .fieldOf("state")
                                                    .forGetter(Stratum3DConfig::state),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(Stratum3DConfig::replaceable),
                                            Codec.DOUBLE
                                                    .fieldOf("scale_h")
                                                    .forGetter(Stratum3DConfig::scaleH),
                                            Codec.DOUBLE
                                                    .fieldOf("scale_v")
                                                    .forGetter(Stratum3DConfig::scaleV),
                                            Codec.DOUBLE
                                                    .fieldOf("threshold")
                                                    .forGetter(Stratum3DConfig::threshold))
                                    .apply(inst, Stratum3DConfig::new));

    public static void validateRegistry(
            Registry<?> registry, Map<ResourceKey<?>, Exception> errors) {
        Map<Identifier, Sampling> domains = new HashMap<>();
        for (var entry : registry.entrySet()) {
            if (entry.getValue() instanceof ConfiguredFeature<?, ?> feature
                    && feature.config() instanceof Stratum3DConfig config) {
                claim(
                        domains,
                        errors,
                        entry.getKey(),
                        config.domainX,
                        "domain_x",
                        config.scaleV,
                        config.scaleH);
                claim(
                        domains,
                        errors,
                        entry.getKey(),
                        config.domainZ,
                        "domain_z",
                        config.scaleH,
                        config.scaleV);
            }
        }
    }

    private static void claim(
            Map<Identifier, Sampling> domains,
            Map<ResourceKey<?>, Exception> errors,
            ResourceKey<?> feature,
            Identifier domain,
            String axis,
            double along,
            double across) {
        Sampling previous = domains.putIfAbsent(domain, new Sampling(feature, axis, along, across));
        if (previous != null
                && (!previous.axis.equals(axis)
                        || previous.along != along
                        || previous.across != across)) {
            errors.putIfAbsent(
                    feature,
                    new IllegalArgumentException(
                            "stratum noise domain "
                                    + domain
                                    + " is sampled by "
                                    + previous.feature.identifier()
                                    + " "
                                    + previous.axis
                                    + " at "
                                    + previous.along
                                    + " x "
                                    + previous.across
                                    + " and "
                                    + feature.identifier()
                                    + " "
                                    + axis
                                    + " at "
                                    + along
                                    + " x "
                                    + across
                                    + "; each domain must use one axis and scale pair"));
        }
    }

    private record Sampling(ResourceKey<?> feature, String axis, double along, double across) {}
}
