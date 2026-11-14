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

package com.hbm.blocks.generic;

import com.hbm.registration.OverridePoint;
import com.hbm.util.RegistryUtil;
import com.hbm.world.feature.OilSurfaceScarring;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public interface FluidReserve {

    OverridePoint<Resolver> RESERVE_OF =
            OverridePoint.of(
                    RegistryUtil.id("fluid_reserve_of"),
                    block -> block instanceof FluidReserve reserve ? reserve : null);

    static @Nullable FluidReserve of(Block block) {
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
        @Nullable FluidReserve reserveOf(Block block);
    }

    interface Drained {}
}
