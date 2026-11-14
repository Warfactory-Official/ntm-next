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

import com.hbm.entity.projectile.ProjectileEntity;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SiegeLaserEntity extends ProjectileEntity {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SiegeLaserEntity.class, EntityDataSerializers.INT);

    private float damage = 2;
    private float explosive = 0F;
    private float breakChance = 0F;
    private boolean incendiary = false;

    public SiegeLaserEntity(EntityType<? extends SiegeLaserEntity> type, Level level) {
        super(type, level);
    }

    public SiegeLaserEntity(
            EntityType<? extends SiegeLaserEntity> type, Level level, LivingEntity thrower) {
        super(type, level);
        initThrower(thrower);
    }

    public SiegeLaserEntity(
            EntityType<? extends SiegeLaserEntity> type,
            Level level,
            double x,
            double y,
            double z) {
        super(type, level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0xffffff);
    }

    public SiegeLaserEntity setDamage(float f) {
        this.damage = f;
        return this;
    }

    public SiegeLaserEntity setExplosive(float f) {
        this.explosive = f;
        return this;
    }

    public SiegeLaserEntity setBreakChance(float f) {
        this.breakChance = f;
        return this;
    }

    public SiegeLaserEntity setIncendiary() {
        this.incendiary = true;
        return this;
    }

    public SiegeLaserEntity setColor(int color) {
        this.entityData.set(COLOR, color);
        return this;
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        if (this.tickCount > 60) this.discard();
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!(level() instanceof ServerLevel server)) return;

        Vec3 hitVec = mop.getLocation();

        if (mop instanceof EntityHitResult entityHit) {
            DamageSource dmg = laserDamageSource();
            Entity hitEntity = entityHit.getEntity();

            if (hitEntity.hurtServer(server, dmg, this.damage)) {
                this.discard();

                if (this.incendiary) hitEntity.igniteForSeconds(3);

                if (this.explosive > 0)
                    server.explode(
                            this,
                            hitVec.x,
                            hitVec.y,
                            hitVec.z,
                            this.explosive,
                            this.incendiary,
                            Level.ExplosionInteraction.NONE);
            }

        } else if (mop instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();

            if (this.explosive > 0) {
                server.explode(
                        this,
                        hitVec.x,
                        hitVec.y,
                        hitVec.z,
                        this.explosive,
                        this.incendiary,
                        Level.ExplosionInteraction.NONE);

            } else if (this.incendiary) {
                BlockPos target = pos.relative(blockHit.getDirection());

                if (server.getBlockState(target).canBeReplaced()) {
                    server.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                }
            }

            if (this.random.nextFloat() < this.breakChance) {
                server.destroyBlock(pos, false);
            }

            this.discard();
        }
    }

    private DamageSource laserDamageSource() {
        Holder<DamageType> holder =
                level().registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.LASER);
        LivingEntity thrower = getThrower();
        return thrower != null ? new DamageSource(holder, this, thrower) : new DamageSource(holder);
    }

    @Override
    public double getGravityVelocity() {
        return 0D;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("damage", this.damage);
        output.putFloat("explosive", this.explosive);
        output.putFloat("breakChance", this.breakChance);
        output.putBoolean("incendiary", this.incendiary);
        output.putInt("color", this.getColor());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.damage = input.getFloatOr("damage", 2F);
        this.explosive = input.getFloatOr("explosive", 0F);
        this.breakChance = input.getFloatOr("breakChance", 0F);
        this.incendiary = input.getBooleanOr("incendiary", false);
        this.setColor(input.getIntOr("color", 0xffffff));
    }
}
