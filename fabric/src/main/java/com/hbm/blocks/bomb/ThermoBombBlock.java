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

import com.hbm.explosion.ExplosionThermo;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class ThermoBombBlock extends Block implements TriggerHandler {
    protected static final int FIELD_RADIUS = 15;
    protected static final int ENTITY_RADIUS = 20;
    private static final float BLAST = 5.0F;

    protected ThermoBombBlock(Properties props) {
        super(props);
    }

    protected abstract void field(Level level, BlockPos pos);

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
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        field(level, pos);
        level.explode(
                null, pos.getX(), pos.getY(), pos.getZ(), BLAST, Level.ExplosionInteraction.BLOCK);
        return TriggerResult.DETONATED;
    }

    public static class Endothermic extends ThermoBombBlock {

        public Endothermic(Properties props) {
            super(props);
        }

        @Override
        protected void field(Level level, BlockPos pos) {
            ExplosionThermo.freeze(level, pos, FIELD_RADIUS);
            ExplosionThermo.freezer(level, pos, ENTITY_RADIUS);
        }
    }

    public static class Exothermic extends ThermoBombBlock {

        public Exothermic(Properties props) {
            super(props);
        }

        @Override
        protected void field(Level level, BlockPos pos) {
            ExplosionThermo.scorch(level, pos, FIELD_RADIUS);
            ExplosionThermo.setEntitiesOnFire(level, pos, ENTITY_RADIUS);
        }
    }
}
