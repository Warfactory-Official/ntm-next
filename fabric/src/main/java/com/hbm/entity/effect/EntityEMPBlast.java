// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityEMPBlast extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityEMPBlast.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityEMPBlast(EntityType<? extends EntityEMPBlast> type, Level level) {
        super(type, level);

        this.age = 0;
        this.scale = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        this.age++;

        if (!level().isClientSide() && this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        age = input.getShortOr("age", (short) 0);
        scale = input.getShortOr("scale", (short) 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("age", (short) age);
        output.putShort("scale", (short) scale);
    }
}
