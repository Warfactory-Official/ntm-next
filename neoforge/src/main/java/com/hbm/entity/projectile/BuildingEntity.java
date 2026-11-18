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

import com.hbm.explosion.BlastSmoke;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BuildingEntity extends Entity {
    public BuildingEntity(EntityType<? extends BuildingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        if (this.tickCount == 1) {
            for (int i = 0; i < 100; i++) {
                FlameCreator.composeEffect(
                        level(),
                        getX() + (random.nextDouble() - 0.5) * 15,
                        getY() + (random.nextDouble() - 0.5) * 15,
                        getZ() + (random.nextDouble() - 0.5) * 15,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.03;
        if (mY < -1.5) mY = -1.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            WorldSounds.play(
                    level(),
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.OLD_EXPLOSION.get(),
                    SoundSource.AMBIENT,
                    10000.0F,
                    0.5F + this.random.nextFloat() * 0.1F);
            discard();
            BlastSmoke.cloud(level(), getX(), getY() + 3, getZ(), 150);
            BlastSmoke.shock(level(), getX(), getY() + 1, getZ(), 24, 6);
            BlastSmoke.shock(level(), getX(), getY() + 1, getZ(), 24, 5);
            BlastSmoke.shock(level(), getX(), getY() + 1, getZ(), 24, 4);
            BlastSmoke.shock(level(), getX(), getY() + 1, getZ(), 24, 3);
            BlastSmoke.shock(level(), getX(), getY() + 1, getZ(), 24, 3);

            List<Entity> list =
                    level().getEntities(
                                    null,
                                    new AABB(
                                            getX() - 8,
                                            getY() - 8,
                                            getZ() - 8,
                                            getX() + 8,
                                            getY() + 8,
                                            getZ() + 8));
            if (level() instanceof ServerLevel server) {
                for (Entity e : list)
                    e.hurtServer(
                            server, level().damageSources().source(ModDamageTypes.BUILDING), 1000);
            }

            for (int i = 0; i < 250; i++) {
                double pitch = this.random.nextFloat() * Math.PI / 2;
                double yaw = this.random.nextFloat() * Math.PI * 2;

                RubbleEntity rubble = new RubbleEntity(level(), getX(), getY() + 3, getZ());
                rubble.setBlockState(Blocks.BRICKS.defaultBlockState());
                rubble.setDeltaMovement(
                        Math.cos(pitch) * Math.cos(yaw),
                        Math.sin(pitch),
                        -Math.cos(pitch) * Math.sin(yaw));
                level().addFreshEntity(rubble);
            }
        }
    }
}
