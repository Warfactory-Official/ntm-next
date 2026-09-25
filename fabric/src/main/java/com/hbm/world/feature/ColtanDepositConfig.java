// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public record ColtanDepositConfig(
        OreConfiguration vein,
        int outerAttempts,
        int rings,
        double spread,
        int baseRange,
        int minY,
        int yRange)
        implements FeatureConfiguration {

    public static final Codec<ColtanDepositConfig> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            OreConfiguration.CODEC
                                                    .fieldOf("vein")
                                                    .forGetter(ColtanDepositConfig::vein),
                                            Codec.INT
                                                    .fieldOf("outer_attempts")
                                                    .forGetter(ColtanDepositConfig::outerAttempts),
                                            Codec.INT
                                                    .fieldOf("rings")
                                                    .forGetter(ColtanDepositConfig::rings),
                                            Codec.DOUBLE
                                                    .fieldOf("spread")
                                                    .forGetter(ColtanDepositConfig::spread),
                                            Codec.INT
                                                    .fieldOf("base_range")
                                                    .forGetter(ColtanDepositConfig::baseRange),
                                            Codec.INT
                                                    .fieldOf("min_y")
                                                    .forGetter(ColtanDepositConfig::minY),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("y_range")
                                                    .forGetter(ColtanDepositConfig::yRange))
                                    .apply(inst, ColtanDepositConfig::new));
}
