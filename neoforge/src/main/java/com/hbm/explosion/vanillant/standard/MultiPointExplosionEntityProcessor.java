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
import com.hbm.util.LineOfSight;
import com.hbm.util.SectionAccess;
import it.unimi.dsi.fastutil.objects.Reference2FloatLinkedOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public non-sealed class MultiPointExplosionEntityProcessor extends ExplosionEntityProcessor {

    protected final double nodeDistance;
    double knockbackMultiplier = 1.0;

    public MultiPointExplosionEntityProcessor(double nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    public MultiPointExplosionEntityProcessor range(float multiplier) {
        rangeMultiplier = multiplier;
        return this;
    }

    public MultiPointExplosionEntityProcessor knockback(double multiplier) {
        knockbackMultiplier = multiplier;
        return this;
    }

    protected float damage(double distance, float exposure, double impact, float range) {
        return defaultDamage(impact, range);
    }

    public void attack(Entity entity, ComposableExplosion blast, float amount) {
        if (entity.level() instanceof ServerLevel level)
            entity.hurtServer(level, blast.getDamageSource(), amount);
    }

    protected float nodeExposure(LineOfSight.Batch batch, Vec3 node, Entity entity) {
        return exposure(batch, node, entity);
    }

    protected @Nullable Entity attribution(ComposableExplosion blast) {
        return blast.getDamageSource().getEntity();
    }

    @Override
    public void sweep(ComposableExplosion blast) {
        var level = blast.level();
        float range = range(blast);
        var entities = candidates(blast, level, range);
        if (entities == null || entities.isEmpty()) return;
        double x = blast.getX(), y = blast.getY(), z = blast.getZ();
        var centre = blast.center();
        var nodes =
                nodeDistance > 0
                        ? new Vec3[] {
                            centre,
                            new Vec3(x + nodeDistance, y, z),
                            new Vec3(x - nodeDistance, y, z),
                            new Vec3(x, y + nodeDistance, z),
                            new Vec3(x, y - nodeDistance, z),
                            new Vec3(x, y, z + nodeDistance),
                            new Vec3(x, y, z - nodeDistance)
                        }
                        : new Vec3[] {centre};
        var attribution = attribution(blast);
        var amounts = new Reference2FloatLinkedOpenHashMap<Entity>(entities.size());
        amounts.defaultReturnValue(Float.NEGATIVE_INFINITY);
        var batch = new LineOfSight.Batch(level, new SectionAccess.LoadingReader(level));
        for (var entity : entities) {
            if (entity.ignoreExplosion(blast)) continue;
            double distance = boxDistance(entity.getBoundingBox(), x, y, z) / range;
            if (!(distance <= 1.0)) continue;
            float exposure = 0F;
            for (var node : nodes) {
                exposure = Math.max(exposure, nodeExposure(batch, node, entity));
                if (exposure >= 1F) break;
            }
            double impact = (1.0 - distance) * exposure;
            float amount = damage(distance, exposure, impact, range);
            if (amount > amounts.getFloat(entity)) amounts.put(entity, amount);
            var impulse =
                    takesKnockback(entity)
                            ? push(blast, level, entity, impact, knockbackMultiplier)
                            : null;
            settle(blast, entity, impulse, attribution);
        }
        for (var it = amounts.reference2FloatEntrySet().fastIterator(); it.hasNext(); ) {
            var hit = it.next();
            attack(hit.getKey(), blast, hit.getFloatValue());
        }
    }

    static double boxDistance(AABB box, double x, double y, double z) {
        double dx = axisGap(box.minX, box.maxX, x), dy = axisGap(box.minY, box.maxY, y);
        double dz = axisGap(box.minZ, box.maxZ, z);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisGap(double min, double max, double at) {
        return min <= at && at <= max ? 0.0 : Math.min(Math.abs(min - at), Math.abs(max - at));
    }
}
