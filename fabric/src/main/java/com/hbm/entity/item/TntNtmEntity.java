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

import com.hbm.api.block.ExplosiveBlock;
import com.hbm.blocks.bomb.TntRule;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TntNtmEntity extends PrimedTnt {

    public TntNtmEntity(EntityType<? extends TntNtmEntity> type, Level level) {
        super(type, level);
    }

    public TntNtmEntity(
            EntityType<? extends TntNtmEntity> type,
            Level level,
            double x,
            double y,
            double z,
            @Nullable LivingEntity igniter,
            BlockState bomb) {
        this(type, level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2, -Math.cos(rot) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = EntityReference.of(igniter);
        this.setBlockState(bomb);
    }

    @Override
    public void explode() {
        if (!TntRule.explodes(level())) return;
        if (getBlockState().getBlock() instanceof ExplosiveBlock bomb) {
            bomb.explodeEntity(level(), getX(), getY(0.5D), getZ(), this);
        }
    }
}
