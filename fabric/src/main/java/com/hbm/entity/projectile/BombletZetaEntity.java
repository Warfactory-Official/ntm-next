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

import com.hbm.data.ExplosionData;
import com.hbm.entity.effect.MistEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class BombletZetaEntity extends Entity {

    private int type;

    public float renderPitch;
    public float renderPitchO;

    public BombletZetaEntity(EntityType<? extends BombletZetaEntity> type, Level level) {
        super(type, level);
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        type = input.getIntOr("type", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", type);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        renderPitchO = renderPitch;
        yRotO = getYRot();
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        rotation();
        BlockPos block = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (level().getBlockState(block).isAir() || level().isClientSide()) return;
        if (type == 0) {
            ComposableExplosion vnt =
                    new ComposableExplosion(
                            level(), getX() + 0.5F, getY() + 1.5F, getZ() + 0.5F, 4F);
            vnt.blockSelector(new AttenuatedRayBlockSelector());
            vnt.blockProcessor(CellDestruction.decay());
            vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, 100));
            vnt.effects(new SmallExplosionEffect(15, 3.5F, 1.25F));
            vnt.explode();
        }
        if (type == 1) {
            ComposableExplosion vnt =
                    new ComposableExplosion(
                            level(), getX() + 0.5F, getY() + 1.5F, getZ() + 0.5F, 4F);
            vnt.blockSelector(new AttenuatedRayBlockSelector());
            vnt.blockProcessor(CellDestruction.decay().transform(new BlockMutatorFire()));
            vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, 100));
            vnt.effects(new SmallExplosionEffect(15, 5F, 1.75F));
            vnt.explode();
        }
        if (type == 2) {
            WorldSounds.play(
                    level(),
                    null,
                    getX() + 0.5D,
                    getY() + 0.5D,
                    getZ() + 0.5D,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS,
                    5F,
                    2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
            MistEntity mist = new MistEntity(level()).setType(NTMFluids.CHLORINE).setArea(15, 7.5F);
            mist.setPos(
                    getX() - getDeltaMovement().x,
                    getY() - getDeltaMovement().y,
                    getZ() - getDeltaMovement().z);
            level().addFreshEntity(mist);
        }
        if (type == 4) {
            level().addFreshEntity(
                            NukeExplosionMK5Entity.statFac(
                                    level(),
                                    (int) (ExplosionData.FATMAN_RADIUS.get() * 1.5F),
                                    getX(),
                                    getY(),
                                    getZ()));
            MiniNukeAftermath.cloud(level(), position(), 0.5D, random.nextInt(100) == 0);
            MiniNukeAftermath.sound(level(), position(), 15F, 1F);
        }
        discard();
    }

    public void rotation() {
        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI);
        renderPitch = (float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI) - 90F;
        while (renderPitch - renderPitchO < -180F) renderPitchO -= 360F;
        while (renderPitch - renderPitchO >= 180F) renderPitchO += 360F;
        while (yaw - yRotO < -180F) yRotO -= 360F;
        while (yaw - yRotO >= 180F) yRotO += 360F;
        setYRot(yaw);
    }
}
