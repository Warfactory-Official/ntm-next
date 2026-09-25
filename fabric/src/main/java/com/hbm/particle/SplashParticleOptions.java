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

public record SplashParticleOptions(int color) implements ParticleOptions {

    public static final int NO_TINT = -1;

    public static MapCodec<SplashParticleOptions> codec(ParticleType<SplashParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(
                i ->
                        i.group(
                                        Codec.INT
                                                .optionalFieldOf("color", NO_TINT)
                                                .forGetter(SplashParticleOptions::color))
                                .apply(i, SplashParticleOptions::new));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, SplashParticleOptions> streamCodec(
            ParticleType<SplashParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, SplashParticleOptions::color, SplashParticleOptions::new);
    }

    @Override
    public ParticleType<?> getType() {
        return HbmParticles.SPLASH.get();
    }
}
