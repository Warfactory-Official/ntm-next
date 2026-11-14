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

import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidTrait;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class FluidBuilder {

    public static final int ROOM_TEMPERATURE = 20;

    private static final List<FluidBuilder> PENDING = new ArrayList<>();
    private static final Map<String, FluidBuilder> BY_NAME = new HashMap<>();

    private final String name;
    private final int color;
    private final int poison;
    private final int flammability;
    private final int reactivity;
    private final HazardSymbol symbol;
    private final Map<Class<? extends FluidTrait>, FluidTrait> traits = new LinkedHashMap<>();
    private int temperature = ROOM_TEMPERATURE;
    private final List<String> shares = new ArrayList<>();

    public FluidBuilder(
            String name,
            int color,
            int poison,
            int flammability,
            int reactivity,
            HazardSymbol symbol) {
        this.name = name;
        this.color = color;
        this.poison = poison;
        this.flammability = flammability;
        this.reactivity = reactivity;
        this.symbol = symbol;
        BY_NAME.put(name, this);
    }

    public static FluidBuilder get(String name) {
        return BY_NAME.get(name);
    }

    public static FluidBuilder copyOf(
            String name, FluidBuilder base, Predicate<FluidTrait> include) {
        FluidBuilder copy =
                new FluidBuilder(
                                name,
                                base.color,
                                base.poison,
                                base.flammability,
                                base.reactivity,
                                base.symbol)
                        .setTemp(base.temperature);
        for (FluidTrait trait : base.traits.values())
            if (include.test(trait)) copy.addTraits(trait);
        return copy;
    }

    public static void declareAll() {
        for (FluidBuilder b : PENDING) {
            Fluid fluid = NTMFluids.byName(b.name);
            NTMFluidProperty property = b.property();
            NTMFluidProperties.register(fluid, property);
        }
    }

    public FluidBuilder setTemp(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public FluidBuilder addTraits(FluidTrait... entries) {
        for (FluidTrait t : entries) traits.put(t.getClass(), t);
        return this;
    }

    public FluidBuilder removeTrait(Class<? extends FluidTrait> type) {
        if (traits.remove(type) == null)
            throw new IllegalStateException(name + " has no " + type.getSimpleName());
        return this;
    }

    public FluidBuilder shares(String... conventionalTags) {
        shares.addAll(Arrays.asList(conventionalTags));
        return this;
    }

    public static void visitShares(SharesVisitor visitor) {
        for (FluidBuilder b : PENDING) {
            Fluid fluid = NTMFluids.byName(b.name);
            b.shares.forEach(tag -> visitor.visit(fluid, tag));
        }
    }

    @FunctionalInterface
    public interface SharesVisitor {
        void visit(Fluid fluid, String conventionalTag);
    }

    public void declare() {
        PENDING.add(this);
    }

    public long flammableEnergy() {
        var fuel = (FlammableTrait) traits.get(FlammableTrait.class);
        return fuel == null ? 0L : fuel.heatPerBucket();
    }

    public NTMFluidProperty property() {
        return new NTMFluidProperty(
                color,
                symbol,
                temperature,
                poison,
                flammability,
                reactivity,
                traits.values().toArray(new FluidTrait[0]),
                shares.stream()
                        .map(tag -> TagKey.create(Registries.FLUID, Identifier.parse(tag)))
                        .toList());
    }
}
