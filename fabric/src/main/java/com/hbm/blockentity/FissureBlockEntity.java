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

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FissureBlockEntity extends BlockEntity implements BlockEntityTicks, FluidTankHandler {

    private static final int CAPACITY = 1_000;

    private final FluidTankNTM lava = new FluidTankNTM(NTMFluids.LAVA, CAPACITY);
    private final FluidTankNTM[] sending = {lava};
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public FissureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISSURE.get(), pos, state);
    }

    @Override
    public void tickServer() {
        this.lava.setFill(CAPACITY);
        this.flush.provide((ServerLevel) level, this);
    }

    @Override
    public int tankCount() {
        return sending.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return sending[index];
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(lava, (level, pos, contact) -> contact.contact(pos.above(), Direction.DOWN));
    }
}
