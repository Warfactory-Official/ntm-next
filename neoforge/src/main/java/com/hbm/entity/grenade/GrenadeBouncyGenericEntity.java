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

package com.hbm.entity.grenade;

import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.GenericGrenadeItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GrenadeBouncyGenericEntity extends GrenadeBouncyBaseEntity {
    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(
                    GrenadeBouncyGenericEntity.class, EntityDataSerializers.ITEM_STACK);

    public GrenadeBouncyGenericEntity(
            EntityType<? extends GrenadeBouncyGenericEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeBouncyGenericEntity(Level level) {
        this(ModEntities.GRENADE_BOUNCY_GENERIC.get(), level);
    }

    public GrenadeBouncyGenericEntity(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    public GrenadeBouncyGenericEntity(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    public GrenadeBouncyGenericEntity setType(GenericGrenadeItem grenade) {
        entityData.set(GRENADE, new ItemStack(grenade));
        return this;
    }

    public GenericGrenadeItem getGrenade() {
        return entityData.get(GRENADE).getItem() instanceof GenericGrenadeItem gren
                ? gren
                : ModItems.STICK_DYNAMITE.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GRENADE, ItemStack.EMPTY);
    }

    @Override
    public void explode() {
        discard();
        getGrenade().explode(this, getThrower(), level(), getX(), getY(), getZ());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("grenade", ItemStack.OPTIONAL_CODEC, entityData.get(GRENADE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("grenade", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(GRENADE, stack));
    }

    @Override
    protected int getMaxTimer() {
        return getGrenade().getMaxTimer();
    }

    @Override
    protected double getBounceMod() {
        return getGrenade().getBounceMod();
    }
}
