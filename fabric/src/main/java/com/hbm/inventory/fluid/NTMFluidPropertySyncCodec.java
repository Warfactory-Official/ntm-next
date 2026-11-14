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

package com.hbm.inventory.fluid;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class NTMFluidPropertySyncCodec {
    private static final StreamCodec<ByteBuf, NTMFluidProperty> PROPERTY =
            ByteBufCodecs.fromCodec(NTMFluidPropertyCodec.CODEC);
    public static final StreamCodec<ByteBuf, Map<Fluid, NTMFluidProperty>> STREAM_CODEC =
            StreamCodec.of(NTMFluidPropertySyncCodec::encode, NTMFluidPropertySyncCodec::decode);

    private NTMFluidPropertySyncCodec() {}

    private static Map<Fluid, NTMFluidProperty> decode(ByteBuf input) {
        int count = input.readInt();
        if (count < 0 || count > input.readableBytes() / 5) {
            throw new DecoderException("Invalid fluid-property entry count");
        }
        Map<Fluid, NTMFluidProperty> properties = new Reference2ObjectOpenHashMap<>(count);
        for (int i = 0; i < count; i++) {
            int id = input.readInt();
            Fluid fluid = BuiltInRegistries.FLUID.byId(id);
            if (fluid == Fluids.EMPTY) {
                throw new DecoderException("Unknown fluid-property registry id " + id);
            }
            properties.put(fluid, PROPERTY.decode(input));
        }
        return properties;
    }

    private static void encode(ByteBuf output, Map<Fluid, NTMFluidProperty> properties) {
        output.writeInt(properties.size());
        for (Map.Entry<Fluid, NTMFluidProperty> entry : properties.entrySet()) {
            Fluid fluid = entry.getKey();
            int id = BuiltInRegistries.FLUID.getId(fluid);
            if (id < 0 || fluid == Fluids.EMPTY)
                throw new EncoderException("Invalid fluid-property fluid " + fluid);
            output.writeInt(id);
            PROPERTY.encode(output, entry.getValue());
        }
    }
}
