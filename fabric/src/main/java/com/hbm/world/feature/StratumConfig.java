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
