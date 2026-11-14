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

public record NetherSurfaceConfig(
        BlockState state, int attempts, int minD, int dRange, int scanDown)
        implements FeatureConfiguration {

    public static final Codec<NetherSurfaceConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BlockState.CODEC
                                                    .fieldOf("state")
                                                    .forGetter(NetherSurfaceConfig::state),
                                            Codec.INT
                                                    .fieldOf("attempts")
                                                    .forGetter(NetherSurfaceConfig::attempts),
                                            Codec.INT
                                                    .fieldOf("min_d")
                                                    .forGetter(NetherSurfaceConfig::minD),
                                            Codec.INT
                                                    .fieldOf("d_range")
                                                    .forGetter(NetherSurfaceConfig::dRange),
                                            Codec.INT
                                                    .fieldOf("scan_down")
                                                    .forGetter(NetherSurfaceConfig::scanDown))
                                    .apply(inst, NetherSurfaceConfig::new));
}
