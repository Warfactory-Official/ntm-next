// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
