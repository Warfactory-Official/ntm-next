// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RBMKFuelData(double yield, double xenon, double coreHeat, double hullHeat) {

    public static final Codec<RBMKFuelData> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.DOUBLE
                                                    .fieldOf("yield")
                                                    .forGetter(RBMKFuelData::yield),
                                            Codec.DOUBLE
                                                    .fieldOf("xenon")
                                                    .forGetter(RBMKFuelData::xenon),
                                            Codec.DOUBLE
                                                    .fieldOf("core")
                                                    .forGetter(RBMKFuelData::coreHeat),
                                            Codec.DOUBLE
                                                    .fieldOf("hull")
                                                    .forGetter(RBMKFuelData::hullHeat))
                                    .apply(i, RBMKFuelData::new));

    public static final StreamCodec<ByteBuf, RBMKFuelData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    RBMKFuelData::yield,
                    ByteBufCodecs.DOUBLE,
                    RBMKFuelData::xenon,
                    ByteBufCodecs.DOUBLE,
                    RBMKFuelData::coreHeat,
                    ByteBufCodecs.DOUBLE,
                    RBMKFuelData::hullHeat,
                    RBMKFuelData::new);

    public static RBMKFuelData fresh(double yield) {
        return new RBMKFuelData(yield, 0, 20D, 20D);
    }

    public RBMKFuelData withYield(double y) {
        return new RBMKFuelData(y, xenon, coreHeat, hullHeat);
    }

    public RBMKFuelData withXenon(double x) {
        return new RBMKFuelData(yield, x, coreHeat, hullHeat);
    }

    public RBMKFuelData withCoreHeat(double c) {
        return new RBMKFuelData(yield, xenon, c, hullHeat);
    }

    public RBMKFuelData withHullHeat(double h) {
        return new RBMKFuelData(yield, xenon, coreHeat, h);
    }
}
