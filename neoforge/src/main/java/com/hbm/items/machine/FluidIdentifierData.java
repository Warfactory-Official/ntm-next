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

package com.hbm.items.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record FluidIdentifierData(Fluid primary, Fluid secondary) {

    public static final FluidIdentifierData EMPTY =
            new FluidIdentifierData(Fluids.EMPTY, Fluids.EMPTY);

    public static final Codec<FluidIdentifierData> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("primary")
                                                    .forGetter(FluidIdentifierData::primary),
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("secondary")
                                                    .forGetter(FluidIdentifierData::secondary))
                                    .apply(inst, FluidIdentifierData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIdentifierData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.FLUID),
                    FluidIdentifierData::primary,
                    ByteBufCodecs.registry(Registries.FLUID),
                    FluidIdentifierData::secondary,
                    FluidIdentifierData::new);

    public FluidIdentifierData withPrimary(Fluid p) {
        return new FluidIdentifierData(p, this.secondary);
    }

    public FluidIdentifierData withSecondary(Fluid s) {
        return new FluidIdentifierData(this.primary, s);
    }

    public FluidIdentifierData swap() {
        return new FluidIdentifierData(this.secondary, this.primary);
    }
}
