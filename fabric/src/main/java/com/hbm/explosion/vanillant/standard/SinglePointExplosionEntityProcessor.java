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
import com.hbm.explosion.vanillant.ExplosionHitListener;
import com.hbm.util.LineOfSight;
import com.hbm.util.SectionAccess;
import org.jspecify.annotations.Nullable;

public final class SinglePointExplosionEntityProcessor extends ExplosionEntityProcessor {

    private @Nullable ExplosionHitListener hook;

    public SinglePointExplosionEntityProcessor range(float multiplier) {
        rangeMultiplier = multiplier;
        return this;
    }

    public SinglePointExplosionEntityProcessor selfDamage() {
        selfDamage = true;
        return this;
    }

    public SinglePointExplosionEntityProcessor hook(@Nullable ExplosionHitListener hook) {
        this.hook = hook;
        return this;
    }

    @Override
    public void sweep(ComposableExplosion blast) {
        var level = blast.level();
        float range = range(blast);
        var entities = candidates(blast, level, range);
        if (entities == null) return;
        var centre = blast.center();
        var source = blast.getDamageSource();
        var batch = new LineOfSight.Batch(level, new SectionAccess.LoadingReader(level));
        for (var entity : entities) {
            if (entity.ignoreExplosion(blast)) continue;
            double distance =
                    Math.sqrt(entity.distanceToSqr(blast.getX(), blast.getY(), blast.getZ()))
                            / range;
            if (!(distance <= 1.0)) continue;
            double impact = (1.0 - distance) * exposure(batch, centre, entity);
            entity.hurtServer(level, source, defaultDamage(impact, range));
            var impulse = push(blast, level, entity, impact, 1.0);
            settle(blast, entity, impulse, source.getEntity());
            if (hook != null) hook.afterHit(blast, entity, distance);
        }
    }
}
