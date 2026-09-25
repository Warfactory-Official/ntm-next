// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
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
}
