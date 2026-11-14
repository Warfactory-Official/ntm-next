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

package com.hbm.blockentity.machine.oil;

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
