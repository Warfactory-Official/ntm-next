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

package com.hbm.items.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ConveyorRunData(BlockPos anchor, Direction side, int budget) {

    public static final Codec<ConveyorRunData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            BlockPos.CODEC
                                                    .fieldOf("anchor")
                                                    .forGetter(ConveyorRunData::anchor),
                                            Direction.CODEC
                                                    .fieldOf("side")
                                                    .forGetter(ConveyorRunData::side),
                                            Codec.INT
                                                    .fieldOf("budget")
                                                    .forGetter(ConveyorRunData::budget))
                                    .apply(instance, ConveyorRunData::new));

    public static final StreamCodec<ByteBuf, ConveyorRunData> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ConveyorRunData::anchor,
                    Direction.STREAM_CODEC,
                    ConveyorRunData::side,
                    ByteBufCodecs.VAR_INT,
                    ConveyorRunData::budget,
                    ConveyorRunData::new);
}
