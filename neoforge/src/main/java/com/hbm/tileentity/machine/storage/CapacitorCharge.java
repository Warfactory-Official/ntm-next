// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CapacitorCharge(long power, long maxPower) {

    public static final Codec<CapacitorCharge> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .fieldOf("power")
                                                    .forGetter(CapacitorCharge::power),
                                            Codec.LONG
                                                    .fieldOf("max_power")
                                                    .forGetter(CapacitorCharge::maxPower))
                                    .apply(i, CapacitorCharge::new));

    public static final StreamCodec<ByteBuf, CapacitorCharge> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    CapacitorCharge::power,
                    ByteBufCodecs.VAR_LONG,
                    CapacitorCharge::maxPower,
                    CapacitorCharge::new);
}
