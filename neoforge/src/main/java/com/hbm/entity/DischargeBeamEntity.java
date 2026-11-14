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

package com.hbm.entity;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.Optional;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DischargeBeamEntity extends Entity {

    private double headingX;
    private double headingY;
    private double headingZ;

    public DischargeBeamEntity(EntityType<? extends DischargeBeamEntity> type, Level level) {
        super(type, level);
    }

    public static void explodeDischarge(Level level, double x, double y, double z) {
        new ComposableExplosion(level, x, y, z, 5F)
                .entityProcessor(
                        new WeaponExplosionEntityProcessor(1D, 20F)
                                .damageType(ModDamageTypes.ELECTRIC))
                .effects(new ExplosionEffectStandard())
                .explode();
        WorldSounds.play(
                level,
                null,
                x,
                y,
                z,
                ModSounds.UFO_BLAST.get(),
                SoundSource.BLOCKS,
                5F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    public void setRotationsFromVector(double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float pitch = (float) (-Math.asin(dy / len) * 180D / Math.PI);
        float yaw = (float) (-Math.atan2(dx, dz) * 180D / Math.PI);
        setXRot(pitch);
        setYRot(yaw);
        float y = yaw / 180F * Mth.PI;
        float p = pitch / 180F * Mth.PI;
        headingX = -Mth.sin(y) * Mth.cos(p);
        headingZ = Mth.cos(y) * Mth.cos(p);
        headingY = -Mth.sin(p);
    }

    public void performHitscan(double range) {
        if (!(level() instanceof ServerLevel)) return;

        Vec3 start = position();
        Vec3 end = start.add(headingX * range, headingY * range, headingZ * range);

        BlockHitResult blockHit =
                level().clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3 sweepEnd = hitBlock ? blockHit.getLocation() : end;

        Entity hit = null;
        Vec3 hitVec = null;
        double nearestSqr = 0D;
        AABB sweep =
                getBoundingBox()
                        .expandTowards(headingX * range, headingY * range, headingZ * range)
                        .inflate(1D);
        for (Entity e : level().getEntities(this, sweep)) {
            if (!e.isAlive() || !e.isPickable()) continue;
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.3D).clip(start, sweepEnd);
            if (clip.isPresent()) {
                double distSqr = start.distanceToSqr(clip.get());
                if (hit == null || distSqr < nearestSqr) {
                    hit = e;
                    nearestSqr = distSqr;
                    hitVec = clip.get();
                }
            }
        }

        if (hit != null) {
            explodeDischarge(level(), hitVec.x, hitVec.y, hitVec.z);
        } else if (hitBlock) {
            level().destroyBlock(blockHit.getBlockPos(), false);
            Vec3 at = blockHit.getLocation();
            explodeDischarge(level(), at.x, at.y, at.z);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > 3) discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
