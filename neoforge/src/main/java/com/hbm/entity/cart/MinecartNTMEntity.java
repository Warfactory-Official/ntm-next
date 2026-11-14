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

package com.hbm.entity.cart;

import com.hbm.items.tool.ModMinecartItem.EnumCartBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class MinecartNTMEntity extends AbstractMinecart implements CartChassis {
    private static final EntityDataAccessor<Integer> BASE =
            SynchedEntityData.defineId(MinecartNTMEntity.class, EntityDataSerializers.INT);

    protected MinecartNTMEntity(EntityType<? extends MinecartNTMEntity> type, Level level) {
        super(type, level);
    }

    protected MinecartNTMEntity(
            EntityType<? extends MinecartNTMEntity> type,
            Level level,
            double x,
            double y,
            double z,
            EnumCartBase base) {
        this(type, level);
        setInitialPos(x, y, z);
        setBase(base);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BASE, 0);
    }

    public void setBase(EnumCartBase type) {
        entityData.set(BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[entityData.get(BASE)];
    }

    @Override
    protected Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return getCartItem();
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        kill(level);

        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            ItemStack stack = getCartItem();
            if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
            spawnAtLocation(level, stack);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("base", entityData.get(BASE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(BASE, input.getIntOr("base", 0));
    }
}
