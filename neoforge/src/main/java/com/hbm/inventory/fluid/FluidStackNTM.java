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

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record FluidStackNTM(Fluid type, long amount, int pressure, DataComponentPatch components) {

    public static final FluidStackNTM[] EMPTY = new FluidStackNTM[0];
    public static final Codec<FluidStackNTM> CODEC =
            RecordCodecBuilder.create(
                    inst ->
                            inst.group(
                                            BuiltInRegistries.FLUID
                                                    .byNameCodec()
                                                    .fieldOf("type")
                                                    .forGetter(FluidStackNTM::type),
                                            Codec.LONG
                                                    .fieldOf("amount")
                                                    .forGetter(FluidStackNTM::amount),
                                            Codec.INT
                                                    .optionalFieldOf("pressure", 0)
                                                    .forGetter(FluidStackNTM::pressure),
                                            DataComponentPatch.CODEC
                                                    .optionalFieldOf(
                                                            "components", DataComponentPatch.EMPTY)
                                                    .forGetter(FluidStackNTM::components))
                                    .apply(inst, FluidStackNTM::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackNTM> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.FLUID),
                    FluidStackNTM::type,
                    ByteBufCodecs.VAR_LONG,
                    FluidStackNTM::amount,
                    ByteBufCodecs.VAR_INT,
                    FluidStackNTM::pressure,
                    DataComponentPatch.STREAM_CODEC,
                    FluidStackNTM::components,
                    FluidStackNTM::new);
    public static final Codec<FluidStackNTM[]> ARRAY_CODEC =
            CODEC.listOf().xmap(list -> list.toArray(FluidStackNTM[]::new), Arrays::asList);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackNTM[]> ARRAY_STREAM_CODEC =
            STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(list -> list.toArray(FluidStackNTM[]::new), Arrays::asList);

    public FluidStackNTM(Fluid type, long amount, int pressure) {
        this(type, amount, pressure, DataComponentPatch.EMPTY);
    }

    public FluidStackNTM(Fluid type, long amount) {
        this(type, amount, 0);
    }

    public static List<FluidStackNTM> snapshot(FluidTankNTM[] tanks) {
        List<FluidStackNTM> out = new ArrayList<>(tanks.length);
        for (FluidTankNTM tank : tanks) {
            Fluid content = tank.getFluid();
            out.add(
                    new FluidStackNTM(
                            content == null ? Fluids.EMPTY : content,
                            tank.getFill(),
                            tank.getPressure(),
                            tank.getComponents()));
        }
        return out;
    }

    public static void restore(List<FluidStackNTM> stacks, FluidTankNTM[] tanks) {
        for (int i = 0; i < Math.min(stacks.size(), tanks.length); i++) {
            FluidStackNTM stack = stacks.get(i);
            if (stack.type() == Fluids.EMPTY || stack.amount() <= 0) continue;
            tanks[i].setTankType(stack.type());
            tanks[i].withPressure(stack.pressure());
            tanks[i].setFill(0);
            tanks[i].receive(
                    stack.type(),
                    stack.components(),
                    (int) Math.min(stack.amount(), Integer.MAX_VALUE));
        }
    }

    public boolean matches(FluidTankNTM tank) {
        return tank.getTankType() == type
                && tank.getPressure() == pressure
                && tank.getFill() >= amount;
    }

    public boolean fitsInto(FluidTankNTM tank) {
        if (tank.getTankType() != type) return true;
        return (long) tank.getFill() + amount <= tank.getMaxFill();
    }

    public void drainFrom(FluidTankNTM tank) {
        tank.setFill(tank.getFill() - (int) amount);
    }

    public void fillInto(FluidTankNTM tank) {
        if (tank.getTankType() != type) tank.setTankType(type);
        tank.setFill(tank.getFill() + (int) amount);
    }
}
