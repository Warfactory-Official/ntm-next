// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
