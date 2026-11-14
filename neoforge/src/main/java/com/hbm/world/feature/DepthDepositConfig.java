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
