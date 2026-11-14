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

package com.hbm.world.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

public record OreGenerationProfile(
        Identifier id,
        Identifier dimension,
        Identifier feature,
        List<Identifier> biomes,
        List<Identifier> blocks,
        OreHeightProfile height,
        double attempts,
        int veinSize,
        List<Component> conditions) {
    public static final Codec<OreGenerationProfile> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Identifier.CODEC
                                                    .fieldOf("id")
                                                    .forGetter(OreGenerationProfile::id),
                                            Identifier.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(OreGenerationProfile::dimension),
                                            Identifier.CODEC
                                                    .fieldOf("feature")
                                                    .forGetter(OreGenerationProfile::feature),
                                            Identifier.CODEC
                                                    .listOf()
                                                    .fieldOf("biomes")
                                                    .forGetter(OreGenerationProfile::biomes),
                                            Identifier.CODEC
                                                    .listOf()
                                                    .fieldOf("blocks")
                                                    .forGetter(OreGenerationProfile::blocks),
                                            OreHeightProfile.CODEC
                                                    .fieldOf("height")
                                                    .forGetter(OreGenerationProfile::height),
                                            Codec.DOUBLE
                                                    .fieldOf("attempts")
                                                    .forGetter(OreGenerationProfile::attempts),
                                            Codec.INT
                                                    .fieldOf("vein_size")
                                                    .forGetter(OreGenerationProfile::veinSize),
                                            ComponentSerialization.CODEC
                                                    .listOf()
                                                    .fieldOf("conditions")
                                                    .forGetter(OreGenerationProfile::conditions))
                                    .apply(instance, OreGenerationProfile::new));

    public OreGenerationProfile {
        biomes = List.copyOf(biomes);
        blocks = List.copyOf(blocks);
        conditions = List.copyOf(conditions);
        if (!Double.isFinite(attempts) || attempts < -1 || veinSize < -1)
            throw new IllegalArgumentException();
    }
}
