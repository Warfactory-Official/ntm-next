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
