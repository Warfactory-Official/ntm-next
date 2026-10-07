// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record BedrockOilDepositConfig(
        BlockState core, RuleTest replaceable, List<OreConfiguration.TargetBlockState> surfaceScars)
        implements FeatureConfiguration {

    public static final Codec<BedrockOilDepositConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("core")
                                                    .forGetter(BedrockOilDepositConfig::core),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(
                                                            BedrockOilDepositConfig::replaceable),
                                            OreConfiguration.TargetBlockState.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("surface_scars", List.of())
                                                    .forGetter(
                                                            BedrockOilDepositConfig::surfaceScars))
                                    .apply(inst, BedrockOilDepositConfig::new));

    public BedrockOilDepositConfig(BlockState core, RuleTest replaceable) {
        this(core, replaceable, List.of());
    }
}
