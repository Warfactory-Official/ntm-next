// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record PipeData(Fluid fluid) {

    public static final PipeData UNSET = new PipeData(Fluids.EMPTY);

    public static final Codec<PipeData> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("fluid")
                                                    .forGetter(PipeData::fluid))
                                    .apply(i, PipeData::new));
}
