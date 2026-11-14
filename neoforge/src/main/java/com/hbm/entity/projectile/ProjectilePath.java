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

package com.hbm.entity.projectile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public final class ProjectilePath {
    public static List<EntityHitResult> entities(
            Projectile projectile,
            Vec3 from,
            Vec3 to,
            Predicate<Entity> matching,
            boolean spectral) {
        return entities(
                projectile,
                from,
                to,
                matching,
                spectral ? 0 : ProjectileUtil.computeMargin(projectile));
    }

    public static List<EntityHitResult> entities(
            Projectile projectile, Vec3 from, Vec3 to, Predicate<Entity> matching, float margin) {
        var area = projectile.getBoundingBox().expandTowards(to.subtract(from)).inflate(1);
        var hits =
                new ArrayList<>(
                        ProjectileUtil.getManyEntityHitResult(
                                projectile.level(),
                                projectile,
                                from,
                                to,
                                area,
                                matching,
                                margin,
                                ClipContext.Block.COLLIDER,
                                false));
        hits.sort(Comparator.comparingDouble(hit -> from.distanceToSqr(hit.getLocation())));
        return hits;
    }

    public static List<EntityHitResult> entitiesUnobstructed(
            Projectile projectile, Vec3 from, Vec3 to, Predicate<Entity> matching, float margin) {
        var area = projectile.getBoundingBox().expandTowards(to.subtract(from)).inflate(1);
        var hits = new ArrayList<EntityHitResult>();
        for (var entity : projectile.level().getEntities(projectile, area, matching)) {
            var contact = entity.getBoundingBox().inflate(margin).clip(from, to);
            if (contact.isPresent()) hits.add(new EntityHitResult(entity, contact.get()));
        }
        hits.sort(Comparator.comparingDouble(hit -> from.distanceToSqr(hit.getLocation())));
        return hits;
    }

    public static @Nullable ProjectileDeflection impact(Projectile projectile, HitResult hit) {
        if (EventHooks.onProjectileImpact(projectile, hit)) return null;
        if (!projectile.isAlive()) return null;
        return projectile.hitTargetOrDeflectSelf(hit);
    }

    private ProjectilePath() {}
}
