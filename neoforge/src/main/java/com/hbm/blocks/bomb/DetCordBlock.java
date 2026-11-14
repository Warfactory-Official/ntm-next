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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class DetCordBlock extends Block implements ExplosiveBlock {

    public DetCordBlock(Properties props) {
        super(props);
    }

    public static boolean joins(BlockGetter level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        return block instanceof DetCordBlock || block instanceof ExplosiveChargeBlock;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
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
            explode(level, pos);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), 0);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, TntNtmEntity entity) {
        explode(level, BlockPos.containing(x, y, z));
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    public void explode(Level level, BlockPos pos) {
        if (!TntRule.explodes(level)) return;
        level.removeBlock(pos, false);
        level.explode(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                1.5F,
                Level.ExplosionInteraction.TNT);
    }
}
