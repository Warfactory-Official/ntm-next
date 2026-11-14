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

package com.hbm.blockentity.machine.storage;

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
