// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityCloudTom extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudTom.class, EntityDataSerializers.INT);

    private static final EntityDimensions MISSILE_SIZE = EntityDimensions.scalable(20.0F, 40.0F);

    public int maxAge = 100;
    public int age;
    private boolean missileSpawned;

    public EntityCloudTom(EntityType<? extends EntityCloudTom> type, Level level) {
        super(type, level);
        this.age = 0;
    }

    public static EntityCloudTom statFac(Level level, int maxAge, double x, double y, double z) {
        EntityCloudTom cloud = new EntityCloudTom(ModEntities.MOONSTONE_BLAST.get(), level);
        cloud.setPos(x, y, z);
        cloud.setMaxAge(maxAge);
        cloud.missileSpawned = true;
        cloud.refreshDimensions();
        return cloud;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.missileSpawned ? MISSILE_SIZE : super.getDimensions(pose);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }

    @Override
    public void tick() {
        this.age++;

        level().setSkyFlashTime(2);

        if (!level().isClientSide() && this.age >= this.getMaxAge()) {
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getShortOr("age", (short) 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("age", (short) age);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.maxAge = i;
        this.entityData.set(MAX_AGE, i);
    }
}
