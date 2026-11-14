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
import com.hbm.entity.effect.MistEntity;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.DisperserItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DisperserCanisterEntity extends GrenadeBaseEntity {
    private static final EntityDataAccessor<Integer> FLUID =
            SynchedEntityData.defineId(DisperserCanisterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> TYPE =
            SynchedEntityData.defineId(
                    DisperserCanisterEntity.class, EntityDataSerializers.ITEM_STACK);

    public DisperserCanisterEntity(
            EntityType<? extends DisperserCanisterEntity> type, Level level) {
        super(type, level);
    }

    public DisperserCanisterEntity(Level level) {
        this(ModEntities.DISPERSER_CANISTER.get(), level);
    }

    public DisperserCanisterEntity(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID, 0);
        builder.define(TYPE, ItemStack.EMPTY);
    }

    public Fluid getFluid() {
        Fluid fluid = BuiltInRegistries.FLUID.byId(entityData.get(FLUID));
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public DisperserCanisterEntity setFluid(Fluid fluid) {
        entityData.set(FLUID, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public DisperserItem getDisperserItem() {
        return entityData.get(TYPE).getItem() instanceof DisperserItem disperser
                ? disperser
                : ModItems.DISPERSER_CANISTER.get();
    }

    public DisperserCanisterEntity setDisperserItem(DisperserItem item) {
        entityData.set(TYPE, new ItemStack(item));
        return this;
    }

    @Override
    public void explode() {
        if (!level().isClientSide()) {
            MistEntity mist = new MistEntity(level());
            mist.setType(getFluid());
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 5);
            mist.setDuration(80);
            level().addFreshEntity(mist);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("fluid", entityData.get(FLUID));
        output.store("item", ItemStack.OPTIONAL_CODEC, entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(FLUID, input.getIntOr("fluid", 0));
        input.read("item", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(TYPE, stack));
    }
}
