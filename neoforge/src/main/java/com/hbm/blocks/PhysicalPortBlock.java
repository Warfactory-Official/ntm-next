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

package com.hbm.blocks;

import com.hbm.capability.PhysicalConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public final class PhysicalPortBlock extends Block implements PhysicalConnector {
    private final boolean power;

    private PhysicalPortBlock(Properties properties, boolean power) {
        super(properties);
        this.power = power;
    }

    public static Block power(Properties properties) {
        return new PhysicalPortBlock(properties, true);
    }

    public static Block fluid(Properties properties) {
        return new PhysicalPortBlock(properties, false);
    }

    @Override
    public boolean hasPowerContact(Level level, BlockPos pos, BlockState state, Direction side) {
        return power;
    }

    @Override
    public boolean hasFluidContact(
            Level level, BlockPos pos, BlockState state, Direction side, Fluid fluid) {
        return !power;
    }
}
