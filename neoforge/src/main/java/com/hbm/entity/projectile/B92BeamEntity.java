/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.projectile;

import com.hbm.entity.effect.CloudFleijaRainbowEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.entity.projectile.ProjectileEntity;
import com.hbm.sound.WorldSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class B92BeamEntity extends ProjectileEntity {
    public B92BeamEntity(EntityType<? extends B92BeamEntity> type, Level level) {
        super(type, level);
    }

    public B92BeamEntity(
            EntityType<? extends B92BeamEntity> type, LivingEntity shooter, float velocity) {
        this(type, shooter.level());
        initThrower(shooter, velocity * 1.5F, 1);
    }

    @Override
    public Vec3 getMovementToShoot(double x, double y, double z, float power, float uncertainty) {
        double spread = 0.002499999832361937D * uncertainty;
        return new Vec3(x, y, z)
                .normalize()
                .add(
                        random.nextGaussian() * spread,
                        random.nextGaussian() * spread,
                        random.nextGaussian() * spread)
                .scale(power);
    }

    @Override
    public double getGravityVelocity() {
        return 0;
    }

    @Override
    protected float getAirDrag() {
        return 1;
    }

    @Override
    public float getWaterDrag() {
        return 1;
    }

    @Override
    public void tick() {
        if (tickCount > 100) {
            discard();
            return;
        }
        super.tick();
        if (!isRemoved() && isInWater() && level() instanceof ServerLevel server) detonate(server);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (level() instanceof ServerLevel server) detonate(server);
    }

    private void detonate(ServerLevel server) {
        discard();
        explode(server);
    }

    protected void explode(ServerLevel server) {
        var explosion = NukeExplosionMK3Entity.statFacFleija(server, getX(), getY(), getZ(), 10);
        if (explosion.isRemoved()) return;
        WorldSounds.play(
                server,
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                100,
                random.nextFloat() * 0.1F + 0.9F);
        server.addFreshEntity(explosion);
        server.addFreshEntity(CloudFleijaRainbowEntity.statFac(server, 10, getX(), getY(), getZ()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 10 * 64;
        return distance < range * range;
    }
}
