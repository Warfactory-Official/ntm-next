// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record StratumConfig(
        Identifier domain,
        BlockState state,
        RuleTest replaceable,
        double scale,
        int threshold,
        int rangeMult,
        int maxRange,
        int yLevel,
        float density)
        implements FeatureConfiguration {

    public static final Codec<StratumConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            Identifier.CODEC
                                                    .fieldOf("domain")
                                                    .forGetter(StratumConfig::domain),
                                            BlockState.CODEC
                                                    .fieldOf("state")
                                                    .forGetter(StratumConfig::state),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(StratumConfig::replaceable),
                                            Codec.DOUBLE
                                                    .fieldOf("scale")
                                                    .forGetter(StratumConfig::scale),
                                            Codec.INT
                                                    .fieldOf("threshold")
                                                    .forGetter(StratumConfig::threshold),
                                            Codec.INT
                                                    .fieldOf("range_mult")
                                                    .forGetter(StratumConfig::rangeMult),
                                            Codec.INT
                                                    .fieldOf("max_range")
                                                    .forGetter(StratumConfig::maxRange),
                                            Codec.INT
                                                    .fieldOf("y_level")
                                                    .forGetter(StratumConfig::yLevel),
                                            Codec.FLOAT
                                                    .fieldOf("density")
                                                    .forGetter(StratumConfig::density))
                                    .apply(inst, StratumConfig::new));
}
