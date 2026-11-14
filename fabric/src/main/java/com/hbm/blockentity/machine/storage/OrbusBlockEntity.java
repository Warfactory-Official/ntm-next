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

package com.hbm.blockentity.machine.storage;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class OrbusBlockEntity extends FluidBarrelBlockEntity {

    public static final int CAPACITY = 512_000;
    private static final long RATE_FLOOR = 1_000L;

    public OrbusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORBUS.get(), pos, state, CAPACITY);
    }

    @Override
    protected long receiveRate() {
        return getMode().receives()
                ? Math.max(RATE_FLOOR, (tank.getMaxFill() - tank.getFill()) / 100L)
                : 0L;
    }

    @Override
    protected long provideRate() {
        return getMode().sends() ? Math.max(RATE_FLOOR, tank.getFill() / 100L) : 0L;
    }

    @Override
    protected void checkHazards() {}

    @Override
    protected boolean hasArms() {
        return false;
    }

    @Override
    protected void joinFluidGraph(ServerLevel server, Fluid fluid) {
        FluidBufferCells.join(
                server,
                worldPosition,
                FluidBufferCells.activePlane(worldPosition, getBlockState()),
                fluid);
    }

    @Override
    protected void leaveFluidGraph(ServerLevel server) {
        FluidBufferCells.leave(
                server, FluidBufferCells.activePlane(worldPosition, getBlockState()));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbus");
    }
}
