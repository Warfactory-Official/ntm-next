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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.CyberCrabEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class CyberCrabBlockEntity extends BlockEntity implements BlockEntityTicks {
    private int elapsedTicks;

    public CyberCrabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRABS.get(), pos, state);
    }

    @Override
    public void tickServer() {
        if (++this.elapsedTicks <= 200
                || !level.isEmptyBlock(worldPosition.above())
                || level.getNearestPlayer(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 1D,
                                worldPosition.getZ() + 0.5D,
                                25D,
                                false)
                        == null) return;
        var bounds =
                new AABB(
                        worldPosition.getX() - 5D,
                        worldPosition.getY() - 2D,
                        worldPosition.getZ() - 5D,
                        worldPosition.getX() + 6D,
                        worldPosition.getY() + 4D,
                        worldPosition.getZ() + 6D);
        if (level.getEntitiesOfClass(CyberCrabEntity.class, bounds, EntitySelector.NO_SPECTATORS)
                        .size()
                < 5) {
            var type =
                    level.getRandom().nextInt(5) == 0
                            ? ModEntities.TESLA_CRAB.get()
                            : ModEntities.CYBER_CRAB.get();
            var crab = type.create((ServerLevel) level, EntitySpawnReason.SPAWNER);
            if (crab != null) {
                crab.setPos(
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D);
                level.addFreshEntity(crab);
            }
        }
        this.elapsedTicks = 0;
    }
}
