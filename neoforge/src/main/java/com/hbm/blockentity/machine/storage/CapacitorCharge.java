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
