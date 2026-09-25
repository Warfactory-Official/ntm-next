// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FluidTankContents(
        FluidStackNTM tank, int capacity, int mode, boolean damaged, boolean onFire) {
    public static final Codec<FluidTankContents> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            FluidStackNTM.CODEC
                                                    .fieldOf("tank")
                                                    .forGetter(FluidTankContents::tank),
                                            Codec.INT
                                                    .fieldOf("capacity")
                                                    .forGetter(FluidTankContents::capacity),
                                            Codec.intRange(0, 3)
                                                    .fieldOf("mode")
                                                    .forGetter(FluidTankContents::mode),
                                            Codec.BOOL
                                                    .fieldOf("damaged")
                                                    .forGetter(FluidTankContents::damaged),
                                            Codec.BOOL
                                                    .fieldOf("on_fire")
                                                    .forGetter(FluidTankContents::onFire))
                                    .apply(i, FluidTankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidTankContents> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackNTM.STREAM_CODEC,
                    FluidTankContents::tank,
                    ByteBufCodecs.INT,
                    FluidTankContents::capacity,
                    ByteBufCodecs.VAR_INT,
                    FluidTankContents::mode,
                    ByteBufCodecs.BOOL,
                    FluidTankContents::damaged,
                    ByteBufCodecs.BOOL,
                    FluidTankContents::onFire,
                    FluidTankContents::new);
}
