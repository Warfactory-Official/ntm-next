// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AshRevealParticleOptions(float red, float green, float blue)
        implements ParticleOptions {

    public static MapCodec<AshRevealParticleOptions> codec(
            ParticleType<AshRevealParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(
                i ->
                        i.group(
                                        Codec.FLOAT
                                                .fieldOf("red")
                                                .forGetter(AshRevealParticleOptions::red),
                                        Codec.FLOAT
                                                .fieldOf("green")
                                                .forGetter(AshRevealParticleOptions::green),
                                        Codec.FLOAT
                                                .fieldOf("blue")
                                                .forGetter(AshRevealParticleOptions::blue))
                                .apply(i, AshRevealParticleOptions::new));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, AshRevealParticleOptions> streamCodec(
            ParticleType<AshRevealParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.FLOAT,
                AshRevealParticleOptions::red,
                ByteBufCodecs.FLOAT,
                AshRevealParticleOptions::green,
                ByteBufCodecs.FLOAT,
                AshRevealParticleOptions::blue,
                AshRevealParticleOptions::new);
    }

    @Override
    public ParticleType<?> getType() {
        return HbmParticles.ASH_REVEAL.get();
    }
}
