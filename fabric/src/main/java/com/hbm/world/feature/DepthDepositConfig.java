// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record DepthDepositConfig(
        BlockState core, BlockState filler, RuleTest replaceable, int size, float fill)
        implements FeatureConfiguration {

    public static final Codec<DepthDepositConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("core")
                                                    .forGetter(DepthDepositConfig::core),
                                            BlockState.CODEC
                                                    .fieldOf("filler")
                                                    .forGetter(DepthDepositConfig::filler),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(DepthDepositConfig::replaceable),
                                            Codec.INT
                                                    .fieldOf("size")
                                                    .forGetter(DepthDepositConfig::size),
                                            Codec.FLOAT
                                                    .fieldOf("fill")
                                                    .forGetter(DepthDepositConfig::fill))
                                    .apply(inst, DepthDepositConfig::new));
}
