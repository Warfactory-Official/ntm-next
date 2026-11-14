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

package com.hbm.blockentity.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LockStateData(int lock, boolean isLocked, double lockMod, boolean cheesable) {

    public static final Codec<LockStateData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.INT
                                                    .fieldOf("lock")
                                                    .forGetter(LockStateData::lock),
                                            Codec.BOOL
                                                    .fieldOf("isLocked")
                                                    .forGetter(LockStateData::isLocked),
                                            Codec.DOUBLE
                                                    .fieldOf("lockMod")
                                                    .forGetter(LockStateData::lockMod),
                                            Codec.BOOL
                                                    .fieldOf("cheesable")
                                                    .forGetter(LockStateData::cheesable))
                                    .apply(instance, LockStateData::new));

    public static final StreamCodec<ByteBuf, LockStateData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    LockStateData::lock,
                    ByteBufCodecs.BOOL,
                    LockStateData::isLocked,
                    ByteBufCodecs.DOUBLE,
                    LockStateData::lockMod,
                    ByteBufCodecs.BOOL,
                    LockStateData::cheesable,
                    LockStateData::new);

    public boolean isDefault() {
        return lock == 0 && !isLocked && lockMod == 0.1D && cheesable;
    }
}
