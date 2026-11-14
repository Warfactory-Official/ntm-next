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

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.util.LineOfSight;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public abstract sealed class ExplosionEntityProcessor
        permits SinglePointExplosionEntityProcessor, MultiPointExplosionEntityProcessor {

    float rangeMultiplier = 1F;
    boolean selfDamage;

    public abstract void sweep(ComposableExplosion blast);

    final @Nullable List<Entity> candidates(
            ComposableExplosion blast, ServerLevel level, float range) {
        if (blast.radius() < 1.0E-5F) return null;
        double x = blast.getX(), y = blast.getY(), z = blast.getZ();
        var entities =
                level.getEntities(
                        selfDamage ? null : blast.getDirectSourceEntity(),
                        new AABB(
                                x - range - 1,
                                y - range - 1,
                                z - range - 1,
                                x + range + 1,
                                y + range + 1,
                                z + range + 1));
        EventHooks.onExplosionDetonate(
                level, (ServerExplosion) blast, entities, blast.getBlockSnapshot());
        return entities;
    }

    final float range(ComposableExplosion blast) {
        return blast.radius() * 2F * rangeMultiplier;
    }

    static Vec3 push(
            ComposableExplosion blast,
            ServerLevel level,
            Entity entity,
            double impact,
            double multiplier) {
        double dx = entity.getX() - blast.getX();
        double dy = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - blast.getY();
        double dz = entity.getZ() - blast.getZ();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5F) {
            dx = dy = dz = 0;
        } else {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        double power = impact * multiplier * knockbackDamping(entity);
        var impulse =
                EventHooks.getExplosionKnockback(
                        level,
                        (ServerExplosion) blast,
                        entity,
                        new Vec3(dx * power, dy * power, dz * power),
                        blast.getBlockSnapshot());
        entity.push(impulse);
        return impulse;
    }

    static void settle(
            ComposableExplosion blast,
            Entity entity,
            @Nullable Vec3 impulse,
            @Nullable Entity attribution) {
        if (entity instanceof Projectile projectile
                && entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE)) {
            projectile.setOwner(attribution);
        } else if (impulse != null
                && entity instanceof Player player
                && !player.isSpectator()
                && (!player.isCreative() || !player.getAbilities().flying)) {
            blast.getHitPlayers().put(player, impulse);
        }
        entity.onExplosionHit(blast.getDirectSourceEntity());
    }

    static float defaultDamage(double impact, float range) {
        return (float) ((impact * impact + impact) / 2.0 * 7.0 * range + 1.0);
    }

    public static double knockbackDamping(Entity entity) {
        return entity instanceof LivingEntity living
                ? 1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                : 1.0;
    }

    public static boolean takesKnockback(Entity entity) {
        return !(entity instanceof BulletBaseMK4Entity)
                && !(entity instanceof GrenadeUniversalEntity);
    }

    public static float exposure(BlockGetter view, Vec3 node, Entity entity) {
        var context = CollisionContext.of(entity);
        int[] visible = {0};
        int count =
                samples(
                        entity.getBoundingBox(),
                        (x, y, z) -> {
                            var clip =
                                    new ClipContext(
                                            new Vec3(x, y, z),
                                            node,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            context);
                            if (view.clip(clip).getType() == HitResult.Type.MISS) visible[0]++;
                        });
        return count == 0 ? 0F : (float) visible[0] / count;
    }

    public static float exposure(LineOfSight.Batch batch, Vec3 node, Entity entity) {
        batch.reset(
                node.x,
                node.y,
                node.z,
                ClipContext.Block.COLLIDER,
                CollisionContext.of(entity),
                true);
        int count = samples(entity.getBoundingBox(), (x, y, z) -> batch.add(x, y, z, 0));
        int visible = 0;
        for (int i = 0; i < count; i++) {
            if (!batch.blocked(i)) visible++;
        }
        return count == 0 ? 0F : (float) visible / count;
    }

    private interface Sample {
        void at(double x, double y, double z);
    }

    private static int samples(AABB box, Sample sink) {
        double xs = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        if (xs < 0.0 || ys < 0.0 || zs < 0.0) return 0;
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        int count = 0;
        for (double a = 0.0; a <= 1.0; a += xs) {
            for (double b = 0.0; b <= 1.0; b += ys) {
                for (double c = 0.0; c <= 1.0; c += zs) {
                    sink.at(
                            Mth.lerp(a, box.minX, box.maxX) + xOffset,
                            Mth.lerp(b, box.minY, box.maxY),
                            Mth.lerp(c, box.minZ, box.maxZ) + zOffset);
                    count++;
                }
            }
        }
        return count;
    }
}
