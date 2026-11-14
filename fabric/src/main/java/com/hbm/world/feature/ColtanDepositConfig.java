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
