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

package com.hbm.blocks.fluid;

import java.util.function.Supplier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

public final class NeoForgeClassicFluid {

    private NeoForgeClassicFluid() {}

    public static final class Source extends ClassicFluid.Source {

        private final Supplier<? extends FluidType> type;

        public Source(
                Spec spec,
                Supplier<? extends Fluid> source,
                Supplier<? extends Fluid> flowing,
                Supplier<? extends FluidType> type) {
            super(spec, source, flowing);
            this.type = type;
        }

        @Override
        public FluidType getFluidType() {
            return type.get();
        }
    }

    public static final class Flowing extends ClassicFluid.Flowing {

        private final Supplier<? extends FluidType> type;

        public Flowing(
                Spec spec,
                Supplier<? extends Fluid> source,
                Supplier<? extends Fluid> flowing,
                Supplier<? extends FluidType> type) {
            super(spec, source, flowing);
            this.type = type;
        }

        @Override
        public FluidType getFluidType() {
            return type.get();
        }
    }
}
