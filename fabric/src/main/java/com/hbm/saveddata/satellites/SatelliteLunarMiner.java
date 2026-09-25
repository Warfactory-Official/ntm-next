// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.saveddata.satellites;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SatelliteLunarMiner extends SatelliteMiner {

    public static final MapCodec<SatelliteLunarMiner> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .optionalFieldOf("progress", 0D)
                                                    .forGetter(sat -> sat.progress))
                                    .apply(i, SatelliteLunarMiner::new));

    public SatelliteLunarMiner() {
        this(0D);
    }

    private SatelliteLunarMiner(double progress) {
        super(progress);
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.LUNAR_MINER;
    }
}
