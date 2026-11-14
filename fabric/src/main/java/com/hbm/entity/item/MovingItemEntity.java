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

package com.hbm.entity.item;

import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorItem;
import com.hbm.entity.ModEntities;
import com.hbm.interfaces.StoredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MovingItemEntity extends MovingConveyorObjectEntity
        implements ConveyorItem, StoredItems {

    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(MovingItemEntity.class, EntityDataSerializers.ITEM_STACK);

    public MovingItemEntity(EntityType<? extends MovingItemEntity> type, Level level) {
        super(type, level);
    }

    public MovingItemEntity(Level level) {
        super(ModEntities.MOVING_ITEM.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY);
    }

    public void setItemStack(ItemStack stack) {
        entityData.set(STACK, stack);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        ItemStack stack = getItemStack();
        if (visitor.visit(stack))
            visitor.afterChanges(
                    () -> {
                        if (!isRemoved() && getItemStack() == stack) setItemStack(stack.copy());
                    });
    }

    @Override
    public ItemStack getItemStack() {
        return entityData.get(STACK);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide() || isRemoved()) return InteractionResult.PASS;

        player.getInventory().placeItemBackInInventory(getItemStack().copy());
        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            discard();
            server.addFreshEntity(new ItemEntity(server, getX(), getY(), getZ(), getItemStack()));
        }
        return false;
    }

    @Override
    public void enterBlock(ConveyorInputHandler enterable, BlockPos pos, @Nullable Direction dir) {
        if (isRemoved()) return;

        if (enterable.canItemEnter(level(), pos, dir, this)) {
            enterable.onItemEnter(level(), pos, dir, this);
            discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        if (isRemoved()) return true;

        discard();

        Vec3 motion = getDeltaMovement();
        ItemEntity item =
                new ItemEntity(
                        level(),
                        getX() + motion.x * 2,
                        getY() + motion.y * 2,
                        getZ() + motion.z * 2,
                        getItemStack());
        item.age = ItemEntity.LIFETIME - (60 * 20);
        item.setDeltaMovement(motion.x * 2, 0.1, motion.z * 2);
        level().addFreshEntity(item);

        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setItemStack(input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if (getItemStack().isEmpty()) discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (!getItemStack().isEmpty()) output.store("Item", ItemStack.CODEC, getItemStack());
    }
}
