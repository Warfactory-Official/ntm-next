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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class CoolableTrait implements FluidTrait {
    public enum Mode {
        TURBINE("hbmfluid.trait.steam"),
        HEATEXCHANGER("hbmfluid.trait.coolable");

        final String labelKey;

        Mode(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private final Supplier<Fluid> product;
    private final int consumed;
    private final int produced;
    private final int heat;
    private final double[] efficiency;

    CoolableTrait(
            Supplier<Fluid> product,
            int consumed,
            int produced,
            int heat,
            Map<Mode, Double> efficiency) {
        this.product = product;
        this.consumed = consumed;
        this.produced = produced;
        this.heat = heat;
        this.efficiency = new double[Mode.values().length];
        efficiency.forEach((mode, value) -> this.efficiency[mode.ordinal()] = value);
    }

    public static Builder builder(Supplier<Fluid> product, int consumed, int produced, int heat) {
        return new Builder(product, consumed, produced, heat);
    }

    public static @Nullable CoolableTrait forMode(@Nullable Fluid fluid, Mode mode) {
        var cooling = NTMFluidProperties.getTrait(fluid, CoolableTrait.class);
        return cooling != null && cooling.efficiency(mode) > 0 ? cooling : null;
    }

    public Fluid product() {
        return product.get();
    }

    public int consumed() {
        return consumed;
    }

    public int produced() {
        return produced;
    }

    public int heat() {
        return heat;
    }

    public double efficiency(Mode mode) {
        return efficiency[mode.ordinal()];
    }

    @Override
    public void describeDetails(Consumer<Component> lines) {
        lines.accept(FluidTraitText.capacity(heat, consumed));
        for (var mode : Mode.values()) {
            if (efficiency(mode) > 0)
                lines.accept(FluidTraitText.efficiency(mode.labelKey, efficiency(mode)));
        }
    }

    public static final class Builder {

        private final Supplier<Fluid> product;
        private final int consumed;
        private final int produced;
        private final int heat;
        private final Map<Mode, Double> efficiency = new EnumMap<>(Mode.class);

        private Builder(Supplier<Fluid> product, int consumed, int produced, int heat) {
            this.product = product;
            this.consumed = consumed;
            this.produced = produced;
            this.heat = heat;
        }

        public Builder efficiency(Mode mode, double value) {
            efficiency.put(mode, value);
            return this;
        }

        public CoolableTrait build() {
            return new CoolableTrait(product, consumed, produced, heat, efficiency);
        }
    }
}
