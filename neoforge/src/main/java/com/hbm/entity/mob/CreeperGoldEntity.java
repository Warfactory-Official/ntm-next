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

package com.hbm.entity.mob;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.LimitedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.ShellBlockTransform;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class CreeperGoldEntity extends CreeperBaseEntity {

    public CreeperGoldEntity(EntityType<? extends CreeperGoldEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void detonate(ServerLevel level) {
        ComposableExplosion vnt =
                new ComposableExplosion(level, getX(), getY(), getZ(), isPowered() ? 14 : 7, this);
        vnt.blockSelector(new LimitedRayBlockSelector(60, isPowered() ? 32 : 16));
        vnt.blockProcessor(
                CellDestruction.decay()
                        .transform(new ShellBlockTransform(Blocks.GOLD_ORE.defaultBlockState())));
        vnt.entityProcessor(new SinglePointExplosionEntityProcessor().range(0.5F));
        vnt.effects(new ExplosionEffectStandard());
        vnt.explode();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return super.checkSpawnRules(level, reason)
                && getY() <= 40D
                && level().dimension() == Level.OVERWORLD;
    }
}
