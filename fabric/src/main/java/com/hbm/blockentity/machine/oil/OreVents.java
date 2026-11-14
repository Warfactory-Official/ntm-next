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

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class OreVents {
    @FunctionalInterface
    interface Pattern {
        void vent(Level level, BlockPos core, BlockState gas);
    }

    private OreVents() {}

    static void vent(Level level, BlockPos core, BlockState bored, Pattern pattern) {
        var ore = bored.getBlock().asItem().builtInRegistryHolder();
        if (ore.is(MaterialTagRegistry.U.ore())) {
            pattern.vent(level, core, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
        }
        if (ore.is(MaterialTagRegistry.ASBESTOS.ore())) {
            pattern.vent(level, core, ModBlocks.GAS_ASBESTOS.get().defaultBlockState());
        }
    }
}
