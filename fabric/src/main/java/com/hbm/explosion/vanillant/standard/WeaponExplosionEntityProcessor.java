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

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class WeaponExplosionEntityProcessor extends MultiPointExplosionEntityProcessor {

    private final float damage;
    private ResourceKey<DamageType> damageType = ModDamageTypes.EXPLOSIVE;
    private float pierceThreshold;
    private float pierceResistance;
    private @Nullable ComposableExplosion sourcedFor;
    private @Nullable DamageSource classSource;
    private @Nullable DamageSource piercingSource;

    public WeaponExplosionEntityProcessor(double nodeDistance, float damage) {
        super(nodeDistance);
        this.damage = damage;
        selfDamage = true;
    }

    public WeaponExplosionEntityProcessor damageType(ResourceKey<DamageType> damageType) {
        this.damageType = damageType;
        sourcedFor = null;
        return this;
    }

    public WeaponExplosionEntityProcessor piercing(float threshold, float resistance) {
        pierceThreshold = threshold;
        pierceResistance = resistance;
        sourcedFor = null;
        return this;
    }

    @Override
    public WeaponExplosionEntityProcessor range(float multiplier) {
        super.range(multiplier);
        return this;
    }

    @Override
    public WeaponExplosionEntityProcessor knockback(double multiplier) {
        super.knockback(multiplier);
        return this;
    }

    protected final float pierceThreshold() {
        return pierceThreshold;
    }

    protected final float pierceResistance() {
        return pierceResistance;
    }

    @Override
    protected float damage(double distance, float exposure, double impact, float range) {
        return exposure < 0.125F ? 0F : (float) (damage * (1.0 - distance));
    }

    protected final DamageSource classSource(ComposableExplosion blast) {
        if (sourcedFor != blast) {
            var type =
                    blast.level()
                            .registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(damageType);
            classSource = new DamageSource(type, null, attribution(blast));
            piercingSource =
                    ModifiedDamageSource.of(
                            classSource, true, 0, pierceThreshold, pierceResistance);
            sourcedFor = blast;
        }
        return classSource;
    }

    @Override
    protected @Nullable Entity attribution(ComposableExplosion blast) {
        return blast.getDirectSourceEntity() instanceof LivingEntity living ? living : null;
    }

    @Override
    public void attack(Entity entity, ComposableExplosion blast, float amount) {
        if (!entity.isAlive()) return;
        if (entity == blast.getDirectSourceEntity()) amount *= 0.5F;
        var source = classSource(blast);
        if (entity instanceof LivingEntity living) {
            living.hurtServer(blast.level(), piercingSource, amount);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
        } else if (entity.level() instanceof ServerLevel level) {
            entity.hurtServer(level, source, amount);
        }
    }
}
