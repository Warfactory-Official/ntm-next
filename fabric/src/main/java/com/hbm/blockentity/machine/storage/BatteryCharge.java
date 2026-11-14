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

package com.hbm.blockentity.machine.storage;

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
