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

package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.NTMFluidProperties;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class HeatableTrait implements FluidTrait {
    public enum Mode {
        BOILER("hbmfluid.trait.boilable"),
        HEATEXCHANGER("hbmfluid.trait.heatable"),
        PWR("hbmfluid.trait.coolantPWR"),
        ICF("hbmfluid.trait.coolantICF"),
        PA("hbmfluid.trait.coolantPA");

        final String labelKey;

        Mode(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    public static final class Step {

        private final int heat;
        private final int consumed;
        private final Supplier<Fluid> product;
        private final int produced;

        Step(int heat, int consumed, Supplier<Fluid> product, int produced) {
            if (heat <= 0 || consumed <= 0 || produced <= 0) {
                throw new IllegalArgumentException(
                        "heating step heat and fluid amounts must be positive");
            }
            this.heat = heat;
            this.consumed = consumed;
            this.product = product;
            this.produced = produced;
        }

        public int heat() {
            return heat;
        }

        public int consumed() {
            return consumed;
        }

        public Fluid product() {
            return product.get();
        }

        public int produced() {
            return produced;
        }
    }

    private final List<Step> steps;
    private final double[] efficiency;

    HeatableTrait(List<Step> steps, Map<Mode, Double> efficiency) {
        this.steps = List.copyOf(steps);
        this.efficiency = new double[Mode.values().length];
        efficiency.forEach((mode, value) -> this.efficiency[mode.ordinal()] = value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static @Nullable HeatableTrait forMode(@Nullable Fluid fluid, Mode mode) {
        var heating = NTMFluidProperties.getTrait(fluid, HeatableTrait.class);
        return heating != null && heating.efficiency(mode) > 0 ? heating : null;
    }

    public Step first() {
        return steps.getFirst();
    }

    public List<Step> steps() {
        return steps;
    }

    public double efficiency(Mode mode) {
        return efficiency[mode.ordinal()];
    }

    @Override
    public void describeDetails(Consumer<Component> lines) {
        lines.accept(FluidTraitText.capacity(first().heat, first().consumed));
        for (var mode : Mode.values()) {
            if (efficiency(mode) > 0)
                lines.accept(FluidTraitText.efficiency(mode.labelKey, efficiency(mode)));
        }
    }

    public static final class Builder {

        private final List<Step> steps = new ArrayList<>();
        private final Map<Mode, Double> efficiency = new EnumMap<>(Mode.class);

        private Builder() {}

        public Builder efficiency(Mode mode, double value) {
            efficiency.put(mode, value);
            return this;
        }

        public Builder step(int heat, int consumed, Supplier<Fluid> product, int produced) {
            steps.add(new Step(heat, consumed, product, produced));
            return this;
        }

        public HeatableTrait build() {
            if (steps.isEmpty())
                throw new IllegalStateException("a heating trait needs at least one step");
            return new HeatableTrait(steps, efficiency);
        }
    }
}
