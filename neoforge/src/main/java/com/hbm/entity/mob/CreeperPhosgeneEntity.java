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

import com.hbm.entity.effect.MistEntity;
import com.hbm.inventory.fluid.NTMFluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class CreeperPhosgeneEntity extends CreeperBaseEntity {

    public CreeperPhosgeneEntity(EntityType<? extends CreeperPhosgeneEntity> type, Level level) {
        super(type, level);
        maxSwell = 20;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount -= 4F;
        }

        if (amount < 0) return false;

        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return super.checkSpawnRules(level, reason) && level().dimension() == Level.OVERWORLD;
    }

    @Override
    public void detonate(ServerLevel level) {
        level.explode(
                this,
                getX(),
                getY() + getBbHeight() / 2,
                getZ(),
                2F,
                Level.ExplosionInteraction.NONE);

        MistEntity mist = new MistEntity(level);
        mist.setType(NTMFluids.PHOSGENE);
        mist.setPos(getX(), getY(), getZ());
        mist.setArea(10, 5);
        mist.setDuration(150);
        level.addFreshEntity(mist);
    }
}
