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

package com.hbm.blocks.generic;

import com.hbm.blocks.bomb.ExplosiveBarrelBlock;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.explosion.ExplosionThermo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

public class LoxBarrelBlock extends ExplosiveBarrelBlock {
    private static final int FREEZER_RADIUS = 7;

    public LoxBarrelBlock(BlockBehaviour.Properties props) {
        super(props, 100, false);
    }

    @Override
    public void explodeEntity(
            Level level, double x, double y, double z, @Nullable TntNtmEntity entity) {
        level.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
        ExplosionThermo.freezer(level, BlockPos.containing(x, y, z), FREEZER_RADIUS);
    }
}
