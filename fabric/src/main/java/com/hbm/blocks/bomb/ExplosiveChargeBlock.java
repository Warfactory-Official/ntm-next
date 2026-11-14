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

package com.hbm.blocks.bomb;

import com.hbm.api.block.ExplosiveBlock;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.interfaces.TriggerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class ExplosiveChargeBlock extends Block implements TriggerHandler, ExplosiveBlock {

    protected ExplosiveChargeBlock(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            trigger(level, pos, null);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), 0);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, TntNtmEntity entity) {
        trigger(level, BlockPos.containing(x, y, z), entity);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
