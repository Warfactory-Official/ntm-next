// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ICFPelletData(int type1, int type2, boolean muon, long depletion) {

    public static final Codec<ICFPelletData> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("type1")
                                                    .forGetter(ICFPelletData::type1),
                                            Codec.INT
                                                    .fieldOf("type2")
                                                    .forGetter(ICFPelletData::type2),
                                            Codec.BOOL
                                                    .fieldOf("muon")
                                                    .forGetter(ICFPelletData::muon),
                                            Codec.LONG
                                                    .fieldOf("depletion")
                                                    .forGetter(ICFPelletData::depletion))
                                    .apply(i, ICFPelletData::new));

    public static final StreamCodec<ByteBuf, ICFPelletData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ICFPelletData::type1,
                    ByteBufCodecs.VAR_INT,
                    ICFPelletData::type2,
                    ByteBufCodecs.BOOL,
                    ICFPelletData::muon,
                    ByteBufCodecs.VAR_LONG,
                    ICFPelletData::depletion,
                    ICFPelletData::new);

    public ICFPelletData withDepletion(long depletion) {
        return new ICFPelletData(type1, type2, muon, depletion);
    }
}
