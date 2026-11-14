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
