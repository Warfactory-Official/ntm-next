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

package com.hbm.explosion;

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.entity.projectile.B92BeamEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.util.LineOfSight;
import com.hbm.util.SectionAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class NuclearShockwave {

    private static final float PEAK = 250F;
    private static final float BURN_SECONDS = 5F;
    private static final double PUSH = 0.2D;
    private static final double PUSH_FLOOR = 1.0E-4D;

    private NuclearShockwave() {}

    public static void sweep(Level level, double x, double y, double z, double radius) {
        sweep(level, x, y, z, radius, PEAK);
    }

    public static void sweep(Level level, double x, double y, double z, double radius, float peak) {
        if (!(level instanceof ServerLevel server)) return;
        var candidates =
                server.getEntities(
                        (Entity) null,
                        new AABB(
                                x - radius,
                                y - radius,
                                z - radius,
                                x + radius,
                                y + radius,
                                z + radius));
        if (candidates.isEmpty()) return;
        double reach = radius * radius;
        DamageSource source = server.damageSources().source(ModDamageTypes.NUCLEAR_BLAST);
        DamageSource piercing = ModifiedDamageSource.of(source, true, 0, 100F, 0F);
        var batch = new LineOfSight.Batch(server, new SectionAccess.LoadingReader(server));
        batch.reset(x, y, z);
        int[] slots = new int[candidates.size()];
        for (int c = 0; c < slots.length; c++) {
            var entity = candidates.get(c);
            slots[c] =
                    squared(entity, x, y, z) > reach || exempt(entity)
                            ? -1
                            : batch.add(
                                    entity.getX(),
                                    entity.getEyeY(),
                                    entity.getZ(),
                                    SectionPos.asLong(entity.blockPosition()));
        }
        for (int c = 0; c < slots.length; c++) {
            var entity = candidates.get(c);
            double squared = squared(entity, x, y, z);
            if (squared > reach || exempt(entity)) continue;
            double ex = entity.getX(), ey = entity.getEyeY(), ez = entity.getZ();
            if (slots[c] < 0 ? batch.blocked(ex, ey, ez) : batch.blocked(slots[c], ex, ey, ez))
                continue;
            apply(server, entity, x, y, z, radius, peak, squared, source, piercing);
        }
    }

    private static double squared(Entity entity, double x, double y, double z) {
        double dx = entity.getX() - x, dy = entity.getY() - y, dz = entity.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void apply(
            ServerLevel server,
            Entity entity,
            double x,
            double y,
            double z,
            double radius,
            float peak,
            double squared,
            DamageSource source,
            DamageSource piercing) {
        float amount = (float) (peak * (radius - Math.sqrt(squared)) / radius);
        boolean landed = true;
        if (entity instanceof LivingEntity living && living.isAlive()) {
            landed = living.hurtServer(server, piercing, amount);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
        } else {
            entity.hurtServer(server, source, amount);
        }
        entity.igniteForSeconds(BURN_SECONDS);
        if (!landed) return;
        double wx = entity.getX() - x, wy = entity.getEyeY() - y, wz = entity.getZ() - z;
        double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (length >= PUSH_FLOOR)
            entity.push(PUSH * wx / length, PUSH * wy / length, PUSH * wz / length);
    }

    private static boolean exempt(Entity entity) {
        return entity instanceof Ocelot
                || entity instanceof B92BeamEntity
                || entity instanceof BulletBaseMK4Entity
                || entity instanceof GrenadeUniversalEntity
                || entity instanceof Player player && player.isCreative();
    }
}
