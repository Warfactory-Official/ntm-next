// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record OilBubbleConfig(
        BlockState core, RuleTest replaceable, int minSize, int maxSize, boolean fuzzy)
        implements FeatureConfiguration {

    public static final Codec<OilBubbleConfig> CODEC =
            RecordCodecBuilder.<OilBubbleConfig>create(
                            inst ->
                                    inst.group(
                                                    BlockState.CODEC
                                                            .fieldOf("core")
                                                            .forGetter(OilBubbleConfig::core),
                                                    RuleTest.CODEC
                                                            .fieldOf("replaceable")
                                                            .forGetter(
                                                                    OilBubbleConfig::replaceable),
                                                    Codec.intRange(0, Integer.MAX_VALUE)
                                                            .fieldOf("min_size")
                                                            .forGetter(OilBubbleConfig::minSize),
                                                    Codec.intRange(1, Integer.MAX_VALUE)
                                                            .fieldOf("max_size")
                                                            .forGetter(OilBubbleConfig::maxSize),
                                                    Codec.BOOL
                                                            .fieldOf("fuzzy")
                                                            .forGetter(OilBubbleConfig::fuzzy))
                                            .apply(inst, OilBubbleConfig::new))
                    .validate(
                            config ->
                                    config.maxSize() > config.minSize()
                                            ? DataResult.success(config)
                                            : DataResult.error(
                                                    () -> "max_size must exceed min_size"));
}
