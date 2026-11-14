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
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public record WindowedOreConfig(
        OreConfiguration vein,
        int attempts,
        int minY,
        int yRange,
        int windowSize,
        int minimumCenterRadius,
        int maximumCenterRadius)
        implements FeatureConfiguration {

    public static final Codec<WindowedOreConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            OreConfiguration.CODEC
                                                    .fieldOf("vein")
                                                    .forGetter(WindowedOreConfig::vein),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("attempts")
                                                    .forGetter(WindowedOreConfig::attempts),
                                            Codec.INT
                                                    .fieldOf("min_y")
                                                    .forGetter(WindowedOreConfig::minY),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("y_range")
                                                    .forGetter(WindowedOreConfig::yRange),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("window_size")
                                                    .forGetter(WindowedOreConfig::windowSize),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("minimum_center_radius")
                                                    .forGetter(
                                                            WindowedOreConfig::minimumCenterRadius),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("maximum_center_radius")
                                                    .forGetter(
                                                            WindowedOreConfig::maximumCenterRadius))
                                    .apply(inst, WindowedOreConfig::new));

    public WindowedOreConfig {
        if (minimumCenterRadius > maximumCenterRadius) {
            throw new IllegalArgumentException("minimum center radius exceeds maximum");
        }
    }
}
