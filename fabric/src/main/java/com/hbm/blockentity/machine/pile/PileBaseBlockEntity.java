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

package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.handler.neutron.NeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PileBaseBlockEntity extends BlockEntity implements BlockEntityTicks {

    protected PileBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public abstract void tickServer();

    protected void castRay(int flux) {
        if (flux == 0) return;
        RandomSource rand = level.getRandom();
        float roll = fullTurn(rand);
        float yaw = fullTurn(rand);
        float pitch = fullTurn(rand);
        NeutronHandler.enqueue((ServerLevel) level, PileRay.turned(this, roll, yaw, pitch, flux));
    }

    private static float fullTurn(RandomSource rand) {
        return (float) (Math.PI * 2D * rand.nextDouble());
    }
}
