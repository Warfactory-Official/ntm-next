// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.lib.Library;
import com.hbm.registration.OverridePoint;
import com.hbm.world.feature.OilSurfaceScarring;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface FluidReserveBlock {

    OverridePoint<Resolver> RESERVE_OF =
            OverridePoint.of(
                    Library.id("fluid_reserve_of"),
                    block -> block instanceof FluidReserveBlock reserve ? reserve : null);

    static @Nullable FluidReserveBlock of(Block block) {
        return RESERVE_OF.get().reserveOf(block);
    }

    Fluid primaryFluid();

    @Nullable Fluid secondaryFluid();

    int primaryAmount();

    int secondaryAmount(RandomSource random);

    void drain(Level level, BlockPos pos, RandomSource random, double chanceMultiplier);

    boolean requiresFracking();

    default void scarSurface(Level level, RandomSource random, int x, int z, int range) {
        OilSurfaceScarring.generateOilSpot(level, random, x, z, range, 10);
    }

    int blockFluidAmount();

    default void settle(Level level, BlockPos pos, BlockState state) {}

    @FunctionalInterface
    interface Resolver {
        @Nullable FluidReserveBlock reserveOf(Block block);
    }

    interface Drained {}
}
