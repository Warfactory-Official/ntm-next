/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
