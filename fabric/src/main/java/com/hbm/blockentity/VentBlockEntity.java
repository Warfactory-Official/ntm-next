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

package com.hbm.blockentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.generic.VentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VentBlockEntity extends BlockEntity implements BlockEntityTicks {

    public VentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENT.get(), pos, state);
    }

    @Override
    public void tickServer() {
        var state = getBlockState();
        if (!(state.getBlock() instanceof VentBlock vent)) return;

        RandomSource rand = level.getRandom();
        double x = rand.nextGaussian() * vent.spread();
        double y = rand.nextGaussian() * vent.spread();
        double z = rand.nextGaussian() * vent.spread();

        BlockPos target = worldPosition.offset((int) x, (int) y, (int) z);
        if (level.getBlockState(target).isSolidRender()) return;

        level.addFreshEntity(
                vent.plume()
                        .spawn(
                                level,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                x / 2,
                                y / 2,
                                z / 2));
    }
}
