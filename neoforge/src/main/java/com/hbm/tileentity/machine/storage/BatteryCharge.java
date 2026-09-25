// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BatteryCharge(long power, long prevPower, int redLow, int redHigh, int priority) {

    public static final Codec<BatteryCharge> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .fieldOf("power")
                                                    .forGetter(BatteryCharge::power),
                                            Codec.LONG
                                                    .fieldOf("prev_power")
                                                    .forGetter(BatteryCharge::prevPower),
                                            Codec.INT
                                                    .fieldOf("red_low")
                                                    .forGetter(BatteryCharge::redLow),
                                            Codec.INT
                                                    .fieldOf("red_high")
                                                    .forGetter(BatteryCharge::redHigh),
                                            Codec.INT
                                                    .fieldOf("priority")
                                                    .forGetter(BatteryCharge::priority))
                                    .apply(i, BatteryCharge::new));

    public static final StreamCodec<ByteBuf, BatteryCharge> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    BatteryCharge::power,
                    ByteBufCodecs.VAR_LONG,
                    BatteryCharge::prevPower,
                    ByteBufCodecs.VAR_INT,
                    BatteryCharge::redLow,
                    ByteBufCodecs.VAR_INT,
                    BatteryCharge::redHigh,
                    ByteBufCodecs.VAR_INT,
                    BatteryCharge::priority,
                    BatteryCharge::new);
}
