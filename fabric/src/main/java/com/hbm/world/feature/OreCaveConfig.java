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
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public record OreCaveConfig(
        BlockState ore,
        Optional<BlockState> fluid,
        Optional<BlockState> stalactite,
        Optional<BlockState> stalagmite,
        RuleTest replaceable,
        double threshold,
        int rangeMult,
        int maxRange,
        int yLevel,
        Identifier domain,
        boolean exposedByFluid)
        implements FeatureConfiguration {

    public static final Codec<OreCaveConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("ore")
                                                    .forGetter(OreCaveConfig::ore),
                                            BlockState.CODEC
                                                    .optionalFieldOf("fluid")
                                                    .forGetter(OreCaveConfig::fluid),
                                            BlockState.CODEC
                                                    .optionalFieldOf("stalactite")
                                                    .forGetter(OreCaveConfig::stalactite),
                                            BlockState.CODEC
                                                    .optionalFieldOf("stalagmite")
                                                    .forGetter(OreCaveConfig::stalagmite),
                                            RuleTest.CODEC
                                                    .fieldOf("replaceable")
                                                    .forGetter(OreCaveConfig::replaceable),
                                            Codec.DOUBLE
                                                    .fieldOf("threshold")
                                                    .forGetter(OreCaveConfig::threshold),
                                            Codec.INT
                                                    .fieldOf("range_mult")
                                                    .forGetter(OreCaveConfig::rangeMult),
                                            Codec.INT
                                                    .fieldOf("max_range")
                                                    .forGetter(OreCaveConfig::maxRange),
                                            Codec.INT
                                                    .fieldOf("y_level")
                                                    .forGetter(OreCaveConfig::yLevel),
                                            Identifier.CODEC
                                                    .fieldOf("domain")
                                                    .forGetter(OreCaveConfig::domain),
                                            Codec.BOOL
                                                    .optionalFieldOf("exposed_by_fluid", false)
                                                    .forGetter(OreCaveConfig::exposedByFluid))
                                    .apply(inst, OreCaveConfig::new));
}
