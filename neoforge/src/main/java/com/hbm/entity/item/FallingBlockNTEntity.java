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

package com.hbm.entity.item;

import com.hbm.blocks.machine.LightBeamSource;
import com.hbm.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FallingBlockNTEntity extends FallingBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FallingBlockNTEntity(EntityType<? extends FallingBlockNTEntity> type, Level level) {
        super(type, level);
    }

    public static FallingBlockNTEntity fall(ServerLevel level, BlockPos pos, BlockState state) {
        FallingBlockNTEntity entity = create(level, pos, state);
        level.addFreshEntity(entity);
        return entity;
    }

    static FallingBlockNTEntity create(Level level, BlockPos pos, BlockState state) {
        FallingBlockNTEntity entity =
                new FallingBlockNTEntity(ModEntities.FALLING_BLOCK_NT.get(), level);
        entity.blockState =
                state.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                        : state;
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        return entity;
    }

    static FallingBlockNTEntity resume(
            Level level,
            BlockPos source,
            BlockState state,
            double y,
            double velocity,
            double fallDistance,
            int age) {
        FallingBlockNTEntity entity = create(level, source, state);
        entity.setPos(source.getX() + 0.5, y, source.getZ() + 0.5);
        entity.setDeltaMovement(0.0, velocity, 0.0);
        entity.xo = entity.getX();
        entity.yo = y;
        entity.zo = entity.getZ();
        entity.time = age;
        entity.tickCount = age;
        entity.fallDistance = fallDistance;
        entity.dropItem = false;
        entity.setOldPosAndRot();
        return entity;
    }

    int virtualAge() {
        return time;
    }

    void syncVirtualVisual(double y, double velocity, int age) {
        setOldPosAndRot();
        setPos(getX(), y, getZ());
        setDeltaMovement(0.0, velocity, 0.0);
        time = age;
        tickCount = age;
    }

    void advanceVirtualVisual() {
        setOldPosAndRot();
        double movement = getDeltaMovement().y - 0.04;
        setPos(getX(), getY() + movement, getZ());
        setDeltaMovement(0.0, movement * 0.98F, 0.0);
        time++;
        tickCount++;
    }

    @Override
    public void tick() {
        if (blockState.isAir() || blockState.getBlock() instanceof LightBeamSource) {
            discard();
            return;
        }
        if (time == 0 && level() instanceof ServerLevel server) {
            var source = getStartPos();
            if (!server.getBlockState(source).is(blockState.getBlock())) {
                discard();
                return;
            }
            server.removeBlock(source, false);
        }
        super.tick();
    }
}
