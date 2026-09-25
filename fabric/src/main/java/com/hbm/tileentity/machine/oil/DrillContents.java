// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DrillContents(long power, List<FluidStackNTM> tanks) {

    public static final Codec<DrillContents> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .fieldOf("power")
                                                    .forGetter(DrillContents::power),
                                            FluidStackNTM.CODEC
                                                    .listOf()
                                                    .fieldOf("tanks")
                                                    .forGetter(DrillContents::tanks))
                                    .apply(i, DrillContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrillContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    DrillContents::power,
                    FluidStackNTM.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    DrillContents::tanks,
                    DrillContents::new);
}
