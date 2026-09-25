// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
