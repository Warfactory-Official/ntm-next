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

public record RefineryContents(List<FluidStackNTM> tanks, boolean hasExploded, boolean onFire) {

    public static final Codec<RefineryContents> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            FluidStackNTM.CODEC
                                                    .listOf()
                                                    .fieldOf("tanks")
                                                    .forGetter(RefineryContents::tanks),
                                            Codec.BOOL
                                                    .fieldOf("has_exploded")
                                                    .forGetter(RefineryContents::hasExploded),
                                            Codec.BOOL
                                                    .fieldOf("on_fire")
                                                    .forGetter(RefineryContents::onFire))
                                    .apply(i, RefineryContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RefineryContents> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackNTM.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    RefineryContents::tanks,
                    ByteBufCodecs.BOOL,
                    RefineryContents::hasExploded,
                    ByteBufCodecs.BOOL,
                    RefineryContents::onFire,
                    RefineryContents::new);
}
