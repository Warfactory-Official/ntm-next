// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
