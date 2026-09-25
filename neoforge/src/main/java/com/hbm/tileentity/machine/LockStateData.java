// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

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
