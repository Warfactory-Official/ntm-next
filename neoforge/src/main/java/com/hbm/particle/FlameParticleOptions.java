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

public record FlameParticleOptions(int meta) implements ParticleOptions {

    public static MapCodec<FlameParticleOptions> codec(ParticleType<FlameParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(
                i ->
                        i.group(
                                        Codec.INT
                                                .optionalFieldOf("meta", 0)
                                                .forGetter(FlameParticleOptions::meta))
                                .apply(i, FlameParticleOptions::new));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, FlameParticleOptions> streamCodec(
            ParticleType<FlameParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, FlameParticleOptions::meta, FlameParticleOptions::new);
    }

    @Override
    public ParticleType<?> getType() {
        return HbmParticles.FLAME.get();
    }
}
