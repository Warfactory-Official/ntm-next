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

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CloudSoliniumEntity extends Entity {
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(CloudSoliniumEntity.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public CloudSoliniumEntity(EntityType<? extends CloudSoliniumEntity> type, Level level) {
        super(type, level);
        this.age = 0;
        this.scale = 0;
    }

    public static CloudSoliniumEntity statFac(
            Level level, int maxAge, double x, double y, double z) {
        CloudSoliniumEntity cloud =
                new CloudSoliniumEntity(ModEntities.CLOUD_SOLINIUM.get(), level);
        cloud.setPos(x, y, z);
        cloud.setMaxAge(maxAge);
        return cloud;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }

    @Override
    public void tick() {
        this.age++;

        if (level() instanceof ServerLevel server) {
            LightningBolt bolt =
                    EntityTypes.LIGHTNING_BOLT.create(server, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(getX(), getY() + 200, getZ());
                server.addFreshEntity(bolt);
            }
        }

        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
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

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }
}
